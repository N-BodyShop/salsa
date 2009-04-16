// Class to send off python, handle server response, pick up the
// printed output, then send the output to handlePrint()

import charm.ccs.CcsServer;
import charm.ccs.CcsThread;
import charm.ccs.PythonAbstract;
import charm.ccs.PythonExecute;
import charm.ccs.PythonPrint;
import charm.ccs.PythonFinished;

public class HighLevelPython 
{
    CcsThread ccs;
    PyPrintHandler handler;
    int interpreterHandle;
    
    public HighLevelPython(PythonExecute request,
				  CcsThread ccsThread,
				  PyPrintHandler pyprintHandler) {
	ccs = ccsThread;
	handler = pyprintHandler;
	request.setKeepPrint(true);
	ccs.addRequest(new PythonRequest(request.pack(), handler));
    }
    
	private class PythonRequest extends CcsThread.request {
	    PyPrintHandler handler;
		public PythonRequest(byte[] s, PyPrintHandler handler_) {
			super("ExecutePythonCode", s);
			handler = handler_;
		}
		
	    // ExecutePythonCode Replies:
	    // interpreterHandle: either handle of existing
	    // interpreter (if we asked for one), or handle of a new
	    // interpreter.  0xFFFFFFFF if we asked for an interpreter
	    // that is in use.
	    // From python print:
	    // 0 if print request on unknown interpreter
	    // string if valid print request
	    public void handleReply(byte[] data) {
		    System.err.println("In handle Reply\n");
		    // data will be 4 bytes... convert them to an int
		    interpreterHandle = CcsServer.readInt(data, 0);
		    System.err.println("Request print " + interpreterHandle);
		    ccs.addRequest(new PythonPrintRequest((new PythonPrint(interpreterHandle, true)).pack(), handler));
	    }
	}

	private class PythonPrintRequest extends CcsThread.request {
	    PyPrintHandler handler;
	    public PythonPrintRequest(byte[] s, PyPrintHandler handler_) {
		    super("ExecutePythonCode", s);
		    handler = handler_;
	    }
	    public void handleReply(byte[] data) {
		String result = new String(data);
		System.err.println("In handle Print Reply:" + result);
		if (result.length() > 0) {
		    handler.handle(result);
		    }
		//System.out.println("Return from code execution: \"" + result + "\"");
	    }
	}
    }
