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
        setLocationRelativeTo(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
        
        simJList = new JList( con.simlist );
        simJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        simJList.setSelectedIndex(0);
        simJList.setVisibleRowCount(8);
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { actionPerformed(new ActionEvent(simJList,2,"clicked"));
 /*                   s.Name = (String)c.simlist.get( simJList.locationToIndex(e.getPoint()) );
                    System.out.println("Selected "+s.Name);
                    try{
                        CcsThread ccs = new CcsThread( new Label(), c.host,c.port );
                        ccs.addRequest( new ChooseSimulation(s.Name) );
                        setVisible(false);
                    } catch (IOException ioe) {
                        System.err.println("Couldn't connect to "+
                                    c.host+":"+c.port+".");
                    }*/
                }
            }
        };
        simJList.addMouseListener(mouseListener);
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
        s.Name = (String)c.simlist.get( simJList.getSelectedIndex() );
        System.out.println("Selected "+s.Name);
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
			sf.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					s.reset();
					setVisible(true);
				}
			});
       }
    }

}
