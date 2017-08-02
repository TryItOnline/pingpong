import java.util.*;


public class Mapping {
    
    private byte[] toRef;
    private byte[] fromRef;
    private int size;
    
    // descr ::= <size>[':'<range-list>]
    // range-list ::= <range>[','<range-list>]
    // range ::= <number>|<number>'-'<number>
    public Mapping(String descr) {
        try {
            StringTokenizer tok = new StringTokenizer(descr, ":,");
        
            // first token is size of map
            String tmp = tok.nextToken();
            size = Integer.parseInt(tmp);
        
            toRef = new byte[size];
            fromRef = new byte[size];
            int pos = 0;
            int i;
            int from, to;
            
            // ranges
            while (tok.hasMoreTokens()) {
                tmp = tok.nextToken();
                if ((i = tmp.indexOf('-')) >= 0) {
                    from = Integer.parseInt(tmp.substring(0, i));
                    to = Integer.parseInt(tmp.substring(i + 1));
                } else {
                    from = to = Integer.parseInt(tmp);
                }
                
                //System.err.println(pos + ": " + from + "-" + to);
                
                while (from <= to) {
                    toRef[pos] = (byte)from;
                    fromRef[from] = (byte)pos;
                    from++;
                    pos++;
                }
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    public final int toReference(int val) {
        return ((val < 0) || (val >= size)) ? val : ((int)toRef[val] & 255);
    }
    
    public final int fromReference(int val) {
        return ((val < 0) || (val >= size)) ? val : ((int)fromRef[val] & 255);
    }
    
}
