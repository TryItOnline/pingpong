

public interface SequentialAccess {
	public static final int DIR_UP = 0;
	public static final int DIR_RIGHT = 1;
	public static final int DIR_DOWN = 2;
	public static final int DIR_LEFT = 3;

	public int read();
	public void write(int data);
	
	public int move(int direction);
	public int move(int x, int y);
	public int move(SequentialAccess source);

	public int getX();
	public int getY();
	
	public Object clone();
}
