//
//  SelectGroupPanel.java
//  
//
//  Created by Greg Stinson on Tue Sep 30 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.net.UnknownHostException;
import java.lang.Math;

public class SelectGroupPanel extends JPanel
                        implements ActionListener {
    Simulation s;
    ViewPanel vp;
    SelectGroupFrame sgf;
    NameValue minPanel, maxPanel;
    Vector attributes;
    String attrib;
    int selectedAttributeIndex;
    JComboBox linLog;
    JComboBox attributeList;

    public SelectGroupPanel( Simulation sim, ViewPanel viewP, SelectGroupFrame selgf ){
        s = sim;
        vp = viewP;
        sgf = selgf;
        selectedAttributeIndex = 0;
        attributes = new Vector();
    
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        for ( int i=0; i < s.numberOfFamilies; i++ ){
            for ( int j = 0; j < ((Family)s.Families.get(i)).numberOfAttributes; j++ ){
                if ( !attributes.contains(((Family)s.Families.get(i)).attributes.get(j)) ){
                    attributes.addElement(((Family)s.Families.get(i)).attributes.get(j));
                }
            }
        }
        attributeList = new JComboBox(attributes);
        attributeList.setSelectedIndex(selectedAttributeIndex);
        attributeList.addActionListener(this);
        
        String[] linLogStrings = {"lin","log"};
        linLog = new JComboBox(linLogStrings);
        linLog.setSelectedIndex(0);
        linLog.addActionListener(this);

        minPanel = new NameValue("min. value:");
        maxPanel = new NameValue("max. value:");
        attrib = (String)attributeList.getSelectedItem();
        s.ccs.addRequest( new ValueRange(attrib) );

        JButton plusButton = new JButton("+");
        plusButton.setActionCommand("addPanel");
        plusButton.addActionListener(this);

        JButton minusButton = new JButton("-");
        minusButton.setActionCommand("delPanel");
        minusButton.addActionListener(this);
        
        add(attributeList);
        add(linLog);
        add(minPanel);
        add(maxPanel);
        add(plusButton);
        add(minusButton);
    }

    public void actionPerformed(ActionEvent e){
        if ( "addPanel".equals(e.getActionCommand()) ){
            sgf.addPanel();
        } else if ( "delPanel".equals(e.getActionCommand()) ){
            sgf.delPanel(this);
        } else {
            attrib = (String)attributeList.getSelectedItem();
            selectedAttributeIndex = attributeList.getSelectedIndex();
            // attribute selected, so we need to find out its possible values
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
                if (ll.equals("lin") ){
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
    
}
