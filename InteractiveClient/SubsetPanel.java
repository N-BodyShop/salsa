import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * SubsetPanel sets up/modifies the Subset tab of the CommandPane program
 */
public class SubsetPanel extends JPanel {

	public SubsetPanel() {
		setLayout(new GridLayout(2,1));
		JPanel spacialSubset = new JPanel();
		JPanel propertySubset = new JPanel();
		spacialSubset.setBorder(BorderFactory.createTitledBorder("Spacial Subsetting"));
		propertySubset.setBorder(BorderFactory.createTitledBorder("Property Subsetting"));
		
		/* set up spacialSubset */

		/* set up propertySubset */
		propertySubset.setLayout(new GridLayout(2,1));
		JPanel lowerHalf = new JPanel();
		lowerHalf.setLayout(new GridLayout(2,1));
		JTextField t = new JTextField("commands for subsetting go here");
		JButton b = new JButton("Display subset");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				/* check that the command is valid */
				System.out.println("Button pushed in subset panel");
			}
		});
		lowerHalf.add(t);
		lowerHalf.add(b);
		propertySubset.add(new JPanel());
		propertySubset.add(lowerHalf);

		add(spacialSubset);
		add(propertySubset);
	}
}
