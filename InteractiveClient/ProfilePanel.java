import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * ProfilePanel sets up/modifies the Profile tab of the CommandPane program
 */
public class ProfilePanel extends JPanel implements ItemListener, ActionListener{
	private JButton b;
	private CommandPane refComPane;
	private JTextField xField;
	private JTextField yField;
	private JTextField zField;
	private JTextField shellField;
	private JTextField radiusField;
	private String geoString;
	private String spacingString;
	private JLabel current;

//*****************************************************************************************************************
	/**
	 * set up the profilePanel tab
	 */
	public ProfilePanel(CommandPane r) {
		refComPane = r;
		setBorder(BorderFactory.createTitledBorder("Create custom Profile"));
		setLayout(new GridLayout(6,1));

		/* initialize vars */
		JPanel main = new JPanel();

		/* set up subset panel */
		JLabel s = new JLabel("Subset being Profiled:");
		current = new JLabel((String)refComPane.getCurrentListItem());
		main.add(s); main.add(current); add(main);
		
		/* set up centerDefine panel */
		main = new JPanel();
		main.setBorder(BorderFactory.createTitledBorder("Define center"));
		xField = new JTextField(5);
		yField = new JTextField(5);
		zField = new JTextField(5);
		main.add(new JLabel("X coord:")); 
		main.add(xField); 
		main.add(new JLabel("Y coord:")); 
		main.add(yField); 
		main.add(new JLabel("Z coord:"));
		main.add(zField); 
		add(main);

		JPanel geoSpacing = new JPanel();
		geoSpacing.setLayout(new GridLayout(1,2));

		main = new JPanel();
		s = new JLabel("Select Geometry");
		String[] geo = { "cylindrical", "spherical" };
		geoString = "cylindrical";
		JComboBox c = new JComboBox(geo);
		c.addItemListener(this);
		main.add(s); main.add(c); geoSpacing.add(main);

		main = new JPanel();
		s = new JLabel("Select spacing");
		String[] spa = { "linear", "logarithmic"};
		spacingString = "linear";
		JComboBox d = new JComboBox(spa);
		d.addItemListener(this);
		main.add(s); main.add(d); geoSpacing.add(main);
		add(geoSpacing);
		
		/* set up shells panel */
		main = new JPanel();
		s = new JLabel("Number of Shells to work with: ");
		shellField = new JTextField(5);
		main.add(s); main.add(shellField); add(main);

		/* set up radius panel */
		main = new JPanel();
		s = new JLabel("Radius of smallest shell");
		radiusField = new JTextField(5);
		main.add(s); main.add(radiusField); add(main);

		b = new JButton("Launch Profile");
		b.setActionCommand("LAUNCH");
		b.addActionListener(this);
		add(b);
	}
	
//*****************************************************************************************************************
	/**
	 * void itemStateChanged
	 */
	public void itemStateChanged(ItemEvent e){
		if(((String)e.getItem()).compareTo("cylindrical") == 0 && e.getStateChange()==ItemEvent.SELECTED){
			System.out.println("cylinder case");
			geoString = "cylindrical";
		}else if(((String)e.getItem()).compareTo("spherical") == 0 && e.getStateChange()==ItemEvent.SELECTED){
			System.out.println("sphere case");
			geoString = "spherical";
		}else if(((String)e.getItem()).compareTo("linear") == 0 && e.getStateChange()==ItemEvent.SELECTED){
			System.out.println("linear case");
			spacingString = "linear";
		}else if(((String)e.getItem()).compareTo("logarithmic") == 0 && e.getStateChange()==ItemEvent.SELECTED){
			System.out.println("log case");
			spacingString = "logarithmic";
		}else{
			//System.out.println(e.paramString());
		}
	}
	
//*****************************************************************************************************************
	/**
	 * void actionPerformed
	 */
	public void actionPerformed(ActionEvent e){
		String command = e.getActionCommand();
		if(command=="LAUNCH"){
			if(xField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Please fill in the x field");
			}else if(yField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Please fill in the y field");
			}else if(zField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Please fill in the z field");
			}else if(shellField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Please fill in the shell field");
			}else if(radiusField.getText().compareTo("")==0){
				JOptionPane.showMessageDialog(this, "Please fill in the radius field");
			}else{
				System.out.println("Word:");
				System.out.println("X Field says: " + xField.getText());
				System.out.println("Y Field says: " + yField.getText());
				System.out.println("z Field says: " + zField.getText());
				System.out.println("the geometry is: " + geoString);
				System.out.println("the spacing is: " + spacingString);
				System.out.println("Shell field says: " + shellField.getText());
				System.out.println("Radius field says: " + radiusField.getText());
			}
		}
	}
	
//*****************************************************************************************************************
	/**
	 * update the label which tells the user which group they're profiling
	 */
	public void updateLable(String s){
		current.setText(s);
	}
	
//*****************************************************************************************************************
	/**
	 * access method for classes to get what the current profile group is
	 */
	public String getLabel(){
		return current.getText();
	}
}
