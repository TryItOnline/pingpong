public class LongStack {
	
	private long[] array;
	private int size;
	private int cap;
	private int initialCap;
	private int lowLimit;

	
	public LongStack() {
		this(64);
	}

	public LongStack(int cap) {
		initialCap = cap;
		if (initialCap < 8) initialCap = 8;
		this.cap = initialCap;
		array = new long[cap];
	}

	private LongStack(LongStack pattern) {
		size = pattern.size;
		cap = pattern.cap;
		initialCap = pattern.initialCap;
		lowLimit = pattern.lowLimit;
		array = new long[cap];
		System.arraycopy(pattern.array, 0, array, 0, size);
	}

	
	private void resize(int newCap) {
		lowLimit = ((newCap >> 1) < initialCap) ? 0 : newCap >> 2;

		long[] newArray = new long[newCap];
		System.arraycopy(array, 0, newArray, 0, size);
		
		array = newArray;
		cap = newCap;
	}
	
	public void push(long value) {
		if (size == cap) {
			resize(cap << 1);
		}
		array[size++] = value;
	}
	
	public long pop() {
		if (size < lowLimit) {
			resize(cap >> 1);
		}
		return (size <= 0) ? 0 : array[--size];
	}
	
	public long top() {
		return (size <= 0) ? 0 : array[size - 1];
	}
	
	public int size() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
		int newCap = initialCap;
		while (newCap < size) newCap <<= 1;
		resize(newCap);
	}
	
	
	public long get(int index) {
		return (index >= 0) ? array[index] : 0;
	}
	
	public void set(int index, long value) {
		if (index >= 0) {
			array[index] = value;
		}
	}
	
	
	public Object clone() {
		return new LongStack(this);
	}
	
}
