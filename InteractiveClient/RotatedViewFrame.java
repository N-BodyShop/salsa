//
//  RotatedViewFrame.java
//  
//
//  Created by Greg Stinson on Fri Oct 24 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;
import java.net.UnknownHostException;

public class RotatedViewFrame extends JFrame implements MouseListener {
    
    Simulation s;
    Line2D.Double line;
    
    public RotatedViewFrame( Simulation sim, double bs, Vector3D oh ){
        s = sim;
        
        setTitle("Top View");
        Container contentPane = getContentPane();
//        line = new Line2D.Double();
        
//        why.rotate(Math.PI/2.0,ex);
        ViewPanel rvp = new ViewPanel(s,128,128,bs,oh);
        
        contentPane.add(rvp, BorderLayout.CENTER);
        
        pack();
        setVisible(true);
    }
    
    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        e.getY();
    }
    
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
    
}
