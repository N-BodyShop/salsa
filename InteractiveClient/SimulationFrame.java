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
    Connection c;
    Simulation s;
                    
    public SimulationFrame( Connection con, Simulation sim ){
        c = con;
        s = sim;
        
        setTitle("NChilada");
        Container contentPane = getContentPane();
        ParentPanel p = new ParentPanel(c, s, "xall" );
        contentPane.add(p);
        
        //make it pretty
        pack();
        //make it appear now that it's pretty
        setVisible(true);
     }

}
