import javax.swing.*;


/**
 * QuantitativePanel sets up/modifies the quantitative tab of the CommandPane program
 */
public class QuantitativePanel extends JPanel {

	public QuantitativePanel() {
		this("toString displayed here");
	}

	public QuantitativePanel(String arg){
		JTextField t = new JTextField(arg);
		t.setEditable(false);
		add(t);
	}
}
