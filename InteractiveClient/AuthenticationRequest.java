//
//  AuthenticationRequest.java
//  
//
//  Created by Greg Stinson on Fri Dec 05 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

public class AuthenticationRequest extends CcsThread.request {

    CcsThread ccs;

    public AuthenticationRequest(String username, String password, CcsThread ccsT) {
        super("AuthenticateNChilada", (username + ":" + password).getBytes());
        ccs = ccsT;
    }

    public void handleReply(byte[] data) {
        //initialize simlist
        ccs.addRequest( new ListSimulations(ccs) );
    }
}