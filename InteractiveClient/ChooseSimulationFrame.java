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
import javax.swing.border.*;

public class ChooseSimulationFrame extends JFrame {
	String chosenSimulation = null;
	
	public ChooseSimulationFrame(Vector simulationList) {
		super("Salsa: Choose simulation");
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder("Available simulations"));
		
		JList l = new JList(simulationList);
		l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		l.setSelectedIndex(0);
		l.setVisibleRowCount(8);
		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					chosenSimulation = (String) ((JList) e.getSource()).getSelectedValue();
					dispose();
				}
			}
		};
		l.addMouseListener(mouseListener);
		p.add(new JScrollPane(l));
		getContentPane().add(p);
		
		pack();
    }
}
