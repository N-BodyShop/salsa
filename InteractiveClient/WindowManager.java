//WindowManager.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

public class WindowManager extends WindowAdapter {
	CcsThread ccs;
	Simulation sim = new Simulation();
	
	AttributeManager attributeManager;
	ColoringManager coloringManager;
	GroupManager groupManager;
	LinkedList windowList = new LinkedList();
	
	DefaultComboBoxModel coloringModel = new DefaultComboBoxModel();
	
	public WindowManager(CcsThread ccsThread) {
		ccs = ccsThread;
		
		ccs.doBlockingRequest(new GetAttributeInformation());
		attributeManager = new AttributeManager(this);
		attributeManager.addWindowListener(this);
		
		ccs.doBlockingRequest(new GetColoringInformation());
		coloringManager = new ColoringManager(this);
		coloringManager.addWindowListener(this);

		//ccs.doBlockingRequest(new GetGroupInformation());
		sim.fillGroups();
		groupManager = new GroupManager(this);
		groupManager.addWindowListener(this);
		
		ccs.doBlockingRequest(new lvConfig());
		addView();
	}
	
	private class GetAttributeInformation extends CcsThread.request {
		public GetAttributeInformation() {
			super("GetAttributeInformation", (byte[]) null);
		}
		
		public void handleReply(byte[] data) {
			Properties props = new Properties();
			try {
				props.load(new StringBufferInputStream(new String(data)));
				sim.fill(props);
			} catch(IOException e) {
				System.err.println("Problem loading properties");
				quit();
			}
		}
	}
	
	private class GetColoringInformation extends CcsThread.request {
		public GetColoringInformation() {
			super("GetColoringInformation", (byte[]) null);
		}
		
		public void handleReply(byte[] data) {
			Properties props = new Properties();
			try {
				props.load(new StringBufferInputStream(new String(data)));
				sim.fillColorings(props);
			} catch(IOException e) {
				System.err.println("Problem loading properties");
				quit();
			}
		}
	}
	
	private class lvConfig extends CcsThread.request {
		public lvConfig() {
			super("lvConfig", (byte[]) null);
		}
		
		public void handleReply(byte[] configData) {
			Config config = null;
            try {
				config = new Config(new DataInputStream(new ByteArrayInputStream(configData)));
			} catch(IOException e) {
				System.err.println("Fatal: Couldn't obtain liveViz configuration information");
				e.printStackTrace();
				quit();
			}
			Vector3D origin = config.max.plus(config.min).scalarMultiply(0.5);
			double boxSize = config.max.x - config.min.x;
			double tempSize = config.max.y - config.min.y;
			if(boxSize < tempSize)
				boxSize = tempSize;
			tempSize = config.max.z - config.min.z;
			if(boxSize < tempSize)
				boxSize = tempSize;
			sim.origin = origin;
			sim.boxSize = boxSize;
		}
	}
	
	public void addView() {
		JFrame frame = new ViewingPanel(this);
		windowList.add(frame);
	}
	
	public void addCodeFrame() {
		JFrame frame = new JFrame("Salsa: Code");
		frame.getContentPane().add(new CodePanel(this));
		frame.addWindowListener(this);
		windowList.add(frame);
		frame.pack();
		frame.setVisible(true);
	}
	
	public void windowClosing(WindowEvent e) {
		Object source = e.getSource();
		if(source instanceof Manager) {
			((Manager) source).setVisible(false);
		} else if(source instanceof JFrame) {
			JFrame frame = (JFrame) source;
			windowList.remove(frame);
			frame.dispose();
			if(windowList.isEmpty())
				quit();
		}
	}
	
	public void quit() {
		attributeManager.setVisible(false);
		coloringManager.setVisible(false);
		groupManager.setVisible(false);
		
		ccs.doBlockingRequest(new SalsaRequests.ShutdownServer());
		System.exit(1);
	}
	
}
