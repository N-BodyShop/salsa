//
//  Salsa.java
//  
//
//  Created by Greg Stinson on Tue Sep 23 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.io.*;
import java.net.UnknownHostException;
public class Salsa  {

    public static void main(String s[]){
        if ( s.length >= 2){
            try{
                CcsThread ccs = new CcsThread( new Label(), s[0], Integer.parseInt(s[1]) );
                if(s.length == 2) {
                    ccs.addRequest( new ListSimulations(ccs));
 //                   ccs.addRequest( new AuthenticationRequest("nobody","nowhere",ccs));
                }else if(s.length == 3) {
                    Simulation sim = new Simulation(ccs);
                    sim.Name = s[2];
                    ccs.addRequest( new ChooseSimulation(sim));
                }
            } catch (UnknownHostException uhe) {            
                System.err.println( "Couldn't find host "+ s[0]+":"+s[1]+".");
            } catch (IOException ioe) {
                System.err.println( "Couldn't connect to "+ s[0]+":"+s[1]+".");
            }    
        } else if(s.length == 3){ 
            
        } else { ConnectFrame cf = new ConnectFrame();}
    }
}
