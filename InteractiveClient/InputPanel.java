import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.net.*;

/**
 * Input Panel extends JPanel and sets up/modifies the input tab of the CommandPane program
  */
public class InputPanel extends JPanel implements ActionListener, MouseListener, KeyListener {
	private CommandPane refComPane;
	private JPanel infoDisplay;
	private JTextField userField;
	private JPasswordField passField;
	private JTextField hostField;
	private JTextField portField;
	private JButton connect;
	private JList simListBox;
	private DefaultListModel model;
	private String[] simList;
	private JButton select;
	private boolean authenticated;
	private boolean connected;
	private String selectedSim;
	public CcsThread ccs;
	private long firstClick;


	/**
	 * set up the inputPanel tab
	 */
	public InputPanel(CommandPane p) {
		refComPane = p;
		setBorder(BorderFactory.createTitledBorder("Connect to source for display"));
		setLayout(new GridLayout(2,1));

		userField = new JTextField("enter username");
		passField = new JPasswordField();
		hostField = new JTextField("localhost");
		portField = new JTextField("1235");
		userField.addKeyListener(this);
		passField.addKeyListener(this);
		hostField.addKeyListener(this);
		portField.addKeyListener(this);

		JPanel connectPanel = new JPanel(new BorderLayout());
		connectPanel.setBorder(BorderFactory.createTitledBorder("Connect"));
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1));
		labelPane.add(new JLabel("Username: ", SwingConstants.RIGHT));
		labelPane.add(new JLabel("Password: ", SwingConstants.RIGHT));
		labelPane.add(new JLabel("HostName: ", SwingConstants.RIGHT));
		labelPane.add(new JLabel("Port Number ", SwingConstants.RIGHT));
		connectPanel.add(labelPane, BorderLayout.WEST);
		JPanel inputPane = new JPanel();
		inputPane.setLayout(new GridLayout(0,1));
		inputPane.add(userField);
		inputPane.add(passField);
		inputPane.add(hostField);
		inputPane.add(portField);
		connectPanel.add(inputPane, BorderLayout.CENTER);
		connect = new JButton("Connect");
		connect.setActionCommand("CONNECT");
		connect.addActionListener(this);
		connectPanel.add(connect, BorderLayout.SOUTH);


		/* set up infoDisplay panel */
		infoDisplay = new JPanel();
		infoDisplay.setBorder(BorderFactory.createTitledBorder("Select Simulation"));
		infoDisplay.setLayout(new BorderLayout());

		model = new DefaultListModel();
		simListBox = new JList(model);
		simListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		simListBox.addMouseListener(this);
		JScrollPane scroll = new JScrollPane(simListBox);

		select = new JButton("Select simulation");
		select.setActionCommand("SELECT");
		select.addActionListener(this);
		select.setEnabled(false);

		infoDisplay.add(new JLabel("Please select a simulation to run"), BorderLayout.NORTH);
		infoDisplay.add(scroll, BorderLayout.CENTER);
		infoDisplay.add(select, BorderLayout.SOUTH);

		infoDisplay.setVisible(false);
		//simListBox.setEnabled(false);
		//select.setEnabled(false);
		
		/* set firstClick to zero to keep track of double clicking */
		firstClick = 0;


		/* add all components */
		add(connectPanel);
		add(infoDisplay);

	}

//*****************************************************************************************************************
	
	public void keyPressed(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){
		if(e.getKeyChar() == '\n'){
			System.out.println("Enter pressed!");
			connect.doClick();
		}
	}

//*****************************************************************************************************************
	/**
	 * this method is almost an exact copy of code found in InteractiveClient.java, which
	 * connects to the server
	 */
	private void connect(String[] args){
		if(args.length != 2) {
			System.out.println("Usage: InteractiveClient <ccs server> <ccs port>\n");
			System.exit(1);
		}
		String hostname = args[0];
		int port;
		try {
			port = Integer.parseInt(args[1]);
		} catch(NumberFormatException e) {
			port = 1234;
			System.out.println("Invalid port number, using default (" + port + ")");
		}

		CcsThread ccs = null;
		ccs = new CcsThread(new Label(), hostname, port);
	}
	
//*****************************************************************************************************************

	public void actionPerformed(ActionEvent e){
		String command = e.getActionCommand();
		if(command=="CONNECT"){
			if(userField.getText().equals("")){
				userField.setText("Please enter a username");
			}else if(passField.getText().equals("")){
				//JOptionPane.showMessageDialog(new JFrame(), "Please enter a password");
			}else{
				refComPane.setHost(hostField.getText());
				refComPane.setPort(portField.getText());
				ccs = new CcsThread(new Label(), refComPane.getHost(), refComPane.getPort());
				refComPane.setCCS();
				ccs.addRequest(new AuthenticationRequest(userField.getText(), passField.getText()));
			}
		}else if(command=="SELECT"){
			ccs.addRequest(new ChooseSimulation(selectedSim));
			refComPane.clearGroups();
		}else{
			//unkonwn command
			System.out.println("Unknown command");
		}
	}
	
