//
//  ChooseSimulation.java
//  
//
//  Created by Greg Stinson on Fri Dec 05 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.UnknownHostException;

public class ChooseSimulation extends CcsThread.request{
    Simulation s;
    ChooseSimulationFrame csf=null;
    
    public ChooseSimulation(Simulation sim){
        super("ChooseSimulation", sim.Name.getBytes());
        s = sim;
    }

    public ChooseSimulation(Simulation sim, ChooseSimulationFrame chsf){
        super("ChooseSimulation", sim.Name.getBytes());
        s = sim;
        csf = chsf;
    }
    
    public void handleReply(byte[] data) {
        Family family;
        String familyName;
        String reply = new String(data);
//           System.out.println(reply);
        int index = -1;
        int lastindex = 0;
        index = reply.indexOf(",",lastindex);
        s.numberOfColors = Integer.parseInt(reply.substring(lastindex,index));
        lastindex = index + 1;
        index = reply.indexOf(",",lastindex);
        int numberOfFamilies = Integer.parseInt(reply.substring(lastindex,index));
        for ( int i=0; i < numberOfFamilies; i++ ){
            lastindex = index + 1;
            index = reply.indexOf(",",lastindex);
            familyName = new String(reply.substring(lastindex,index));
            family = new Family(familyName);
            lastindex = index + 1;
            index = reply.indexOf(",",lastindex);
            int numberOfAttributes = Integer.parseInt(reply.substring(lastindex,index));
            for ( int j = 0; j < numberOfAttributes; j++ ){
                lastindex = index + 1;
                index = reply.indexOf(",",lastindex);
                family.attributes.addElement(reply.substring(lastindex,index));
            }
            s.Families.put(familyName, family);
        }
        s.ccs.addRequest( new lvConfig() );

    }


    private class lvConfig extends CcsThread.request {
        public lvConfig() {
            super("lvConfig", null);
        }
        
        public void handleReply(byte[] configData){
            try {
                Config config = new Config(new DataInputStream(new ByteArrayInputStream(configData)));
                Vector3D origin;
                origin = new Vector3D(0, 0, 0);
                origin = config.max.plus(config.min).scalarMultiply(0.5);
                double boxSize = config.max.x - config.min.x;
                if((config.max.y - config.min.y != boxSize) || 
                    (config.max.z - config.min.z != boxSize)) 
                    {    System.err.println("Box is not a cube!"); }
                SimulationFrame sf = new SimulationFrame(s,boxSize,origin);
                sf.addWindowListener( new WindowAdapter() {
                        public void windowClosing(WindowEvent e){
                            if ( csf != null ) { csf.setVisible(true);}
                            else {  System.exit(1); }
                        }
                    });
//                RotatedViewFrame rvf = new RotatedViewFrame(s,boxSize,origin);
            } catch(IOException e) {
                System.err.println("Fatal: Couldn't obtain configuration information");
                e.printStackTrace();
            }
        }
    }
}