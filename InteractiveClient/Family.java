//
//  Family.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import java.util.*;
import java.awt.image.*;

public class Family {
    String Name;
    int numberOfAttributes;
    Vector attributes;
    JCheckBox checkBox;
    boolean on;
    
    public Family(String N){
        Name = N;
        checkBox = new JCheckBox(Name);
        checkBox.setSelected(true);
        reset();
    }
	
    public void reset() {
        attributes = new Vector();
        numberOfAttributes = 0;
        on = true;
    }
}
