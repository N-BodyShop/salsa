//
//  NameValue.java
//  
//
//  Created by Greg Stinson on Wed Sep 24 2003.
//  Copyright (c) 2003 University of Washington. All rights reserved.
//

import javax.swing.*;
import java.awt.*;

public class NameValue extends JPanel{

    JTextField valueField;
    
    public NameValue(String name, String value){
//        setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
        setBorder(BorderFactory.createEmptyBorder(2,10,2,10));

        JLabel nameLabel = new JLabel(name);
        valueField = new JTextField(value);
        
        add(nameLabel,BorderLayout.WEST);
        add(valueField,BorderLayout.EAST);
    }
    
    public NameValue(String name){
//        setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
        setBorder(BorderFactory.createEmptyBorder(2,10,2,10));

        JLabel nameLabel = new JLabel(name);
        valueField = new JTextField(8);

        add(nameLabel,BorderLayout.WEST);
        add(valueField,BorderLayout.EAST);
    }

    public String getValue(){
        return(valueField.getText());
    }

    public void setValue(String value){
        valueField.setText(value);
    }
}
