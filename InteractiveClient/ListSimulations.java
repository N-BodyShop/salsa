//
//  ListSimulations.java
//  
//
//  Created by Greg Stinson on Fri Dec 05 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.util.*;

public class ListSimulations extends CcsThread.request {

    CcsThread ccs;
    
    public ListSimulations(CcsThread ccsT) {
            super("ListSimulations", null);
            ccs = ccsT;
    }

    public void handleReply(byte[] data) {
        String reply = new String(data);
        Vector simlist = new Vector();
        int index = -1;
        int lastindex = 0;
        while( index < reply.length()-1 ){
            lastindex = index + 1;
            index = reply.indexOf(",",lastindex);
  //          System.out.println("Adding:  "+reply.substring(lastindex,index));
            simlist.addElement(reply.substring(lastindex,index));
        }
        // we've established a connection, now we can choose what
        // to look at
        ChooseSimulationFrame csf = new ChooseSimulationFrame(ccs,simlist);
    }
}
