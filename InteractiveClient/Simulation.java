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
    int numberOfColors;
    ColorModel cm;
    Hashtable Families;
    int selectedAttributeIndex;
    Hashtable Groups;
    String selectedGroup;
    boolean groupSelecting;
    int centerMethod;
    Vector3D rotationOrigin;
    
    public Simulation(CcsThread ccsThread){
        ccs = ccsThread;
        reset();
    }
	
    public void reset() {
        Name = new String();
        Families = new Hashtable();
        Groups = new Hashtable();
        Groups.put( "All", new Group() );
        selectedGroup = "All";
        centerMethod = 0;
        selectedAttributeIndex = 0;
    }
}
