//
//  NameValue.java
//  
//
//  Created by Greg Stinson on Wed Sep 24 2003.
//  Copyright (c) 2003 University of Washington. All rights reserved.
//

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class NameValue extends JPanel{

    JTextField valueField;
    
    public NameValue(String name, String value){
        setLayout( new GridLayout(0,2) );
        setBorder(BorderFactory.createEmptyBorder(2,10,2,10));

        valueField = new JTextField(value);
        JLabel nameLabel = new JLabel(name,JLabel.LEFT);
        
        add(nameLabel,BorderLayout.WEST);
        add(valueField,BorderLayout.EAST);
    }
    
    public NameValue(String name){
        setLayout( new GridLayout(0,2) );
        setBorder(BorderFactory.createEmptyBorder(2,10,2,10));

        JLabel nameLabel = new JLabel(name);
        valueField = new JTextField(12);

        add(nameLabel,BorderLayout.WEST);
        add(valueField,BorderLayout.EAST);
    }

    public String getValue(){
        return(valueField.getText());
    }

    public void setValue(String value){
        valueField.setText(value);
    }
    
    public void addActionListener(ActionListener l){
        valueField.addActionListener(l);
    }

    public void setActionCommand(String command){
        valueField.setActionCommand(command);
    }
}
