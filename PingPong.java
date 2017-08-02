import java.io.*;
import java.util.*;



public class PingPong {
    
    public static final String APP_NAME = "PingPong Interpreter";
    public static final String APP_VERSION = "0.2beta";
    public static final String APP_FEEDBACK = "pingpong@inz.info";
    
    public static final String PPCCT_DEF = "256:48-57,65-90,97-122,169,174," +
            "192-255,0-31,128-159,32-47,58-64,91-96,123-127,160-168,170-173,175-191";
    
    private static final Mapping PPCCT = new Mapping(PPCCT_DEF);
	
	private List threads = new ArrayList();
	
	private HugeArray space;
	private Debugger debugger;
	private boolean debug;
	private Object debugInfo;
	private boolean running;
	private boolean notified;
	private int nextThreadId;
	
	private int exitCode;
	
	public PingPong() {
		space = new HugeArray(226);	// #226 = noP
	}

	public PingPong(String filename) throws IOException {
		this();
		if (!load(filename, 0, 0, false)) {
			throw new IOException("Error loading file: "+filename);
		}
	}
	
	public PingPong(Debugger debugger, boolean enabled) {
	    this();
		if (debugger == null) throw new IllegalArgumentException();
		this.debugger = debugger;
		this.debug = enabled;
	}

	public PingPong(String filename, Debugger debugger, boolean enabled)
	throws IOException {

		this(debugger, enabled);
		if (!load(filename, 0, 0, false)) {
			throw new IOException("Error loading file: "+filename);
		}
	}
	
	public boolean load(String filename, int x, int y, boolean underwrite) {
		try {
			BufferedReader fin = new BufferedReader(new InputStreamReader(
			        new FileInputStream(filename), "ISO-8859-1"));
			
			String line;
			SequentialAccess seqAccess = space.getSequentialAccess(x, y);
		
			while ((line = fin.readLine())!=null) {
				int len = line.length();
				int newCh;
				int oldCh = seqAccess.move(x, y);
				for (int i=0; i<len; i++) {
					newCh = fromAscii((int)line.charAt(i));
					if ((underwrite && (oldCh == 226)) || (!underwrite && (newCh != 226))) {
						seqAccess.write(newCh);
					}
					oldCh = seqAccess.move(SequentialAccess.DIR_RIGHT);
				}
				y++;
			}
		} catch (IOException e) {return false;}
		
		//System.err.println("Loaded file " + filename + " to (" + x + ", " + y + ")");
		return true;
	}
	
	
	public synchronized void start() {
		nextThreadId = 0;
		
		PingPongThread thread = new PingPongThread(this, space, 0, 0,
				SequentialAccess.DIR_RIGHT, debugger, debug);
				
		running = true;
		notified = false;
		if (debugger != null) debugger.processStarted(this);

		new Thread(thread, "main").start();
	}
	
	public synchronized int waitForTermination() {
		if (running) {
			try {
				wait();
			} catch (InterruptedException e) {};
		}
		return exitCode;
	}

	protected synchronized int registerThread(PingPongThread thread) {
		threads.add(thread);
		if (debugger != null) debugger.threadStarted(thread);
		return nextThreadId++;
	}
	
	protected synchronized void deregisterThread(PingPongThread thread, int exitCode) {
		if (!threads.remove(thread)) return;
		if (debugger != null) debugger.threadFinished(thread, exitCode);
		if (threads.size()==0) {
			this.exitCode = exitCode;
			running = false;
			if (debugger != null) debugger.processFinished(this, exitCode);
			notifyAll();
		}
	}
	
	public HugeArray getSpace() {
		return space;
	}
	
	protected synchronized void terminating(int exitCode) {
		if (!running) return;
		
		this.exitCode = exitCode;
		
		notifyTerminate();
		//System.err.println("term1");
		if (running) {
			try {
				wait(3000);
			} catch (InterruptedException e) {};
		}
		//System.err.println("term2");
		if (running) {
			// XXX: handling
			System.err.println();
			System.err.println("Abnormal termination!");
			for (int i = threads.size() - 1; i >= 0; i--) {
				deregisterThread((PingPongThread)threads.get(i), -1);
			}
		}
	}
	
	public void terminate() {
		terminating(0);
	}
	
	public synchronized void notifyTerminate() {
		if (notified) return;
		
		notified = true;
		int size = threads.size();
		for (int i = 0; i < size; i++) {
			((PingPongThread)threads.get(i)).terminating();
		}
		if (debugger != null) debugger.notifyProcessFinish(this);
	}
	
	
	
	public synchronized void suspendAll() {
		int size = threads.size();
		for (int i = 0; i < size; i++) {
			((PingPongThread)threads.get(i)).suspend();
		}
	}
	
	public synchronized void resumeAll() {
		int size = threads.size();
		for (int i = 0; i < size; i++) {
			((PingPongThread)threads.get(i)).resume();
		}
	}


	// debugger support
	public void setDebugInfo(Object info) {
		debugInfo = info;
	}
	
	public Object getDebugInfo() {
		return debugInfo;
	}

	public int getExitCode() {
		return exitCode;
	}
	


	// ASCII <-> PPCCT
	public static final int toAscii(int val) {
	    return PPCCT.toReference(val);
	}
	
	public static final int fromAscii(int val) {
	    return PPCCT.fromReference(val);
	}



	public static void main(String args[]) {
		System.err.println("Starting PingPong. Interpreting file: "+args[0]);

		PingPong interpreter = new PingPong();
		
		if (!interpreter.load(args[0], 0, 0, false)) {
			System.err.println("Error loading source file");
			return;
		}
		
		System.err.println();
		
		interpreter.start();
		int exitCode = interpreter.waitForTermination();
		
		System.err.println();
		System.err.println("Execution terminated. Exit Code: " + exitCode);
		System.exit(exitCode);
	}
}
