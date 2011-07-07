//ColoringManager.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import javax.swing.text.*;

import charm.ccs.CcsThread;


import java.text.*;

public class ColoringManager extends Manager 
							 implements ListSelectionListener,
							 			ActionListener {
	Simulation sim;
	int coloringCount = 0;
	
	JList coloringList;
	
	Box infoPanel;
	JTextField coloringNameField;
	JList activeFamilyList;
	JComboBox attributeNameBox;
	JComboBox logLinearBox;
	JComboBox clippingBox;
	JFormattedTextField minValField;
	JFormattedTextField maxValField;
	
	JPanel displayPanel;
	JButton applyButton;
	
	public ColoringManager(WindowManager wm) {
		super("Salsa: Coloring Manager", wm);
		
		sim = windowManager.sim;
		
		coloringList = new JList(sim.createColoringModel());
		coloringList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		coloringList.setVisibleRowCount(8);
		coloringList.setSelectedIndex(0);
		coloringList.setPrototypeCellValue("Log Density Color");
		coloringList.addListSelectionListener(this);
		
		JPanel lhs = new JPanel(new BorderLayout());
		//lhs.setBorder(BorderFactory.createTitledBorder("Colorings"));
		lhs.add(new JLabel("Colorings on Server:"), BorderLayout.NORTH);
		lhs.add(new JScrollPane(coloringList), BorderLayout.WEST);
		
		displayPanel = new JPanel();
		//displayPanel.setBorder(BorderFactory.createTitledBorder("Coloring information"));
		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.PAGE_AXIS));
		
		Box b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Coloring name: "));
		coloringNameField = new JTextField(15);
		b.add(coloringNameField);
		displayPanel.add(b);
		
		infoPanel = new Box(BoxLayout.PAGE_AXIS);
		
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Active families: "));
		activeFamilyList = new JList(sim.createFamilyListModel());
		b.add(activeFamilyList);
		infoPanel.add(b);
		
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Attribute: "));
		attributeNameBox = new JComboBox(sim.createAttributeModel());
		attributeNameBox.setActionCommand("chooseAttribute");
		attributeNameBox.addActionListener(this);
		b.add(attributeNameBox);
		infoPanel.add(b);
		
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Scaling: "));
		String[] loglinearchoices = {"Linear", "Logarithmic"};
		logLinearBox = new JComboBox(loglinearchoices);
		logLinearBox.setActionCommand("chooseScaling");
		logLinearBox.addActionListener(this);
		b.add(logLinearBox);
		infoPanel.add(b);
		
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Clipping: "));
		String[] clippingchoices = {"None", "Clip High Values", "Clip Low Values", "Clip Outside Range"};
		clippingBox = new JComboBox(clippingchoices);
		b.add(clippingBox);
		infoPanel.add(b);

		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Minimum value: "));
		DecimalFormat format = new DecimalFormat("0.######E0");
		minValField = new JFormattedTextField(format);
		minValField.setColumns(10);
		b.add(minValField);
		infoPanel.add(b);
		
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Maximum value: "));
		maxValField = new JFormattedTextField(format);
		maxValField.setColumns(10);
		b.add(maxValField);
		infoPanel.add(b);

		JButton button = new JButton("Swap Min and Max");
		button.setActionCommand("swap");
		button.addActionListener(this);
		infoPanel.add(button);
		
		displayPanel.add(infoPanel);
		
		button = new JButton("Create new coloring");
		button.setActionCommand("new");
		button.addActionListener(this);
		
		applyButton = new JButton("Apply changes");
		applyButton.setActionCommand("apply");
		applyButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(button);
		buttonPanel.add(applyButton);
		
		Box rhs = new Box(BoxLayout.PAGE_AXIS);
		rhs.add(displayPanel);
		rhs.add(Box.createVerticalGlue());
		rhs.add(buttonPanel);
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		getContentPane().add(lhs);
		getContentPane().add(rhs);
		
		pack();
	}
	
	/* Return the currently selected coloring */
	private Simulation.Coloring getColoring() {
		if (coloringList.getSelectedValue() == null) return null;
		return sim.findColoring((String)coloringList.getSelectedValue());
	}
	
	public void valueChanged(ListSelectionEvent e) {
		Simulation.Coloring c = getColoring();
		if(e.getValueIsAdjusting() == false && c != null) {
			coloringNameField.setText(c.name);
			if(c.infoKnown) {
				coloringNameField.setEnabled(true);
				activeFamilyList.clearSelection();
				int i = 0;
				for(Enumeration en = sim.families.keys(); en.hasMoreElements(); ++i)
					if(c.activeFamilies.indexOf((String) en.nextElement()) != -1)
						activeFamilyList.addSelectionInterval(i, i);
				activeFamilyList.setEnabled(true);
				attributeNameBox.setSelectedItem(c.attributeName);
				attributeNameBox.setEnabled(true);
				
				int index = 0;
				if(c.logarithmic)
					index = 1;
				logLinearBox.setSelectedIndex(index);
				logLinearBox.setEnabled(true);
				
				if(c.clipping.equals("clipno"))
					index = 0;
				else if(c.clipping.equals("cliphigh"))
					index = 1;
				else if(c.clipping.equals("cliplow"))
					index = 2;
				else if(c.clipping.equals("clipboth"))
					index = 3;
				clippingBox.setSelectedIndex(index);
				clippingBox.setEnabled(true);
				
				minValField.setValue(new Double(c.minValue));
				minValField.setEnabled(true);
				maxValField.setValue(new Double(c.maxValue));
				maxValField.setEnabled(true);
				
				applyButton.setEnabled(true);
			} else {
				//could switch cards here to display "Sorry, no knowledge" message?
				coloringNameField.setEnabled(false);
				activeFamilyList.setEnabled(false);
				attributeNameBox.setEnabled(false);
				logLinearBox.setEnabled(false);
				clippingBox.setEnabled(false);
				minValField.setEnabled(false);
				maxValField.setEnabled(false);
				applyButton.setEnabled(false);
			}

			updateDisplay();
		}
	}
	
	// Make the currently-selected coloring show up onscreen
	private void updateDisplay() {
		Simulation.Coloring c=getColoring();
		
		// Update the server-side coloring (if needed)
		if (c.id<0) {
			windowManager.ccs.doBlockingRequest(new CreateColoring(c));
		}
		
		// Make the new coloring active
		ViewingPanel vp=((ViewingPanel)windowManager.windowList.peek());
		vp.view.activeColoring = c.id;
		vp.view.getNewImage(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("swap")) { /* Swap min and max */
			double minValue = ((Number) minValField.getValue()).doubleValue();
			double maxValue = ((Number) maxValField.getValue()).doubleValue();
			minValField.setValue(new Double(maxValue));
			maxValField.setValue(new Double(minValue));
		} else if(command.equals("apply")) { /* Apply button: pull values from GUI, put to server */
			Simulation.Coloring c = getColoring();
			String oldName = c.name;
			c.name = coloringNameField.getText();
			c.infoKnown = true;
			Object[] activeFamilies = activeFamilyList.getSelectedValues();
			c.activeFamilies = "";
			for(int i = 0; i < activeFamilies.length; ++i)
				c.activeFamilies += ((String) activeFamilies[i]) + ",";
			c.attributeName = (String) attributeNameBox.getSelectedItem();
			c.logarithmic = (logLinearBox.getSelectedIndex() == 0 ? false : true);
			switch(clippingBox.getSelectedIndex()) {
				case 0: c.clipping = "clipno"; break;
				case 1: c.clipping = "cliphigh"; break;
				case 2: c.clipping = "cliplow"; break;
				case 3: c.clipping = "clipboth"; break;
			}
			c.minValue = ((Number) minValField.getValue()).doubleValue();
			c.maxValue = ((Number) maxValField.getValue()).doubleValue();
			
			c.id=-1; // Updated coloring needs a new coloring number
			updateDisplay();
		} else if(command.equals("new")) { /* Make a new coloring */
			++coloringCount;
			Simulation.Coloring c = new Simulation.Coloring("New Coloring " + coloringCount);
			//fill in c more correctly here
			for(Enumeration en = sim.families.keys(); en.hasMoreElements(); )
				c.activeFamilies += ((String) en.nextElement()) + ",";
			c.attributeName = (String) attributeNameBox.getSelectedItem();
			sim.colorings.add(c);
			coloringList.clearSelection();
			coloringList.setSelectedValue(c.name, true);
		} else if(command.equals("chooseAttribute")) {
			String attribute = (String) attributeNameBox.getSelectedItem();
			if(attribute == null)
			    return;
			
			double minVal = 1E200;
			double maxVal = -1E200;
			for(Enumeration en = sim.families.elements(); en.hasMoreElements(); ) {
				Simulation.Family family = (Simulation.Family) en.nextElement();
				if(family.attributes.containsKey(attribute)) {
					Simulation.Attribute attr = (Simulation.Attribute) family.attributes.get(attribute);
					if(attr.minValue < minVal)
						minVal = attr.minValue;
					if(attr.maxValue > maxVal)
						maxVal = attr.maxValue;
				}
			}
			minValField.setValue(new Double(minVal));
			maxValField.setValue(new Double(maxVal));
		} else if(command.equals("chooseScaling")) {
			/* nothing to do here */
		}
	}
	
	public class CreateColoring extends CcsThread.request {
		Simulation.Coloring c;
		public CreateColoring(Simulation.Coloring coloring) {
			super("CreateColoring", (byte[]) null);
			c = coloring;
			double lo=c.minValue, hi=c.maxValue;
			if (c.logarithmic) {
				lo=Math.log(lo) / Math.log(10.0);
				hi=Math.log(hi) / Math.log(10.0);
			}
			if (c.attributeName.equals("potential")) { /* swap min and max: should be viewed backwards (by default) */
				double t=lo; lo=hi; hi=t;
			}
			setData((c.name + "," + (c.logarithmic ? "logarithmic," : "linear,") + c.attributeName + "," + lo + "," + hi + "," + c.clipping + "," + c.activeFamilies).getBytes());
		}
		
		public void handleReply(byte[] data) {
			try {
				// The server will give me a number with which to refer to this coloring
				c.id = Integer.parseInt(new String(data));
			} catch(NumberFormatException e) {
				System.err.println("Problem parsing coloring id");
			}
		}
	}
}
