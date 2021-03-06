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

/** A instance of Simulation holds information about the families of particles,
 colorings, and groups that can be used to manipulate a simulation.
 */
public class Simulation {
    String name;
	
	//clients can register to be notified when these structures change
    NotifyingHashtable families = new NotifyingHashtable();
	Vector colorings = new Vector();
	NotifyingHashtable groups = new NotifyingHashtable();

	/// The original origin of the simulation
	Vector3D origin;
	double boxSize;
        // max x,y and z of loaded simulation in sim coords
    double maxX, maxY, maxZ, minX, minY, minZ;
		
	public Simulation() { }

	static public class Family {
		public String name;
		public int index;
		public long numParticles;
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
	
	/// Get family information out of a Properties
	/// XXX This should be replaced by a python call
	public void fill(Properties props) {
	    System.err.println("Filling family information from network data");
		name = props.getProperty("simulationName");
		try {
			int numFamilies = Integer.parseInt(props.getProperty("numFamilies"));
			for(int i = 0; i < numFamilies; ++i) {
				Family family = new Family();
				family.name = props.getProperty("family-" + i + ".name");
				family.index = i;
				family.numParticles = Long.parseLong(props.getProperty("family-" + i + ".numParticles"));
				family.defaultColor = Integer.parseInt(props.getProperty("family-" + i + ".defaultColor"));
				family.numAttributes = Integer.parseInt(props.getProperty("family-" + i + ".numAttributes"));
				for(int j = 0; j < family.numAttributes; ++j) {
					Attribute attr = new Attribute();
					attr.name = props.getProperty("family-" + i + ".attribute-" + j + ".name");
					attr.dimensionality = props.getProperty("family-" + i + ".attribute-" + j + ".dimensionality");
					attr.dataType = props.getProperty("family-" + i + ".attribute-" + j + ".dataType");
					attr.definition = props.getProperty("family-" + i + ".attribute-" + j + ".definition");
					attr.minValue = Double.parseDouble(props.getProperty("family-" + i + ".attribute-" + j + ".minScalarValue"));
					attr.maxValue = Double.parseDouble(props.getProperty("family-" + i + ".attribute-" + j + ".maxScalarValue"));
					family.attributes.put(attr.name, attr);
					
					if (!attr.name.equals("position")) 
					{
						/* Make a Coloring for this attribute, if there isn't already one */
						Coloring c=findColoring(attr.name);
						if (c!=null) { /* just update existing coloring */
							c.minValue=Math.min(c.minValue,attr.minValue);
							c.maxValue=Math.max(c.maxValue,attr.maxValue);
							c.activeFamilies+=","+family.name;
						} else 
						{/* Create a new coloring */
							//System.out.println("Creating coloring for attribute "+attr.name+": index "+colorings.size());
							c=new Coloring(attr.name);
							c.id=-1; /* lazy create-on-demand by ColoringManager */
							c.name=attr.name;
							c.infoKnown=true;
							c.activeFamilies=family.name;
							c.attributeName=attr.name;
							c.minValue=attr.minValue;
							c.maxValue=attr.maxValue;
							c.logarithmic=false;
							c.clipping="clipno";
							colorings.add(c);
						}
						
						if (c.minValue>0 && c.maxValue>0 && c.maxValue/c.minValue>10.0) 
						{ /* Positive attributes with large dynamic range should be plotted in log scale */
							c.logarithmic=true;
						}
					}
				}
				families.put(family.name, family);
			}
		} catch(NumberFormatException e) {
			System.err.println("Problem parsing simulation properties");
			e.printStackTrace();
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
				colorings.add(c);
			}
		} catch(NumberFormatException e) {
			System.err.println("Problem parsing simulation properties");
			e.printStackTrace();
		}
	}
	
