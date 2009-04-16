//
//  ListSimulations.java
//  
//
//  Created by Greg Stinson on Fri Dec 05 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.util.*;

import charm.ccs.CcsThread;


public class ListSimulations extends CcsThread.request {
	Vector simulationList = new Vector();
	
    public ListSimulations() {
		super("ListSimulations", (String) null);
    }

    public void handleReply(byte[] data) {
        String reply = new String(data);
        simulationList = new Vector();
        int index = -1;
        int lastindex = 0;
        while(index < reply.length() - 1) {
            lastindex = index + 1;
            index = reply.indexOf(",", lastindex);
            simulationList.addElement(reply.substring(lastindex, index));
        }
    }
}
