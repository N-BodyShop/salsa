import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class GroupQuery extends JFrame
                        implements ActionListener {
    
    JTextField groupNameField;
    SimulationView simulationView;
    
    public GroupQuery(SimulationView v) {
	super("Salsa: Name group");

	simulationView = v;

	JPanel displayPanel;
	
	displayPanel = new JPanel();
	Box b = new Box(BoxLayout.LINE_AXIS);
	b.add(new JLabel("Group Name: "));
	groupNameField = new JTextField(15);
	b.add(groupNameField);
	displayPanel.add(b);
	JButton button = new JButton("Create group");
	button.setActionCommand("new");
	button.addActionListener(this);
	displayPanel.add(button);
	getContentPane().add(displayPanel);
	pack();
	}
    public void actionPerformed(ActionEvent e) {
	
	System.out.println("Creating group: " + groupNameField.getText());
	simulationView.makeBox(groupNameField.getText());
    
	setVisible(false);
    }
}

	
