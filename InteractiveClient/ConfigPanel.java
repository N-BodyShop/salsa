import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * class ConfigPanel sets up/modifies the Config tab of the CommandPane program
 */
public class ConfigPanel extends JPanel implements ActionListener {
	private CommandPane refComPane;
	private JButton render;
	private JTextField particleField;
	private JTextField cmField;
	private JTextField cfField;
	private String viewString;
	private String particleText;
	private String cmText;
	private String cfText;

	public ConfigPanel(CommandPane p) {
		setBorder(BorderFactory.createTitledBorder("Configure the Image to be displayed"));
		setLayout(new BorderLayout());
		refComPane = p;

		JPanel mainPanel = new JPanel(new GridLayout(4,1));
		JPanel labelsAndTexts = new JPanel(new GridLayout(0,2));
		JPanel buttons = new JPanel();

		JPanel labels = new JPanel(new GridLayout(3,1));
		labels.add(new JLabel("Type of Particles:"));
		labels.add(new JLabel("Type of Color Map:"));
		labels.add(new JLabel("Color Function:"));
		labelsAndTexts.add(labels);
		labels = new JPanel(new GridLayout(3,1));
		particleField = new JTextField();
		cmField = new JTextField();
		cfField = new JTextField();
		labels.add(particleField);
		labels.add(cmField);
		labels.add(cfField);
		labelsAndTexts.add(labels);

		ButtonGroup groupOfButtons = new ButtonGroup();
		JRadioButton[] radio = new JRadioButton[3];
		radio[0] = new JRadioButton("Xall");
		radio[0].setActionCommand("XALL");
		radio[0].setSelected(true);
		radio[0].addActionListener(this);
		radio[1] = new JRadioButton("Yall");
		radio[1].setActionCommand("YALL");
		radio[1].addActionListener(this);
		radio[2] = new JRadioButton("Zall");
		radio[2].setActionCommand("ZALL");
		radio[2].addActionListener(this);
		for (int i = 0; i < 3; i++){
			groupOfButtons.add(radio[i]);
			buttons.add(radio[i]);
		}
		viewString = "xall";

		mainPanel.add(labelsAndTexts);

		render = new JButton("Render Image");
		render.setActionCommand("RENDER");
		render.addActionListener(this);
		buttons.add(render);
		mainPanel.add(buttons);
		//mainPanel.add(config);
		
		add(mainPanel, BorderLayout.CENTER);

	}
	
//*****************************************************************************************************************
	
	public void actionPerformed(ActionEvent e){
		String command = e.getActionCommand();
		if(command=="XALL"){
			viewString = "xall";
		}else if(command=="YALL"){
			viewString = "yall";
		}else if(command=="ZALL"){
			viewString = "zall";
		}else if(command=="RENDER"){
			/* render button has been pushed, check to make sure all fields are filled out */
			/*if(particleField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Enter type of particles");
			}else if(cmField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Enter a color map");
			}else if(cfField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Enter a color function");
			}else{*/
				/* all fields filled out, display field info and render the image */
				particleText = particleField.getText();
				cmText = cmField.getText();
				cfText = cfField.getText();
				System.out.println("RENDERed");
				System.out.println("Here's what u got: ");
				System.out.println("particle field says: " + particleText);
				System.out.println("color map field says: " + cmText);
				System.out.println("color func field says: " + cfText);
				System.out.println("the view is: " + viewString);


				JFrame frame = new JFrame("NChilada *Main* Visualization");
				frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						render.setEnabled(true);
					}
				});
				
				JPanel mainPanel = new JPanel();
				ViewingPanel leftPanel = new ViewingPanel(refComPane.getHost(), refComPane.getPort(), viewString, refComPane, frame);
				SelectionView rightPanel = new SelectionView(refComPane.getHost(), refComPane.getPort(), leftPanel.getXVector(), leftPanel.getYVector(), leftPanel.getZVector(), leftPanel.getOriginVector(), leftPanel);
				mainPanel.add(leftPanel);
				mainPanel.add(rightPanel);


				frame.getContentPane().add(mainPanel);
				frame.pack();
				frame.setVisible(true);
				render.setEnabled(false);
				
				refComPane.enableTwo();
				refComPane.updateStatus("Simulation Launched");
			//}
		}
	}

}
