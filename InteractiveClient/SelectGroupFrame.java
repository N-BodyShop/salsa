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
import java.util.*;
import java.lang.Math;

public class SelectGroupFrame extends JFrame
                        implements ActionListener {
    Simulation s;
    ViewPanel vp;
    Vector sgps;
    NameValue groupName;
    String attrib;
    Container contentPane;
    JButton chooseButton;

    public SelectGroupFrame( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
    
        setTitle("Select Group");
        contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
        
        groupName = new NameValue("Create a Group Named:");
        
        JLabel text = new JLabel("based on the following criteria:");
        
        SelectGroupPanel sgp = new SelectGroupPanel(s,vp,this);
        sgps = new Vector();
        sgps.addElement(sgp);
        groupName.setValue(sgp.attrib+"1");
        
        chooseButton = new JButton("Create Group");
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
        s.groupSelecting = true;
    }
    
    public void addPanel(){
        SelectGroupPanel newsgp = new SelectGroupPanel(s,vp,this);
        contentPane.remove(chooseButton);
        contentPane.add(newsgp);
        contentPane.add(chooseButton);
        sgps.addElement(newsgp);
        pack();
    }

    public void delPanel(SelectGroupPanel delsgp){
        contentPane.remove(delsgp);
        sgps.remove(delsgp);
        pack();
    }

    public void actionPerformed(ActionEvent e){
        if ( "choose".equals(e.getActionCommand()) ){
            String groupInfo = new String( groupName.getValue() +"," );
            Group group = new Group( groupName.getValue() );
            for ( int i = 0; i < sgps.size(); i++ ) {
                groupInfo = groupInfo + ((SelectGroupPanel)sgps.get(i)).attrib + "," +
                    ((SelectGroupPanel)sgps.get(i)).minPanel.getValue() + ","+
                    ((SelectGroupPanel)sgps.get(i)).maxPanel.getValue() + ",";
                group.groupPieces.addElement(
                    new GroupPiece(((SelectGroupPanel)sgps.get(i)).attrib,
                        Double.parseDouble(((SelectGroupPanel)sgps.get(i)).minPanel.getValue()), 
                        Double.parseDouble(((SelectGroupPanel)sgps.get(i)).maxPanel.getValue()) )
                    );
            }
            s.Groups.addElement( group ); 
            s.groupSelecting = false;
            s.ccs.addRequest( new CreateGroup( groupInfo ) );
        } else {
            attrib = (String)((SelectGroupPanel)sgps.get(0)).attributeList.getSelectedItem();
            s.selectedAttributeIndex = ((SelectGroupPanel)sgps.get(0)).attributeList.getSelectedIndex();
            // attribute selected, so we need to find out its possible values
            groupName.setValue(attrib+"1");
            pack();
        }
    }

    private class CreateGroup extends CcsThread.request{
        public CreateGroup(String groupInfo){
            super("CreateGroup", groupInfo.getBytes());
            System.out.println("Sent CreateGroup message: "+groupInfo);
        }
        public void handleReply(byte[] data) {
            setVisible(false);
            vp.getNewImage();
       }
    }
}
