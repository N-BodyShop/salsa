//
//  PreferencesFrame.java
//  
//
//  Created by Greg Stinson on Tue Sep 30 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.UnknownHostException;
import java.lang.Math;

public class PreferencesFrame extends JFrame
                        implements ActionListener {
    Simulation s;
    ViewPanel vp;
    JComboBox centerChoice;

    public PreferencesFrame( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
    
        setTitle("Preferences");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
                
        String[] centerChoiceStrings = {"average z","largest value","lowest potential"};
        centerChoice = new JComboBox(centerChoiceStrings);
        centerChoice.setSelectedIndex(s.centerMethod);
        centerChoice.addActionListener(this);

        JButton chooseButton = new JButton("OK");
        chooseButton.setActionCommand("choose");
        chooseButton.addActionListener(this);

        contentPane.add(centerChoice);
        contentPane.add(chooseButton);
        
        pack();
		
		//place it in the center of the screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2, screenSize.height / 2 - getSize().height / 2);

		setVisible(true);
    }

    public void actionPerformed(ActionEvent e){
        if ( "choose".equals(e.getActionCommand()) ){
            s.centerMethod = centerChoice.getSelectedIndex();
//            int type = 0;
//            if (ll.equals("average z") ){s.centerMethod = 0;} else { s.centerMethod = 1;};
            vp.center();
            setVisible(false);
        } 
    }

}