//
//  Simulation.java
//  
//
//  Created by Greg Stinson on Mon Sep 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import javax.swing.*;
import java.util.*;
import java.awt.image.*;
import javax.swing.event.*;

public class Simulation {
    String name;
    NotifyingHashtable families = new NotifyingHashtable();
	NotifyingHashtable colorings = new NotifyingHashtable();
	NotifyingHashtable groups = new NotifyingHashtable();
	//Vector colorings = new Vector();
	//Vector groups = new Vector();
	
	Vector3D origin;
	double boxSize;
	
	//ColoringModel coloringModel;
	//GroupModel groupModel;
	
	/*
    ColorModel cm;
    int selectedAttributeIndex, selectedVectorIndex;
    int selectedClippingIndex;
    Hashtable Groups;
    String selectedGroup;
    boolean groupSelecting;
    int centerMethod;
    Vector3D rotationOrigin;
    
	*/
	
	public Simulation() {
		//coloringModel = new ColoringModel(colorings);
		//groupModel = new GroupModel(groups);
	}

	static public class Family {
		public String name;
		public int index;
		public int numParticles;
		public int numAttributes;
		public NotifyingHashtable attributes = new NotifyingHashtable();
		public int defaultColor;
	}
	
	
	static public class Attribute {
		public String name;
		public String dimensionality;
		public String dataType;
		public String definition;
		public double minValue;
		public double maxValue;
	}
	
	public void fill(Properties props) {
		name = props.getProperty("simulationName");
		try {
			int numFamilies = Integer.parseInt(props.getProperty("numFamilies"));
			for(int i = 0; i < numFamilies; ++i) {
				Family family = new Family();
				family.name = props.getProperty("family-" + i + ".name");
				family.index = i;
				family.numParticles = Integer.parseInt(props.getProperty("family-" + i + ".numParticles"));
				family.defaultColor = Integer.parseInt(props.getProperty("family-" + i + ".defaultColor"));
				family.numAttributes = Integer.parseInt(props.getProperty("family-" + i + ".numAttributes"));
				for(int j = 0; j < family.numAttributes; ++j) {
					Attribute attr = new Attribute();
					attr.name = props.getProperty("family-" + i + ".attribute-" + j + ".name");
					attr.dimensionality = props.getProperty("family-" + i + ".attribute-" + j + ".dimensions");
					attr.dataType = props.getProperty("family-" + i + ".attribute-" + j + ".dataType");
					attr.definition = props.getProperty("family-" + i + ".attribute-" + j + ".definition");
					attr.minValue = Double.parseDouble(props.getProperty("family-" + i + ".attribute-" + j + ".minScalarValue"));
					attr.maxValue = Double.parseDouble(props.getProperty("family-" + i + ".attribute-" + j + ".maxScalarValue"));
					family.attributes.put(attr.name, attr);
				}
				families.put(family.name, family);
			}
		} catch(NumberFormatException e) {
			System.err.println("Problem parsing simulation properties");
			e.printStackTrace();
		}
	}
	
	static public class Coloring {
		public String name;
		public int id;
		public boolean infoKnown;
		public String activeFamilies;
		public String attributeName;
		public boolean logarithmic;
		public String clipping;
		public double minValue;
		public double maxValue;
		
		public Coloring(String n) {
			name = n;
			id = 0;
			infoKnown = true;
			activeFamilies = "";
			attributeName = "";
			logarithmic = false;
			clipping = "clipno";
			minValue = 0;
			maxValue = 0;
		}
	}
	
	static public class Group {
		public String name;
		public int id;
		public boolean infoKnown;
		public String attributeName;
		public double minValue;
		public double maxValue;
		
		public Group(String n) {
			name = n;
			id = 0;
			infoKnown = true;
			attributeName = "";
			minValue = 0;
			maxValue = 0;
		}
	}
	
	public void fillColorings(Properties props) {
		try {
			int numColorings = Integer.parseInt(props.getProperty("numColorings"));
			for(int i = 0; i < numColorings; ++i) {
				Coloring c = new Coloring(props.getProperty("coloring-" + i + ".name"));
				c.id = Integer.parseInt(props.getProperty("coloring-" + i + ".id"));
				c.infoKnown = Boolean.valueOf(props.getProperty("coloring-" + i + ".infoKnown")).booleanValue();
				if(c.infoKnown) {
					c.activeFamilies = props.getProperty("coloring-" + i + ".activeFamilies");
					c.logarithmic = Boolean.valueOf(props.getProperty("coloring-" + i + ".logarithmic")).booleanValue();
					c.clipping = props.getProperty("coloring-" + i + ".clipping");
					c.minValue = Double.parseDouble(props.getProperty("coloring-" + i + ".minValue"));
					c.maxValue = Double.parseDouble(props.getProperty("coloring-" + i + ".maxValue"));
				}
				colorings.put(c.name, c);
			}
			//coloringModel.update();
		} catch(NumberFormatException e) {
			System.err.println("Problem parsing simulation properties");
			e.printStackTrace();
		}
	}
	
