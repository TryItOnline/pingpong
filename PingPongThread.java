
import java.io.*;
import java.util.*;
import java.net.*;


public class PingPongThread implements Runnable {
	
	private static final Random rnd = new Random();
	
	private LongStack stack = new LongStack();
	private LongStack helpStack = new LongStack();
	private Stack contextStack = new Stack();
	
	private HugeArray.SeqAccess ip;
	private int dir;
	private Context context;
	private PingPong master;
	private HugeArray space;
	
	private boolean escaped = false;
	
	private boolean halt = false;
	private boolean quit = false;
	private boolean pause = false;
	private int exitCode;
	
	private PingPongStream in;
	private PingPongStream out;
	
	private List handles;
	
	private Debugger debugger;
	private boolean debug;
	private Object debugInfo;
	private int id;

	
	// primaray thread
	public PingPongThread(PingPong master, HugeArray space, int x, int y, int dir,
			Debugger debugger, boolean debug) {

		this.master = master;
		this.space = space;
		this.context = new Context(0, 0, 0, 0);
		this.ip = space.getSequentialAccess(x, y);
		this.dir = dir;
		handles = new ArrayList();
		PingPongStream console = new PingPongStream(System.in, System.out, PingPongStream.TYPE_ASCII);
		addIODevice(new IODevice(console, null, IODevice.CONSOLE), true, true);
		this.stack = new LongStack();
		this.helpStack = new LongStack();
		this.contextStack = new Stack();
		this.debugger = debugger;
		this.debug = debug;
		id = master.registerThread(this);
	}
	
	// forked thread
	private PingPongThread(PingPongThread pattern) {
		master = pattern.master;
		space = pattern.space;
		context = pattern.context;
		ip = (HugeArray.SeqAccess)pattern.ip.clone();
		dir = pattern.dir;
		handles = new ArrayList(pattern.handles);
		for (Iterator iter=handles.iterator(); iter.hasNext();) {
			Device obj = (Device)iter.next();
			obj.copied();
		}
		in = pattern.in;
		out = pattern.out;
		stack = (LongStack)pattern.stack.clone();
		helpStack = (LongStack)pattern.helpStack.clone();
		contextStack = (Stack)pattern.contextStack.clone();
		debugger = pattern.debugger;
		debug = pattern.debug;
		id = master.registerThread(this);
	}
	
	
	// Api-Functions
	private final PingPongStream getIODeviceIn(int num) {
		IODevice dev;
		try {
			dev = (IODevice)handles.get(num);
			if (!dev.canRead()) {
				return (num == 0) ? null : getIODeviceIn(0);
			} else {
				return dev.stream;
			}
		} catch (ClassCastException e) {
			return (num == 0) ? null :  getIODeviceIn(0);
		}
	}
	
	private final PingPongStream getIODeviceOut(int num) {
		IODevice dev;
		try {
			dev = (IODevice)handles.get(num);
			if (!dev.canWrite()) {
				return (num == 0) ? null : getIODeviceOut(0);
			} else {
				return dev.stream;
			}
		} catch (ClassCastException e) {
			return (num == 0) ? null :  getIODeviceOut(0);
		}
	}
	
	private final int addHandle(Device dev) {
		for (int i=0; i<handles.size(); i++) {
			if (handles.get(i)==null) {
				handles.set(i,dev);
				return i;
			}
		}
		handles.add(dev);
		return handles.size()-1;
	}
	
	private final int addIODevice(IODevice dev, boolean read, boolean write) {
		if (dev==null) return -1;
		if (read && dev.canRead()) in = dev.stream;
		if (write && dev.canWrite()) out = dev.stream;
//		System.err.println("addDevice: "+dev.toString());
		return addHandle(dev);
	}
	
	private final void removeHandle(int num, boolean removeall) {
		if (num <= 0) return;
		Device obj = (Device)handles.set(num,null);
//		System.err.println("remove: num=" + num + ": " + obj);
		if (obj == null) return;
		if (obj instanceof IODevice) {
			IODevice tmp = (IODevice)obj;
			IODevice def = (IODevice)handles.get(0);
			if (in==tmp.stream) in = def.stream;
			if (out==tmp.stream) out = def.stream;
		}
		if (removeall) {
			obj.closeAll();
		} else {
			obj.close();
		}
	}
	