//*****************************************************************************************************************
	/*
	 * give classes access to the list of simulations
	 */
	public DefaultListModel getSimList(){
		return model;
	}

//*****************************************************************************************************************

	/* recognize double clicking */
	public void mouseClicked(MouseEvent e){
		if(e.getModifiers()==InputEvent.BUTTON1_MASK){
			if(select.isEnabled()==false){select.setEnabled(true);}//enable the select button, now that the user has chosen a simulation from the list
			long click = System.currentTimeMillis();
			long clickTime = System.currentTimeMillis()-firstClick;
			if(clickTime<300){
				//Double clicked
				selectedSim = ((String)(((JList)e.getComponent()).getSelectedValue()));
				ccs.addRequest(new ChooseSimulation(selectedSim));
				firstClick = 0;
				refComPane.clearGroups();
			}else{
				//Single click
				selectedSim = ((String)(((JList)e.getComponent()).getSelectedValue()));
				firstClick = click;
			}
		}
	}
	//useless mouse events
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mousePressed(MouseEvent e){}
	public void mouseExited(MouseEvent e){}

//*****************************************************************************************************************
	/*
	 * helper method, for adding simulation names to the DefaultListModel
	 */
	private void parseBytes(byte[] data){
		String r = new String(data);
		char[] word = new char[0];
		int index = 0;
		for(int x = 0; x<data.length; x++){
			char temp = r.charAt(x);
			//System.out.println("The char is: " + temp);
			if(temp==','){
				//make the char[] a string and toss it onto the list, then reset word
				String listItem = new String(word);
				//System.out.println("Got word: " + listItem);
				model.addElement(listItem);

				word = new char[0];
				index = 0;
			}else{
				word = expand(word);
				word[index] = temp;
				index++;
			}
		}
	}
	
//*****************************************************************************************************************
	/*
	 * helper method, to expand the array that is keeping track of the current String, which will
	 * eventually be added to the DefaultListModel
	 */
	private char[] expand(char[] tooShort){
		if(tooShort.length==0){
			return new char[1];
		}else{
			char[] storage = new char[tooShort.length + 1];
			for(int x=0; x<tooShort.length; x++){
				storage[x] = tooShort[x];
			}
			//char[] returned = new char[tooShort.length + 1];
			//for(int j = 0; j<storage.length; j++){
			//	returned[j] = storage[j];
			//}
			return storage;
		}
	}
	
//*****************************************************************************************************************

	public String getSimName(){
		return selectedSim;
	}
	
	//*******************************************************************************************************//
	//*******************************************************************************************************//
	//													 //
	//                         SERVER REQUESTS APPEAR BELOW HERE		                                 //
	//													 //
	//*******************************************************************************************************//
	//*******************************************************************************************************//

//*****************************************************************************************************************

	private class ChooseSimulation extends CcsThread.request{

		public ChooseSimulation(String sim){
			super("ChooseSimulation", sim.getBytes());
			//System.out.println("constructor");
		}

		public void handleReply(byte[] data){
			System.out.println("Handled");
			if(data[0]==0){
				System.out.println("Not good!");
			}else{
				//good
				System.out.println("Launching " + selectedSim);
				refComPane.enableOne();		//enable the tabs of the CommandPane
				refComPane.updateStatus("Simulation loaded: " + selectedSim);
				refComPane.clearGroups();
				refComPane.updateGroupList("All Particles");

			}
		}
	}

//*****************************************************************************************************************

	private class ListSimulations extends CcsThread.request {

		public ListSimulations(){
			super("ListSimulations", null);
		}

		public void handleReply(byte[] data){
			if(data[0]==0){
				System.out.println("ListSimulations reply is bad!");
			}else{
				//good
				//store strings and throw them on the JList
				parseBytes(data);

				/* username, password, hostname, and port numbers are all valid... */
				connect.setEnabled(false);	//disable connection capabilities
				userField.setEnabled(false);
				passField.setEnabled(false);
				hostField.setEnabled(false);
				portField.setEnabled(false);

				infoDisplay.setVisible(true);	//display info
				//simListBox.setEnabled(true);
				//select.setEnabled(true);

				refComPane.updateStatus("Connection active, please chose a simulation to load");
			}
		}
	}

//*****************************************************************************************************************

	private class AuthenticationRequest extends CcsThread.request {

		public AuthenticationRequest(String username, String password) {
			super("AuthenticateNChilada", (username + ":" + password).getBytes());
		}

		public void handleReply(byte[] data) {
			if(data[0]==0){
				// bad
				//JOptionPane.showMessageDialog(new JFrame(), "The server does not like you");
			}else{
				// good
				try{
					refComPane.updateStatus("Authenticated successfully...connecting...");
					String[] args = new String[2];
					args[0] = hostField.getText();
					args[1] = portField.getText();
					connect(args);
					ccs.addRequest(new ListSimulations());
				}catch(NumberFormatException nf){
					JOptionPane.showMessageDialog(new JFrame(), "Please input a valid port number");
					portField.setText("");
				}
			}
		}
	}
}

