//
//  ToolBarPanel.java
//  
//
//  Created by Greg Stinson on Sat Oct 18 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.UnknownHostException;

public class ToolBarPanel extends JPanel 
        implements ActionListener, ChangeListener {
    Simulation s;
    ViewPanel vp;
    RotateSlider upDownSlider;
    RotateSlider leftRightSlider;
    RotateSlider clockwiseSlider;

    public ToolBarPanel( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
        vp.rcm.tbp = this;
    
        setLayout( new BoxLayout(this,BoxLayout.X_AXIS) );
        
        JToolBar viewBar = new JToolBar();
        viewBar.setLayout( new GridLayout(3,1) );
        JButton xButton = new JButton("xall");
        xButton.setActionCommand("xall");
        xButton.addActionListener(this);
        JButton yButton = new JButton("yall");
        yButton.setActionCommand("yall");
        yButton.addActionListener(this);
        JButton zButton = new JButton("zall");
        zButton.setActionCommand("zall");
        zButton.addActionListener(this);
        viewBar.add(xButton);
        viewBar.add(yButton);
        viewBar.add(zButton);        

        JToolBar middleBar = new JToolBar();
        middleBar.setLayout( new GridLayout(3,1) );
        JButton reColor = new JButton("ReColor");
        reColor.setActionCommand("recolor");
        reColor.addActionListener(this);
        JButton newMap = new JButton("Switch Color Map");
        newMap.setActionCommand("switchmap");
        newMap.addActionListener(this);
        JButton clear = new JButton("Clear Boxes");
        clear.setActionCommand("clear");
        clear.addActionListener(this);
        middleBar.add(reColor);
//        middleBar.add(newMap);
//        middleBar.add(clear);
        
        JToolBar sliderBar = new JToolBar();
        sliderBar.setLayout( new GridLayout(3,1) );
        leftRightSlider = new RotateSlider("left","right");
        leftRightSlider.addChangeListener(this);
        clockwiseSlider = new RotateSlider("cw","ccw");
        clockwiseSlider.addChangeListener(this);
        upDownSlider = new RotateSlider("down","up");
        upDownSlider.addChangeListener(this);
        sliderBar.add(leftRightSlider);
        sliderBar.add(clockwiseSlider);
        sliderBar.add(upDownSlider);
        
        add(viewBar);
        add(middleBar);
        add(sliderBar);

    }

    public void actionPerformed(ActionEvent e){
        // Since most of the actions are exactly the same,
        // please find the actions in RightClickMenu.java
        vp.rcm.actionPerformed(e);
    }
    
    public void stateChanged(ChangeEvent e){
        RotateSlider slider = (RotateSlider)e.getSource();
        String sliderName = slider.leftLabel;
        double theta = (Math.PI/180.0)*( slider.oldAngle - slider.getValue());
        if (sliderName.equals("left")){ vp.rotateLeft(theta);} 
        else if (sliderName.equals("cw")){ vp.rotateCcw(theta); } 
        else if (sliderName.equals("down")){vp.rotateUp(theta);}
        slider.oldAngle = slider.getValue();
    }
    
    public void resetSliders(){
        clockwiseSlider.setValue(180);
        clockwiseSlider.oldAngle=180;
        upDownSlider.setValue(180);
        upDownSlider.oldAngle=180;
        leftRightSlider.setValue(180);
        leftRightSlider.oldAngle=180;
    }
}
