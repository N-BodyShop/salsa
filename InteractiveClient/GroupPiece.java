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
    double min;
    double max;
    
    public GroupPiece(){
        reset();
    }
	
    public void reset() {
        min = 0;
        max = 0;
    }
    
    public GroupPiece(double mini, double maxi){
        min = mini;
        max = maxi;
    }
}
