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
    int numberOfFamilies;
    int numberOfColors;
    ColorModel cm;
    Vector Families;
    int selectedAttributeIndex;
    Vector Groups;
    boolean groupSelecting;
    int centerMethod;
    Vector3D rotationOrigin;
    
    public Simulation(CcsThread ccsThread){
        ccs = ccsThread;
        reset();
    }
	
    public void reset() {
        Name = new String();
        Families = new Vector();
        Groups = new Vector();
        Group group = new Group( "All" );
        Groups.addElement( group );
        centerMethod = 0;
        selectedAttributeIndex = 0;
    }
}
