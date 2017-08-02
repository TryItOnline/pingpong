

public abstract class Device {
	
	int copyCount;
	

	public Device() {
		copyCount = 1;
	}

	
	public synchronized void copied() {
		copyCount++;
	}
	
	public synchronized void close() {
		if (copyCount > 0) {
			if (--copyCount == 0) {
				destroy();
			} else {
				destroyReference();
			}
		}
	}
	
	public synchronized void closeAll() {
		if (copyCount > 0) {
			copyCount = 0;
			destroy();
		}
	}

	
	protected abstract void newReference();
	
	protected abstract void destroyReference();
	
	protected abstract void destroy();
	
}
