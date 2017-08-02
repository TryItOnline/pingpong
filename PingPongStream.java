import java.io.*;


public class PingPongStream {
	
	public static final int TYPE_PPCCT = 0;
	public static final int TYPE_ASCII = 1;
	public static final int TYPE_BINARY = 2;


	private int dir;
	private int type;
	private SequentialAccess seqAccess;
	private InputStream in;
	private OutputStream out;
	private boolean open = true;
	private boolean wasCR;
	private boolean wasLF;

	
	public PingPongStream(SequentialAccess seqAccess, int dir, int type) {
		this.seqAccess = seqAccess;
		this.dir = dir;
		this.type = type;
	}
	
	public PingPongStream(InputStream in, OutputStream out, int type) {
		this.in = in;
		this.out = out;
		this.type = type;
	}



	public void setDirection(int dir) {
		this.dir = dir;
	}
	
	public void setType(int type) {
		this.type = type;
		if (type == TYPE_BINARY) wasCR = false;
	}
	
	

	private final int doRead() {
		if (in != null) {
			try {
				return in.read();
			} catch (Exception e) {
				open = false;
				return -1;
			}
		} else
		if (seqAccess != null) {
			int value = seqAccess.read();
			seqAccess.move(dir);
			//System.err.println("doRead: " + value);
			return value;
		}
		return -1;
	}
	
	public final int read() {
		if (!open) return -1;
		int value;
		while (true) {
			if (type != TYPE_ASCII) {
				value = doRead();
			} else {
				value = PingPong.fromAscii(doRead());
			}
			if ((value == 138) && wasCR) {
				wasCR = false;
			} else
			if (value == 141) {
				wasCR = true;
				return 138;
			} else {
				wasCR = false;
				return value;
			}
		}
	}
	
	private final String readLine() {
		StringBuffer str = new StringBuffer();
		int value;
		boolean nonBlank = false;
		while (true) {
			if (type == TYPE_PPCCT) {
				value = PingPong.toAscii(doRead());
			} else {
				value = doRead();
			}
			switch (value) {
			case -1:
			case 160:
			    return str.toString();
			case 9:
			case 32:
				if (nonBlank) return str.toString();
				break;
			case 10:
				if (!wasCR) {
					return str.toString();
				} else {
					wasCR = false;
					break;
				}
			case 13:
				wasCR = true;
				return str.toString();
			default: 
				str.append((char)value);
				nonBlank = true;
			}
			wasCR = false;
		}
	}
	
	public final long readLong() {
		if (!open) return 0;
		if (type == TYPE_BINARY) {
			int value = doRead();
			value += ((long)doRead() << 8);
			value += ((long)doRead() << 16);
			value += ((long)doRead() << 24);
			value += ((long)doRead() << 32);
			value += ((long)doRead() << 40);
			value += ((long)doRead() << 48);
			return value + ((long)doRead() << 56);
		} else {
			try {
				return Long.parseLong(readLine());
			} catch (Exception e) {
				return 0;
			}
		}
	}


	
	private final void doWrite(int value) {
		if (out != null) {
			try {
				out.write(value);
				out.flush();
			} catch (Exception e) {
				open = false;
			}
		} else
		if (seqAccess != null) {
			seqAccess.write(value);
			seqAccess.move(dir);
			//System.err.println("doWrite: " + value);
		}
	}
	
	public final void write(int value) {
		if (type == TYPE_ASCII) {
			doWrite(PingPong.toAscii(value));
		} else {
			doWrite(value);
		}
	}
	
	public final void writeLong(long value) {
		if (type == TYPE_BINARY) {
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
			value >>>= 8;
			doWrite((int)(value & 255));
		} else {
			String number = String.valueOf(value);
			int len = number.length();
			
			if (type == TYPE_PPCCT) {
				for (int i = 0; i < len; i++) {
					doWrite(PingPong.fromAscii(number.charAt(i)));
				}
			} else {
				for (int i = 0; i < len; i++) {
					doWrite(number.charAt(i));
				}
			}
		}
	}


	public void close() {
		open = false;
		if (in != null) {
			try {
				in.close();
			} catch (Exception e) {};
			in = null;
		}
		if (out != null) {
			try {
				out.flush();
				out.close();
			} catch (Exception e) {};
			out = null;
		}
	}

	protected void finalize() {
		close();
	}
	
	
	public boolean canRead() {
		return ((in != null) || (seqAccess != null));
	}
	
	public boolean canWrite() {
		return ((out != null) || (seqAccess != null));
	}
	
}
