import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.*;

/**
 * CommandPane is the main command window for the NChilada Visualization client.
 * It allows the user to select, view, modify, and store information about particle
 * simulations
 * Original mastermind: Amir Kouretchian
 * Version 0.Best
 */
public class CommandPane implements ActionListener, ItemListener {
	private JFrame frame;
	private JTabbedPane tabPane;
	private JMenuBar menu;
	//private JToolBar toolBar;
	private JComboBox groups;
	private String[] groupNames;
	private int numGroups;
	private JLabel status;
	private InputPanel inputPanel;
	private ConfigPanel configPanel;
	private SubsetPanel subsetPanel;
	private QuantitativePanel quantitativePanel;
	private ProfilePanel profilePanel;
	private CommandPrompt manualPanel;
	private String host;
	private int port;
	private CcsThread ccs;
	private JButton activate;

//*****************************************************************************************************************
	/**
	 * run the CommandPane
	 * @param args the arguments to the String[]
	 */
	public static void main(String[] args) {
		CommandPane pane = new CommandPane(600,500);
	}
	
//*****************************************************************************************************************
	/**
	 * Default constructor
	 */
	public CommandPane() {
		//for accessor methods
	}

//*****************************************************************************************************************
	/**
	 * This constructor sets up the GUI
	 */
	public CommandPane(int w, int h) {
		/* initialize instance variables */
		frame = new JFrame("NChilada Command Window");
		frame.getContentPane().setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.LEFT);
		menu = new JMenuBar();
		//toolBar = new JToolBar();
		numGroups = 0;
		groupNames = new String[numGroups];
		groups = new JComboBox(/*groupNames*/);
		groups.addItemListener(this);
		status = new JLabel("Welcome to the big NChilada!");

		/* setup menu */
		JMenu file = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");
		exit.setActionCommand("EXIT");
		exit.addActionListener(this);
		file.add(exit);
		menu.add(file);
		
		/* setup toolbar
		JButton save = new JButton("Save Configuration as group");
		save.setActionCommand("SAVE_BUTTON");
		save.addActionListener(this);
		toolBar.add(save);
		toolBar.add(new JButton("Save Image"));
		*/

		/* setup tabs */
		inputPanel = new InputPanel(this);
		configPanel = new ConfigPanel(this);
		subsetPanel = new SubsetPanel();
		quantitativePanel = new QuantitativePanel();
		profilePanel = new ProfilePanel(this);
		manualPanel = new CommandPrompt(this);
		tabPane.addTab("Input", inputPanel);
		tabPane.addTab("Configure", configPanel);
		tabPane.addTab("Subset", subsetPanel);
		tabPane.addTab("Quantitative", quantitativePanel);
		tabPane.addTab("Profile", profilePanel);
		tabPane.addTab("Manual", manualPanel);

		/* add to frame */
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(menu, BorderLayout.NORTH);
		//panel.add(toolBar, BorderLayout.CENTER);
		JPanel inset = new JPanel(new BorderLayout());
		inset.add(new JLabel("Config Groups"), BorderLayout.NORTH);
		inset.add(groups, BorderLayout.SOUTH);
		activate = new JButton("Activate Subset");
		activate.setActionCommand("activate");
		activate.addActionListener(this);
		inset.add(activate, BorderLayout.WEST);
		panel.add(inset, BorderLayout.EAST);
		frame.getContentPane().add(panel, BorderLayout.NORTH);
		frame.getContentPane().add(tabPane, BorderLayout.CENTER);
		frame.getContentPane().add(status, BorderLayout.SOUTH);

		frame.setSize(w,h);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		disable();
	}
	
	public void launch(){
		configPanel.render.doClick();
	}

//*****************************************************************************************************************
	/**
	 * update the status bar
	 * @param s the new status text
	 */
	public void updateStatus(String s){
		status.setText(s);
	}

//*****************************************************************************************************************
	/* helper method */
	private void show(){
		for(int x = 0; x<numGroups; x++){
			System.out.println("List item " + (x+1) + " = " + groupNames[x]);
		}
	}
	
//*****************************************************************************************************************
	/**
	 * called from InputPanel to set the hostname
	 */
	public void setHost(String s){
		host = s;
	}
	
//*****************************************************************************************************************
	/**
	 * called from InputPanel to set the port number
	 */
	public void setPort(String s){
		port = Integer.parseInt(s);
	}
	
//*****************************************************************************************************************

	public void setCCS(){
		ccs = new CcsThread(new Label(), host, port);
	}

//*****************************************************************************************************************
	/**
	 * accessor method to give classes access to the current host name
	 */
	public String getHost(){
		return host;
	}
	
//*****************************************************************************************************************
	/**
	 * accessor method to give classes access to the current port number
	 */
	public int getPort(){
		return port;
	}

//*****************************************************************************************************************
	/**
	 * used in the constructor to disable the UI, until it is ready to be enabled, in InputPanel
	 * when a successful connection is made
	 */
	public void disable(){
		tabPane.setEnabled(false);
		//toolBar.getComponentAtIndex(0).setEnabled(false);
		//toolBar.getComponentAtIndex(1).setEnabled(false);
		groups.setEnabled(false);
		activate.setEnabled(false);
	}

//*****************************************************************************************************************
	/**
	 * called from InputPanel to enable the tabs of the UI after a successful 
	 * connection/authentication is made
	 */
	public void enableOne(){
		tabPane.setEnabled(true);
		//manualPanel.disable();
		//config tab opens
	}

