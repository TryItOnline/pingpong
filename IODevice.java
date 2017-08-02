
import java.io.*;
import java.net.*;

class IODevice extends Device{
	
	public static final int CONSOLE = 0;
	public static final int FILE = 1;
	public static final int RESOURCE = 2;
	public static final int PINGPONG_SPACE = 4;
	public static final int TCP_SOCKET = 8;
	public static final int UDP_SOCKET = 9;
	public static final int TCP_SERVER = 10;
	public static final int UDP_SERVER = 11;
	
	public PingPongStream stream;
	private Object device;
	private String locator;
	private int type;
	private boolean read;
	private boolean write;
	
	private void connect(int type, String locator, boolean input, boolean output) throws IOException {
		this.locator = locator;
		this.type = type;
		int port = 80;
		int pos;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			switch (type) {
			case CONSOLE:
				break;
				
			case FILE:
				if (input) {
					in = new FileInputStream(locator);
					device = in;
				} else
				if (output) {
					out = new FileOutputStream(locator);
					device = out;
				}
				stream = new PingPongStream(in, out, PingPongStream.TYPE_ASCII);
				break;
				
			case RESOURCE:
				URL url = new URL(locator);
				URLConnection conn = url.openConnection();
				conn.setDoInput(input);
				conn.setDoOutput(output);
				conn.connect();
				if (output) out = conn.getOutputStream();
				if (input) in = conn.getInputStream();
				device = conn;
				stream = new PingPongStream(in, out, PingPongStream.TYPE_ASCII);
				break;
				
			case TCP_SOCKET:
				pos = locator.indexOf(':');
				if (pos>=0) try {
					port = Integer.parseInt(locator.substring(pos+1));
					locator = locator.substring(0,pos);
				} catch (Exception e) {};
				Socket tcp = new Socket(locator,port);
				if (output) out = tcp.getOutputStream();
				if (input) in = tcp.getInputStream();
				device = tcp;
				stream = new PingPongStream(in, out, PingPongStream.TYPE_ASCII);
				break;
				
			case TCP_SERVER:
				try {
					port = Integer.parseInt(locator);
				} catch (Exception e) {};
				ServerSocket server = new ServerSocket(port);
				device = server;
				break;
				
			default:
				throw new IOException();
			}
		} finally {
			if (stream != null) {
				read = stream.canRead();
				write = stream.canWrite();
			}
		}
	}
	
	private void init(Object device, int type) {
		this.device = device;
		this.type = type;
		if (stream != null) {
			read = stream.canRead();
			write = stream.canWrite();
		}
	}
	
	public IODevice(int type, String locator, boolean input, boolean output) throws IOException {
		connect(type,locator,input,output);
	}
	
	public IODevice(int type, int[] locator, boolean input, boolean output) throws IOException {
		if (type==TCP_SOCKET || type==UDP_SOCKET) {
			StringBuffer addr = new StringBuffer();
			if (locator.length<4) throw new IOException();
			for (int i=0; i<4; i++) {
				addr.append(locator[i]);
				if (i<3) addr.append('.'); else addr.append(':');
			}
			if (locator.length>4) addr.append(locator[5]); else addr.append("80");
			connect(type,addr.toString(),input,output);
		} else throw new IOException();
	}
	
	public IODevice(PingPongStream stream, Object device, int type) {
		this.stream = stream;
		init(device, type);
	}
	
	public IODevice(boolean input, boolean output, HugeArray device,
			int x, int y, int dir, int type) throws IOException {

		try {
			switch (type) {
			case PINGPONG_SPACE:
				HugeArray array = (HugeArray)device;
				stream = new PingPongStream(array.getSequentialAccess(x, y), dir, PingPongStream.TYPE_PPCCT);
				break;
			default:
				throw new IOException();
			}
		} catch (ClassCastException e) {
			throw new IOException();
		} finally {
			init(device, type);
		}
	}
	
	public IODevice(boolean input, boolean output, Object device, int type) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		
		try {
			switch (type) {
			case TCP_SERVER:
				break;
			case TCP_SOCKET:
				Socket tcp = (Socket)device;
				if (output) out = tcp.getOutputStream();
				if (input) in = tcp.getInputStream();
				stream = new PingPongStream(in, out, PingPongStream.TYPE_ASCII);
				break;
			default:
				throw new IOException();
			}
		} catch (ClassCastException e) {
			throw new IOException();
		} finally {
			init(device, type);
		}
	}
	
	public boolean canRead() {return read;}
	public boolean canWrite() {return write;}
	
	public Object getDevice() {return device;}
	

	protected void newReference() {
	}
	
	protected void destroyReference() {
	}

	protected void destroy() {
		try {
			switch (type) {
				
			case CONSOLE:
				break;
				
			case FILE:
				//if (in!=null) ((FileInputStream)device).close();
				//if (out!=null) ((FileOutputStream)device).close();
				break;
				
			case RESOURCE:
				break;
				
			case TCP_SOCKET:
				((Socket)device).close();
				break;
				
			case UDP_SOCKET:
				break;
				
			case TCP_SERVER:
				((ServerSocket)device).close();
				break;
			}
		} catch (Exception e) {};
		
		if (stream != null) {
			stream.close();
			stream = null;
		}
		
		device = null;
		read = false;
		write = false;
	}
	

	public String toString() {
		return "Device: ["+device+", "+stream+", "+locator+", "+type+"]";
	}
}