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
        s.reset();
        s.Name = (String)simlist.get( simJList.getSelectedIndex() );
        System.out.println("Selected "+s.Name);
        s.ccs.addRequest( new ChooseSimulation(s, this) );
        setVisible(false);
    }

}
