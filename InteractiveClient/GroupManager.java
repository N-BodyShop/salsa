//GroupManager.java

import charm.ccs.CcsThread;
import charm.ccs.PythonAbstract;
import charm.ccs.PythonExecute;
import charm.ccs.PythonPrint;
import charm.ccs.PythonFinished;
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
	
    JList groupList, markgroupList;
	
	Box infoPanel;
	JTextField groupNameField;
	JComboBox attributeNameBox;
	JFormattedTextField minValField;
	JFormattedTextField maxValField;
	
	JPanel displayPanel;
	JButton applyButton;
	JButton refreshButton;
	
	public GroupManager(WindowManager wm) {
		super("Salsa: Group Manager", wm);
		
		sim = windowManager.sim;
		
		groupList = new JList(sim.createGroupModel());
		groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupList.setVisibleRowCount(8);
		groupList.setPrototypeCellValue("Log Density Color");
		markgroupList = new JList(sim.createGroupModel());
		markgroupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		markgroupList.setLayoutOrientation(JList.VERTICAL);
		
		JPanel lhs = new JPanel(new BorderLayout());
		lhs.setBorder(BorderFactory.createTitledBorder("Groups"));
                lhs.setLayout(new BoxLayout(lhs, BoxLayout.Y_AXIS));

		Box b2 = new Box(BoxLayout.LINE_AXIS);
		b2.add(new JLabel("Active Group:"));
		JComboBox groupCombo = new JComboBox(windowManager.sim.createGroupModel());
		groupCombo.setPrototypeDisplayValue("Density");
		groupCombo.setSelectedIndex(0);
		groupCombo.setActionCommand("activateGroup");
		groupCombo.addActionListener(this);
		b2.add(groupCombo);
		lhs.add(b2, BorderLayout.NORTH);

		Box b3 = new Box(BoxLayout.LINE_AXIS);
	        b3.add(new JLabel("Marked groups: "), BorderLayout.WEST);
		JScrollPane markPanel = new JScrollPane(markgroupList, 
							ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
							ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	
		JButton markbutton = new JButton("Apply mark");
		
		markbutton.setActionCommand("applymark");
		markbutton.addActionListener(this);
		b3.add(markPanel);
		b3.add(markbutton);
	        lhs.add(b3);
		lhs.add(new JScrollPane(groupList), BorderLayout.WEST);

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
		attributeNameBox = new JComboBox(sim.createScalarAttributeModel());
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
		
		refreshButton = new JButton("Refresh");
		refreshButton.setActionCommand("refresh");
		refreshButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(button);
		buttonPanel.add(applyButton);
		buttonPanel.add(refreshButton);
		
		Box rhs = new Box(BoxLayout.PAGE_AXIS);
		rhs.add(displayPanel);
		rhs.add(Box.createVerticalGlue());
		rhs.add(buttonPanel);
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		getContentPane().add(lhs);
		getContentPane().add(rhs);
		pack();
		
		groupList.addListSelectionListener(this);
		groupList.setSelectedIndex(0);
		markgroupList.addListSelectionListener(this);
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
				groupNameField.setEnabled(true);
				attributeNameBox.setEnabled(false);
				minValField.setEnabled(false);
				maxValField.setEnabled(false);
				applyButton.setEnabled(false);
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("activateGroup")) {
			//System.out.println("Activate group: " + ((JComboBox) e.getSource()).getSelectedItem());
			((ViewingPanel)windowManager.windowList.peek()).view.activeGroup = ((Simulation.Group) windowManager.sim.groups.get((String) ((JComboBox) e.getSource()).getSelectedItem())).name;
			((ViewingPanel)windowManager.windowList.peek()).view.getNewImage(true);
		} else if(command.equals("apply")) {
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
		} else if(command.equals("refresh")) { // get new list of
						       // groups from server
		    // Convert the Python list into a comma separated string
		    String getGroupsCode
			= "ck.printclient(str(charm.getGroups()).replace(\"', '\",\",\").strip(\"[]'\"))\n";
		    PythonExecute code = new PythonExecute(getGroupsCode,
						false, true, 0);
		    HighLevelPython execute = new HighLevelPython(code, windowManager.ccs, new GetGroupsHandler());
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

	        } else if(command.equals("applymark")){
		    {
			String unmarkgroupscode
			    = "ck.printclient(str(charm.unmarkParticlesGroup('All')))";
			PythonExecute unmarkcode = new PythonExecute(unmarkgroupscode,false, true, 0);
		        HighLevelPython execute = new HighLevelPython(unmarkcode, windowManager.ccs, new MarkGroupsHandler());
			}
		    Object[] markedvalues = markgroupList.getSelectedValues();
		    int markarraysize = markedvalues.length;
		    for (int i=0;i<markarraysize;i++){
		        String markgroupscode
			    = "ck.printclient(str(charm.markParticlesGroup('"+markedvalues[i]+"')))";
			PythonExecute markcode = new PythonExecute(markgroupscode,false, true, 0);
		        HighLevelPython execute = new HighLevelPython(markcode, windowManager.ccs, new MarkGroupsHandler());}
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
	public class GetGroupsHandler extends PyPrintHandler {
		public void handle(String result) {
			System.out.println("Return from code execution: \"" + result + "\"");
			sim.groups.clear();
			DelimitedStringEnumeration glist
			    = new DelimitedStringEnumeration(result);
			while(glist.hasMoreElements()) {
			    Simulation.Group g = new Simulation.Group((String) glist.nextElement());
			    sim.groups.put(g.name, g);
			    }
		}
	}
    
	public class MarkGroupsHandler extends PyPrintHandler{
	    public void handle(String result){
		((ViewingPanel)windowManager.windowList.peek()).view.getNewImage(true);
	    }
	}
}
