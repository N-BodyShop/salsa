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

public class RightClickMenu extends JPopupMenu 
            implements ActionListener {
    Simulation s;
    ViewPanel vp;
    ToolBarPanel tbp;
    String[] centerChoiceStrings = {"average z","largest value","lowest potential"};
    
    public RightClickMenu( Simulation sim, ViewPanel viewP ) {
        super();
        s = sim;
        vp = viewP;
                
        JMenuItem item;
        JRadioButtonMenuItem rbItem;
        JCheckBoxMenuItem cbItem;
        item = new JMenuItem("xall view");
        item.setActionCommand("xall view");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("yall view");
        item.setActionCommand("yall view");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("zall view");
        item.setActionCommand("zall view");
        item.addActionListener(this);
        add(item);
        JMenu submenu = new JMenu("Choose centering...");
        ButtonGroup group = new ButtonGroup();
        for (int i=0; i < centerChoiceStrings.length; i++) {
            rbItem = new JRadioButtonMenuItem(centerChoiceStrings[i]);
            if (i==s.centerMethod) rbItem.setSelected(true);
            rbItem.addActionListener(this);
            rbItem.setActionCommand(centerChoiceStrings[i]);
            group.add(rbItem);
            submenu.add(rbItem);
        }
        add(submenu);
        item = new JMenuItem("Update z");
        item.setActionCommand("Update z");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("recolor image");
        item.setActionCommand("recolor image");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("Create a Group...");
        item.setActionCommand("Create a Group...");
        item.addActionListener(this);
        add(item);
        ButtonGroup group2 = new ButtonGroup();
        submenu = new JMenu("Display groups...");
        for (int i=0; i < s.Groups.size(); i++) {
            rbItem = new JRadioButtonMenuItem(((Group)s.Groups.get(i)).Name);
            rbItem.addActionListener(this);
            rbItem.setActionCommand("ActivateGroup");
            group2.add(rbItem);
            submenu.add(rbItem);
        }
        add(submenu);
        item = new JMenuItem("Save image as png...");
        item.setActionCommand("Save image as png...");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("Another simulation...");
        item.setActionCommand("cs");
        item.addActionListener(this);
//        add(item);
        item = new JMenuItem("clear boxes/spheres");
        item.setActionCommand("clear");
        item.addActionListener(this);
//        add(item);
        item = new JMenuItem("review");
        item.setActionCommand("review");
        item.addActionListener(this);
//        add(item);
    }
    
    public void actionPerformed(ActionEvent e){
        String command =  e.getActionCommand();
        if ( command.equals("xall view") ) { xall();}
        else if (command.equals("yall view"))  { yall();}
        else if (command.equals("zall view"))  { zall();}
        else if (command.equals("average z")) { s.centerMethod = 0; vp.center();}
        else if (command.equals("largest value")) { s.centerMethod = 1; vp.center();}
        else if (command.equals("lowest potential")) { s.centerMethod = 2; vp.center();}
        else if (command.equals("Update z"))  { vp.center();}
        else if (command.equals("fixo"))  { System.out.println("Origin fixed");}
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
            s.ccs.addRequest( new ActivateGroup( source.getText(), vp ) );
        }
//        else if (command.equals("cs")){ ChooseSimulationFrame csf = 
//                                    new ChooseSimulationFrame(s.ccs,);}
        else if (command.equals("switchmap")){}
        else if (command.equals("Save image as png...")){ vp.writePng(); }
        else if (command.equals("clear")) {}
    }
    

    public void xall(){
        tbp.resetSliders();
        vp.x = new Vector3D(0, vp.boxSize*0.5, 0);
        vp.y = new Vector3D(0, 0, vp.boxSize*0.5);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        s.ccs.addRequest( new ActivateGroup( "All", vp ) );
    }
    public void yall(){
        tbp.resetSliders();
        vp.x = new Vector3D(0, 0, vp.boxSize*0.5);
        vp.y = new Vector3D(vp.boxSize*0.5, 0, 0);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        s.ccs.addRequest( new ActivateGroup( "All", vp ) );
    }
    public void zall(){
        tbp.resetSliders();
        vp.x = new Vector3D(vp.boxSize*0.5, 0, 0);
        vp.y = new Vector3D(0, vp.boxSize*0.5, 0);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        s.ccs.addRequest( new ActivateGroup( "All", vp ) );
    }
    
}
