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

public class RotateSlider extends JSlider implements MouseWheelListener {

    String leftLabel;
    int oldAngle;

    public RotateSlider(String leftLab, String rightLabel) {
        super(-180, 180, 0);
        leftLabel = leftLab;
        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(-180), new JLabel(leftLabel));
        labelTable.put(new Integer(180), new JLabel(rightLabel));
        setLabelTable(labelTable);
        setPaintLabels(true);
		
		addMouseWheelListener(this);
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        setValue(getValue() - 15 * e.getWheelRotation());
    }
	
}
