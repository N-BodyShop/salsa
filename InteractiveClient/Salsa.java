//
//  Salsa.java
//  
//
//  Created by Greg Stinson on Tue Sep 23 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.io.*;
import java.net.UnknownHostException;
import java.util.Vector;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import charm.ccs.CcsThread;

public class Salsa {
	CcsThread ccs;
	Vector simulationList;
	ChooseSimulationFrame csf;
	
	public Salsa(String hostname, int port) throws UnknownHostException, IOException {
		ccs = new CcsThread(new Label(), hostname, port);
	}
	
	public void connectSimulation() {
			new WindowManager(ccs);
	}
	public void chooseSimulation(String name) {
		SalsaRequests.ChooseSimulation cs = new SalsaRequests.ChooseSimulation(name);
		ccs.doBlockingRequest(cs);
		if(cs.status)
			new WindowManager(ccs);
		else {
			System.err.println("Couldn't choose simulation from server");
			ccs.doBlockingRequest(new SalsaRequests.ShutdownServer());
			System.exit(1);
		}
	}
	
	public void getSimulationList() {
		SalsaRequests.ListSimulations ls = new SalsaRequests.ListSimulations();
		ccs.doBlockingRequest(ls);
		simulationList = ls.simulationList;
	}
	
	public void displaySimulationList() {
		csf = new ChooseSimulationFrame(simulationList);
		csf.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
    			if(csf.chosenSimulation == null) {
					ccs.doBlockingRequest(new SalsaRequests.ShutdownServer());
					System.exit(1);
				} else
					chooseSimulation(csf.chosenSimulation);
			}
		});
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		csf.setLocation(screenSize.width / 2 - csf.getSize().width / 2, screenSize.height / 2 - csf.getSize().height / 2);
		csf.setVisible(true);
	}
	
	public static void main(String[] args) {
		if(args.length < 2) {
			System.err.println("Usage: java -jar Salsa.jar hostname port [simulation_name]");
			return;
		}
		try {
			Salsa salsa = new Salsa(args[0], Integer.parseInt(args[1]));
			if(args.length > 2)
				salsa.chooseSimulation(args[2]);
			else {
			//	salsa.getSimulationList();
			//	salsa.displaySimulationList();
				salsa.connectSimulation();
			}
		} catch(UnknownHostException uhe) {            
			System.err.println("Couldn't find host " + args[0] + ":" + args[1]);
		} catch(IOException ioe) {
			System.err.println("Couldn't connect to " + args[0] + ":" + args[1]);
		}    
	}
}
