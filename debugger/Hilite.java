package debugger;

import java.awt.Color;
import SequentialAccess;
import HugeArray;


class Hilite {
	
	private SequentialAccess seqAccess;
	private HugeArray array;
	private int oldX;
	private int oldY;
	private int curX;
	private int curY;
	private Color color = Color.white;
	private Color oldColor;
	private int dir = SequentialAccess.DIR_RIGHT;
	private boolean displaying = false;
	private boolean visible = true;
	private boolean filled;
	
	
	public Hilite(HugeArray array, int x, int y, boolean filled) {
		this.array = array;
		this.filled = filled;
		move(x, y);
	}
	
	public Hilite(SequentialAccess seqAccess, boolean filled) {
		this.seqAccess = seqAccess;
		this.filled = filled;
	}
	
	public int read() {
		return (seqAccess == null) ? array.get(curX, curY) : seqAccess.read();
	}
	
	public void write(int value) {
		if (seqAccess == null) {
			array.set(curX, curY, value);
		} else {
			seqAccess.write(value);
		}
	}
	

	public void move(int x, int y) {
		curX = x;
		curY = y;
	}
	
	public void snapShot() {
		if (seqAccess != null) {
			curX = seqAccess.getX();
			curY = seqAccess.getY();
		}
	}
	
	public int getX() {
		return curX;
	}
	
	public int getY() {
		return curY;
	}
	
	public int getOldX() {
		return oldX;
	}
	
	public int getOldY() {
		return oldY;
	}
	
	public boolean paintedAt(int x, int y) {
		return (displaying && (oldX == x) && (oldY == y));
	}
	
	public boolean moved() {
		return ((oldX != curX) || (oldY != curY));
	}
	
	public boolean toClear() {
		return displaying && (!visible || (oldX != curX) || (oldY != curY) || (oldColor != color));
	}
	
	public boolean toPaint() {
		return (!displaying && visible);
	}
	
	
	public void painted() {
		oldX = curX;
		oldY = curY;
		oldColor = color;
		displaying = true;
		//System.out.println("painted");
	}
	
	public void cleared() {
		displaying = false;
		//System.out.println("cleared");
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	

	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public Color getOldColor() {
		return oldColor;
	}
	
	
	public boolean isFilled() {
		return filled;
	}
	
	
	public void setDirection(int dir) {
		this.dir = dir;
	}
	
	public int getDirection() {
		return dir;
	}
	
}
