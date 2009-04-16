//SalsaRequests.java

import java.util.Vector;

import charm.ccs.CcsThread;


public class SalsaRequests {

	public static class ShutdownServer extends CcsThread.request {
		public ShutdownServer() {
			super("ShutdownServer", (byte[])null);
		}
		
		public void handleReply(byte[] data) {
			if(data[0] != 1)
				System.err.println("Error shutting down server, client quitting anyway");
		}
	}

	public static class ListSimulations extends CcsThread.request {
		Vector simulationList = new Vector();

    	public ListSimulations() {
			super("ListSimulations", (byte[])null);
    	}

    	public void handleReply(byte[] data) {
        	String reply = new String(data);
        	simulationList = new Vector();
        	int index = -1;
        	int lastindex = 0;
        	while(index < reply.length() - 1) {
            	lastindex = index + 1;
            	index = reply.indexOf(",", lastindex);
            	simulationList.addElement(reply.substring(lastindex, index));
        	}
    	}
	}
	
	static public class ChooseSimulation extends CcsThread.request {
		boolean status = false;

    	public ChooseSimulation() {
        	super("ChooseSimulation", (byte[])null);
    	}

    	public ChooseSimulation(String name) {
        	super("ChooseSimulation", name.getBytes());
    	}

    	public void handleReply(byte[] data) {
			status = (data != null) && (data[0] == 1);
    	}
	}
}