//*****************************************************************************************************************
	/**
	 * called from configPanel after the LAUNCH button has been pushed and the simulation
	 * started
	 */
	public void enableTwo(){
		//toolBar.getComponentAtIndex(0).setEnabled(true);
		//toolBar.getComponentAtIndex(1).setEnabled(true);
		groups.setEnabled(true);
		activate.setEnabled(true);
		//manualPanel.enable();
		//everything else opens after render button pushed
	}
	


//*****************************************************************************************************************
	/**
	 * This method is used by CommandPane to update the array of Objects (Strings)
	 * to be used to keep track of the group names
	 * @param x the current number of names
	 */
	private void expandGroupList(int x) {
		if(x==0){
			groupNames = new String[1];
			numGroups++;
		}else{
			Object storage[] = new String[x];
			for(int i = 0; i<x; i++){
				storage[i] = (String)groupNames[i];
			}
			groupNames = new String[x+1];
			for(int j = 0; j<numGroups; j++){
				groupNames[j] = (String)storage[j];
			}
			numGroups++;
		}
	}
	
//*****************************************************************************************************************
	/**
	 * checks the array groupNames, which is identical to the list in the JComboBox groups,
	 * to see if String s is already in it, in which case it will not allow duplicates to occur
	 * @param s the string to check against for duplicates
	 * return true if no duplicates are found
	 */
	public boolean checkDoubles(String s){
		if(numGroups==0){
			expandGroupList(numGroups);
			groupNames[numGroups-1] = s;
			return true;
		}else{
			for(int x = 0; x < numGroups; x++){
				if(groupNames[x].compareTo(s)==0){
					JOptionPane.showMessageDialog(new JFrame(), "That group name already exists!");
					return false;
				}
			}
			expandGroupList(numGroups);
			groupNames[numGroups-1] = s;
			return true;
		}
	}

//*****************************************************************************************************************
	/**
	 * Called whenever a new group is saved.
	 * @param arg the name of the group to be added, as a String
	 */
	public void updateGroupList(String arg) {
		try{
		/* check the length of arg, ask for a new name if too long */
		if(arg.length()<20){
			if(checkDoubles(arg)){
				if(arg.length()==0){
					/*do nothing*/
				}else{
					//profilePanel.updateLable(arg);
					groups.addItem(arg);
					//groups.setSelectedItem(arg);
				}
			}else{
				String in = JOptionPane.showInputDialog("Try again:");
				updateGroupList(in);
			}
		}else{
			String in = JOptionPane.showInputDialog("Config name too long, try again:");
			updateGroupList(in);
		}
		}catch(Exception ex){/*this happens when the cancel button is pushed, so do nothing*/}
	}

//*****************************************************************************************************************

	public void clearGroups(){
		if(groups.getItemCount()>0){
			groups.removeAllItems();
		}
	}
	
//*****************************************************************************************************************

	/**
	 * void actionPerformed
	 */
	public void actionPerformed(ActionEvent e){
		String command = e.getActionCommand();
		if(command.equals("SAVE_BUTTON")){
			String in = JOptionPane.showInputDialog("Enter the name of the configuration");
			updateGroupList(in);
		}else if(command.equals("EXIT")){
			System.exit(0);
		}else if(command.equals("activate")){
			System.out.println("Activating: " + getCurrentListItem());
			ccs.addRequest(new Activate(getCurrentListItem()));
		}

	}

//*****************************************************************************************************************
	/**
	 * void itemStateChanged - check to make sure the label in profilePanel is the same as
	 * the currently selected item in the combo box
	 * @param e the ItemEvent fired
	 */
	public void itemStateChanged(ItemEvent e){
		try{
			if(getCurrentListItem().equals(profilePanel.getLabel())){
				//do nothing, they're the same
			} else {
				//they're different, so reset the label in profilePanel
				profilePanel.updateLable(getCurrentListItem());
			}
		}catch(NullPointerException nexc){
		/* this exception occurs when the combo box is cleared, after a new simulation
		* is chosen, and getCurrentListItem returns null*/
			profilePanel.updateLable("");//set the label to be empty, since no profile is up
		}
	}

//*****************************************************************************************************************
	/**
	 * accessor method
	 * @return the currently selected item in the JComboBox groups
	 */
	public String getCurrentListItem(){
		return (String)groups.getSelectedItem();
	}
	
	/*
	 * accessor method
	 * gives classes access to the DefaultListModel in InputPanel which has the list of simulations
	 */
	public DefaultListModel getSimList(){
		return inputPanel.getSimList();
	}

	/*
	 * propogated method call, used to close the currently running viz window after the command
	 * is entered in the manualPanel/CommandPrompt
	 */
	public void disposeWindow(){
		configPanel.disposeWindow();
		updateStatus(inputPanel.getSimName() + "...closed");
	}
	
	/*
	 * accessor method
	 * gives classes access to the currently selected simulation, according to the InputPanel
	 */
	public String getSimName(){
		return inputPanel.getSimName();
	}
	
	/*
	 * propogated method call.  This method is invoked from ConfigPanel, when the ParentPanel
	 * is initialized.  The ParentPanel is passed from ConfigPanel, to CommandPane, to CommandPrompt,
	 * where it is used to initialize the ParentPanel instance variable in the CommandParser
	 */
	public void callParseSetter(ParentPanel p){
		manualPanel.setParser(p);
	}
	
	//*******************************************************************************************************//
	//*******************************************************************************************************//
	//													 //
	//                         SERVER REQUESTS APPEAR BELOW HERE		                                 //
	//													 //
	//*******************************************************************************************************//
	//*******************************************************************************************************//
	private class Activate extends CcsThread.request{

		public Activate(String arg){
			super("Activate", arg.getBytes());
		}
		public void handleReply(byte[] data){}
	}
}
