//
//  ReColorFrame.java
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

public class ReColorFrame extends JFrame
                        implements ActionListener, ItemListener {
    Simulation s;
    ViewPanel vp;
    NameValue minPanel, maxPanel;
    Vector attributes;
    String attrib;
    JComboBox linLog;
    JComboBox attributeList;

    public ReColorFrame( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
        attributes = new Vector();
    
        setTitle("Choose Attribute Color");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
                                
        JPanel particlePanel = new JPanel();
        particlePanel.setLayout(new BoxLayout(particlePanel, 
                                BoxLayout.X_AXIS));
        Family family;
        Vector tempAttrib;
        // This is the fun java way of parsing through all the elements
        // in a Hashtable.  Yippee for casting!  Equivalent to 
        // foreach $e (@Families) in Perl.
        for ( Enumeration e = s.Families.elements(); e.hasMoreElements(); ){
            family = (Family)e.nextElement();
            family.checkBox.addItemListener(this);
            particlePanel.add(family.checkBox);
            tempAttrib = new Vector(family.attributes);
            tempAttrib.removeAll(attributes);
            attributes.addAll(tempAttrib);
        }
        
        attributeList = new JComboBox(attributes);
        attributeList.setSelectedIndex(s.selectedAttributeIndex);
        attributeList.addActionListener(this);
        
        String[] linLogStrings = {"linear","logarithmic"};
        linLog = new JComboBox(linLogStrings);
        linLog.setSelectedIndex(0);
        linLog.addActionListener(this);

        minPanel = new NameValue("min. value:");
        minPanel.setActionCommand("choose");
        minPanel.addActionListener(this);
        maxPanel = new NameValue("max. value:");
        maxPanel.setActionCommand("choose");
        maxPanel.addActionListener(this);
        attrib = (String)attributeList.getSelectedItem();
        s.ccs.addRequest( new ValueRange(attrib) );
        
        JButton chooseButton = new JButton("Recolor");
        chooseButton.setActionCommand("choose");
        chooseButton.addActionListener(this);
        chooseButton.setAlignmentX(CENTER_ALIGNMENT);

        contentPane.add(particlePanel);
        contentPane.add(attributeList);
        contentPane.add(linLog);
        contentPane.add(minPanel);
        contentPane.add(maxPanel);
        contentPane.add(chooseButton);
        
        pack();
		
		//place it in the center of the screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2, screenSize.height / 2 - getSize().height / 2);

		setVisible(true);
    }

    public void actionPerformed(ActionEvent e){
        if ( "choose".equals(e.getActionCommand()) ){
            String message = (String)linLog.getSelectedItem()+","+attrib+","+
                minPanel.getValue()+","+maxPanel.getValue();
            Family family;
            String key;
            for ( Enumeration en = s.Families.keys(); en.hasMoreElements(); ){
                key = (String)en.nextElement();
                family = (Family)s.Families.get(key);
                if ( family.on ){ message = message +","+ key; }
            }
/*            int type = 0;
            if (ll.equals("linear") ){type = 0;} else { type = 1;};
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(type);
                dos.writeDouble(Double.parseDouble(minPanel.getValue()));
                dos.writeDouble(Double.parseDouble(maxPanel.getValue()));
                dos.writeBytes(attrib);
                s.ccs.addRequest( new ChooseColorValue( +","+ ) );
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}*/
            s.ccs.addRequest( new ChooseColorValue( message ) );

        } else {
            attrib = (String)attributeList.getSelectedItem();
            s.selectedAttributeIndex = attributeList.getSelectedIndex();
            // attribute selected, so we need to find out its possible values
            s.ccs.addRequest( new ValueRange(attrib) );
        }
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        Family family;
        for ( Enumeration en = s.Families.elements(); 
                en.hasMoreElements(); ){
            family = (Family)en.nextElement();
            if ( source == family.checkBox ) {
                if ( e.getStateChange() == ItemEvent.DESELECTED ) {
                    family.on = false;
                } else { family.on = true; }
            }
        }
        
        attributes = new Vector();
        attributeList.removeAllItems();
        for ( Enumeration en = s.Families.elements(); en.hasMoreElements(); ){
            family = (Family)en.nextElement();
            for ( int j = 0; j < family.attributes.size(); j++ ){
                if ( !attributes.contains(family.attributes.get(j)) && family.on ){
                    attributes.addElement(family.attributes.get(j));
                    attributeList.addItem(family.attributes.get(j));
                }
            }
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
        public ChooseColorValue(String message){
            super("ChooseColorValue", message.getBytes());
        }
        public void handleReply(byte[] data) {
            setVisible(false);
            vp.getNewImage();
       }
    }
}
