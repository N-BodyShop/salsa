//
//  java
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

public class RightClickMenu extends JPopupMenu implements ActionListener {
    Simulation s;
    ViewPanel vp;
    ToolBarPanel tbp;
    
    public RightClickMenu( Simulation sim, ViewPanel viewP ) {
        super();
        s = sim;
        vp = viewP;
                
        JMenuItem item;
        item = new JMenuItem("xall view");
        item.setActionCommand("xall");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("yall view");
        item.setActionCommand("yall");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("zall view");
        item.setActionCommand("zall");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("Choose centering...");
        item.setActionCommand("center");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("recolor image");
        item.setActionCommand("recolor");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("Select a Group...");
        item.setActionCommand("group");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("Save image as png...");
        item.setActionCommand("png");
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
        if ( command.equals("xall") ) { xall();}
        else if (command.equals("yall"))  { yall();}
        else if (command.equals("zall"))  { zall();}
        else if (command.equals("center"))  { PreferencesFrame pf = new PreferencesFrame(s);}
        else if (command.equals("recolor"))  { 
            ReColorFrame rcf = new ReColorFrame(s, vp);
        }
        else if (command.equals("group"))  { 
            SelectGroupFrame sgf = new SelectGroupFrame(s, vp);
        }
//        else if (command.equals("cs")){ ChooseSimulationFrame csf = 
//                                    new ChooseSimulationFrame(s.ccs,);}
        else if (command.equals("switchmap")){}
        else if (command.equals("png")){ vp.writePng(); }
        else if (command.equals("clear")) {}
    }
    
    public void xall(){
        tbp.resetSliders();
        vp.x = new Vector3D(0, vp.boxSize*0.5, 0);
        vp.y = new Vector3D(0, 0, vp.boxSize*0.5);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        vp.getNewImage();
    }
    public void yall(){
        tbp.resetSliders();
        vp.x = new Vector3D(0, 0, vp.boxSize*0.5);
        vp.y = new Vector3D(vp.boxSize*0.5, 0, 0);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        vp.getNewImage();
    }
    public void zall(){
        tbp.resetSliders();
        vp.x = new Vector3D(vp.boxSize*0.5, 0, 0);
        vp.y = new Vector3D(0, vp.boxSize*0.5, 0);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        vp.getNewImage();
    }
    
}
