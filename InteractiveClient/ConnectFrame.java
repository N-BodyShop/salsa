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
import java.io.*;
import java.net.UnknownHostException;

public class ConnectFrame extends JFrame
                          implements ActionListener {
    NameValue userPanel;
    NameValue passPanel;
    NameValue hostPanel;
    NameValue portPanel;
    Connection c;

    public ConnectFrame( Connection con ){
        c = con;
        setTitle("Connect to NChilada Server");
        //setLocationRelativeTo(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
         
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
            CcsThread ccs = new CcsThread( new Label(), 
                hostPanel.getValue(), Integer.parseInt(portPanel.getValue()) );
            ccs.addRequest( new AuthenticationRequest(userPanel.getValue(), 
                        passPanel.getValue()) );
            setVisible(false);
        } catch (UnknownHostException uhe) {            
            JOptionPane.showMessageDialog(this, "Couldn't find host "+
                    hostPanel.getValue()+":"+portPanel.getValue()+".");
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Couldn't connect to "+
                    hostPanel.getValue()+":"+portPanel.getValue()+".");
        }    
    }

    private class AuthenticationRequest extends CcsThread.request {

        public AuthenticationRequest(String username, String password) {
                super("AuthenticateNChilada", (username + ":" + password).getBytes());
        }

        public void handleReply(byte[] data) {
            // connected OK, so initialize Connection
            c.host = hostPanel.getValue();
            c.port = Integer.parseInt(portPanel.getValue());
            //initialize simlist
            try{
                CcsThread ccs = new CcsThread( new Label(),c.host,c.port );
//                System.out.println("Requesting simulations");
                ccs.addRequest( new ListSimulations() );
            } 
            catch (UnknownHostException uhe) {
                System.err.println("Unknow host: "+uhe);} 
            catch (IOException ioe) {
                System.err.println("Couldn't connect: "+ioe);}
        }
    }

    private class ListSimulations extends CcsThread.request {

        public ListSimulations() {
                super("ListSimulations", null);
        }

        public void handleReply(byte[] data) {
            String reply = new String(data);
            c.simlist = new DefaultListModel();
            int index = -1;
            int lastindex = 0;
            while( index < reply.length()-1 ){
                lastindex = index + 1;
                index = reply.indexOf(",",lastindex);
                System.out.println("Adding:  "+reply.substring(lastindex,index));
                c.simlist.addElement(reply.substring(lastindex,index));
            }
            // we've established a connection, now we can choose what
            // to look at
            ChooseSimulationFrame csf = new ChooseSimulationFrame(c);
        }
    }

    public static void main(String s[]){
        Connection connection = new Connection();
        ConnectFrame cf = new ConnectFrame(connection);
    }
}