	private final void apiFunction(int num) {
		boolean read = false;
		boolean write = false;
		int a, b, c;
		String name;
		
		if (num>0 && num<16 && num!=9) {		// access-mode for IO-Device
			a = (int)stack.pop();
			read = (a & 1) > 0;
			write = (a & 2) > 0;
		}
		
		//System.err.println("API (" + num + "): " + stack.top());
		
		try {
			switch (num) {
			case -1:	// Close device for all threads
				a = (int)stack.pop();
				removeHandle(a,true);
				break;
			case 0:		// Close device for current thread
				a = (int)stack.pop();
				removeHandle(a,false);
				break;
			case 1:		// FILE
				stack.push(addIODevice(new IODevice(IODevice.FILE,popString(),read,write),read,write));
				break;
			case 2:		// RESOURCE
				stack.push(addIODevice(new IODevice(IODevice.RESOURCE,popString(),read,write),read,write));
				break;
			case 4:		// PingPongSpace - Stream
				a = (int)stack.pop()+context.x;
				b = (int)stack.pop()+context.y;
				c = (int)stack.pop();
				stack.push(addIODevice(new IODevice(read,write,space,a,b,c,IODevice.PINGPONG_SPACE),read,write));
				break;
			case 8:		// TCP_SOCKET (String)
				stack.push(addIODevice(new IODevice(IODevice.TCP_SOCKET,popString(),read,write),read,write));
				break;
			case 9:		// TCP_SERVER
				stack.push(addIODevice(new IODevice(
						IODevice.TCP_SERVER,Integer.toString((int)stack.pop()),false,false),false,false));
				break;
				
			case 10:	// TCP_SOCKET one time listen
				try {
					a = (int)stack.pop();
					ServerSocket socket = (ServerSocket)((IODevice)handles.get(a)).getDevice();
					Socket tcp = socket.accept();
					if (tcp==null) {
						stack.push(-1);
						return;
					}
					stack.push(addIODevice(new IODevice(read,write,tcp,IODevice.TCP_SOCKET),read,write));
				} catch (Exception e) {stack.push(-1);}
				break;
				
			case 11:	// TCP_SOCKET multi listening
				int count = -1;
				try {
					ip.moveSkipping(dir);
					a = (int)stack.pop();
					ServerSocket socket = (ServerSocket)((IODevice)handles.get(a)).getDevice();
					count = 0;
					Socket tcp;
					do {
						tcp = socket.accept();
						if (tcp!=null) {
							PingPongThread thread = new PingPongThread(this);
							thread.stack.push(thread.addIODevice(
									new IODevice(read,write,tcp,IODevice.TCP_SOCKET),read,write));
							new Thread(thread, Thread.currentThread().getName() + "-tcp").start();
							count++;
						}
					} while (tcp!=null);
				} catch (IOException e) {
				} finally {
					stack.push(count);
				}
				break;
				
			case 15:	// sets stdin and/or stdout
				b = (int)stack.pop();
				if (read) in = getIODeviceIn(b);
				if (write) out = getIODeviceOut(b);
				break;
			case 16:	// create SYNC
				stack.push(addHandle(new Sync(false)));
				break;
			case 17:	// create active SYNC (owned MUTEX / triggered EVENT)
				stack.push(addHandle(new Sync(true)));
				break;
			case 18:	// get MUTEX
				((Sync)handles.get((int)stack.pop())).get();
				break;
			case 19:	// release MUTEX
				((Sync)handles.get((int)stack.pop())).release();
				break;
			case 24:	// set EVENT
				((Sync)handles.get((int)stack.pop())).set();
				break;
			case 25:	// reset EVENT
				((Sync)handles.get((int)stack.pop())).reset();
				break;
			case 26:	// pulse EVENT
				((Sync)handles.get((int)stack.pop())).pulse();
				break;
			case 27:	// pulse single thread on EVENT
				((Sync)handles.get((int)stack.pop())).pulseSingle();
				break;
			case 28:	// waitfor EVENT
				((Sync)handles.get((int)stack.pop())).eventWait();
				break;
			case 32:	// sleep
				Thread.sleep(stack.pop());
				break;
			case 48:	// load text file (overwrite)
				name = popString();
				a = (int)stack.pop()+context.x;
				b = (int)stack.pop()+context.y;
				master.load(name, a, b, false);
				break;
			case 49:	// load text file (underwrite)
				name = popString();
				a = (int)stack.pop()+context.x;
				b = (int)stack.pop()+context.y;
				master.load(name, a, b, true);
				break;
			case 50:	// create PingPong-Space
				name = popString();
				stack.push(-1);	// not yet implemented
				break;
			}
		} catch (IOException e) {stack.push(-1);
		} catch (ClassCastException e) {
		} catch (InterruptedException e) {
		} catch (Exception e) {};
	}

	
	private final String popString() {
		StringBuffer str = new StringBuffer();
		int a = 0;
		int size = stack.size();
		while ((a>=0) && (size-- > 0)) {
			a = (int)stack.pop();
			if (a>=0) str.append((char)PingPong.toAscii(a));
		}
		//System.err.println("popString: "+str);
		return str.toString();
	}
	

	


// --------- Source interpreter -------------------

