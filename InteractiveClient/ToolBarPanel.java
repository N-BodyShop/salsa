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
    JTextField zoomFactor;

    public ToolBarPanel( Simulation sim, ViewPanel viewP ){
        s = sim;
        vp = viewP;
        vp.rcm.tbp = this;
    
        setLayout( new BoxLayout(this,BoxLayout.X_AXIS) );
        
        JToolBar viewBar = new JToolBar();
        viewBar.setLayout( new GridLayout(3,1) );
        JButton xButton = new JButton("xall");
        xButton.setActionCommand("xall view");
        xButton.addActionListener(this);
        JButton yButton = new JButton("yall");
        yButton.setActionCommand("yall view");
        yButton.addActionListener(this);
        JButton zButton = new JButton("zall");
        zButton.setActionCommand("zall view");
        zButton.addActionListener(this);
        viewBar.add(xButton);
        viewBar.add(yButton);
        viewBar.add(zButton);        

        JToolBar middleBar = new JToolBar();
        middleBar.setLayout( new GridLayout(3,1) );
        JButton reColor = new JButton("ReColor");
        reColor.setActionCommand("recolor image");
        reColor.addActionListener(this);
        
        JPanel zoomPanel = new JPanel();
        zoomPanel.setLayout( new BoxLayout(zoomPanel,BoxLayout.X_AXIS) );
        JButton zoomIn = new JButton("zoomIn");
        zoomIn.setActionCommand("zoomIn");
        zoomIn.addActionListener(this);
        JButton zoomOut = new JButton("zoomOut");
        zoomOut.setActionCommand("zoomOut");
        zoomOut.addActionListener(this);
        zoomFactor = new JTextField("2.0");
        zoomFactor.setActionCommand("zoomIn");
        zoomFactor.addActionListener(this);
        zoomPanel.add(zoomIn);
        zoomPanel.add(zoomOut);
        zoomPanel.add(zoomFactor);
        
        JCheckBox fixO = new JCheckBox("Fix origin for rotation");
        fixO.setActionCommand("fixo");
        fixO.addActionListener(this);
        JButton clear = new JButton("Clear Boxes");
        clear.setActionCommand("clear");
        clear.addActionListener(this);
        middleBar.add(reColor);
        middleBar.add(zoomPanel);
//        middleBar.add(fixO);
//        middleBar.add(clear);
        
        JToolBar sliderBar = new JToolBar();
        sliderBar.setLayout( new GridLayout(3,1) );
//        JLabel rotateLabel = new JLabel("Rotations");
        leftRightSlider = new RotateSlider("left","right");
        leftRightSlider.addChangeListener(this);
        clockwiseSlider = new RotateSlider("cw","ccw");
        clockwiseSlider.addChangeListener(this);
        upDownSlider = new RotateSlider("down","up");
        upDownSlider.addChangeListener(this);
//        sliderBar.add(rotateLabel);
        sliderBar.add(leftRightSlider);
        sliderBar.add(clockwiseSlider);
        sliderBar.add(upDownSlider);
        resetSliders();
        
        add(viewBar);
        add(middleBar);
        add(sliderBar);

    }

    public void actionPerformed(ActionEvent e){
        String command =  e.getActionCommand();
        if (command.equals("zoomIn")) {
            vp.zoom( 1.0/(Double.parseDouble( zoomFactor.getText() )) );}
        else if (command.equals("zoomOut")) {
            vp.zoom( Double.parseDouble( zoomFactor.getText() ) );}
        // Since most of the actions are exactly the same,
        // please find the actions in RightClickMenu.java
        else{vp.rcm.actionPerformed(e);}
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
