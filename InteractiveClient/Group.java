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
    Hashtable groupPieces;
    
    public Group(){
        reset();
    }
	
    public void reset() {
        groupPieces = new Hashtable();
    }
}