	public void fillGroups() {
		Group g = new Group("All");
		g.id = 0;
		g.infoKnown = false;
		groups.put(g.name, g);
		//groupModel.update();
	}
	/*
	static public class ColoringModel extends DefaultComboBoxModel {
		Vector colorings;
		Vector coloringNames = new Vector();
		
		public ColoringModel(Vector v) {
			colorings = v;
			update();
		}
		
		public int getSize() {
			return coloringNames.size();
		}
		
		public Object getElementAt(int index) {
			return coloringNames.get(index);
		}
		
		public void update() {
			coloringNames.clear();
			for(Enumeration e = colorings.elements(); e.hasMoreElements(); )
				coloringNames.add(((Coloring) e.nextElement()).name);
			fireContentsChanged(this, -1, -1);
		}
		
	}
	*/
	static public class ColoringModel extends DefaultComboBoxModel implements ChangeListener {
		Vector coloringNames = new Vector();
		Hashtable colorings = null;
		
		public ColoringModel(NotifyingHashtable c) {
			colorings = c;
			stateChanged(null);
			c.addChangeListener(this);
		}
		
		public int getSize() {
			return coloringNames.size();
		}
		
		public Object getElementAt(int index) {
			return coloringNames.get(index);
		}
		
		public void stateChanged(ChangeEvent ev) {
			coloringNames.clear();
			for(Enumeration e = colorings.elements(); e.hasMoreElements(); )
				coloringNames.add(((Coloring) e.nextElement()).name);
			fireContentsChanged(this, -1, -1);
		}
	}
	
	public ColoringModel createColoringModel() {
		return new ColoringModel(colorings);
	}
	/*
	static public class GroupModel extends DefaultComboBoxModel {
		Vector groups;
		Vector groupNames = new Vector();
		
		public GroupModel(Vector v) {
			groups = v;
			update();
		}
		
		public int getSize() {
			return groupNames.size();
		}
		
		public Object getElementAt(int index) {
			return groupNames.get(index);
		}
		
		public void update() {
			groupNames.clear();
			for(Enumeration e = groups.elements(); e.hasMoreElements(); )
				groupNames.add(((Group) e.nextElement()).name);
			fireContentsChanged(this, -1, -1);
		}
		
	}
	*/
	static public class GroupModel extends DefaultComboBoxModel implements ChangeListener {
		Vector groupNames = new Vector();
		Hashtable groups = null;
		
		public GroupModel(NotifyingHashtable c) {
			groups = c;
			stateChanged(null);
			c.addChangeListener(this);
		}
		
		public int getSize() {
			return groupNames.size();
		}
		
		public Object getElementAt(int index) {
			return groupNames.get(index);
		}
		
		public void stateChanged(ChangeEvent ev) {
			groupNames.clear();
			for(Enumeration e = groups.elements(); e.hasMoreElements(); )
				groupNames.add(((Group) e.nextElement()).name);
			fireContentsChanged(this, -1, -1);
		}
	}
	
	public GroupModel createGroupModel() {
		return new GroupModel(groups);
	}

	public class FamilyListModel extends AbstractListModel {
		Vector familyNames = new Vector();
		public FamilyListModel() {
			for(Enumeration e = families.keys(); e.hasMoreElements(); )
				familyNames.add(e.nextElement());
		}
		
		public int getSize() {
			return familyNames.size();
		}
		
		public Object getElementAt(int index) {
			return familyNames.get(index);
		}
	}
	
	public Vector getAttributeNames() {
		Vector attributeNames = new Vector();
		for(Enumeration e = families.elements(); e.hasMoreElements(); ) {
			for(Enumeration e2 = ((Family) e.nextElement()).attributes.keys(); e2.hasMoreElements(); ) {
				String name = ((String) e2.nextElement());
				if(!attributeNames.contains(name))
					attributeNames.add(name);
			}
		}
		return attributeNames;
	}
	
}