	private final void interpret() {

		int data = ip.read();
		
		long a, b, c;
		int i, j, k;

		boolean suppressMove = false;
		boolean preserveData = false;
		
		while (true) {
			if (pause) waiting();

			if (escaped) {
				escaped = false;
				stack.push(data);
				data = ip.moveSkipping(dir);
			}

			while (((data == 192) || (data == 226)) && !quit) { // skip all nOps and noPs
				data = ip.moveSkipping(dir);
			}

			if (debug) {
				debugger.step(this);
				if (quit || halt) break;
				if (!preserveData) {
				    data = ip.readInvalidate();
				} else {
				    preserveData = false;
				}
			}
		
			switch (data) {
			// arithmetic operations:
			case 203: // +
				stack.push(stack.pop()+stack.pop());
				break;
			case 205: // -
				b = stack.pop();
				a = stack.pop();
				stack.push(a-b);
				break;
			case 202: // *
				stack.push(stack.pop()*stack.pop());
				break;
			case 197: // %
				b = stack.pop();
				a = stack.pop();
				if (b != 0) {
					stack.push(a/b);
					stack.push(a%b);
					break;
				} else {
					if (a == 0) stack.push(rnd.nextLong()); else
					if (a < 0)  stack.push(Long.MIN_VALUE); else
								stack.push(Long.MAX_VALUE);
					stack.push(a);
					break;
				}
			case 198: // &
				stack.push(stack.pop() & stack.pop());
				break;
			case 224: // ~
				stack.push(~stack.pop());
				break;
			
			// stack-Operations:
			case 240: // °
				stack.pop();
				break;
			case 194: // "
				a = stack.pop();
				stack.push(a);
				stack.push(a);
				break;
			case 218: // ^
				a = stack.pop();
				b = stack.pop();
				stack.push(a);
				stack.push(b);
				break;
			case 220: // `
				helpStack.push(stack.pop());
				break;
			case 199: // '
				stack.push(helpStack.pop());
				break;
				
			// context
			case 200: // (
				contextStack.push(context);
				context = new Context(ip.getX(), ip.getY(), stack.size(), helpStack.size());
				break;
			case 201: // )
				if (contextStack.size() > 0) {
					context = (Context)contextStack.pop();
				}
				stack.setSize(context.stackSize);
				helpStack.setSize(context.helpStackSize);
				break;
				
			// (contitional) skips:
			case 210: // <
				if (stack.top()<0) ip.moveSkipping(dir);
				break;
			case 212: // >
				if (stack.top()>0) ip.moveSkipping(dir);
				break;
			case 211: // =
				if (stack.top()==0) ip.moveSkipping(dir);
				break;
			case 195: // #
				ip.moveSkipping(dir);
				break;
				
			// escape next character
			case 196: // $
				escaped = true;
				break;
				
			// execute instruction on stack
			case 230: // ¤
				suppressMove = true;
				preserveData = true;
				data = (int)stack.pop();
				break;
				
			// IO-functions
			case 209: // ;
				stack.push(in.readLong());
				break;
			case 204: // ,
				stack.push(in.read());
				break;
			case 208: // :
				out.writeLong(stack.pop());
				break;
			case 206: // .
				out.write((int)stack.pop());
				break;
				
			// memory access
			case 213: // ?
				i = (int)stack.pop()+context.x;
				j = (int)stack.pop()+context.y;
				stack.push(space.get(i, j));
				break;
			case 193: // !
				k = (int)(stack.pop()%65536);
				i = (int)stack.pop()+context.x;
				j = (int)stack.pop()+context.y;
				space.set(i, j, k);
				break;
				
			// subroutine call
			case 215: // [
				stack.pop();	// ignore space-id
				i = (int)stack.pop()+context.x;
				j = (int)stack.pop()+context.y;
				stack.push(ip.getY()-context.y);
				stack.push(ip.getX()-context.x);
				stack.push(0);
				data = ip.move(i, j);
				suppressMove = true;
				break;
			// return / jump
			case 217: // ]
				stack.pop();	// ignore space-id
				i = (int)stack.pop()+context.x;
				j = (int)stack.pop()+context.y;
				ip.move(i, j);
				break;

			// thread functions
			case 221: // {
				ip.moveSkipping(dir);
				PingPongThread thread = new PingPongThread(this);
				new Thread(thread).start();
				break;
			case 223: // }
				exitCode = (int)stack.pop();
				quit = true;
				break;
				
			// API function calls
			case 233: // §
				apiFunction((int)stack.pop());
				break;
				
			// execution direction
			case 207: // /
				dir ^= 1;
				break;
			case 216: // \
				dir ^= 3;
				break;
			case 219: // _
				if ((dir & 1) == 0) dir ^= 2;
				break;
			case 222: // |
				if ((dir & 1) == 1) dir ^= 2;
				break;
			case 192: // <sp>		// nOp
			case 226: // <nbsp>		// noP
				break;
				
			// end of program
			case 214: // @
				exitCode = (int)stack.pop();
				halt = true;
				break;
				
			// all other chars are pushed
			default:
				//System.err.println(this.toString() + "-" + ip + " - pushing: " +
				//		data + " (" + (char)PingPong.toAscii(data) + ")");
				stack.push(data);
			}
		
		
			if (quit || halt) break;
			
			if (!suppressMove) {
				data = ip.moveSkipping(dir);
			} else {
				suppressMove = false;
			}
			
		}
	}

