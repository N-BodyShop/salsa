//
//  ConnectFrame.java
//  
//
//  Created by Greg Stinson on Tue Sep 23 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.UnknownHostException;

public class ConnectFrame extends JFrame
                          implements ActionListener {
    NameValue userPanel;
    NameValue passPanel;
    NameValue hostPanel;
    NameValue portPanel;
    CcsThread ccs;

    public ConnectFrame( ){
        setTitle("Connect to NChilada Server");
        //setLocationRelativeTo(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         
        userPanel = new NameValue("username:");
        passPanel = new NameValue("password:");
        hostPanel = new NameValue("hostname:","localhost");
        hostPanel.addActionListener(this);
        portPanel = new NameValue("port:","1235");
        portPanel.addActionListener(this);

        contentPane.add(hostPanel);
        contentPane.add(portPanel);
//        contentPane.add(userPanel);
        userPanel.setValue("nobody");
//        contentPane.add(passPanel);
        passPanel.setValue("nowhere");

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        connectButton.setAlignmentX(CENTER_ALIGNMENT);

        contentPane.add(connectButton);
        
        //make it pretty
        pack();
		
		//place it in the center of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2, screenSize.height / 2 - getSize().height / 2);

        //make it appear now that it's pretty
        setVisible(true);
    }
    
    public void actionPerformed(ActionEvent e){
        try{
            ccs = new CcsThread( new Label(), 
                hostPanel.getValue(), Integer.parseInt(portPanel.getValue()) );
            ccs.addRequest( new AuthenticationRequest(userPanel.getValue(), 
                        passPanel.getValue(),ccs));
            setVisible(false);
        } catch (UnknownHostException uhe) {            
            JOptionPane.showMessageDialog(this, "Couldn't find host "+
                    hostPanel.getValue()+":"+portPanel.getValue()+".");
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Couldn't connect to "+
                    hostPanel.getValue()+":"+portPanel.getValue()+".");
        }    
    }


}
