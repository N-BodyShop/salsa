//
//  SelectGroupFrame.java
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

public class SelectGroupFrame extends JFrame
                        implements ActionListener {
    Simulation s;
    ViewPanel vp;
    NameValue minPanel, maxPanel, groupName;
    String attrib;
    JComboBox linLog;
    JComboBox attributeList;

    public SelectGroupFrame( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
    
        setTitle("Select Group");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
        
        attributeList = new JComboBox(s.attributes);
        attributeList.setSelectedIndex(s.selectedAttributeIndex);
        attributeList.addActionListener(this);
        
        String[] linLogStrings = {"linear","logarithmic"};
        linLog = new JComboBox(linLogStrings);
        linLog.setSelectedIndex(0);
        linLog.addActionListener(this);

        minPanel = new NameValue("min. value:");
        maxPanel = new NameValue("max. value:");
        attrib = (String)attributeList.getSelectedItem();
        s.ccs.addRequest( new ValueRange(attrib) );
        
        groupName = new NameValue("Group Name:");
        groupName.setValue(attrib+"1");
        
        JButton chooseButton = new JButton("Create Group");
        chooseButton.setActionCommand("choose");
        chooseButton.addActionListener(this);

        contentPane.add(attributeList);
        contentPane.add(linLog);
        contentPane.add(minPanel);
        contentPane.add(maxPanel);
        contentPane.add(groupName);
        contentPane.add(chooseButton);
        
        pack();
		
		//place it in the center of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2, screenSize.height / 2 - getSize().height / 2);

        setVisible(true);
    }

    public void actionPerformed(ActionEvent e){
        if ( "choose".equals(e.getActionCommand()) ){
            String ll = (String)linLog.getSelectedItem();
            int type = 0;
            if (ll.equals("linear") ){type = 0;} else { type = 1;};
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(type);
                dos.writeDouble(Double.parseDouble(minPanel.getValue()));
                dos.writeDouble(Double.parseDouble(maxPanel.getValue()));
                dos.writeBytes(attrib);
                s.Groups.addElement(groupName.getValue());
                s.ccs.addRequest( new CreateGroup( baos.toByteArray() ) );
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}
        } else {
            attrib = (String)attributeList.getSelectedItem();
            s.selectedAttributeIndex = attributeList.getSelectedIndex();
            // attribute selected, so we need to find out its possible values
            groupName.setValue(attrib+"1");
            s.ccs.addRequest( new ValueRange(attrib) );
        }
    }

    private class ValueRange extends CcsThread.request{
        public ValueRange(String attrib){
            super("ValueRange", attrib.getBytes());
        }
        public void handleReply(byte[] data) {
            DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(data));
         // set boundary values
            String pass = new String();
            try {
                String ll = (String)linLog.getSelectedItem();
                if (ll.equals("linear") ){
                    minPanel.setValue(pass.valueOf(dis.readDouble()));
                    maxPanel.setValue(pass.valueOf(dis.readDouble()));
                } else {
                    minPanel.setValue(pass.valueOf(Math.log(dis.readDouble())/Math.log(10)));
                    maxPanel.setValue(pass.valueOf(Math.log(dis.readDouble())/Math.log(10)));
                }
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}
       }
    }
    
    private class ChooseColorValue extends CcsThread.request{
        public ChooseColorValue(byte[] data){
            super("ChooseColorValue", data);
        }
        public void handleReply(byte[] data) {
            setVisible(false);
            vp.getNewImage();
       }
    }
    
    private class CreateGroup extends CcsThread.request{
        public CreateGroup(byte[] data){
            super("CreateGroup", data);
        }
        public void handleReply(byte[] data) {
            setVisible(false);
            vp.getNewImage();
       }
    }
}
