//  This is the "Rendering" tab in the Sidebar.
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

public class RenderingPanel extends JPanel 
						  implements ActionListener, 
						  			 ChangeListener, 
									 ViewListener {
	WindowManager windowManager;
	SimulationView view;
	RotateSlider upDownSlider;
	RotateSlider leftRightSlider;
	RotateSlider clockwiseSlider;
	JFormattedTextField minMassText;
	JFormattedTextField maxMassText;
        JFormattedTextField zoomfieldText;
        JFormattedTextField orientationText;
	JCheckBox splatterCheck;
	JCheckBox disable3D;
        JButton zoomapply;

	public RenderingPanel(WindowManager wm, SimulationView v) {
		windowManager = wm;
		view = v;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
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
		disable3D = new JCheckBox("Disable 3D Volume Impostors", false);
		disable3D.setActionCommand("change3D");
		disable3D.addActionListener(this);
		
		//arrowCheck = new JCheckBox("Velocity Arrows", false);
		//JButton butt = new JButton("Update mass range");
		//butt.addActionListener(this);
		Box b3 = new Box(BoxLayout.PAGE_AXIS);
		b3.add(minMassText);
		b3.add(maxMassText);
		b3.add(splatterCheck); 
		b3.add(disable3D); 
		b3.add(sliderBar);
		Box b4 = new Box(BoxLayout.LINE_AXIS);
		//b4.add(b);
		b4.add(b3);
		add(b4);

		Box b5 = new Box(BoxLayout.LINE_AXIS);
		b5.add(new JLabel("Zoom factor: "));
		zoomfieldText = new JFormattedTextField(view.coord.factor);
		zoomfieldText.setColumns(10);
		b5.add(zoomfieldText);
		Box b7 = new Box(BoxLayout.PAGE_AXIS);
		JButton zoomapply = new JButton("Apply zoom");
		zoomapply.setActionCommand("zoomapply");
		zoomapply.addActionListener(this);
		b7.add(zoomapply);
		b5.add(b7);
		add(b5);

		Box b6 = new Box(BoxLayout.LINE_AXIS);
		b6.add(new JLabel("Orientation: "));
		orientationText = new JFormattedTextField("0,0,0");
		orientationText.setColumns(10);
		b6.add(orientationText);		
		JButton orientationapply = new JButton("Apply orientation");
		orientationapply.setActionCommand("orientationapply");
		orientationapply.addActionListener(this);
		b6.add(orientationapply);
		add(b6);
		
    }


    public void actionPerformed(ActionEvent e) {
		String command =  e.getActionCommand();
		if(command.equals("changeMassRange")) {
			//System.out.println("Changing mass range");
			view.minMass = ((Number) minMassText.getValue()).doubleValue();
			view.maxMass = ((Number) maxMassText.getValue()).doubleValue();
			System.out.println("Min mass: " + view.minMass);
			System.out.println("Max mass: " + view.maxMass);
			view.doSplatter = (splatterCheck.isSelected() ? 1 : 0);
			System.out.println("Splatter? " + view.doSplatter);
			view.getNewImage(true);
		}
		if(command.equals("change3D")) {
			view.disable3D = disable3D.isSelected();
		}
		if(command.equals("zoomapply")) {
		        /*view.coord.origin = view.coord.origin.plus(
			view.coord.x.scalarMultiply(view.delta/view.coord.factor*2.0*(-1*view.width2D*0.5)).plus(
			view.coord.y.scalarMultiply(view.delta/view.coord.factor*2.0*(view.height2D*0.5))
			));*/
		        view.coord.factor = Float.parseFloat(zoomfieldText.getText());
		     	((ViewingPanel)windowManager.windowList.peek()).view.getNewImage(true);
			System.out.println(view.coord.origin);
		}
		if(command.equals("orientationapply")) {
		        String delims = "[,]";
		        String[] eulerangles = orientationText.getText().split(delims);
		        view.coord.rotateClock(Float.valueOf(eulerangles[2])*Math.PI/180.);
		        view.coord.rotateUp(Float.valueOf(eulerangles[1])*Math.PI/180.);
		        view.coord.rotateClock(Float.valueOf(eulerangles[0])*Math.PI/180.);
		     	((ViewingPanel)windowManager.windowList.peek()).view.getNewImage(true);
		}
    }
    
    public void stateChanged(ChangeEvent e) {
        RotateSlider slider = (RotateSlider) e.getSource();
        String sliderName = slider.leftLabel;
        double theta = (Math.PI / 180.0) * (slider.getValue() - slider.oldAngle);
        if(theta == 0)
			return;
		if(sliderName.equals("Down"))
			view.coord.rotateUp(theta);
		else if(sliderName.equals("Left"))
			view.coord.rotateRight(theta);
		else if(sliderName.equals("Cntr"))
			view.coord.rotateClock(theta);
        view.getNewImage(false);
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
	
    public void updateZoomFactor(){
	zoomfieldText.setValue(view.coord.factor);
	System.out.println("hi");
    }
}
