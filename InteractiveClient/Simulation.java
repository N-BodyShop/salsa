//
//  Simulation.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import java.util.*;
import java.awt.image.*;

public class Simulation {
    String Name;
    CcsThread ccs;
    DefaultComboBoxModel attributes;
    int selectedAttributeIndex;
    int numberOfFamilies;
    int numberOfColors;
    ColorModel cm;
    Vector Families;
    Vector Groups;
    int centerMethod;
    
    public Simulation(CcsThread ccsThread){
        ccs = ccsThread;
        reset();
    }
	
    public void reset() {
        Name = new String();
        attributes = new DefaultComboBoxModel();
        selectedAttributeIndex = 0;
        Families = new Vector();
        Groups = new Vector();
        centerMethod = 0;
    }
}
