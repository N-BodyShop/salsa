//
//  Group.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import java.util.*;
import java.awt.image.*;

public class Group {
    String Name;
    Vector groupPieces;
    
    public Group(String N){
        Name = N;
        reset();
    }
	
    public void reset() {
        groupPieces = new Vector();
    }
}
