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
		} else if(command.equals("close")){
                    //find containing JFrame
                    Container c = this.getParent();
                    while(!(c instanceof JFrame))
                            c = c.getParent();
                    //tell frame to close, the WindowManager will hear this event
                    c.dispatchEvent(new WindowEvent((JFrame) c, WindowEvent.WINDOW_CLOSING));
		} else if(command.equals("quit"))
                    windowManager.quit();
	}
    
}