	/// Look up Coloring based on its name.
	public Coloring findColoring(String name) {
		for (int i=0;i<colorings.size();i++) 
		{
			Coloring c=(Coloring)colorings.elementAt(i);
			if (c.name.equals(name)) return c;
		}
		return null;
	}
	
	/// A ColoringModel can be used to present a view of the names of the Colorings for a JList.
	public class ColoringModel extends DefaultComboBoxModel {
		public ColoringModel() {
		}
		
		public int getSize() {
			return colorings.size();
		}
		
		public Object getElementAt(int index) {
			Coloring c=(Coloring)colorings.elementAt(index);
			return c.name;
		}
	}
	
	public ColoringModel createColoringModel() {
		return new ColoringModel();
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

	public void fillGroups() {
		Group g = new Group("All");
		g.id = 0;
		g.infoKnown = false;
		groups.put(g.name, g);
	}

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

	static public class FamilyListModel extends AbstractListModel
	    implements ChangeListener {
	    Vector familyNames = new Vector();
	    Hashtable families = null;
	    public FamilyListModel(NotifyingHashtable cf) 
	    {
		families = cf;
		stateChanged(null);
		cf.addChangeListener(this);
	    }
	    
	    public void stateChanged(ChangeEvent ev) {
		familyNames.clear();
		for(Enumeration e = families.keys(); e.hasMoreElements(); )
				familyNames.add(e.nextElement());
		fireContentsChanged(this, -1, -1);
	    }
		
		public int getSize() {
			return familyNames.size();
		}
		
		public Object getElementAt(int index) {
			return familyNames.get(index);
		}
	}
	
	public FamilyListModel createFamilyListModel() {
	    return new FamilyListModel(families);
	}

	static public class AttributeModel extends DefaultComboBoxModel
	    implements ChangeListener {
	    Vector attributeNames = new Vector();
	    Hashtable families = null;
		
	    public AttributeModel(NotifyingHashtable cf) {
		families = cf;
		stateChanged(null);
		cf.addChangeListener(this);
	    }
		
	    public int getSize() {
		return attributeNames.size();
	    }
		
	    public Object getElementAt(int index) {
		return attributeNames.get(index);
	    }
		
	    public void stateChanged(ChangeEvent ev) {
		attributeNames.clear();
		for(Enumeration e = families.elements(); e.hasMoreElements(); ) {
		    for(Enumeration e2 = ((Family) e.nextElement()).attributes.keys(); e2.hasMoreElements(); ) {
			String name = ((String) e2.nextElement());
			if(!attributeNames.contains(name))
			    attributeNames.add(name);
			}
		    }
		fireContentsChanged(this, -1, -1);
	    }
	    }
    
	public AttributeModel createAttributeModel() {
	    return new AttributeModel(families);
	}

	static public class ScalarAttributeModel extends DefaultComboBoxModel
	    implements ChangeListener {
	    Vector attributeNames = new Vector();
	    Hashtable families = null;
		
	    public ScalarAttributeModel(NotifyingHashtable cf) {
		families = cf;
		stateChanged(null);
		cf.addChangeListener(this);
	    }
		
	    public int getSize() {
		return attributeNames.size();
	    }
		
	    public Object getElementAt(int index) {
		return attributeNames.get(index);
	    }
		
	    public void stateChanged(ChangeEvent ev) {
		attributeNames.clear();
		for(Enumeration e = families.elements(); e.hasMoreElements(); ) {
		    Family f = ((Family) e.nextElement());
		    for(Enumeration e2 = f.attributes.keys(); e2.hasMoreElements(); ) {
			String name = ((String) e2.nextElement());
			Attribute a = ((Attribute) f.attributes.get(name));
			if((!attributeNames.contains(name)) && a.dimensionality.equals("scalar"))
			    attributeNames.add(name);
			}
		    }
		fireContentsChanged(this, -1, -1);
	    }
	    }
    
	public ScalarAttributeModel createScalarAttributeModel() {
	    return new ScalarAttributeModel(families);
	}
}
