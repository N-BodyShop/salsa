import java.util.*;

public class DelimitedStringEnumeration implements Enumeration {
	private String s;
	private char delimiter;
	private int index = 0;
	private int lastindex = 0;
	
	public DelimitedStringEnumeration(String s, char delimiter) {
		this.s = new String(s);
		this.delimiter = delimiter;
	}
	
	public DelimitedStringEnumeration(String s) {
		this(s, ',');
	}
	
	public boolean hasMoreElements() {
System.err.println("length: " + s.length() + " index: " + index + "\n");
		return index < s.length() - 1;
	}
	
	public Object nextElement() {
System.err.println("string:" + s + " index " + index + "\n");
	    if(index != 0) lastindex = index + 1;
	    index = s.indexOf(delimiter, lastindex);
System.err.println("lastindex: " + lastindex + " index " + index + "\n");
	    if(index < 0) index = s.length();
	    return s.substring(lastindex, index);
	}
}
