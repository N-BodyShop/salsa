/*
  A Python interface, synchronized with the code in libs/ck-libs/pythonCCS for
  the dynamic insertion of python code into running charm++ programs.

  by Filippo Gioachin, gioachin@uiuc.edu, 11/23/2004
*/

package charm.ccs;



public class PythonPrint extends PythonAbstract {
    private static final int localmagic = 989370215;
    private int interpreter;
    private byte flags;

    private static final int memorySize = 12;

    private static final byte FLAG_WAIT = (byte)128;
    private static final byte FLAG_KILL = (byte)64;

    public PythonPrint(int _interp, boolean _wait) {
	magic = memorySize ^ localmagic;
	interpreter = _interp;
	flags = 0;
	if (_wait) flags |= FLAG_WAIT;
    }

    public void setWait(boolean _set) {
	if (_set) flags |= FLAG_WAIT;
	else flags &= ~FLAG_WAIT;
    }

    public void setKill(boolean _set) {
	if (_set) flags |= FLAG_KILL;
	else flags &= ~FLAG_KILL;
    }

    public boolean isWait() {
	return (flags & FLAG_WAIT) != 0;
    }

    public boolean isKill() {
	return (flags & FLAG_KILL) != 0;
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
