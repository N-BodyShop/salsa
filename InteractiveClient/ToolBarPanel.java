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
						  implements ActionListener, 
						  			 ChangeListener, 
									 ViewListener {
	WindowManager windowManager;
	SimulationView view;
	RotateSlider upDownSlider;
	RotateSlider leftRightSlider;
	RotateSlider clockwiseSlider;
    //JTextField zoomFactor;
	
	public ToolBarPanel(WindowManager wm, SimulationView v) {
		windowManager = wm;
		view = v;
    
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		Box b = new Box(BoxLayout.PAGE_AXIS);
		Box b2 = new Box(BoxLayout.LINE_AXIS);
		b2.add(new JLabel("Coloring: "));
		JComboBox coloringCombo = new JComboBox(windowManager.sim.createColoringModel());
		coloringCombo.setPrototypeDisplayValue("Density");
		coloringCombo.setSelectedIndex(0);
		coloringCombo.setActionCommand("chooseColoring");
		coloringCombo.addActionListener(this);
		b2.add(coloringCombo);
		b.add(b2);
		b2 = new Box(BoxLayout.LINE_AXIS);
		//b2.add(Box.createHorizontalGlue());
		b2.add(new JLabel("Group: "));
		JComboBox groupCombo = new JComboBox(windowManager.sim.createGroupModel());
		groupCombo.setPrototypeDisplayValue("Density");
		groupCombo.setSelectedIndex(0);
		groupCombo.setActionCommand("activateGroup");
		groupCombo.addActionListener(this);
		b2.add(groupCombo);
		b.add(b2);
		b.add(Box.createVerticalGlue());
		
		/*
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
        */
		JToolBar sliderBar = new JToolBar();
		sliderBar.setLayout(new GridLayout(3,1));
		upDownSlider = new RotateSlider("Down", "Up   ");
		upDownSlider.addChangeListener(this);
		leftRightSlider = new RotateSlider("Left", "Right");
		leftRightSlider.addChangeListener(this);
		clockwiseSlider = new RotateSlider("Cntr", "Clock");
		clockwiseSlider.addChangeListener(this);
		sliderBar.add(upDownSlider);
		sliderBar.add(leftRightSlider);
		sliderBar.add(clockwiseSlider);
		resetSliders();
        
		view.addViewListener(this);
		
        //add(middleBar);
		add(b);
        add(sliderBar);

    }

    public void actionPerformed(ActionEvent e) {
		String command =  e.getActionCommand();
		if(command.equals("chooseColoring")) {
			System.out.println("Choose coloring: " + ((JComboBox) e.getSource()).getSelectedItem());
			//view.activeColoring = ((JComboBox) e.getSource()).getSelectedIndex();
			view.activeColoring = ((Simulation.Coloring) windowManager.sim.colorings.get((String) ((JComboBox) e.getSource()).getSelectedItem())).id;
			view.getNewImage();
		} else if(command.equals("activateGroup")) {
			System.out.println("Activate group: " + ((JComboBox) e.getSource()).getSelectedItem());
			//view.activeGroup = ((JComboBox) e.getSource()).getSelectedIndex();
			view.activeGroup = ((Simulation.Group) windowManager.sim.groups.get((String) ((JComboBox) e.getSource()).getSelectedItem())).id;
			view.getNewImage();
		}
    }
    
    public void stateChanged(ChangeEvent e) {
        RotateSlider slider = (RotateSlider) e.getSource();
        String sliderName = slider.leftLabel;
        double theta = (Math.PI / 180.0) * (slider.getValue() - slider.oldAngle);
        if(theta == 0)
			return;
		if(sliderName.equals("Down"))
			view.rotateUp(theta);
		else if(sliderName.equals("Left"))
			view.rotateRight(theta);
		else if(sliderName.equals("Cntr"))
			view.rotateClock(theta);
        view.getNewImage();
		slider.oldAngle = slider.getValue();
		
    }
    
    public void resetSliders() {
		//when setValue is called, it will fire a stateChanged.  So, must assign old value first so that theta == 0 is detected
        clockwiseSlider.oldAngle = 0;
        clockwiseSlider.setValue(0);
        upDownSlider.oldAngle = 0;
        upDownSlider.setValue(0);
        leftRightSlider.oldAngle = 0;
        leftRightSlider.setValue(0);
    }
	
	public void rotationPerformed(ViewEvent e) { }
	
	public void viewReset(ViewEvent e) {
		resetSliders();
	}
}
