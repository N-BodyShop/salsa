import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.text.DateFormat;

/**
 * A Command Prompt-like interface.
 * This class returns a panel that contains a non-editable display of commands,
 * and a text field input for commands.  The input box supports up and down arrows
 * to move through the history of commands.
 * Comments can also be displayed, with a timestamp.
 * @author Graeme Lufkin <a href=mailto:gwl@u.washington.edu>gwl@u.washington.edu</a>
 */
public class CommandPrompt extends JPanel implements ActionListener, KeyListener {

	private JTextField commandField;
	private JTextArea commandLog;
	private Vector history;		//The history of previous commands is contained in a Vector.
	private int location = 0;	//Gives the current location in the history.
	private String currentCommand;	//Saves the command that may be partially typed when we begin to move through history.
	private CommandParser parser;
	private CommandPane refComPane;
	private boolean disabledOnce = false;


	/*************************************************************************************************
	 * Takes a Collection of items as the history of previous commands.
	 * Sets up the panel, setting the display to non-editable, and
	 * adding listeners to the text input field.
	 */
	public CommandPrompt(Collection c, CommandPane cp) {
		super();
		
		refComPane = cp;
		history = new Vector(c);
		location = history.size();

		setLayout(new BorderLayout());

		commandLog = new JTextArea();
		commandLog.setEditable(false);
		commandLog.setBackground(Color.black);
		commandLog.setForeground(Color.white);

		add(new JScrollPane(commandLog), BorderLayout.CENTER);

		addComment("Started");

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(new JLabel("Command: "), BorderLayout.WEST);

		commandField = new JTextField();
		commandField.addActionListener(this);
		commandField.addKeyListener(this);
		p.add(commandField, BorderLayout.CENTER);

		add(p, BorderLayout.SOUTH);
		instructions();
	}

	public void instructions(){
		commandLog.append("" + '\n');
		commandLog.append("List of known commands:\n");
		commandLog.append("Query for list of commands: " + " $commands\n");
		commandLog.append("Get list of simulations: " + " $simlist\n");
		commandLog.append("Chose a simulation: " + " $chosesim <int>\n");
		commandLog.append("Launch the simulation: " + " $launch\n");
		commandLog.append("Xall: " + " $xall\n");
		commandLog.append("Yall: " + " $yall\n");
		commandLog.append("Zall: " + " $zall\n");
		commandLog.append("Clear command: " + " $clear\n");
		commandLog.append("Zoom command: " + " $zoom <double>\n");
		commandLog.append("Rotating: " + " $rotate <left/right/up/down/counter/clock> <double>\n");
		commandLog.append("Panning: " + " $pan <left/right/up/down/in/out> <double>\n");
		commandLog.append("Recoloring: " + " $recolor <lin/log> <double> <double>\n");
		commandLog.append("Value range request: " + " $range\n");
		commandLog.append("Close the visualization: " + " $closeviz\n");
		commandLog.append("Quit Program: " + " $quit\n");
		commandLog.append("\n");
	}
	/*************************************************************************************************
	 * Creates an instance with no previous history.
	 */
	public CommandPrompt(CommandPane p) {
		this(new Vector(), p);
	}

	/*************************************************************************************************/

	public void updateScreen(String arg){
		commandLog.append(arg + '\n');
	}

	public void clear(){
		commandLog.setText("");
		addComment("Cleared");
	}
	/*************************************************************************************************
	 * This method is called by the text input (and can be called by other
	 * areas of a program) to add a command to the display and add that command
	 * to the history.  Subclasses may wish to add functionality to this
	 * function, for example to actually perform the action specified by
	 * the command.
	 */
	public void addCommand(String command) {
		commandLog.append(command + '\n');

		//don't add sequences of duplicate command to history
		if(history.size() == 0 || !command.equals((String) history.lastElement())){
			history.add(command);
		}
		location = history.size();

		//commandField.setText("");
		//currentCommand = "";

		//sendCommand()?
	}

	/*************************************************************************************************
	 * This method adds a timestamped comment to the display.
	 */
	public void addComment(String comment) {
		String timestamp = DateFormat.getDateTimeInstance().format(new Date());
		commandLog.append("# " + timestamp + " : " + comment + '\n');
	}

	/*************************************************************************************************/

	public void actionPerformed(ActionEvent e) {
		try{
			parser.parseString(commandField.getText());
			//addCommand(commandField.getText());
			commandField.setText("");
			currentCommand = "";
		}catch(Exception ex){
			//System.out.println("Exception caugth...haven't initialized yet...?");
			updateScreen("Please launch the simulation from the Configure tab in order to initialize the program variables");
		}
	}

	/*************************************************************************************************/

	public void keyTyped(KeyEvent e) {
	}

	/*************************************************************************************************
	 * Handles navigation through the history.
	 */
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_UP) {
			if(location == history.size()) //save partially typed command
				currentCommand = commandField.getText();
			if(location > 0) //move back in the history
				commandField.setText((String) history.get(--location));
		} else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
			if(location == history.size() - 1) { //bring back the saved partially typed command
				location++;
				commandField.setText(currentCommand);
			} else if(location < history.size()) //move forward in the history
				commandField.setText((String) history.get(++location));
		}
	}

	/*************************************************************************************************/

	public void keyReleased(KeyEvent e) {
	}
	
	public void disable(){
		if(disabledOnce){
			//do nothing
		}else{
			commandField.setEnabled(false);
			disabledOnce = true;
		}
	}

	public void enable(){
		commandField.setEnabled(true);
	}

	public void setParser(ParentPanel p){
		parser = new CommandParser(p, this);
	}
	
	public DefaultListModel getSimList(){
		return refComPane.getSimList();
	}

	public void updateStatus(String arg){
		refComPane.updateStatus("Launched: " + arg);
	}
	
	public void launch(){
		refComPane.launch();
	}
	
	public void disposeWindow(){
		refComPane.disposeWindow();
	}
}
