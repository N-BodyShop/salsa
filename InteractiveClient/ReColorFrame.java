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
    String attrib, dvAttrib;
    JComboBox linLog, attributeList, dvAttributeList;
    Hashtable clipHash;
    JComboBox clipList;
    JTextField dvScale;
    JCheckBox dvCheckBox;
    boolean dvBool;

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

        JLabel particleLabel = new JLabel("Display:");
        particlePanel.add(particleLabel);
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
        
        JPanel clipPanel = new JPanel();
        clipPanel.setLayout( new BoxLayout(clipPanel,BoxLayout.X_AXIS) );
        JLabel clipLabel = new JLabel("Display values");
        clipHash = new Hashtable();
        clipHash.put("on both sides of","clipno");
        clipHash.put("lower than","cliphigh");
        clipHash.put("higher than","cliplow");
        clipHash.put("only inside","clipboth");
        Enumeration en = clipHash.keys();
        String[] clipArr = {(String)en.nextElement(),(String)en.nextElement(),(String)en.nextElement(),(String)en.nextElement()};
        clipList = new JComboBox(clipArr);
        clipList.setSelectedIndex(s.selectedClippingIndex);
        clipList.addActionListener(this);
        clipList.setActionCommand("clip set");
        JLabel clipLabel2 = new JLabel("range");
        clipPanel.add(clipLabel);
        clipPanel.add(clipList);
        clipPanel.add(clipLabel2);

        minPanel = new NameValue("min. value:");
        minPanel.setActionCommand("choose");
        minPanel.addActionListener(this);
        maxPanel = new NameValue("max. value:");
        maxPanel.setActionCommand("choose");
        maxPanel.addActionListener(this);
        attrib = (String)attributeList.getSelectedItem();
        s.ccs.addRequest( new ValueRange(attrib) );
        
        JPanel drawVectorsPanel = new JPanel();
        dvBool = false;
        drawVectorsPanel.setLayout(new BoxLayout(drawVectorsPanel, 
                                BoxLayout.X_AXIS));
        dvCheckBox = new JCheckBox("Draw");
        dvCheckBox.addItemListener(this);
        drawVectorsPanel.add(dvCheckBox);
        dvAttributeList = new JComboBox(attributes);
        dvAttributeList.setSelectedIndex(s.selectedAttributeIndex);
        dvAttributeList.addActionListener(this);
        dvAttributeList.setActionCommand("dvSelected");
        drawVectorsPanel.add(dvAttributeList);
        JLabel dvLabel2 = new JLabel("vectors x");
        drawVectorsPanel.add(dvLabel2);
        dvScale = new JTextField("0.01");
        drawVectorsPanel.add(dvScale);

        JButton chooseButton = new JButton("Recolor");
        chooseButton.setActionCommand("choose");
        chooseButton.addActionListener(this);
        chooseButton.setAlignmentX(CENTER_ALIGNMENT);

        contentPane.add(particlePanel);
        contentPane.add(attributeList);
        contentPane.add(linLog);
        contentPane.add(clipPanel);
        contentPane.add(minPanel);
        contentPane.add(maxPanel);
        contentPane.add(drawVectorsPanel);
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
                minPanel.getValue()+","+maxPanel.getValue()+","+
                clipHash.get((String)clipList.getSelectedItem());
            Family family;
            String key;
            for ( Enumeration en = s.Families.keys(); en.hasMoreElements(); ){
                key = (String)en.nextElement();
                family = (Family)s.Families.get(key);
                if ( family.on ){ message = message +","+ key; }
            }
            if( dvBool ) {
                s.ccs.addRequest( new DrawVectors( dvAttrib + ","+dvScale.getText() ) );
            }
            System.out.println(message);
            s.ccs.addRequest( new ChooseColorValue( message ) );
/*            String ll = (String)linLog.getSelectedItem();
            int type = 0;
            if (ll.equals("linear") ){type = 0;} else { type = 1;};
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(type);
                dos.writeDouble(Double.parseDouble(minPanel.getValue()));
                dos.writeDouble(Double.parseDouble(maxPanel.getValue()));
                dos.writeBytes(attrib);
                s.ccs.addRequest( new ChooseColorValue( baos.toByteArray() ) );
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}*/

        } else if ( "clip set".equals(e.getActionCommand()) ){
            s.selectedClippingIndex = clipList.getSelectedIndex(); 
        } else if ( "dvSelected".equals(e.getActionCommand()) ){
            s.selectedVectorIndex = dvAttributeList.getSelectedIndex();
            dvAttrib = (String)dvAttributeList.getSelectedItem();
        } else {
            s.selectedAttributeIndex = attributeList.getSelectedIndex();
            attrib = (String)attributeList.getSelectedItem();
            // attribute selected, so we need to find out its possible values
            s.ccs.addRequest( new ValueRange(attrib) );
        }
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if ( source == dvCheckBox ){
            if ( e.getStateChange() == ItemEvent.DESELECTED ) { dvBool = false;
            } else{ dvBool = true;}
            return;
        }
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
        
        attrib = (String)attributeList.getSelectedItem();
        attributes = new Vector();
        attributeList.removeAllItems();
        dvAttributeList.removeAllItems();
        for ( Enumeration en = s.Families.elements(); en.hasMoreElements(); ){
            family = (Family)en.nextElement();
            for ( int j = 0; j < family.attributes.size(); j++ ){
                if ( !attributes.contains(family.attributes.get(j)) && family.on ){
                    attributes.addElement(family.attributes.get(j));
                    if ( attrib.equals(family.attributes.get(j)) ) {
                        s.selectedAttributeIndex = attributes.size() -1;
                    }
                    attributeList.addItem(family.attributes.get(j));
                    dvAttributeList.addItem(family.attributes.get(j));
                }
            }
        }
        attributeList.setSelectedIndex(s.selectedAttributeIndex);
        dvAttributeList.setSelectedIndex(s.selectedAttributeIndex);
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
    
    private class DrawVectors extends CcsThread.request{
        public DrawVectors(String message){
            super("DrawVectors", message.getBytes());
            System.out.println(message);
        }
        public void handleReply(byte[] data) {
       }
    }

    private class ChooseColorValue extends CcsThread.request{
        public ChooseColorValue(String message){
            super("ChooseColorValue", message.getBytes());
        }
/*        public ChooseColorValue(byte [] bytes){
            super("ChooseColorValue", bytes);
        }*/
        public void handleReply(byte[] data) {
            setVisible(false);
            vp.getNewImage();
       }
    }
}
