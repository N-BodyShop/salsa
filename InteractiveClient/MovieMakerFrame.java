//
//  MovieMakerFrame.java
//  
//
//  Created by Greg Stinson on Fri Oct 24 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.UnknownHostException;

public class MovieMakerFrame extends JFrame {

    public MovieMakerFrame(){
        setTitle("Choose Simulation");
        setLocationRelativeTo(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, 
                                BoxLayout.Y_AXIS));
                                
///        contentPane.add();
        
        pack();
        setVisible(true);

    }
}
