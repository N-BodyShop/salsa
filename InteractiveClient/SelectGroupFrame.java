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
    SelectGroupPanel sgp;
    NameValue groupName;
    String attrib;
    Container contentPane;

    public SelectGroupFrame( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
    
        setTitle("Select Group");
        contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
        
        groupName = new NameValue("Create a Group Named:");
        groupName.setValue(attrib+"1");
        
        JLabel text = new JLabel("based on the following criteria:");
        
        sgp = new SelectGroupPanel(s,vp,this);
        
        JButton chooseButton = new JButton("Create Group");
        chooseButton.setActionCommand("choose");
        chooseButton.addActionListener(this);

        contentPane.add(groupName);
        contentPane.add(text);
        contentPane.add(sgp);
        contentPane.add(chooseButton);
        
        pack();
		
		//place it in the center of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2, screenSize.height / 2 - getSize().height / 2);

        setVisible(true);
    }
    
    public void addPanel(){
        SelectGroupPanel newsgp = new SelectGroupPanel(s,vp,this);
        contentPane.add(newsgp);
        pack();
    }

    public void delPanel(SelectGroupPanel delsgp){
        contentPane.remove(delsgp);
        pack();
    }

    public void actionPerformed(ActionEvent e){
        if ( "choose".equals(e.getActionCommand()) ){
            String ll = (String)sgp.linLog.getSelectedItem();
            int type = 0;
            if (ll.equals("linear") ){type = 0;} else { type = 1;};
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(type);
                dos.writeDouble(Double.parseDouble(sgp.minPanel.getValue()));
                dos.writeDouble(Double.parseDouble(sgp.maxPanel.getValue()));
                dos.writeBytes(attrib);
                s.Groups.addElement(groupName.getValue());
                s.ccs.addRequest( new CreateGroup( baos.toByteArray() ) );
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}
        } else {
            attrib = (String)sgp.attributeList.getSelectedItem();
            s.selectedAttributeIndex = sgp.attributeList.getSelectedIndex();
            // attribute selected, so we need to find out its possible values
            groupName.setValue(attrib+"1");
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
