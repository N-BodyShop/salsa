import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class GroupQuery extends JFrame
                        implements ActionListener, KeyListener {
    
    JTextField groupNameField;
    SimulationView simulationView;
    
    public GroupQuery(SimulationView v) {
	super("Salsa: Name group");

	simulationView = v;

	JPanel displayPanel;
	
	displayPanel = new JPanel();
	Box b = new Box(BoxLayout.PAGE_AXIS);
	b.add(new JLabel("Group Name: "));
	groupNameField = new JTextField(15);
	groupNameField.setText("Default Name");
	groupNameField.selectAll();
	groupNameField.addKeyListener(this);
	b.add(groupNameField);
	displayPanel.add(b);
	JButton button = new JButton("Create group");
	button.setMnemonic(KeyEvent.VK_ENTER);
	button.addActionListener(this);
	displayPanel.add(button);
	getContentPane().add(displayPanel);
	pack();
	}
    public void actionPerformed(ActionEvent e) {
	
	System.out.println("Creating group: " + groupNameField.getText());
	simulationView.request2D();
	simulationView.makeBox(groupNameField.getText());
	simulationView.windowManager.groupManager.refreshButton.doClick();
	setVisible(false);
    }

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode()==KeyEvent.VK_ENTER)
		{
			System.out.println("Creating group: " + groupNameField.getText());
			simulationView.request2D();
			simulationView.makeBox(groupNameField.getText());
			setVisible(false);
		}
	}

	public void keyReleased(KeyEvent e) {
		// Nothing
		
	}

	public void keyTyped(KeyEvent e) {
		// Nothing
		
	}
}

	
