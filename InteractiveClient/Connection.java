//
//  Connection.java
//  
//
//  Created by Greg Stinson on Wed Sep 24 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;

public class Connection {
    String host;
    int port;
    DefaultListModel simlist;
    
    public Connection (){
        simlist = new DefaultListModel();
    }
}
