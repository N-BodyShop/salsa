//AtttributeManager.java

import charm.ccs.PythonAbstract;
import charm.ccs.PythonExecute;
import charm.ccs.PythonPrint;
import charm.ccs.PythonFinished;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.text.*;

public class AttributeManager extends Manager implements ActionListener, TreeSelectionListener, KeyListener {
	Simulation sim;
	JTree tree;
	DefaultTreeModel treeModel;
	JPanel displayPanel;
	JButton applyButton;
	JButton refreshButton;
	DefaultMutableTreeNode rootNode;
	DefaultMutableTreeNode familyNode;
	JLabel attributeNameLabel;
	JLabel attributeTypeLabel;
	JLabel attributeDimensionalityLabel;
	JLabel attributeDefinitionLabel;
	JLabel numberLabel;
	JTextArea attributeCodeArea;
	JFormattedTextField minValField;
	JFormattedTextField maxValField;
	JFormattedTextField sumField;
	JFormattedTextField meanField;

	JLabel attributeNameField;
	Box infoPanel;
	
	public AttributeManager(WindowManager wm) {
		super("Salsa: Attribute Manager", wm);
		
		sim = windowManager.sim;
		
		rootNode = new DefaultMutableTreeNode(sim.name);
		
		treeModel = new DefaultTreeModel(rootNode);
		tree = new JTree(treeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		refreshData();
		
		displayPanel = new JPanel();
		displayPanel.setBorder(BorderFactory.createTitledBorder("Attribute properties"));
		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.PAGE_AXIS));
		
		Box b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Attribute name: "));
		attributeNameField = new JLabel("");
		b.add(attributeNameField);
		displayPanel.add(b);

		infoPanel = new Box(BoxLayout.PAGE_AXIS);

		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Dimension: "));
		attributeDimensionalityLabel = new JLabel("");
		b.add(attributeDimensionalityLabel);
		infoPanel.add(b);

		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Data Type: "));
		attributeTypeLabel = new JLabel("");
		b.add(attributeTypeLabel);
		infoPanel.add(b);

		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Number of Particles: "));
		numberLabel = new JLabel("");
		b.add(numberLabel);
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

		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Sum: "));
		sumField = new JFormattedTextField(format);
		sumField.setColumns(10);
		b.add(sumField);
		infoPanel.add(b);
		displayPanel.add(infoPanel);

		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Mean: "));
		meanField = new JFormattedTextField(format);
		meanField.setColumns(10);
		b.add(meanField);
		infoPanel.add(b);
		displayPanel.add(infoPanel);

		JButton button = new JButton("Create new attribute");
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
		
		JPanel rhs = new JPanel(new BorderLayout());
		rhs.add(displayPanel, BorderLayout.CENTER);
		rhs.add(buttonPanel, BorderLayout.SOUTH);
		
