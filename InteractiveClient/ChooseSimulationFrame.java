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
import java.util.*;
import java.io.*;
import java.net.UnknownHostException;

public class ChooseSimulationFrame extends JFrame
                                implements ActionListener {
    JList simJList;
    Simulation s;
    Vector simlist;

    public ChooseSimulationFrame( CcsThread ccs, Vector list ){
        simlist = list;
        s = new Simulation(ccs);
        
        setTitle("Choose Simulation");
        setLocationRelativeTo(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
        
        simJList = new JList( simlist );
        simJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        simJList.setSelectedIndex(0);
        simJList.setVisibleRowCount(8);
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { actionPerformed(new ActionEvent(simJList,2,"clicked")); }
            }
        };
        simJList.addMouseListener(mouseListener);
//        simJList.addActionListener(this);
        JScrollPane simScrollPane = new JScrollPane(simJList);
        
        JButton chooseButton = new JButton("Choose");
        chooseButton.addActionListener(this);
        chooseButton.setAlignmentX(CENTER_ALIGNMENT);

        contentPane.add(simScrollPane);
        contentPane.add(chooseButton);
        
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(1);
			}
		});
			
        pack();
		
		//place it in the center of the screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2, screenSize.height / 2 - getSize().height / 2);
        
		setVisible(true);
    }

    public void actionPerformed(ActionEvent e){
        s.Name = (String)simlist.get( simJList.getSelectedIndex() );
        System.out.println("Selected "+s.Name);
        s.ccs.addRequest( new ChooseSimulation(s.Name) );
        setVisible(false);
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
                s.Families.addElement(new Family(reply.substring(lastindex,index)));
                lastindex = index + 1;
                index = reply.indexOf(",",lastindex);
                ((Family)s.Families.get(i)).numberOfAttributes = 
                            Integer.parseInt(reply.substring(lastindex,index));
                for ( int j = 0; j < ((Family)s.Families.get(i)).numberOfAttributes; j++ ){
                    lastindex = index + 1;
                    index = reply.indexOf(",",lastindex);
                    ((Family)s.Families.get(i)).attributes.addElement(reply.substring(lastindex,index));
                }
            }
        s.ccs.addRequest( new lvConfig() );

        }
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
                            setVisible(true);
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
