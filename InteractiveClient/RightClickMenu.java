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
    
	/*
    public void actionPerformed(ActionEvent e){
        String command =  e.getActionCommand();
        if ( command.equals("xall view") ) { xall();}
        else if (command.equals("yall view"))  { yall();}
        else if (command.equals("zall view"))  { zall();}
        else if (command.equals("average z")) { s.centerMethod = 0; vp.center();}
        else if (command.equals("largest value")) { s.centerMethod = 1; vp.center();}
        else if (command.equals("lowest potential")) { s.centerMethod = 2; vp.center();}
        else if (command.equals("Update z"))  { vp.center();}
//        else if (command.equals("fixo"))  { System.out.println("Origin fixed");}
        else if (command.equals("recolor image"))  { 
            ReColorFrame rcf = new ReColorFrame(s, vp);
        }
        else if (command.equals("Create a Group..."))  { 
            SelectGroupFrame sgf = new SelectGroupFrame(s, vp);
            sgf.addWindowListener( new WindowAdapter() {
                    public void windowClosing(WindowEvent e){
                        s.groupSelecting = false;
                    }
                });
        }
        else if (command.equals("ActivateGroup"))  { 
            JMenuItem source = (JMenuItem)(e.getSource());
            s.selectedGroup = source.getText();
            s.ccs.addRequest( new ActivateGroup( source.getText(), vp ) );
        }
//        else if (command.equals("cs")){ ChooseSimulationFrame csf = 
//                                    new ChooseSimulationFrame(s.ccs,);}
        else if (command.equals("switchmap")){}
        else if (command.equals("Save image as png...")){ vp.writePng(); }
        else if (command.equals("clear")) {}
        else if (command.equals("manageAttributes")) {
			s.ccs.addRequest(new GetAttributeInformation());
		}
    }
    */
	
    public void refresh() {
		/*
        remove(groupSubmenu);
        ButtonGroup group2 = new ButtonGroup();
        JRadioButtonMenuItem rbItem;        
        groupSubmenu = new JMenu("Display groups...");
        String groupName;
        for ( Enumeration en = s.Groups.keys(); en.hasMoreElements(); ){
            groupName = (String)en.nextElement();
            rbItem = new JRadioButtonMenuItem(groupName);
            rbItem.addActionListener(this);
            rbItem.setActionCommand("ActivateGroup");
            if (groupName.equals(s.selectedGroup)) {rbItem.setSelected(true);}
            group2.add(rbItem);
            groupSubmenu.add(rbItem);
        }
        add(groupSubmenu);
		*/
    }
    
}
