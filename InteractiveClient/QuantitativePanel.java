import javax.swing.*;


/**
 * QuantitativePanel sets up/modifies the quantitative tab of the CommandPane program
 */
public class QuantitativePanel extends JPanel {
	JTextArea infoField;

	public QuantitativePanel() {
		this("toString displayed here");
	}

	public QuantitativePanel(String arg){
		infoField = new JTextArea(arg);
		infoField.setEditable(false);
		JScrollPane sp = new JScrollPane(infoField);
		add(sp);
	}

	public void setInfo(String arg){
		infoField.setText(arg);
	}
}
