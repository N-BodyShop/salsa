import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * ManualPanel sets up/modifies the Manual tab of the CommandPane program
 */
public class ManualPanel extends JPanel {
	private JEditorPane textArea;
	private JTextField textField;
	private String currentText;

	public ManualPanel() {
		setBorder(BorderFactory.createTitledBorder("Input data and commands manually here"));
		setLayout(new BorderLayout());
		textArea = new JEditorPane();
		textArea.setEditable(false);
		textField = new JTextField();
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				currentText = textField.getText();
				textField.setText("");
				textArea.setText(currentText);

			}
		});
		textField.setBorder(BorderFactory.createTitledBorder("Type Commands here"));

		add(textArea, BorderLayout.CENTER);
		add(textField, BorderLayout.SOUTH);

	}
}
