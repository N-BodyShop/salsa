//
//  ActivateGroup.java
//  
//
//  Created by Greg Stinson on Thu Dec 04 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

public class ActivateGroup extends CcsThread.request{
    ViewPanel vp;
    public ActivateGroup(String gName, ViewPanel viewP){
        super("ActivateGroup", gName.getBytes());
        vp = viewP;
        System.out.println("Sent ActivateGroup message: "+gName);
    }
    public void handleReply(byte[] data) {
        vp.getNewImage();
    }
}
