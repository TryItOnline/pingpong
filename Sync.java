

class Sync extends Device {
	
	private boolean triggered;
	private Thread owner;
	private int ownCount;
	
	public Sync(boolean triggered) {
		this.triggered = triggered;
		if (triggered) owner = Thread.currentThread();
	}
	
	// Event behavior
	public synchronized void pulse() {
		set();
		reset();
	}
	
	public synchronized void pulseSingle() {
		notify();
	}
	
	public synchronized void set() {
		if (!triggered) {
			triggered = true;
			notifyAll();
		}
	}
	
	public synchronized void reset() {
		triggered = false;
	}
	
	public synchronized void eventWait() {
		try {
			if (!triggered) wait();
		} catch (InterruptedException e) {};
	}
	
	// Mutex behavior
	public synchronized void get() {
		if (owner!=null && owner!=Thread.currentThread()) {
			try {wait();} catch (InterruptedException e) {};
		}
		owner = Thread.currentThread();
		ownCount++;
	}
	
	public synchronized void release() {
		if ((owner==Thread.currentThread()) && (--ownCount == 0)) {
			owner = null;
			notify();
		}
	}
	
	// Multi-Owners behaviour
	protected void newReference() {};
	
	protected void destroyReference() {
		if (owner == Thread.currentThread()) {
			owner = null;
			ownCount = 0;
			notify();
		}
	}
	
	protected void destroy() {
		notifyAll();
		triggered = false;
	}
	
}