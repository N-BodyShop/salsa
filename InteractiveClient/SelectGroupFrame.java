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
        groupName.setActionCommand("choose");
        
        JLabel text = new JLabel("based on the following criteria:");
        text.setAlignmentX(LEFT_ALIGNMENT);
        
        SelectGroupPanel sgp = new SelectGroupPanel(s,vp,this);
        sgps = new Vector();
        sgps.addElement(sgp);
        groupName.setValue(sgp.attrib+"1");
        
        chooseButton = new JButton("Create Group");
        chooseButton.setActionCommand("choose");
        chooseButton.addActionListener(this);
        chooseButton.setAlignmentX(CENTER_ALIGNMENT);

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
            String gName = groupName.getValue();
/*            if ( s.Groups.containsKey(gName) ) {
                JOptionPane.showMessageDialog(this, 
                    "Group named "+gName+" already exists.");
            } else {*/
                String groupInfo = new String( gName +"," );
                Group group = new Group(  );
                SelectGroupPanel tempSGP;
                for ( int i = 0; i < sgps.size(); i++ ) {
                    tempSGP = (SelectGroupPanel)sgps.get(i);
                    groupInfo = groupInfo + tempSGP.attrib + "," +
                        tempSGP.minPanel.getValue() + ","+ 
                        tempSGP.maxPanel.getValue() + ",";
                    group.groupPieces.put(tempSGP.attrib,
                        new GroupPiece( Double.parseDouble(tempSGP.minPanel.getValue()), 
                            Double.parseDouble(tempSGP.maxPanel.getValue()) )
                        );
                }
                s.Groups.put( gName, group ); 
                s.selectedGroup = gName;
                s.groupSelecting = false;
                s.ccs.addRequest( new CreateGroup( groupInfo ) );
   //         }
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
        }
        public void handleReply(byte[] data) {
            s.ccs.addRequest( new ActivateGroup( groupName.getValue(), vp ) );
            setVisible(false);
       }
    }

}
