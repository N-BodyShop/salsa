/*
  A Python interface, synchronized with the code in libs/ck-libs/pythonCCS for
  the dynamic insertion of python code into running charm++ programs.

  by Filippo Gioachin, gioachin@uiuc.edu, 11/23/2004
*/

package charm.ccs;



public class PythonExecute extends PythonAbstract {
    private static final int localmagic = 37492037;
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
    private static final byte FLAG_WAIT = (byte)8;
    private static final byte FLAG_NOCHECK = (byte)4;

    public PythonExecute(String _code, boolean _persistent, boolean _highlevel, int _interp) {
	magic = memorySize ^ localmagic;
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
	magic = memorySize ^ localmagic;
	codeLength = _code.length();
	code = _code;
	methodNameLength = _method.length();
	methodName = _method;
	infoSize = _info.size();
	info = _info;
	flags = FLAG_ITERATE;
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

    public void setWait(boolean _set) {
	if (_set) flags |= FLAG_WAIT;
	else flags &= ~FLAG_WAIT;
    }

    public void setNoCheck(boolean _set) {
	if (_set) flags |= FLAG_NOCHECK;
	else flags &= ~FLAG_NOCHECK;
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

    public boolean isKeepWait() {
	return (flags & FLAG_WAIT) != 0;
    }

    public boolean isNoCheck() {
	return (flags & FLAG_NOCHECK) != 0;
    }

    public int getInterpreter() {
	return interpreter;
    }

    public int size() { return memorySize+codeLength+1+methodNameLength+1+infoSize; }

    public byte[] pack() {
	byte[] result = new byte[size()];
	System.out.println("Code: "+codeLength+", method: "+methodNameLength+", info: "+infoSize);
	CcsServer.writeInt(result, 0, magic);
	CcsServer.writeInt(result, 4, codeLength);
	CcsServer.writeInt(result, 24, methodNameLength);
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
