//
//  SimulationFrame.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.UnknownHostException;

public class SimulationFrame extends JFrame {
    Simulation s;
                    
    public SimulationFrame( Simulation sim, double bs, Vector3D o ){
        s = sim;
        
        setTitle("Salsa:  "+s.Name);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());


        ColorBarPanel cbp = new ColorBarPanel(s,10,512);
        ViewPanel vp = new ViewPanel(s,512,512,bs,o);
        cbp.vp = vp;
        ToolBarPanel tbp = new ToolBarPanel(s, vp);
                
        contentPane.add(cbp, BorderLayout.NORTH);
        contentPane.add(vp, BorderLayout.CENTER);
        contentPane.add(tbp, BorderLayout.EAST);
        contentPane.add(tbp, BorderLayout.WEST);
        contentPane.add(tbp, BorderLayout.SOUTH);
        
        //make it pretty
        pack();
        //make it appear now that it's pretty
        setVisible(true);
     }


}
