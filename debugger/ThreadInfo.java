package debugger;

import java.awt.*;

import PingPongThread;


class ThreadInfo {
	
	public static final int MODE_RUNNING = 0;
	public static final int MODE_WATCHED = 1;
	public static final int MODE_ANIMATED = 2;
	public static final int MODE_STOPPED = 3;
	
	private static final Color COLOR_PRIMARY = Color.cyan;
	private static final Color COLOR_DEFAULT = Color.yellow;

	
	private PingPongThread thread;
	private Hilite hilite;

	private int mode;
	private long delay;
	
	private boolean primary;
	

	public ThreadInfo(int mode, long delay) {
		this.mode = mode;
		this.delay = delay;
	}

	public ThreadInfo(PingPongThread thread) {
		thread.setDebugInfo(this);
		this.thread = thread;
		hilite = new Hilite(thread.getIp(), true);
		setPrimary(false);
	}
	
	public ThreadInfo(PingPongThread thread, ThreadInfo pattern) {
		this(thread);
		setPattern(pattern);
	}
	
	
	public void setPattern(ThreadInfo pattern) {
		setMode(pattern.mode);
		setDelay(pattern.delay);
	}
	
	public void setPrimary(boolean primary) {
		this.primary = primary;
		hilite.setColor(primary ? COLOR_PRIMARY : COLOR_DEFAULT);
	}
	
	public boolean isPrimary() {
		return primary;
	}
	
	public void setMode(int newMode) {
		if (thread != null) {
			thread.setDebugEnabled(newMode != MODE_RUNNING);
			if ((mode != MODE_STOPPED) && (newMode == MODE_STOPPED)) {
				thread.suspend();
			} else
			if ((mode == MODE_STOPPED) && (newMode != MODE_STOPPED)) {
				thread.resume();
			}
		}
		mode = newMode;
	}
	
	public int getMode() {
		return mode;
	}
	
	public void setDelay(long delay) {
		this.delay = delay;
	}
	
	public long getDelay() {
		return delay;
	}
	
	public PingPongThread getThread() {
		return thread;
	}
	
	public Hilite getHilite() {
		return hilite;
	}
	
	
	public String toString() {
		return "thread #" + Integer.toString(thread.getId());
	}
	
}
