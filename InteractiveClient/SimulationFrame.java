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

public class SimulationFrame extends JFrame implements MouseMotionListener {
    Simulation s;
    double xBegin, yBegin;
    Rectangle rect;
                    
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

    public void mouseDragged(MouseEvent e) {
            System.out.println("boolean: "+s.groupSelecting);
        if (s.groupSelecting){
            repaint();
            rect = new Rectangle(e.getX(),e.getY(),0,0);
            System.out.println("Rectangle created: "+rect.x+" "+rect.y+" "+rect.width+" "+rect.height);
            Graphics graph = this.getGraphics();
            graph.setColor(Color.white);
            Double x = new Double(e.getX()-rect.getX());
            Double y = new Double(e.getY()-rect.getY());
            rect.setSize(x.intValue(), y.intValue());
            graph.drawRect(rect.x,rect.y,rect.width,rect.height);
            System.out.println("Draw rectangle: "+rect.x+" "+rect.y+" "+rect.width+" "+rect.height);
        }
    }

    public void mouseMoved(MouseEvent e) {
    }


}
