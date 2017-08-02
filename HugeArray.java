import java.util.*;
import java.io.*;

public final class HugeArray {
	
	private int defaultValue = 0;
	private int clusterSize = 16;
	private int clusterSizeMask = 15;
	private int clusterData = 256;
	private int clusterDataMask = 255;
	private int clusterBits = 4;
	private int height = 8;
	private int begin = Integer.MIN_VALUE;
	private int topDelta = 268435456;
	private AnyArrayArray top;
	
	private ArrayList listeners = new ArrayList();
	private int listenerCount;

	
	public HugeArray(int defaultValue) {
		top = new AnyArrayArray(null, begin, clusterSize);
		this.defaultValue = defaultValue;
	}
	
/*	private HugeArray(HugeArray pattern) {
		clusterSize = pattern.clusterSize;
		indexCorrection = pattern.indexCorrection;
		height = pattern.height;
		top = pattern.top;
		begin = pattern.begin;
		delta = pattern.delta;
	}
*/	

	public void addHugeArrayListener(HugeArrayListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
			listenerCount = listeners.size();
		}
	}
	
	public void removeHugeArrayListener(HugeArrayListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
			listenerCount = listeners.size();
		}
	}
	
	public final void notifyListeners(int x, int y, int oldVal, int newVal) {
		synchronized (listeners) {
			for (int i = 0; i < listenerCount; i++) {
				((HugeArrayListener)listeners.get(i)).hugeArrayChanged(this, x, y, oldVal, newVal);
			}
		}
	}
	
	
	
	
	public final SeqAccess getSequentialAccess(int x, int y) {
		return new SeqAccess(x, y);
	}
	
	public final void set(int x, int y, int val) {
		//System.err.println(" -set ("+x+", "+y+")=" + val);
		AnyArrayArray tmp = top;
		int pos;
		
		for (int i = height - 1; i > 0; i--) {
			pos = ((x >>> tmp.shift) & clusterSizeMask) + (((y >>> tmp.shift) & clusterSizeMask) << clusterBits);
			
			//System.err.println("xx="+xx+", yy="+yy+", x="+x+", y="+y+", tmp.shift="+tmp.shift+", i="+i);
			
			AnyArray next = tmp.array[pos];
			
			if (i==1) {
				IntArray ia = (IntArray)next;
				if (ia==null) {
					if (val == defaultValue) return;
					//System.err.println("new-set");
					synchronized (this) {
						ia = new IntArray(tmp, pos, clusterSize);
						tmp.array[pos] = ia;
					}
				}
				if (listenerCount > 0) {
					int index = (x & clusterSizeMask) + ((y & clusterSizeMask) << clusterBits);
					int old = ia.array[index];
					ia.array[index] = val;
					if (old != val) notifyListeners(x, y, old, val);
					return;
				} else {
					ia.array[(x & clusterSizeMask) + ((y & clusterSizeMask) << clusterBits)] = val;
					return;
				}
			} else {
				if (next==null && val != defaultValue) {
					//System.err.println("new");
					synchronized (this) {
						next = new AnyArrayArray(tmp, pos, clusterSize);
						tmp.array[pos] = next;
					}
				}
				tmp = (AnyArrayArray)next;
			}
		}
	}
	
	public final int get(int x, int y) {
		try {
			//System.err.println(" -get ("+x+", "+y+")");
			AnyArrayArray tmp = top;
			int pos;
			
			while (true) {
				pos = ((x >>> tmp.shift) & clusterSizeMask) + (((y >>> tmp.shift) & clusterSizeMask) << clusterBits);
				
				//System.err.println("xx="+xx+", yy="+yy+", x="+x+", y="+y);
				
				AnyArray next = tmp.array[pos];
				
				if (next == null) {
					return defaultValue;
				} else
				if (next.delta > 1) {
					tmp = (AnyArrayArray)next;
				} else {
					IntArray ia = (IntArray)next;
					return ia.array[(x & clusterSizeMask) + ((y & clusterSizeMask) << clusterBits)];
				}
			}
		} catch (Exception e) {}	// ignore
		return defaultValue;
	}

	
	public static void main(String args[]) {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.err.println("HugeArray - Test");
		String read;
		HugeArray array = new HugeArray(0);
		array.set(0,0,5);
		
		do {
			try {read = in.readLine();} catch (Exception e) {return;}
			StringTokenizer tok = new StringTokenizer(read,",");
			if (tok.countTokens()<2) break;
			int x = Integer.parseInt(tok.nextToken());
			int y = Integer.parseInt(tok.nextToken());
			if (tok.hasMoreTokens()) {
				int val = Integer.parseInt(tok.nextToken());
				array.set(x,y,val);
				System.err.println("Set: ("+x+","+y+")="+val);
			} else {
				System.err.println("Get: ("+x+","+y+")="+array.get(x,y));
			}
		} while (read!=null);
	}
	
	
	
	
	private class AnyArray {
		public AnyArrayArray parent;
		public int parentPos;
		public int delta;
		public int shift;
		public int mask;
		public int nonNullCount = 0;
		public int absX;
		public int absY;
		public int depth;
		public AnyArray left;
		public AnyArray right;
		public AnyArray up;
		public AnyArray down;
		
		public AnyArray(AnyArrayArray parent, int parentPos) {

			this.parent = parent;
			this.parentPos = parentPos;
			if (parent == null) {
				delta = topDelta;
				shift = (height - 1) * clusterBits;
				absX = 0;
				absY = 0;
				depth = 0;
				
				left = this;
				right = this;
				up = this;
				down = this;
			} else {
				depth = parent.depth + 1;
				delta = parent.delta / clusterSize;
				shift = parent.shift - clusterBits;
				absX = parent.absX + (parentPos & clusterSizeMask) * parent.delta;
				absY = parent.absY + (parentPos >> clusterBits) * parent.delta;
				//System.err.println("new AnyArray: absX="+absX+", absY="+absY+", parentPos="+parentPos+", depth="+depth);

				//System.err.println(" ------------- Scanning left ------------- ");
				left = top.find(absX, absY, SequentialAccess.DIR_LEFT, depth);
				if (left == null) {
					left = this;
					right = this;
				} else {
					//System.err.println(" ------------- Scanning right ------------- ");
					right = top.find(absX + delta * clusterSize, absY, SequentialAccess.DIR_RIGHT, depth);
					left.right = this;
					right.left = this;
				}

				//System.err.println(" ------------- Scanning up ------------- ");
				up = top.find(absX, absY, SequentialAccess.DIR_UP, depth);
				//System.err.println("new AnyArray: up="+up);
				if (up == null) {
					up = this;
					down = this;
				} else {
					//System.err.println(" ------------- Scanning down ------------- ");
					down = top.find(absX, absY + delta * clusterSize, SequentialAccess.DIR_DOWN, depth);
					up.down = this;
					down.up = this;
				}
				//print();
				//if (absX==32) {left.print(); right.print();}
			}
			mask = -1 << shift;
			
		}

/*
		public void print() {
					System.err.println("AnyArray: depth="+depth+", absX="+absX+", absY="+absY+
							":\nleft.depth="+left.depth+"left.absX="+left.absX+", left.absY="+left.absY+
							",\nright.depth="+right.depth+"right.absX="+right.absX+", right.absY="+right.absY+
							",\nup.depth="+up.depth+"up.absX="+up.absX+", up.absY="+up.absY+
							",\ndown.depth="+down.depth+"down.absX="+down.absX+", down.absY="+down.absY+"\n");
		}
*/		
		

		public final AnyArray find(int absX, int absY, int dir, int depth) {
			depth--;
			
			int count;
			int oldCount;
			AnyArrayArray tmp = (AnyArrayArray)this;
	
			int x = (absX >>> shift) & clusterSizeMask;
			int y = (absY >>> shift) & clusterSizeMask;
			//int rel = (x + (y << clusterBits)) & clusterDataMask;
			// ((absX >>> shift) + ((absY >>> shift) << clusterBits)) & clusterDataMask
			
			AnyArray next = tmp.array[x + (y << clusterBits)];
			AnyArray result;

			//System.err.println(" --> find: x="+x+", y="+y+", depth="+this.depth+", next="+next);
				
//			try {
//				Thread.sleep(50);
//			} catch (Exception e) {};

			if (next != null) {
				if ((depth <= 0) || (next.delta == 1)) return next;
				result = next.find(absX, absY, dir, depth);
				if (result != null) return result;
				next = null;
			}
	
			switch (dir) {
			case SequentialAccess.DIR_UP:
				count = y;
				absY &= mask;
				absY |= ~mask;
				break;
			case SequentialAccess.DIR_RIGHT:
				count = 15 - x;
				absX &= mask;
				break;
			case SequentialAccess.DIR_DOWN:
				count = 15 - y;
				absY &= mask;
				break;
			case SequentialAccess.DIR_LEFT:
				count = x;
				absX &= mask;
				absX |= ~mask;
				break;
			default:
				count = 0;
			}
	
			//System.err.println("absY="+absY+", absX="+absX+", count="+count+", next="+next+", dir="+dir);
			oldCount = count;

			while (true) {

				if (count-- <= 0) {
					if ((parent != null) || (oldCount >= 16)) break;
					count = 16 - oldCount;
					oldCount = 16;
				}

				switch (dir) {
				case SequentialAccess.DIR_UP:
					absY -= delta;
					break;
				case SequentialAccess.DIR_RIGHT:
					absX += delta;
					break;
				case SequentialAccess.DIR_DOWN:
					absY += delta;
					break;
				case SequentialAccess.DIR_LEFT:
					absX -= delta;
					break;
				default:
					return null;
				}

				next = tmp.array[((absX >>> shift) & clusterSizeMask) + (((absY >>> shift) & clusterSizeMask) << clusterBits)];

				//System.err.println("absY="+absY+", absX="+absX+", count="+count+
				//		", next="+next+", dir="+dir+", depth="+this.depth);
				
				
				if (next != null) {
					if ((depth <= 0) || (next.delta == 1)) return next;
					result = next.find(absX, absY, dir, depth);
					if (result != null) return result;
					next = null;
				}
			}
	/*
				System.err.println("rel="+rel+", count="+count+
						", absY="+absY+", absX="+absX+
						", tmp.shift="+tmp.shift+", dir="+dir+
						", tmp.delta="+tmp.delta);
	*/
			//System.err.println("next="+next);				    			

			return next;
		}


	}
	
	private final class AnyArrayArray extends AnyArray {
		public AnyArray[] array;
		
		public AnyArrayArray(AnyArrayArray parent, int parentPos, int clusterSize) {

			super(parent, parentPos);
			this.array = new AnyArray[clusterData];
		}
	}

	private final class IntArray extends AnyArray {
		public int[] array;
		
		public IntArray(AnyArrayArray parent, int parentPos, int clusterSize) {

			super(parent, parentPos);
			array = new int[clusterData];
			
			if (defaultValue == 0) return;
			for (int i = 0; i < clusterData; i++) {
				array[i] = defaultValue;
			}
		}
	}



	
	public final class SeqAccess implements SequentialAccess {
		
		private int absX;
		private int absY;
		private int relPos;
		private IntArray array;
		private int[] cache;
		private int value;
		
		public SeqAccess(int x, int y) {
			move(x, y);
		}
		
		private SeqAccess(SeqAccess pattern) {
			move(pattern);
		}
		
		
		
		public final int read() {
			return value;
		}
		
		public final int readInvalidate() {
			return (value = cache[relPos]);
		}
		
		public final void write(int data) {
			if (array != null) {
				cache[relPos] = data;
				int old = value;
				value = data;
				if ((listenerCount > 0) && (old != data))
					notifyListeners(absX, absY, old, data);
			} else {
				set(absX, absY, data);
				move(absX, absY, 4);	// optimizable
			}
		}
		
		public final int moveSkipping(int dir) {
			if (array == null) return move(absX, absY, dir);

			switch (dir) {
			case SequentialAccess.DIR_UP:
				relPos -= clusterSize;
				if ((--absY & clusterSizeMask) == clusterSizeMask) {
					array = (IntArray)array.up;
					cache = array.array;
					absY = array.absY + clusterSize - 1;
					relPos += clusterSize * clusterSize;
				}
				return (value = cache[relPos]);
			case SequentialAccess.DIR_RIGHT:
				relPos++;
				if ((++absX & clusterSizeMask) == 0) {
					array = (IntArray)array.right;
					cache = array.array;
					absX = array.absX;
					relPos -= clusterSize;
				}
				return (value = cache[relPos]);
			case SequentialAccess.DIR_DOWN:
				relPos += clusterSize;
				if ((++absY & clusterSizeMask) == 0) {
					array = (IntArray)array.down;
					cache = array.array;
					absY = array.absY;
					relPos -= clusterSize * clusterSize;
				}
				return (value = cache[relPos]);
			case SequentialAccess.DIR_LEFT:
				relPos--;
				if ((--absX & clusterSizeMask) == clusterSizeMask) {
					array = (IntArray)array.left;
					cache = array.array;
					absX = array.absX + clusterSize - 1;
					relPos += clusterSize;
				}
				return (value = cache[relPos]);
			default:
				throw new IllegalArgumentException();
			}
		}
		
		public final int move(int dir) {
			// XXX: nicht 100% thread sicher. was wenn array==null, inzwischen aber set()?

			switch (dir) {
			case SequentialAccess.DIR_UP:
				relPos -= clusterSize;
				if ((--absY & clusterSizeMask) == clusterSizeMask) {
					if (array == null) {
						array = (IntArray)top.find(absX, absY, 4, height);
					} else
					if (array.up.absY != array.absY - clusterSize) {
						array = null;
					} else {
						array = (IntArray)array.up;
					}
					if (array != null) cache = array.array;
					relPos += clusterSize * clusterSize;
				}
				return (value = (array != null) ? cache[relPos] : defaultValue);
			case SequentialAccess.DIR_RIGHT:
				relPos++;
				if ((++absX & clusterSizeMask) == 0) {
					if (array == null) {
						array = (IntArray)top.find(absX, absY, 4, height);
					} else
					if (array.right.absX != array.absX + clusterSize) {
						array = null;
					} else {
						array = (IntArray)array.right;
					}
					if (array != null) cache = array.array;
					relPos -= clusterSize;
				}
				return (value = (array != null) ? cache[relPos] : defaultValue);
			case SequentialAccess.DIR_DOWN:
				relPos += clusterSize;
				if ((++absY & clusterSizeMask) == 0) {
					if (array == null) {
						array = (IntArray)top.find(absX, absY, 4, height);
					} else
					if (array.down.absY != array.absY + clusterSize) {
						array = null;
					} else {
						array = (IntArray)array.down;
					}
					if (array != null) cache = array.array;
					relPos -= clusterSize * clusterSize;
				}
				return (value = (array != null) ? cache[relPos] : defaultValue);
			case SequentialAccess.DIR_LEFT:
				relPos--;
				if ((--absX & clusterSizeMask) == clusterSizeMask) {
					if (array == null) {
						array = (IntArray)top.find(absX, absY, 4, height);
					} else
					if (array.left.absX != array.absX - clusterSize) {
						array = null;
					} else {
						array = (IntArray)array.left;
					}
					if (array != null) cache = array.array;
					relPos += clusterSize;
				}
				return (value = (array != null) ? cache[relPos] : defaultValue);
			default:
				throw new IllegalArgumentException();
			}
			//System.err.println(" -move: ("+absX+", "+absY+") / " + relPos + " - array: " + array + ", value=" + value);
		}
		
		private final int move(int x, int y, int dir) {
			absX = x;
			absY = y;
			array = (IntArray)top.find(x, y, dir, height);
			if (array == null) {
				relPos = (absX & clusterSizeMask) + ((absY & clusterSizeMask) << clusterBits);
				return (value = defaultValue);
			} else {
				cache = array.array;
				switch (dir) {
				case SequentialAccess.DIR_UP:
					if ((absY < array.absY) || (absY >= array.absY + clusterSize))
						absY = array.absY + clusterSize - 1;
					break;
				case SequentialAccess.DIR_RIGHT:
					if ((absX < array.absX) || (absX >= array.absX + clusterSize))
						absX = array.absX;
					break;
				case SequentialAccess.DIR_DOWN:
					if ((absY < array.absY) || (absY >= array.absY + clusterSize))
						absY = array.absY;
					break;
				case SequentialAccess.DIR_LEFT:
					if ((absX < array.absX) || (absX >= array.absX + clusterSize))
						absX = array.absX + clusterSize - 1;
					break;
				}
				relPos = (absX & clusterSizeMask) + ((absY & clusterSizeMask) << clusterBits);
				return (value = cache[relPos]);
			}
		}
		
		public final int move(int x, int y) {
			return move(x, y, 4);
		}
		
		public int move(SequentialAccess source) {
			SeqAccess t = (SeqAccess)source;
			absX = t.absX;
			absY = t.absY;
			array = t.array;
			value = t.value;
			relPos = t.relPos;
			cache = t.cache;
			return value;
		}
		

		public final int getX() {return absX;}
		public final int getY() {return absY;}
		
		public final Object clone() {
			return new SeqAccess(this);
		}
		
		public String toString() {
			return "SeqAccess[x=" + absX + ", y=" + absY + ", value=" + value + "]";
		}
		
	}
	
}