//InteractiveClient.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class InteractiveClient {
	
	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Usage: InteractiveClient <ccs server> <ccs port>\n");
			System.exit(1);
		}
		String hostname = args[0];
		int port;
		try {
			port = Integer.parseInt(args[1]);
		} catch(NumberFormatException e) {
			port = 1234;
			System.out.println("Invalid port number, using default (" + port + ")");
		}
		
		CcsThread ccs = null;
		
		ccs = new CcsThread(new Label(), hostname, port);
		
		/*
		InteractiveWindow window = new InteractiveWindow(ccs);
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		window.pack();
		*/
		
		JFrame frame = new JFrame("NChilada Visualization");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.getContentPane().add(new ViewingPanel(hostname, port));
		frame.pack();
		
		//window.setVisible(true);
		frame.setVisible(true);
		
	}
	
}