		getContentPane().add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), rhs));
		
		pack();
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("new")) {
			System.out.println("Create new attribute!");
		} else if(e.getActionCommand().equals("apply")) {
			applyButton.setEnabled(false);
			System.out.println("Applied!");
		} else if(e.getActionCommand().equals("refresh")) {
		    refreshData();
		}
	}
	
	public void keyReleased(KeyEvent e) {
		int code = e.getKeyCode();
		int modifiers = e.getModifiers();
		if(modifiers == InputEvent.CTRL_MASK) {
			if(code == KeyEvent.VK_Q)
				windowManager.quit();
			else if(code == KeyEvent.VK_W)
				dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		}
	}
	
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

		if(node == null) {
			System.out.println("Null selection object!");
			return;
		} else {
		    if(node.isLeaf()) {
			// Fire off a request:
			// charm.getDimensions(family,attribute)
			// charm.getDataType(family,attribute')
			// charm.getAttributeRange(family,attribute)
			// charm.getAttributeSum('All',family,attribute)
			System.out.println("Selected node was: "
					   + ((String) node.getUserObject()));
			DefaultMutableTreeNode parentNode
			    = (DefaultMutableTreeNode) node.getParent();
			String familyName = (String)parentNode.getUserObject();
			String attrName = (String) node.getUserObject();
			attributeNameField.setText(attrName);
			String getRangeCode
			    = "_family='" + familyName + "'\n"
			    + "_attribute='" + attrName + "'\n"
			    + "ck.printclient(str(charm.getDataType(_family,_attribute))+','+str(charm.getDimensions(_family,_attribute))+','+str(charm.getNumParticles(_family))+','+str(charm.getAttributeRange(_family,_attribute)).strip('()')+','+str(charm.getAttributeSum('All',_family,_attribute)))\n";
			PythonExecute code = new PythonExecute(getRangeCode,
							       false, true, 0);
			HighLevelPython execute =
			    new HighLevelPython(code, windowManager.ccs,
						new GetRangeHandler());
			}
			else
				System.out.println("Selected node wasn't a leaf!");
		}
		
	}
	
	public void refreshData() {
		System.out.println("AttributeManager refreshed!");
		// Convert the Python list into a comma separated string
		String getFamiliesCode
		    = "ck.printclient(str(charm.getFamilies()).replace(\"', '\",\",\").strip(\"[]'\"))\n";
		PythonExecute code = new PythonExecute(getFamiliesCode,
						       false, true, 0);
		HighLevelPython execute = new HighLevelPython(code, windowManager.ccs, new GetFamiliesHandler());
		// This is double work; sim.Families should be merged
		// with the above
		windowManager.refreshAttributes();
	}
	
	public void keyTyped(KeyEvent e) { }
	public void keyPressed(KeyEvent e) { }

	public class GetFamiliesHandler extends PyPrintHandler {
		public void handle(String result) {
			System.out.println("Return from code execution: \"" + result + "\"");
			rootNode.removeAllChildren();
			treeModel.reload();
			
			DelimitedStringEnumeration flist
			    = new DelimitedStringEnumeration(result);
			int i = 0;
			while(flist.hasMoreElements()) {
			    String familyName = (String) flist.nextElement();

			    // Python code to produce comma delimited
			    // list of a family and its attributes.
			    String getAttributesCode = "ck.printclient("
				+ "'" + familyName 
				+ ",'+str(charm.getAttributes('"
				+ familyName
				+ "')).replace(\"', '\",\",\").strip(\"[]'\"))\n";
			    PythonExecute code = new PythonExecute(getAttributesCode,
						       false, true, 0);
			    HighLevelPython execute = new HighLevelPython(code, windowManager.ccs, new GetAttributesHandler());
			    }
		}
	}
	public class GetAttributesHandler extends PyPrintHandler {
		public void handle(String result) {
		    System.out.println("Return from code execution: \"" + result + "\"");
		    DelimitedStringEnumeration alist
			    = new DelimitedStringEnumeration(result);
		    String familyName = (String) alist.nextElement();
		    familyNode = new DefaultMutableTreeNode(familyName);
		    treeModel.insertNodeInto(familyNode, rootNode, rootNode.getChildCount());
		    while(alist.hasMoreElements()) {
			String aName = (String) alist.nextElement();
			treeModel.insertNodeInto(new DefaultMutableTreeNode(aName), familyNode, familyNode.getChildCount());
			}
		}
	    }

	public class GetRangeHandler extends PyPrintHandler {
		public void handle(String result) {
		    try {
			DelimitedStringEnumeration alist
				= new DelimitedStringEnumeration(result);
			String sValue = (String) alist.nextElement();
			attributeTypeLabel.setText(sValue);
			sValue = (String) alist.nextElement();
			attributeDimensionalityLabel.setText(sValue);
			sValue = (String) alist.nextElement();
			numberLabel.setText(sValue);
			double np = Double.valueOf(sValue).doubleValue();
			sValue = (String) alist.nextElement();
			minValField.setValue(new Double(sValue));
			sValue = (String) alist.nextElement();
			maxValField.setValue(new Double(sValue));
			sValue = (String) alist.nextElement();
			sumField.setValue(new Double(sValue));
			double sum = Double.valueOf(sValue).doubleValue();
			meanField.setValue(new Double(sum/np));
			}
		    catch(StringIndexOutOfBoundsException e) {
			System.err.println("Problem parsing attributes: bad index\n");
			}
		    catch(NumberFormatException e) {
			System.err.println("Problem parsing attributes: bad format\n");
			}
		}
	    }
}
