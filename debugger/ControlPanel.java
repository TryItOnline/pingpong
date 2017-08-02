package debugger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import SequentialAccess;
import HugeArray;
import PingPong;

class ControlPanel extends Box implements ActionListener, ChangeListener {
	
	public static final int CMD_RUN = 0;
	public static final int CMD_STEP = 1;
	public static final int CMD_PAUSE = 2;
	public static final int CMD_STOP = 3;
	public static final int CMD_ANIMATE = 4;
	public static final int CMD_RATE = 5;
	public static final int CMD_THREADS = 6;

	public static final int MASK_RUN = 1;
	public static final int MASK_STEP = 2;
	public static final int MASK_PAUSE = 4;
	public static final int MASK_STOP = 8;
	public static final int MASK_ANIMATE = 16;
	public static final int MASK_RATE = 32;
	public static final int MASK_THREADS = 64;
	
	private static final int MAX_BUTTONS = 4;
	private static final int MAX_CONTROLS = 7;
	private static final String[] labels = {"Run", "Step", "Pause", "Stop", "Animate:", "Rate", "no thread"};
	

	private int displayAlways;
	
	private Debug master;
	private JComponent[] controls = new JComponent[MAX_CONTROLS];
	private JButton[] buttons = new JButton[MAX_BUTTONS];
	private JCheckBox animate;
	private JSlider rate;
	private JComboBox threads;
	
	public ControlPanel(Debug master, int displayAlways) {
		super(BoxLayout.X_AXIS);
	    this.master = master;
		this.displayAlways = displayAlways;

	    for (int i = 0; i < MAX_BUTTONS; i++) {
	        buttons[i] = new JButton(labels[i]);
	        controls[i] = buttons[i];
	        buttons[i].addActionListener(this);
	        this.add(buttons[i]);
	    }
	    
	    this.add(Box.createHorizontalStrut(10));
	    
	    animate = new JCheckBox(labels[CMD_ANIMATE], true);
	    controls[CMD_ANIMATE] = animate;
	    animate.addActionListener(this);
	    this.add(animate);
	    
	    rate = new JSlider(0, 15, 5);
	    controls[CMD_RATE] = rate;
	    rate.setInverted(true);
	    rate.setSnapToTicks(true);
	    rate.setPaintTicks(true);
	    rate.addChangeListener(this);
	    rate.setPreferredSize(new Dimension(70,20));
	    rate.setMinimumSize(new Dimension(40,20));
	    this.add(rate);

	    this.add(Box.createHorizontalStrut(10));
	    
	    JLabel label = new JLabel("Track: ");
	    this.add(label);
	    
	    threads = new JComboBox();
	    controls[CMD_THREADS] = threads;
	    threads.addItem(labels[CMD_THREADS]);
	    threads.setEditable(false);
	    threads.setSelectedIndex(0);
	    threads.addActionListener(this);
	    this.add(threads);
	}


    public void actionPerformed(ActionEvent ev) {
        Object ctr = ev.getSource();
        
        for (int i = 0; i < MAX_CONTROLS; i++) {
            if (controls[i] == ctr) {
                master.panelEvent(this, i);
                return;
            }
        }
    }
    
    public void stateChanged(ChangeEvent ev) {
    	master.panelEvent(this, CMD_RATE);
    }
    
    public boolean isAnimated() {
    	return (animate.isSelected());
    }
    
    public long getDelay() {
    	long delay = rate.getValue();
    	return delay * delay * 10;
    }
    
    public void setEnabled(int mask) {
        boolean enabled;
        mask |= displayAlways;
        
        for (int i = 0; i < MAX_CONTROLS; i++) {
            enabled = ((mask & 1) == 1);
            if (enabled != controls[i].isEnabled()) {
                controls[i].setEnabled(enabled);
            }
            mask >>>= 1;
        }
    }
    
    
    public void addThread(Object thread) {
    	threads.addItem(thread);
    }
    
    public void removeThread(Object thread) {
    	if (thread == threads.getSelectedItem()) {
    		threads.setSelectedIndex(0);
    	}
    	threads.removeItem(thread);
    }
    
    public Object getActiveThread() {
    	Object item = threads.getSelectedItem();
    	return (item == labels[CMD_THREADS]) ? null : item;
    }

    public void setActiveThread(Object thread) {
    	threads.setSelectedItem(thread);
    }

}
