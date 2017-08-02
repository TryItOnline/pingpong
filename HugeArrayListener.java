import java.util.*;


public interface HugeArrayListener extends EventListener {
	
	public void hugeArrayChanged(HugeArray source, int x, int y, int oldVal, int newVal);

}
