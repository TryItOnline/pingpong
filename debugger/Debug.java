package debugger;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import Debugger;
import PingPong;
import PingPongThread;
import SequentialAccess;
import LongStack;


public class Debug implements Debugger, WindowListener {
	
	private PingPong process;
	
	private JFrame frame;
	private DataViewer data;
	private StackViewer stack;
	private StackViewer helpStack;
	private ControlPanel controlPanel;

	private ArrayList threads = new ArrayList();
	private ThreadInfo primary;
	private ThreadInfo pattern;
	
	private long performance;

	private boolean running;
	private boolean paused;
	private boolean animated;
	private long delay;
	
	private Font font = new Font("Monospaced", Font.PLAIN, 14);


	public Debug() throws IOException {
		frame = new JFrame("PingPong Debug");
		process = new PingPong(this, true);
		initThreadInfo();
		initGUI();
	}
	
	public Debug(String filename) throws IOException {
		frame = new JFrame("PingPong Debug - " + filename);
		process = new PingPong(filename, this, true);
		initThreadInfo();
		initGUI();
	}
	
	private void initThreadInfo() {
		pattern = new ThreadInfo(ThreadInfo.MODE_WATCHED, 100);
	}
	
	
	
	
	public synchronized void processStarted(PingPong process) {
		running = true;
	}
	
	public synchronized void threadStarted(PingPongThread thread) {
		ThreadInfo info = new ThreadInfo(thread, pattern);
		threads.add(info);
		controlPanel.addThread(info);
		if (thread.getId() == 0) {
			newPrimary(info, true);
		}
		data.addHilite(info.getHilite());
	}

	public final void step(PingPongThread thread) {
		ThreadInfo info = (ThreadInfo)thread.getDebugInfo();
		int mode = info.getMode();
/*
		SequentialAccess ip = thread.getIp();
		int x = ip.getX();
		int y = ip.getY();
		if (x == 47 && y == 0 && performance != 0) {
			System.out.println("Performance: " + (System.currentTimeMillis() - performance));
		} else
		if (x == 2 && y == 0) {
			performance = System.currentTimeMillis();
		}
*/

		if (animated || paused) {
			refreshDisplay(info);

			if (paused) pauseThread(0); else
			if (delay > 0) pauseThread(delay);
		}

	}

	public synchronized void threadFinished(PingPongThread thread, int exitCode) {
		ThreadInfo info = (ThreadInfo)thread.getDebugInfo();
		
		controlPanel.removeThread(info);
		threads.remove(info);
		data.removeHilite(info.getHilite());
	}

	public synchronized void notifyProcessFinish(PingPong process) {
		paused = false;
		animated = false;
		resumeThreads();
	}
	
	public synchronized void processFinished(PingPong process, int exitCode) {
		running = false;
		paused = false;
        controlPanel.setEnabled(ControlPanel.MASK_RUN | ControlPanel.MASK_STEP);
	}


	public void start() {
		if (!running) process.start();
	}
	
	public void terminate() {
		if (running) process.terminate();
	}
	
	
	
	
	private void refreshDisplay(ThreadInfo info) {
		Hilite hilite = info.getHilite();
		if (info.isPrimary()) {
			data.focusTo(hilite);
			stack.refresh();
			helpStack.refresh();
		}
		data.repaintHilite(hilite);
	}
	
	private void newPrimary(ThreadInfo info, boolean updateDisplay) {
		if (primary != null) {
			primary.setPrimary(false);
			data.repaintHilite(primary.getHilite());
		}
		
		primary = info;
		
		if (info != null) {
			info.setPrimary(true);
			stack.setStack(info.getThread().getStack());
			helpStack.setStack(info.getThread().getHelpStack());
			refreshDisplay(info);
		} else {
			stack.setStack(null);
			helpStack.setStack(null);
		}
		
		if (updateDisplay) {
			controlPanel.setActiveThread(info);
		}
	}
	
