//GroupManager.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.text.*;

public class GroupManager extends Manager 
							 implements ListSelectionListener,
							 			ActionListener {
	Simulation sim;
	int groupCount = 0;
	
	JList groupList;
	
	Box infoPanel;
	JTextField groupNameField;
	JComboBox attributeNameBox;
	JFormattedTextField minValField;
	JFormattedTextField maxValField;
	
	JPanel displayPanel;
	JButton applyButton;
	
	public GroupManager(WindowManager wm) {
		super("Salsa: Group Manager", wm);
		
		sim = windowManager.sim;
		
		groupList = new JList(sim.createGroupModel());
		groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupList.setVisibleRowCount(8);
		groupList.setPrototypeCellValue("Log Density Color");
		
		JPanel lhs = new JPanel();
		lhs.setBorder(BorderFactory.createTitledBorder("Groups"));
		lhs.add(new JScrollPane(groupList));
		
		displayPanel = new JPanel();
		displayPanel.setBorder(BorderFactory.createTitledBorder("Group definition"));
		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.PAGE_AXIS));
		
		Box b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Group name: "));
		groupNameField = new JTextField(15);
		b.add(groupNameField);
		displayPanel.add(b);
		
		infoPanel = new Box(BoxLayout.PAGE_AXIS);
				
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Attribute: "));
		attributeNameBox = new JComboBox(sim.getAttributeNames());
		attributeNameBox.setActionCommand("chooseAttribute");
		attributeNameBox.addActionListener(this);
		b.add(attributeNameBox);
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
		
		displayPanel.add(infoPanel);
		
		JButton button = new JButton("Create new group");
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
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS));
		getContentPane().add(lhs);
		getContentPane().add(rhs);
		
		pack();
		
		groupList.addListSelectionListener(this);
		groupList.setSelectedIndex(0);
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting() == false && groupList.getSelectedValue() != null) {
			Simulation.Group g = (Simulation.Group) sim.groups.get(groupList.getSelectedValue());
			groupNameField.setText(g.name);
			if(g.infoKnown) {
				groupNameField.setEnabled(true);
				attributeNameBox.setSelectedItem(g.attributeName);
				attributeNameBox.setEnabled(true);
				minValField.setValue(new Double(g.minValue));
				minValField.setEnabled(true);
				maxValField.setValue(new Double(g.maxValue));
				maxValField.setEnabled(true);
				applyButton.setEnabled(true);
			} else {
				//could switch cards here to display "Sorry, no knowledge" message?
				groupNameField.setEnabled(false);
				attributeNameBox.setEnabled(false);
				minValField.setEnabled(false);
				maxValField.setEnabled(false);
				applyButton.setEnabled(false);
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("apply")) {
			Simulation.Group g = (Simulation.Group) sim.groups.get(groupList.getSelectedValue());
			String oldName = g.name;
			g.name = groupNameField.getText();
			g.infoKnown = true;
			g.attributeName = (String) attributeNameBox.getSelectedItem();
			g.minValue = ((Number) minValField.getValue()).doubleValue();
			g.maxValue = ((Number) maxValField.getValue()).doubleValue();
			windowManager.ccs.addRequest(new CreateGroup(g));
			if(g.name != oldName) {
				sim.groups.remove(oldName);
				sim.groups.put(g.name, g);
			}
			groupList.clearSelection();
			groupList.setSelectedValue(g.name, true);
		} else if(command.equals("new")) {
			++groupCount;
			Simulation.Group g = new Simulation.Group("New Group " + groupCount);
			//fill g more correctly here
			sim.groups.put(g.name, g);
			groupList.clearSelection();
			groupList.setSelectedValue(g.name, true);
		} else if(command.equals("chooseAttribute")) {
			String attribute = (String) attributeNameBox.getSelectedItem();
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
		}
	}
	
	public class CreateGroup extends CcsThread.request {
		Simulation.Group g;
		public CreateGroup(Simulation.Group group) {
			super("CreateGroup", (byte[]) null);
			g = group;
			setData((g.name + "," + g.attributeName + "," + g.minValue + "," + g.maxValue).getBytes());
		}
		
		public void handleReply(byte[] data) {
			try {
				g.id = Integer.parseInt(new String(data));
			} catch(NumberFormatException e) {
				System.err.println("Problem parsing group id");
			}
		}
	}
}
