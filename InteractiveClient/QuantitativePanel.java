import javax.swing.*;


/**
 * QuantitativePanel sets up/modifies the quantitative tab of the CommandPane program
 */
public class QuantitativePanel extends JPanel {
	JTextField infoField;

	public QuantitativePanel() {
		this("toString displayed here");
	}

	public QuantitativePanel(String arg){
		infoField = new JTextField(arg);
		infoField.setEditable(false);
		add(infoField);
	}

	public void setInfo(String arg){
		infoField.setText(arg);
	}
}
