//
//  MenuBar.java
//  
//
//  Created by Greg Stinson on Sun Oct 19 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.UnknownHostException;
import java.util.*;
import charm.ccs.CcsThread;
import charm.ccs.PythonAbstract;
import charm.ccs.PythonExecute;
import charm.ccs.PythonPrint;
import charm.ccs.PythonFinished;

public class MenuBar extends JMenuBar implements ActionListener{
	WindowManager windowManager;
	SimulationView view;
	JMenu groupSubmenu;
	String[] centerChoiceStrings = {"average z","largest value","lowest potential"};
    
	public MenuBar(WindowManager wm, SimulationView v) {
		windowManager = wm;
                view = v;

		JMenu menu;
		JMenuItem item;
		JRadioButtonMenuItem rbItem;
		JCheckBoxMenuItem cbItem;

                menu = new JMenu("File");
                menu.setMnemonic(KeyEvent.VK_F);
                add(menu);
		item = new JMenuItem("Load Simulation",KeyEvent.VK_F);
                item.setAccelerator(KeyStroke.getKeyStroke("control F"));
                item.setActionCommand("loadSimulation");
		item.addActionListener(this);
		menu.add(item);
		item = new JMenuItem("Read Tipsy Array",KeyEvent.VK_A);
                item.setAccelerator(KeyStroke.getKeyStroke("control A"));
                item.setActionCommand("readTipsyArray");
		item.addActionListener(this);
		menu.add(item);
		item = new JMenuItem("Read Tipsy Binary Array",KeyEvent.VK_B);
                item.setAccelerator(KeyStroke.getKeyStroke("control B"));
                item.setActionCommand("readTipsyBinaryArray");
		item.addActionListener(this);
		menu.add(item);
		item = new JMenuItem("New View",KeyEvent.VK_N);
                item.setAccelerator(KeyStroke.getKeyStroke("control N"));
                item.setActionCommand("newview");
		item.addActionListener(this);
		menu.add(item);
		item = new JMenuItem("Save Image",KeyEvent.VK_S);
                item.setAccelerator(KeyStroke.getKeyStroke("control S"));
		item.setActionCommand("screenCapture");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("Close Window",KeyEvent.VK_W);
                item.setAccelerator(KeyStroke.getKeyStroke("control W"));
		item.setActionCommand("close");
		item.addActionListener(this);
		menu.add(item);
		item = new JMenuItem("Exit and Kill Server",KeyEvent.VK_X);
        item.setActionCommand("exitandkill");
        item.addActionListener(this);
        menu.add(item);
		item = new JMenuItem("Quit",KeyEvent.VK_Q);
                item.setAccelerator(KeyStroke.getKeyStroke("control Q"));
		item.setActionCommand("quit");
		item.addActionListener(this);
		menu.add(item);

                menu = new JMenu("View");
                menu.setMnemonic(KeyEvent.VK_V);
                add(menu);
		item = new JMenuItem("Refresh",KeyEvent.VK_R);
		item.setActionCommand("refresh");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("y-z plane",KeyEvent.VK_X);
		item.setActionCommand("xall");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("x-z plane",KeyEvent.VK_Y);
		item.setActionCommand("yall");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("x-y plane",KeyEvent.VK_Z);
		item.setActionCommand("zall");
		item.addActionListener(view);
		menu.add(item);

                menu = new JMenu("Manage");
                menu.setMnemonic(KeyEvent.VK_M);
                add(menu);
		item = new JMenuItem("Write and execute code ...");
		item.setActionCommand("executeCode");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("Write and execute local code");
		item.setActionCommand("executeLocalCode");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("Attributes",KeyEvent.VK_A);
		item.setActionCommand("manageAttributes");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("Coloring",KeyEvent.VK_C);
		item.setActionCommand("manageColoring");
		item.addActionListener(view);
		menu.add(item);
		item = new JMenuItem("Manage groups",KeyEvent.VK_G);
		item.setActionCommand("manageGroups");
		item.addActionListener(view);
		menu.add(item);
		
        }

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("newview")) {
                    windowManager.addView();
		} else if(command.equals("loadSimulation")){
		    JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		    int returnVal = chooser.showOpenDialog(MenuBar.this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
			PythonExecute code = new PythonExecute("charm.loadSimulation(\""
			+ chooser.getSelectedFile() + "\")\n", false, true, 0);
			windowManager.ccs.addRequest(new ExecutePythonCode(code.pack()));
			}
		} else if(command.equals("readTipsyArray")){
		    JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		    int returnVal = chooser.showOpenDialog(MenuBar.this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
			String strPy = "charm.readTipsyArray(\""
			    + chooser.getSelectedFile() + "\", \"array\")\n";
			System.out.println(strPy);
			PythonExecute code = new PythonExecute(strPy, false,
							       true, 0);
			windowManager.ccs.addRequest(new ExecutePythonCode(code.pack()));
			}
		} else if(command.equals("readTipsyBinaryArray")){
		    JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		    int returnVal = chooser.showOpenDialog(MenuBar.this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
			String strPy = "charm.readTipsyBinaryArray(\""
			    + chooser.getSelectedFile() + "\", \"array\")\n";
			System.out.println(strPy);
			PythonExecute code = new PythonExecute(strPy, false,
							       true, 0);
			windowManager.ccs.addRequest(new ExecutePythonCode(code.pack()));
			}
		} else if(command.equals("close")){
                    //find containing JFrame
                    Container c = this.getParent();
                    while(!(c instanceof JFrame))
                            c = c.getParent();
                    //tell frame to close, the WindowManager will hear this event
                    c.dispatchEvent(new WindowEvent((JFrame) c, WindowEvent.WINDOW_CLOSING));
		} else if(command.equals("quit"))
                    windowManager.quit();
		else if(command.equals("exitandkill"))
            windowManager.exitAndKill();
	}
    
	private class ExecutePythonCode extends CcsThread.request {
		public ExecutePythonCode(byte[] s) {
			super("ExecutePythonCode", s);
		}
		
		public void handleReply(byte[] data) {
			String result = new String(data);
			System.out.println("Return from code execution: \"" + result + "\"");
		}
	}
}