	private synchronized void waiting() {
		while (pause) {
			try {
				wait();
			} catch (InterruptedException e) {};
		}
	}
	
	public void run() {
		//System.err.println("new thread: " + ip);
		try {
			interpret();
		} catch (OutOfMemoryError e) {
			error(null, "Please insert some more RAM and try again!");
		} catch (Throwable e) {
			error(e, "An unexpected error! - Hey, can you repeat this?\n" +
					"Please send this message and your source file to " +
					PingPong.APP_FEEDBACK);
		}
		
		//System.err.println("de-registering");
		master.deregisterThread(this, exitCode);
		if (halt) master.terminating(exitCode);
		for (int i=0; i<handles.size(); i++) removeHandle(i,halt);
	}
	
	private void error(Throwable t, String message) {
		System.err.println(message);
		System.err.println("Position: (space, x, y) = (0, " + ip.getX() + ", " + ip.getY() + ")");
		System.err.println("Stack:");
		int size = stack.size();
		for (int i = 0; i < 8; i++) {
			if (size == 0) {
				System.err.println("<<< bottom >>>");
				break;
			}
			System.err.println("[" + (size--) + "]: " + stack.pop());
		}
		
		if (t != null) {
			System.err.println();
			t.printStackTrace();
		}
		System.exit(-1);
	}
	
	public void terminating() {
		//System.err.println("terminating");
		quit = true;
		for (int i=0; i<handles.size(); i++) removeHandle(i,true);
		//System.err.println("terminating");
	}
	
	public void suspend() {
		pause = true;
	}
	
	public synchronized void resume() {
		pause = false;
		notifyAll();
	}


	// methods for debuggers
	public int getExitCode() {
		return exitCode;
	}
	
	public PingPong getProcess() {
		return master;
	}

	
	public void setDebugInfo(Object info) {
		debugInfo = info;
	}
	
	public Object getDebugInfo() {
		return debugInfo;
	}

	
	public SequentialAccess getIp() {
		return ip;
	}
	
	public int getDirection() {
		return dir;
	}
	
	public void setDirection(int dir) {
		this.dir = dir & 3;
	}
	
	
	public LongStack getStack() {
		return stack;
	}
	
	public LongStack getHelpStack() {
		return helpStack;
	}

	public Stack getContextStack() {
		return contextStack;
	}
	
	public Context getContext() {
		return context;
	}
	
	public int getId() {
		return id;
	}
	

	public void setDebugEnabled(boolean debug) {
		this.debug = (debugger != null) & debug;
	}
	
}