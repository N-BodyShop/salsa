/*
A non-blocking wrapper around CcsServer-- allows
asynchronous network communication easily.

by Orion Lawlor,  6/14/2001
olawlor@acm.org
*/

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import java.net.UnknownHostException;

class CcsThread implements Runnable {
	//public static Date startPoint;

	//Thin wrapper around a block of data
	public static class message {
		private byte[] data;

		public message(byte[] data_) {
			//startPoint = new Date();
			data=data_;
		}
		
		public message(int nBytes) {
			data=new byte[nBytes];
		}
		
		public byte[] getData() {
			return data;
		}
		
		protected void setData(byte[] data_) {
			data=data_;
		}
	}
	
	//Represents an outgoing CCS request
	public static class request extends message {
		int onPE = 0;
		String handler;

		public request(String handler_, byte[] data_) {
			super(data_);
			handler=handler_;
		}
		
		public request(String handler_, int nBytes) {
			super(nBytes);
			handler=handler_;
		}
	    
		public void setPE(int pe) {
			onPE=pe;
		}
		
		public int getPE() {
			return onPE;
		}
		
		public String getHandler() {
			return handler;
		}
		
		public void handleReply(byte[] data) {
			if (data.length!=0)
				System.out.println("Overload handleReply to actually use these "+data.length+" bytes of reply!");
		}
	}

	private boolean isBad;//Records that an error occured
	//private Stack requests;//Keeps track of CcsRequests
	private LinkedList requests;//Keeps track of CcsRequests
	private volatile boolean keepGoing;//To signal exit
	private CcsServer ccs;
	private Label status;//Place to show status info.
	private Thread myThread;

	//Initialization just stashes info-- 
	// real work starts when thread begins running.
	private String hostName;
	private int port;
	
	public CcsThread (Label status_, String hostName_, int port_) 
            throws UnknownHostException, IOException {
		requests = new LinkedList();
		status=status_;
		hostName=hostName_;
		port=port_;
		isBad=false;
		keepGoing=true;
		//Start our run method
		myThread=new Thread(this);
                ccs=new CcsServer(hostName,port);
		myThread.start();
	}
		
	public void addRequest(request req) {
		addRequest(req,false);
	}
	
	public void addRequest(request req, boolean flushOld) {
		if(flushOld) //Clean out all previous requests
			requests.clear();
		requests.addLast(req);
		//System.out.println("Ccs.Thread.java addRequest called");
	}
	
	public void finish() {
		keepGoing=false;
	}
	
	public boolean isInvalid() {
		return isBad;
	}
	
	private void ioError(Exception e,String what) {
		isBad=true;
		keepGoing=false;
		status.setText(what+" ("+hostName+":"+port+")");
		System.out.println("ERROR> "+what);
		System.out.println("Traceback: "+e);
		e.printStackTrace();
	}
		
	public void run() {
		status.setText("Connecting to "+hostName+":"+port+"...");
		
		if (!keepGoing) return;
		status.setText("Connected to "+hostName+" ("+
			       ccs.getNumPes()+" processors)");
		while (keepGoing) {
			while(requests.isEmpty() && keepGoing) {
				//Wait for another request
				try { //Give other threads a chance
					int sleepMs=30;
					myThread.sleep(sleepMs);
				} catch (InterruptedException E) {}
			}
			if (!keepGoing) break;
			request curReq=(request)requests.removeFirst();
			status.setText("Sending request "+curReq.getHandler());
			try {
				ccs.sendRequest(curReq.getHandler(),curReq.getPE(),curReq.getData());
			} catch(IOException e) {
				ioError(e,"Error sending request");
				break;
			}
			byte[] reply;
			try {
				reply=ccs.recvResponse();
			} catch(IOException e) {
				ioError(e,"Error receiving response");
				break;
			}
			curReq.handleReply(reply);
			
			status.setText("");
		}
	}
	/*
	private static void startTimer() {
		startPoint = new Date();
	}
	*/
}





