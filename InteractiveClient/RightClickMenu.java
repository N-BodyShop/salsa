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
        item = new JMenuItem("recolor image");
        item.setActionCommand("recolor");
        item.addActionListener(this);
        add(item);
        item = new JMenuItem("center Z");
        item.setActionCommand("center");
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
        else if (command.equals("center"))  {
            s.ccs.addRequest( new Center() );
        }
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
        vp.x = new Vector3D(0, vp.boxSize*0.5, 0);
        vp.y = new Vector3D(0, 0, vp.boxSize*0.5);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        vp.getNewImage();
        tbp.resetSliders();
    }
    public void yall(){
        vp.x = new Vector3D(0, 0, vp.boxSize*0.5);
        vp.y = new Vector3D(vp.boxSize*0.5, 0, 0);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        vp.getNewImage();
        tbp.resetSliders();
    }
    public void zall(){
        vp.x = new Vector3D(vp.boxSize*0.5, 0, 0);
        vp.y = new Vector3D(0, vp.boxSize*0.5, 0);
        vp.z = new Vector3D(vp.x.cross(vp.y));
        vp.origin = new Vector3D(0, 0, 0);
        vp.getNewImage();
        tbp.resetSliders();
    }
    
    private class Center extends CcsThread.request {

        public Center() {
            super("Center", null);
            setData(encodeRequest());
        }

        public void handleReply(byte[] data) {
            DataInputStream dis = new DataInputStream( new ByteArrayInputStream(data));
            try {
                double m = dis.readDouble();
                vp.origin = vp.origin.plus(vp.z.unitVector().scalarMultiply( m ));
                System.out.println("Server response: "+m+"  New origin for rotation: "+vp.origin);
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}
        }
    }

    private byte[] encodeRequest() {
        // for mapping System.out.println("ViewingPanel: encodeRequest");
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        try {
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(1); /*Client version*/
            dos.writeInt(1); /*Request type*/
            dos.writeInt(vp.width);
            dos.writeInt(vp.height);
            dos.writeDouble(vp.x.x);
            dos.writeDouble(vp.x.y);
            dos.writeDouble(vp.x.z);
            dos.writeDouble(vp.y.x);
            dos.writeDouble(vp.y.y);
            dos.writeDouble(vp.y.z);
            dos.writeDouble(vp.z.x);
            dos.writeDouble(vp.z.y);
            dos.writeDouble(vp.z.z);
            dos.writeDouble(vp.origin.x);
            dos.writeDouble(vp.origin.y);
            dos.writeDouble(vp.origin.z);
            System.out.println("x:"+vp.x.toString()+" y:"+vp.y.toString()+" z:"+vp.z.toString()+" or:"+vp.origin.toString());
        } catch(IOException e) {
            System.err.println("Couldn't encode request!");
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
