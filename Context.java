import java.io.*;

class Context {
	
	public int x;
	public int y;
	public int stackSize;
	public int helpStackSize;
	
	public Context(int x, int y, int stackSize, int helpStackSize) {
		this.x = x;
		this.y = y;
		this.stackSize = stackSize;
		this.helpStackSize = helpStackSize;
	}
	
}