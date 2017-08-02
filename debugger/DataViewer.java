package debugger;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

import SequentialAccess;
import HugeArrayListener;
import HugeArray;
import PingPong;

class DataViewer extends JComponent
implements ActionListener, HugeArrayListener, FocusListener, KeyListener, MouseListener {
	
	private static final int MAX_DISPLAY = 64;
	
	private HugeArray array;
	private SequentialAccess seqAccessY;
	private SequentialAccess seqAccessX;
	
	private Rectangle display = new Rectangle();
	private Rectangle oldDisplay = new Rectangle();
	
	private Font font;
	private Color xorColor = Color.orange;
	
	private int charWidth;
	private int charHeight;
	private int charYAscent;
	private Insets insets;
	
	private char[][] content = new char[MAX_DISPLAY][MAX_DISPLAY];
	private ArrayList hilites = new ArrayList();
	
	private boolean inPaint;
	
	private JComponent component;
/*
	private JTextField xEdit;
	private JTextField yEdit;
	private JTextField valEdit;
*/
	
	private Hilite focus;
	
	
	public DataViewer(String name, Font font, HugeArray array) {
		this.font = font;
		this.array = array;
		seqAccessX = array.getSequentialAccess(0, 0);
		seqAccessY = (SequentialAccess)seqAccessX.clone();
		array.addHugeArrayListener(this);
		setBorder(BorderFactory.createTitledBorder(name));
		insets = getInsets();
		addFocusListener(this);
		addKeyListener(this);
		addMouseListener(this);
	}
	
	
	public JComponent getComponent() {
		if (component == null) {
			component = initGUI();
		}
		return component;
	}
	
	private JComponent initGUI() {
		initFontSettings();

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(this, BorderLayout.CENTER);

/*		
		Box box = new Box(BoxLayout.X_AXIS);

		box.add(new JLabel("X: "));
		xEdit = new JTextField("0");
		xEdit.setHorizontalAlignment(JTextField.RIGHT);
		box.add(xEdit);
		
		box.add(Box.createHorizontalStrut(10));
		
		box.add(new JLabel("Y: "));
		yEdit = new JTextField("0");
		yEdit.setHorizontalAlignment(JTextField.RIGHT);
		box.add(yEdit);

		box.add(Box.createHorizontalStrut(10));
		
		box.add(new JLabel("Val: "));
		valEdit = new JTextField("0");
		valEdit.setHorizontalAlignment(JTextField.RIGHT);
		box.add(valEdit);

		box.add(Box.createHorizontalStrut(10));
		
		JButton button;
		button = new JButton("<<");
		button.addActionListener(this);
		button.setActionCommand("left");
		box.add(button);
		button = new JButton(">>");
		button.addActionListener(this);
		button.setActionCommand("right");
		box.add(button);
		button = new JButton("^^");
		button.addActionListener(this);
		button.setActionCommand("up");
		box.add(button);
		button = new JButton("vv");
		button.addActionListener(this);
		button.setActionCommand("down");
		box.add(button);
		
		panel.add(box, BorderLayout.SOUTH);
*/
		
		focus = new Hilite(array, 0, 0, false);
		focus.setVisible(hasFocus());
		addHilite(focus);
		
		return panel;
	}
	
	
	public void actionPerformed(ActionEvent ev) {
	}
	
	public void hugeArrayChanged(HugeArray array, int x, int y, int oldVal, int newVal) {
		if (this.array != array) return;
		if (display.contains(x, y)) {
			Graphics g = getGraphics();
			if (g == null) return;
			synchronized (this) {
				int relX = x - display.x;
				int relY = y - display.y;
				content[relY][relX] = (char)PingPong.toAscii(newVal);
				
				int drawX = insets.left + relX * charWidth;
				int drawY = insets.top + relY * charHeight;
				g.clearRect(drawX, drawY, charWidth, charHeight);
				g.setFont(font);
				g.drawChars(content[relY], relX, 1, drawX, drawY + charYAscent);
				
				int size = hilites.size();
				Hilite hilite;
				for (int i = 0; i < size; i++) {
					hilite = (Hilite)hilites.get(i);
					if (hilite.paintedAt(x, y)) {
						hilite.cleared();
						repaintHilite(hilite);
					}
				}
			}
		}
	}
	
	
	
	public synchronized void focusTo(int x, int y) {
		focus.move(x, y);
		focus();
	}
	
	public synchronized void focusTo(Hilite hilite) {
		hilite.snapShot();
		focus.move(hilite.getX(), hilite.getY());
		focus();
	}
	
	private void focus() {
		int fx = focus.getX();
		int fy = focus.getY();

/*		
		if (focus.moved()) {
			xEdit.setText(Integer.toString(fx));
			yEdit.setText(Integer.toString(fy));
			valEdit.setText(Integer.toString(focus.read()));
		}
*/
		
		oldDisplay.x = display.x;
		oldDisplay.y = display.y;

		if ((fx < display.x + display.width / 4) ||
			(fx > display.x + display.width / 4 * 3) ||
			(fy < display.y + display.height / 4) ||
			(fy > display.y + display.height / 4 * 3)) {
		
			display.x = fx - display.width / 2;
			display.y = fy - display.height / 2;
		}
		
		boolean sameStart = (oldDisplay.x == display.x && oldDisplay.y == display.y);
		if (sameStart && oldDisplay.width == display.width &&
			oldDisplay.height == display.height) return;
		
		int y = (sameStart) ? oldDisplay.height : 0;
		int startX = (sameStart) ? oldDisplay.width : 0;
		
		seqAccessY.move(startX + display.x, y + display.y);
		for (; y < display.height; y++) {
			seqAccessX.move(seqAccessY);
			for (int x = startX; x < display.width; x++) {
				content[y][x] = (char)PingPong.toAscii(seqAccessX.read());
				seqAccessX.move(SequentialAccess.DIR_RIGHT);
			}
			seqAccessY.move(SequentialAccess.DIR_DOWN);
		}
		
		if (!inPaint) repaint();
	}
	
	
	public void repaintHilites() {
		Graphics g = getGraphics();
		int size = hilites.size();
		for (int i = 0; i < size; i++) {
			Hilite hilite = (Hilite)hilites.get(i);
			repaintHilite(hilite);
		}
	}
	
	public void repaintHilite(Hilite hilite) {
		Graphics g = getGraphics();
		if (g != null) repaintHilite(hilite, g);
	}
	
	private synchronized void repaintHilite(Hilite hilite, Graphics g) {
		int x;
		int y;
		
		hilite.snapShot();
		
		if (hilite.toClear()) {
			g.setXORMode(hilite.getOldColor());
			x = hilite.getOldX();
			y = hilite.getOldY();
			
			if (display.contains(x, y)) {
				if (hilite.isFilled()) {
					g.fillRect((x - display.x) * charWidth + insets.left, (y - display.y) *
							charHeight + insets.top + 2, charWidth, charHeight - 4);
				} else {
					g.drawRect((x - display.x) * charWidth + insets.left, (y - display.y) *
							charHeight + insets.top + 2, charWidth - 1, charHeight - 5);
				}
			}
			hilite.cleared();
		}
		
		if (hilite.toPaint()) {
			g.setXORMode(hilite.getColor());
			x = hilite.getX();
			y = hilite.getY();
			
			if (display.contains(x, y)) {
				if (hilite.isFilled()) {
					g.fillRect((x - display.x) * charWidth + insets.left, (y - display.y) *
							charHeight + insets.top + 2, charWidth, charHeight - 4);
				} else {
					g.drawRect((x - display.x) * charWidth + insets.left, (y - display.y) *
							charHeight + insets.top + 2, charWidth - 1, charHeight - 5);
				}
				hilite.painted();
			}
		}
	}
	
	public synchronized void paint(Graphics g) {
		inPaint = true;
		
		newDimension();
		paintBorder(g);
		
		//System.out.println("paint: " + display);
		g.setFont(font);
		int drawY = insets.top + charYAscent;
		
		for (int y = 0; y < display.height; y++) {
			g.drawChars(content[y], 0, display.width, insets.left, drawY);
			drawY += charHeight;
		}

		int size = hilites.size();
		for (int i = 0; i < size; i++) {
			Hilite hilite = (Hilite)hilites.get(i);
			hilite.cleared();
			repaintHilite(hilite, g);
		}
		
		inPaint = false;
	}
	
	
	public void focusGained(FocusEvent ev) {
		focus.setVisible(true);
		repaintHilite(focus);
	}
	
	public void focusLost(FocusEvent ev) {
		focus.setVisible(false);
		repaintHilite(focus);
	}
	
	
	private void edit(char ch) {
		int pp = PingPong.fromAscii((int)ch);
		array.set(focus.getX(), focus.getY(), pp);
		advance();
	}
	
	private void advance() {
		switch (focus.getDirection()) {
		case SequentialAccess.DIR_LEFT:
			focusTo(focus.getX() - 1, focus.getY());
			break;
		case SequentialAccess.DIR_RIGHT:
			focusTo(focus.getX() + 1, focus.getY());
			break;
		case SequentialAccess.DIR_UP:
			focusTo(focus.getX(), focus.getY() - 1);
			break;
		case SequentialAccess.DIR_DOWN:
			focusTo(focus.getX(), focus.getY() + 1);
			break;
		}
	}
	
	
	public void keyPressed(KeyEvent ev) {
		int newDir = -1;
		switch (ev.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			newDir = SequentialAccess.DIR_LEFT;
			break;
		case KeyEvent.VK_RIGHT:
			newDir = SequentialAccess.DIR_RIGHT;
			break;
		case KeyEvent.VK_UP:
			newDir = SequentialAccess.DIR_UP;
			break;
		case KeyEvent.VK_DOWN:
			newDir = SequentialAccess.DIR_DOWN;
			break;
		case KeyEvent.VK_DELETE:
			edit((char)160);
			return;
		}
		if (newDir == -1) return;
		if (newDir != focus.getDirection()) {
			focus.setDirection(newDir);
		}
		advance();
	}

	public void keyReleased(KeyEvent ev) {};
	
	public void keyTyped(KeyEvent ev) {
		char ch = ev.getKeyChar();
		if (ch == KeyEvent.CHAR_UNDEFINED) return;
		edit(ch);
	}
	

	public void mouseClicked(MouseEvent ev) {
		int mx = ev.getX();
		int my = ev.getY();
		int x = (mx - insets.left) / charWidth;
		int y = (my - insets.top) / charHeight;
		focusTo(x + display.x, y + display.y);
		requestFocus();
	}

	public void mouseEntered(MouseEvent ev) {};

	public void mouseExited(MouseEvent ev) {};

	public void mousePressed(MouseEvent ev) {};

	public void mouseReleased(MouseEvent ev) {};
	
	
	private void initFontSettings() {
		FontMetrics m = getFontMetrics(font);
		charHeight = m.getHeight();
		charWidth = m.charWidth(' ');
		charYAscent = m.getAscent();
	}
	
	private void newDimension() {
		getInsets(insets);
		
		display.width = (getWidth() - insets.left - insets.right) / charWidth;
		display.height = (getHeight() - insets.top - insets.bottom) / charHeight;
		if (display.width > MAX_DISPLAY) display.width = MAX_DISPLAY;
		if (display.height > MAX_DISPLAY) display.height = MAX_DISPLAY;
		
		focus();
	}
	
	public boolean isFocusTraversable() {
		return true;
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(charWidth * 32 + insets.left + insets.right,
				charHeight * 16 + insets.top + insets.bottom);
	}
	
	public Dimension getMinimumSize() {
		return new Dimension(charWidth * 5 + insets.left + insets.right,
				charHeight * 3 + insets.top + insets.bottom);
	}
	
	public Dimension getMaximumSize() {
		return new Dimension(charWidth * MAX_DISPLAY + insets.left + insets.right,
				charHeight * MAX_DISPLAY + insets.top + insets.bottom);
	}
	
	public synchronized void addHilite(Hilite hilite) {
		hilites.add(hilite);
		repaintHilite(hilite);
	}
	
	public synchronized void removeHilite(Hilite hilite) {
		hilites.remove(hilite);
		hilite.setVisible(false);
		repaintHilite(hilite);
	}
	
}
