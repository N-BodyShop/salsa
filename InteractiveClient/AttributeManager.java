//AtttributeManager.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

public class AttributeManager extends Manager implements ActionListener, TreeSelectionListener, KeyListener {
	Simulation sim;
	JTree tree;
	JPanel displayPanel;
	JButton applyButton;
	DefaultMutableTreeNode rootNode;
	JLabel attributeNameLabel;
	JLabel attributeTypeLabel;
	JLabel attributeDimensionalityLabel;
	JLabel attributeDefinitionLabel;
	JTextArea attributeCodeArea;
	
	public AttributeManager(WindowManager wm) {
		super("Salsa: Attribute Manager", wm);
		
		sim = windowManager.sim;
		
		rootNode = new DefaultMutableTreeNode(sim.name);
		rootNode.add(new DefaultMutableTreeNode("Fake Family 1"));
		rootNode.add(new DefaultMutableTreeNode("Fake Family 2"));
		
		tree = new JTree(rootNode);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		
		displayPanel = new JPanel();
		displayPanel.setBorder(BorderFactory.createTitledBorder("Attribute properties"));
		displayPanel.add(new JLabel("Stuff goes here"));
		
		JButton button = new JButton("Create new attribute");
		button.setActionCommand("new");
		button.addActionListener(this);
		
		applyButton = new JButton("Apply changes");
		applyButton.setActionCommand("apply");
		applyButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(button);
		buttonPanel.add(applyButton);
		
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
			if(node.isLeaf())
				System.out.println("Selected node was: " + ((String) node.getUserObject()));
			else
				System.out.println("Selected node wasn't a leaf!");
		}
		
	}
	
	public void refreshData() {
		System.out.println("AttributeManager refreshed!");
	}
	
	public void keyTyped(KeyEvent e) { }
	public void keyPressed(KeyEvent e) { }
}
