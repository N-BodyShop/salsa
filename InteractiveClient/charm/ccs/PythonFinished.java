/*
  A Python interface, synchronized with the code in libs/ck-libs/pythonCCS for
  the dynamic insertion of python code into running charm++ programs.

  by Filippo Gioachin, gioachin@uiuc.edu, 11/23/2004
*/

package charm.ccs;



public class PythonFinished extends PythonAbstract {
    private static final int localmagic = 738963580;
    private int interpreter;
    private byte flags;

    private static final int memorySize = 12;

    private static final byte FLAG_WAIT = (byte)128;

    public PythonFinished(int _interp, boolean _wait) {
	magic = memorySize ^ localmagic;
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