	private void initGUI() {
		frame.setSize(600,400);
		frame.addWindowListener(this);
		
		Container cnt = frame.getContentPane();
		cnt.setLayout(new BorderLayout());

		data = new DataViewer("PingPong-Space", font, process.getSpace());
		cnt.add(data.getComponent(), BorderLayout.CENTER);
		
		controlPanel = new ControlPanel(this, ControlPanel.MASK_ANIMATE | ControlPanel.MASK_RATE
										| ControlPanel.MASK_THREADS);
		controlPanel.setEnabled(ControlPanel.MASK_RUN | ControlPanel.MASK_STEP);
		delay = controlPanel.getDelay();
		cnt.add(controlPanel, BorderLayout.NORTH);

		JPanel stacks = new JPanel(new GridLayout(1, 2));
		stack = new StackViewer("Stack", font);
		stacks.add(stack.getComponent());
		
		helpStack = new StackViewer("HelpStack", font);
		stacks.add(helpStack.getComponent());
		
		cnt.add(stacks, BorderLayout.EAST);
		
		frame.setVisible(true);
	}
	
	
	private synchronized void resumeThreads() {
		notifyAll();
	}
	
	private synchronized void pauseThread(long delay) {
		try {
			wait(delay);
		} catch (InterruptedException e) {};
	}
	
	private boolean expectRunning() {
		if (running) return true;
		start();
		return false;
	}
	
	private synchronized void displayAll() {
		int size = threads.size();
		for (int i = 0; i < size; i++) {
			ThreadInfo info = (ThreadInfo)threads.get(i);
			refreshDisplay(info);
		}
	}
	
	protected void panelEvent(Component panel, int type) {
	    if (panel == controlPanel) {
	        switch (type) {
	        case ControlPanel.CMD_RUN:
	        	paused = false;
	        	animated = controlPanel.isAnimated();
	        	if (expectRunning()) resumeThreads();
	            controlPanel.setEnabled(ControlPanel.MASK_PAUSE | ControlPanel.MASK_STOP);
	            break;
	        case ControlPanel.CMD_STEP:
	        	paused = true;
	        	if (expectRunning()) resumeThreads();
	            controlPanel.setEnabled(ControlPanel.MASK_RUN | ControlPanel.MASK_STEP | ControlPanel.MASK_STOP);
	            break;
	        case ControlPanel.CMD_PAUSE:
	        	paused = true;
	        	if (!animated) displayAll();
	            controlPanel.setEnabled(ControlPanel.MASK_RUN | ControlPanel.MASK_STEP | ControlPanel.MASK_STOP);
	            break;
	        case ControlPanel.CMD_STOP:
	        	terminate();
	            controlPanel.setEnabled(ControlPanel.MASK_RUN | ControlPanel.MASK_STEP);
	            break;
	        case ControlPanel.CMD_ANIMATE:
	        	if (running) {
	        		boolean newAnimated = controlPanel.isAnimated();
	        		if (!animated && newAnimated) displayAll();
	        		animated = newAnimated;
	        	}
	        	break;
	        case ControlPanel.CMD_RATE:
	        	delay = controlPanel.getDelay();
	        	break;
	        case ControlPanel.CMD_THREADS:
	        	ThreadInfo info = (ThreadInfo)controlPanel.getActiveThread();
	        	newPrimary(info, false);
	        	break;
	        }
	    }
	}
	
    public void windowActivated(WindowEvent e) {};
    public void windowDeactivated(WindowEvent e) {};
    public void windowDeiconified(WindowEvent e) {};
    public void windowIconified(WindowEvent e) {};
	public void windowOpened(WindowEvent e) {};

    public void windowClosed(WindowEvent e) {
    	terminate();
    	System.exit(0);
    }

    public void windowClosing(WindowEvent e) {
    	frame.dispose();
    }

	
	public static void main(String[] args) throws Exception {
		Debug debugger;
		if (args.length > 0) {
			debugger = new Debug(args[0]);
		} else {
			debugger = new Debug();
		}
		//debugger.start();
	}
}
