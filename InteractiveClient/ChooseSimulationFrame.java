//
//  ChooseSimulationFrame.java
//  
//
//  Created by Greg Stinson on Thu Sep 25 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.UnknownHostException;

public class ChooseSimulationFrame extends JFrame
                                implements ActionListener {
    JList simJList;
    Connection c;
    Simulation s;

    public ChooseSimulationFrame( Connection con ){
        c = con;
        s = new Simulation();
    
        setTitle("Choose Simulation");
        Container contentPane = getContentPane();
        
        simJList = new JList( con.simlist );
        simJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        simJList.setSelectedIndex(0);
        simJList.setVisibleRowCount(8);
        JScrollPane simScrollPane = new JScrollPane(simJList);
        
        JButton chooseButton = new JButton("Choose");
        chooseButton.addActionListener(this);

        contentPane.add(simScrollPane, BorderLayout.NORTH);
        contentPane.add(chooseButton,BorderLayout.SOUTH);
        
        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e){
        s.Name = (String)c.simlist.get( simJList.getSelectedIndex() );
        try{
            CcsThread ccs = new CcsThread( new Label(), c.host,c.port );
            ccs.addRequest( new ChooseSimulation(s.Name) );
            setVisible(false);
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Couldn't connect to "+
                        c.host+":"+c.port+".");
        }
    }

    private class ChooseSimulation extends CcsThread.request{
        public ChooseSimulation(String simulation){
            super("ChooseSimulation", simulation.getBytes());
        }
        public void handleReply(byte[] data) {
            String reply = new String(data);
            System.out.println(reply);
            int index = -1;
            int lastindex = 0;
            index = reply.indexOf(",",lastindex);
            s.numberOfColors = Integer.parseInt(reply.substring(lastindex,index));
            lastindex = index + 1;
            index = reply.indexOf(",",lastindex);
            s.numberOfFamilies = Integer.parseInt(reply.substring(lastindex,index));
            for ( int i=0; i < s.numberOfFamilies; i++ ){
                lastindex = index + 1;
                index = reply.indexOf(",",lastindex);
                s.Families.addElement(reply.substring(lastindex,index));
            }
            while( index < reply.length()-1 ){
                lastindex = index + 1;
                index = reply.indexOf(",",lastindex);
                s.attributes.addElement(reply.substring(lastindex,index));
            }
         // we've chosen and initialized a simulation, now we can look at it
        SimulationFrame sf = new SimulationFrame(c,s);
       }
    }

}
