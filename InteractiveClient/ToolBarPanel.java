//
//  ToolBarPanel.java
//  
//
//  Created by Greg Stinson on Sat Oct 18 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.UnknownHostException;
import java.text.*;

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
	JFormattedTextField minMassText;
	JFormattedTextField maxMassText;
	JCheckBox splatterCheck;
	int numTools=6;
	JButton toolbox []=new JButton[numTools];
	
	public ToolBarPanel(WindowManager wm, SimulationView v) {
		windowManager = wm;
		view = v;
    
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
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
		
		Box toolboxbox = new Box(BoxLayout.LINE_AXIS);
		String s="Rotate";
		toolbox[0]=new JButton(s);
		toolbox[0].setActionCommand(s);
		toolbox[0].addActionListener(this);
		toolboxbox.add(toolbox[0]);
		s="Zoom+";
		toolbox[1]=new JButton(s);
		toolbox[1].setActionCommand(s);
		toolbox[1].addActionListener(this);
		toolboxbox.add(toolbox[1]);
		s="Zoom-";
		toolbox[2]=new JButton(s);
		toolbox[2].setActionCommand(s);
		toolbox[2].addActionListener(this);
		toolboxbox.add(toolbox[2]);
		s="Select Sphere";
		toolbox[3]=new JButton(s);
		toolbox[3].setActionCommand(s);
		toolbox[3].addActionListener(this);
		toolboxbox.add(toolbox[3]);
		s="Select Box";
		toolbox[4]=new JButton(s);
		toolbox[4].setActionCommand(s);
		toolbox[4].addActionListener(this);
		toolboxbox.add(toolbox[4]);
		s="Ruler";
		toolbox[5]=new JButton(s);
		toolbox[5].setActionCommand(s);
		toolbox[5].addActionListener(this);
		toolboxbox.add(toolbox[5]);
		
		for (int i=0; i<6; i++)
			toolbox[i].setEnabled(true);
		toolbox[0].setEnabled(false);
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
		
		DecimalFormat format = new DecimalFormat("0.######E0");
		minMassText = new JFormattedTextField(format);
		minMassText.setValue(new Double(0));
		minMassText.setActionCommand("changeMassRange");
		minMassText.addActionListener(this);
		minMassText.setColumns(10);
		maxMassText = new JFormattedTextField(format);
		maxMassText.setValue(new Double(1E-7));
		maxMassText.setActionCommand("changeMassRange");
		maxMassText.addActionListener(this);
		maxMassText.setColumns(10);
		splatterCheck = new JCheckBox("Splatter Visual", false);
		splatterCheck.setActionCommand("changeMassRange");
		splatterCheck.addActionListener(this);
		//arrowCheck = new JCheckBox("Velocity Arrows", false);
		//JButton butt = new JButton("Update mass range");
		//butt.addActionListener(this);
		Box b3 = new Box(BoxLayout.PAGE_AXIS);
		b3.add(minMassText);
		b3.add(maxMassText);
		b3.add(splatterCheck); 
		Box b4 = new Box(BoxLayout.LINE_AXIS);
		b4.add(b);
        b4.add(sliderBar);
		b4.add(b3);
		
		add(toolboxbox);
		add(b4);
    }

    public void actionPerformed(ActionEvent e) {
		String command =  e.getActionCommand();
		if(command.equals("chooseColoring")) {
			//System.out.println("Choose coloring: " + ((JComboBox) e.getSource()).getSelectedItem());
			view.activeColoring = ((Simulation.Coloring) windowManager.sim.colorings.get((String) ((JComboBox) e.getSource()).getSelectedItem())).id;
			view.getNewImage();
		} else if(command.equals("activateGroup")) {
			//System.out.println("Activate group: " + ((JComboBox) e.getSource()).getSelectedItem());
			view.activeGroup = ((Simulation.Group) windowManager.sim.groups.get((String) ((JComboBox) e.getSource()).getSelectedItem())).name;
			view.getNewImage();
		} else if(command.equals("changeMassRange")) {
			//System.out.println("Changing mass range");
			view.minMass = ((Number) minMassText.getValue()).doubleValue();
			view.maxMass = ((Number) maxMassText.getValue()).doubleValue();
			System.out.println("Min mass: " + view.minMass);
			System.out.println("Max mass: " + view.maxMass);
			view.doSplatter = (splatterCheck.isSelected() ? 1 : 0);
			System.out.println("Splatter? " + view.doSplatter);
			view.getNewImage();
		}
		else if (command.equals("Rotate"))
		{
			switchTool(0);
		}
		else if (command.equals("Zoom+"))
		{
			switchTool(1);
		}
		else if (command.equals("Zoom-"))
		{
			switchTool(2);
		}
		else if (command.equals("Select Sphere"))
		{
			switchTool(3);
		}
		else if (command.equals("Select Box"))
		{
			switchTool(4);
		}
		else if (command.equals("Ruler"))
		{
			switchTool(5);
		}
    }
    public void switchTool(int tool)
    {
    	view.activeTool=tool;
		view.selectState=0;
		
		for (int i=0; i<numTools; i++)
			toolbox[i].setEnabled(true);
		toolbox[tool].setEnabled(false);
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
