//
//  RightClickMenu.java
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

public class RightClickMenu extends JPopupMenu {
	WindowManager windowManager;
	SimulationView view;
	JMenu groupSubmenu;
	String[] centerChoiceStrings = {"average z","largest value","lowest potential"};
    
	public RightClickMenu(WindowManager wm, SimulationView v) {
		windowManager = wm;
		view = v;

		JMenuItem item;
		JRadioButtonMenuItem rbItem;
		JCheckBoxMenuItem cbItem;
		item = new JMenuItem("Refresh view");
		item.setActionCommand("refresh");
		item.addActionListener(view);
		add(item);
		item = new JMenuItem("xall view");
		item.setActionCommand("xall");
		item.addActionListener(view);
		add(item);
		item = new JMenuItem("yall view");
		item.setActionCommand("yall");
		item.addActionListener(view);
		add(item);
		item = new JMenuItem("zall view");
		item.setActionCommand("zall");
		item.addActionListener(view);
		add(item);

		addSeparator();

		item = new JMenuItem("Write and execute code ...");
		item.setActionCommand("executeCode");
		item.addActionListener(view);
		add(item);
		item = new JMenuItem("Manage attributes ...");
		item.setActionCommand("manageAttributes");
		item.addActionListener(view);
		add(item);
		item = new JMenuItem("Manage coloring ...");
		item.setActionCommand("manageColoring");
		item.addActionListener(view);
		add(item);
		item = new JMenuItem("Manage groups ...");
		item.setActionCommand("manageGroups");
		item.addActionListener(view);
		add(item);
		
		addSeparator();
		/*
        JMenu submenu = new JMenu("Choose centering...");
        ButtonGroup group = new ButtonGroup();
        for (int i=0; i < centerChoiceStrings.length; i++) {
            rbItem = new JRadioButtonMenuItem(centerChoiceStrings[i]);
            if (i==s.centerMethod) rbItem.setSelected(true);
            rbItem.addActionListener(view);
            rbItem.setActionCommand(centerChoiceStrings[i]);
            group.add(rbItem);
            submenu.add(rbItem);
        }
		
        add(submenu);
        */
        item = new JMenuItem("Capture image ...");
        item.setActionCommand("screenCapture");
        item.addActionListener(view);
        add(item);
    }
    
}
