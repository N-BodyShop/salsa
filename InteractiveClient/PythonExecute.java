/*
  A Python interface, synchronized with the code in libs/ck-libs/pythonCCS for
  the dynamic insertion of python code into running charm++ programs.

  by Filippo Gioachin, gioachin@uiuc.edu, 11/23/2004
*/

class PythonIterator {
    int size() { return 0; };
    byte[] pack() {return null;};
    void unpack() {};
}

class PythonAbstract {
    protected int magic;
}

class PythonExecute extends PythonAbstract {
    private int codeLength;
    private String code;

    private int methodNameLength;
    private String methodName;

    private int infoSize;
    private PythonIterator info;

    private int interpreter;
    private byte flags;

    private static final int memorySize = 48;

    private static final byte FLAG_PERSISTENT = (byte)128;
    private static final byte FLAG_KEEPPRINT = (byte)64;
    private static final byte FLAG_HIGHLEVEL = (byte)32;
    private static final byte FLAG_ITERATE = (byte)16;

    public PythonExecute(String _code, boolean _persistent, boolean _highlevel, int _interp) {
	magic = memorySize;
	codeLength = _code.length();
	code = _code;
	methodNameLength = 0;
	infoSize = 0;
	flags = 0;
	if (_persistent) {
	    flags |= FLAG_PERSISTENT;
	    flags |= FLAG_KEEPPRINT;
	}
	if (_highlevel) flags |= FLAG_HIGHLEVEL;
	interpreter = _interp;
    }

    public PythonExecute(String _code, String _method, PythonIterator _info, boolean _persistent, boolean _highlevel, int _interp) {
	magic = memorySize;
	codeLength = _code.length();
	code = _code;
	methodNameLength = _method.length();
	methodName = _method;
	infoSize = _info.size();
	info = _info;
	flags = 0;
	if (_persistent) {
	    flags |= FLAG_PERSISTENT;
	    flags |= FLAG_KEEPPRINT;
	}
	if (_highlevel) flags |= FLAG_HIGHLEVEL;
	interpreter = _interp;
    }

    public void setCode(String _set) {
	codeLength = _set.length();
	code = _set;
    }

    public void setMethodName(String _set) {
	methodNameLength = _set.length();
	methodName = _set;
    }

    public void setIterator(PythonIterator _set) {
	infoSize = _set.size();
	info = _set;
    }

    public void setPersistent(boolean _set) {
	if (_set) flags |= FLAG_PERSISTENT;
	else flags &= ~FLAG_PERSISTENT;
    }

    public void setIterate(boolean _set) {
	if (_set) flags |= FLAG_ITERATE;
	else flags &= ~FLAG_ITERATE;
    }

    public void setHighLevel(boolean _set) {
	if (_set) flags |= FLAG_HIGHLEVEL;
	else flags &= ~FLAG_HIGHLEVEL;
    }

    public void setKeepPrint(boolean _set) {
	if (_set) flags |= FLAG_KEEPPRINT;
	else flags &= ~FLAG_KEEPPRINT;
    }

    public void setInterpreter(int i) {
	interpreter = i;
    }

    public boolean isPersistent() {
	return (flags & FLAG_PERSISTENT) != 0;
    }

    public boolean isIterate() {
	return (flags & FLAG_ITERATE) != 0;
    }

    public boolean isHighLevel() {
	return (flags & FLAG_HIGHLEVEL) != 0;
    }

    public boolean isKeepPrint() {
	return (flags & FLAG_KEEPPRINT) != 0;
    }

    public int getInterpreter() {
	return interpreter;
    }

    public int size() { return memorySize+codeLength+1+methodNameLength+1+infoSize; }

    public byte[] pack() {
	byte[] result = new byte[size()];
	CcsServer.writeInt(result, 0, magic);
	CcsServer.writeInt(result, 4, codeLength);
	CcsServer.writeInt(result, 16, methodNameLength);
	CcsServer.writeInt(result, 28, infoSize);
	CcsServer.writeInt(result, 40, interpreter);
	result[44] = flags;
	// calling with code.length()+1 has the effect of placing a zero at the end
	CcsServer.writeString(result, 48, codeLength+1, code);
	if (methodNameLength>0)
	    CcsServer.writeString(result, 48+codeLength+1, methodNameLength+1, methodName);
	if (infoSize>0)
	    CcsServer.writeBytes(result, 48+codeLength+methodNameLength+2, infoSize, info.pack());
	return result;
    }

    // unpack is not needed by the client, so it's not implemented in the java interface
    public void unpack() {}

    public void print() {}

}

class PythonPrint extends PythonAbstract {
    private int interpreter;
    private byte flags;

    private static final int memorySize = 12;

    private static final byte FLAG_WAIT = (byte)128;

    public PythonPrint(int _interp, boolean _wait) {
	magic = memorySize;
	interpreter = _interp;
	flags = 0;
	if (_wait) flags |= FLAG_WAIT;
    }

    public void setWait(boolean _set) {
	if (_set) flags |= FLAG_WAIT;
	else flags &= ~FLAG_WAIT;
    }

    public boolean isWait() {
	return (flags & FLAG_WAIT) != 0;
    }

    public byte[] pack() {
	byte[] result = new byte[memorySize];
	CcsServer.writeInt(result, 0, magic);
	CcsServer.writeInt(result, 4, interpreter);
	result[8] = flags;
	return result;
    }

    public void print() {System.out.println("b");}

}

/*
class testmain {
public static void main (String[] argv) {
    PythonExecute b = new PythonExecute("prova", false, true, 10);
    PythonPrint a = new PythonPrint(4, true);
    PythonPrint d = new PythonPrint(4, true);
    PythonExecute c = new PythonExecute("prova", false, true, 10);
    System.out.println(a);
    System.out.println(d);
    byte[] r = a.pack();
    System.out.println(r[0]+" "+r[1]+" "+r[2]+" "+r[3]+" "+r[4]+" "+r[5]+" "+r[6]+" "+r[7]+" "+r[8]+" "+r[9]+" "+r[10]+" "+r[11]);
    a.print();
    b.print();
}
}
*/
