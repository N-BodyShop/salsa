//
//  GroupPiece.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import java.util.*;
import java.awt.image.*;

public class GroupPiece {
    String attribute;
    double min;
    double max;
    
    public GroupPiece(){
        reset();
    }
	
    public void reset() {
        attribute = new String();
        min = 0;
        max = 0;
    }
    
    public GroupPiece(String attrib, double mini, double maxi){
        attribute = attrib;
        min = mini;
        max = maxi;
    }
}
