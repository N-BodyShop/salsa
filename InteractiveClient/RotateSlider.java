//
//  RotateSlider.java
//  
//
//  Created by Greg Stinson on Sun Oct 19 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class RotateSlider extends JSlider implements MouseWheelListener{

    String leftLabel;
    int oldAngle;

    public RotateSlider( String leftLab, String rightLabel ){
        super(0,359,180);
        leftLabel = leftLab;
        Hashtable labelTable = new Hashtable();
        labelTable.put( new Integer( 0 ), new JLabel(leftLabel) );
        labelTable.put( new Integer( 359 ), new JLabel(rightLabel) );
        setLabelTable( labelTable );
        
        setPaintLabels(true);
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        setValue(getValue()+e.getWheelRotation());
        fireStateChanged();
    }

}
