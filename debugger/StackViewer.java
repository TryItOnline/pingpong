package debugger;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import LongStack;
import PingPong;


class StackViewer extends JComponent {
	
	private static final int MAX_DISPLAY_Y = 64;
	private static final int MAX_DISPLAY_X = 24;

	private LongStack stack;
	private long[] current = new long[MAX_DISPLAY_Y];
	private int currentLines;
	private long[] old = new long[MAX_DISPLAY_Y];
	private int oldLines;
	
	private int offset;

	private int charWidth;
	private int charHeight;
	private int charYAscent;
	private Insets insets;
	
	private int maxLines;
	private int maxChars;
	
	private Font font;
	
	private String name;
	
	private JComponent component;

	
	public StackViewer(String name, Font font) {
		this.font = font;
		this.name = name;
		setBorder(BorderFactory.createTitledBorder(name));
		insets = getInsets();
	}

	
	public JComponent getComponent() {
		if (component == null) {
			component = initGUI();
		}
		return component;
	}
	
	private JComponent initGUI() {
		initFontSettings();
		return this;
	}


	public void setStack(LongStack stack) {
		this.stack = stack;
		refresh();
	}

	private void doPainting(Graphics g, boolean clear) {
		int lines = ((oldLines > currentLines) && clear) ? oldLines : currentLines;
		//System.out.println("lines: " + maxLines);
		
		StringBuffer tmp = new StringBuffer(32);
		int y = (maxLines - 1) * charHeight + insets.top;
		
		g.setFont(font);

		for (int i = 0; i < lines; i++) {
			if ((current[i] != old[i]) || (i >= currentLines)) {
				g.setColor(getBackground());
				g.fillRect(insets.left, y, maxChars * charWidth, charHeight);
				g.setColor(getForeground());
			}
			if (i < currentLines) {
				if ((i == 0) && (offset > 0)) {
					tmp.append("...");
				} else {
					tmp.append(current[i]).append(' ').append('\'').
							append((char)PingPong.toAscii((int)current[i] % 65535)).append('\'');
				}
				int len = tmp.length();
				
				if (len > maxChars) {
					int offset = (current[i] < 0) ? 1 : 0;
					tmp.replace(offset, len - maxChars + 1 + offset, "#");
				}
				while (len++ < maxChars) {
					tmp.insert(0, ' ');
				}
				
				g.drawString(tmp.toString(), insets.left, y + charYAscent);
				//System.out.println("stack: " + tmp);

				tmp.setLength(0);
			}
			y -= charHeight;
		}
	}

	public synchronized void paint(Graphics g) {
		newDimension();
		detectChanges();
		paintBorder(g);
		doPainting(g, false);
	}
	
	public synchronized void refresh() {
		if (detectChanges()) {
			Graphics g = getGraphics();
			if (g != null) doPainting(g, true);
		}
	}
	
	private boolean detectChanges() {
		boolean change = false;
		oldLines = currentLines;

		if (stack == null) {
			currentLines = 0;
			change = (oldLines != currentLines);
			return change;
		}
		
		int size = stack.size();
		if ((size - offset) > maxLines) {
			offset = size - maxLines * 3 / 4;
		} else
		if ((offset > 0) && ((size - offset) < maxLines / 2)) {
			offset = size - maxLines * 3 / 4;
			if (offset < 0) offset = 0;
		}
		currentLines = size - offset;
		
		for (int i = 0; i < currentLines; i++) {
			old[i] = current[i];
			current[i] = stack.get(i + offset);
			if (current[i] != old[i]) change = true;
		}
		//System.out.println("change: " + change + ", size: " + size +
		//		", currentLines: " + currentLines + ", maxLines: " + maxLines + ", offset: " + offset);
		return (change || (oldLines != currentLines));
	}
	

	private void initFontSettings() {
		FontMetrics m = getFontMetrics(font);
		charHeight = m.getHeight() - 4;
		charWidth = m.charWidth(' ');
		charYAscent = m.getAscent() - 2;
	}

	private void newDimension() {
		getInsets(insets);
		
		maxChars = (getWidth() - insets.left - insets.right) / charWidth;
		maxLines = (getHeight() - insets.top - insets.bottom) / charHeight;
		if (maxLines > MAX_DISPLAY_Y) maxLines = MAX_DISPLAY_Y;
		if (maxChars > MAX_DISPLAY_X) maxChars = MAX_DISPLAY_X;
	}

	
	public Dimension getPreferredSize() {
		return new Dimension(charWidth * 14 + insets.left + insets.right,
				charHeight * 16 + insets.top + insets.bottom);
	}
	
	public Dimension getMinimumSize() {
		return new Dimension(charWidth * 7 + insets.left + insets.right,
				charHeight * 3 + insets.top + insets.bottom);
	}
	
	public Dimension getMaximumSize() {
		return new Dimension(charWidth * MAX_DISPLAY_X + insets.left + insets.right,
				charHeight * MAX_DISPLAY_Y + insets.top + insets.bottom);
	}
	
}
