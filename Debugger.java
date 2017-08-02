public interface Debugger {
	
	public void processStarted(PingPong process);
	
	public void threadStarted(PingPongThread thread);

	public void step(PingPongThread thread);

	public void threadFinished(PingPongThread thread, int exitCode);
	
	public void notifyProcessFinish(PingPong process);

	public void processFinished(PingPong process, int exitCode);
	
}
