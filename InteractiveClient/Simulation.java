//
//  Simulation.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import java.util.*;

public class Simulation {
    String Name;
    DefaultComboBoxModel attributes;
    int selectedAttributeIndex;
    int numberOfFamilies;
    int numberOfColors;
    Vector Families;
    
    public Simulation() {
		reset();
	}
	
	public void reset() {
        Name = new String();
        attributes = new DefaultComboBoxModel();
        selectedAttributeIndex = 0;
        Families = new Vector();
    }
}
