import java.io.*;
import javax.swing.*;
import java.awt.*;

/*
 * class CommandParser
 * Instantiated in CommandPrompt, this class is used to parse commands entered in the manual section of
 * the CommandPane
 * Original master mind: Amir Kouretchian
 * Version .00000001
 */
public class CommandParser {
	private CcsThread ccs;
	public ParentPanel theParent;
	private CommandPrompt thePrompt;
	private String theCommand = "";
	private double theArg;
	private int index;
	private DefaultListModel theSimList;
	private String selectedSim;

	//***********************************************************************************************
	/*
	 * constructor
	 * sets theParent and thePrompt, giving this object access to ParentPanel and
	 * CommandPrompt
	 * @param p the ParentPanel object to associate this parser to
	 * @param cp the CommandPrompt object to associate this parser to
	 */
	public CommandParser(ParentPanel p, CommandPrompt cp){
		theParent = p;
		thePrompt = cp;
		theSimList = thePrompt.getSimList();
		ccs = new CcsThread(new Label(), theParent.host, theParent.port);
		System.out.println("Parser is set...ROCK AND ROLL!");
	}

	//***********************************************************************************************
	/*
	 * void parseString
	 * called from CommandPrompt when a command is entered in the manual tab of the CommandPane
	 * this method essentially parses the command and executes the appropriate action for 'recognized'
	 * commands.  For a list of recognized commands, please see method 'instructions' in CommandPrompt,
	 * with the assumption that all known commands have been listed there
	 * @param arg the string to parse, entered in the manual tab of the CommandPane
	 */
	public void parseString(String arg){
		System.out.println("Parsing string command: " + arg);
		int firstSpace = arg.indexOf(" ");
		if(firstSpace == -1){
			//no space
			if(arg.equals("openfile")){
				System.out.println("Filechooser launched!");
				JFileChooser choser = new JFileChooser();
				int selection = choser.showOpenDialog(theParent);
				if(selection == JFileChooser.APPROVE_OPTION){
					System.out.println("The file is: " + choser.getSelectedFile().getName());
					System.out.println("The file path is: " + choser.getSelectedFile().getPath());
					try{
						BufferedReader reader = new BufferedReader(new FileReader(choser.getSelectedFile()));
						while(reader.ready()){
							String currentLine = reader.readLine();
							//System.out.println("line says: " + reader.readLine());
							parseString(currentLine);
						}
						reader.close();
					}catch(FileNotFoundException ex){
						System.out.println("File not found!");
					}catch(IOException ex){
						System.out.println("IO exception caught!");
					}
				}

			}else if(arg.equals("quit")){
				goodCommand(arg);
				System.exit(0);
			}else if(arg.equals("review")){
				goodCommand(arg);
				theParent.reView();
			}else if(arg.equals("clear")){
				//System.out.println("clearing");
				thePrompt.addCommand(arg);
				thePrompt.clear();
			}else if(arg.equals("commands")){
				goodCommand(arg);
				thePrompt.instructions();
			}else if(arg.equals("simlist")){
				//get the simList
				goodCommand(arg);
				for(int x = 0; x < theSimList.getSize(); x++){
					thePrompt.updateScreen(x+1 + " " + (String)theSimList.get(x));
				}
			}else if(arg.equals("launch")){
				if(theParent.isOpen){
					thePrompt.updateScreen("Please close the current simulation");
				}else{
					goodCommand(arg);
					thePrompt.launch();
				}
			}else if(arg.equals("closeviz")){
				if(theParent.isOpen){
					goodCommand(arg);
					thePrompt.disposeWindow();
				}else{
					badCommand("Sorry, you can't close a non-open visualization!");
				}
			}else if(arg.equals("xall")){
				if(theParent.isOpen){
					goodCommand(arg);
					theParent.xButton.doClick();
				}else{
					thePrompt.updateScreen("Don't think so...no visualization is running!");
				}
			}else if(arg.equals("yall")){
				if(theParent.isOpen){
					goodCommand(arg);
					theParent.yButton.doClick();
				}else{
					thePrompt.updateScreen("Don't think so...no visualization is running!");
				}
			}else if(arg.equals("zall")){
				if(theParent.isOpen){
					goodCommand(arg);
					theParent.zButton.doClick();
				}else{
					thePrompt.updateScreen("Don't think so...no visualization is running!");
				}
			}else if(arg.equals("range")){
				if(theParent.isOpen){
					goodCommand(arg);
					ccs.addRequest(new ValueRange());
				}else{
					thePrompt.updateScreen("Don't think so...no visualization is running!");
				}
			}else{
				badCommand("Unknown command: " + arg);
			}
		}else{
			//space/args are included
			try{
				theCommand = arg.substring(0,firstSpace);
				if(theCommand.equals("dump")){
					if(theParent.isOpen){
						String secondString = arg.substring(firstSpace+1);
						int secondSpace = secondString.indexOf(" ");
						String firstArg = secondString.substring(0, secondSpace);	//either 'main' or 'aux'
						String secondArg = secondString.substring(secondSpace+1);
						if(firstArg.equals("aux") || firstArg.equals("main")){
							goodCommand(arg);
							theParent.encodePNG(false, firstArg, secondArg);
						}else{
							badCommand("Check args: " + arg);
						}
					}else{
						thePrompt.updateScreen("Don't think so...no visualization is running!");
					}
				}else if(theCommand.equals("resize")){
					String secondString = arg.substring(firstSpace+1);
					int secondSpace = secondString.indexOf(" ");
					String secondArg = secondString.substring(0, secondSpace);
					String thirdArg = secondString.substring(secondSpace+1);
					int firstInt = Integer.parseInt(secondArg);
					int secondInt = Integer.parseInt(thirdArg);
					goodCommand(arg);
					theParent.resizeMainView(firstInt, secondInt);
				}else if(theCommand.equals("choosesim")){
					String secondArg = arg.substring(firstSpace+1);
					int intArg = Integer.parseInt(secondArg);
					if(intArg<0 || intArg>theSimList.getSize()){
						badCommand("illegal arg " + arg);
					}else{
						selectedSim = (String)theSimList.get(intArg-1);
						goodCommand(arg);
						ccs.addRequest(new ChooseSimulation(selectedSim));
					}
				}else if(theCommand.equals("zoom")){
					if(theParent.isOpen){
						//System.out.println("zoom");
						String secondArg = arg.substring(firstSpace);
						double doubleArg = Double.parseDouble(secondArg);
						goodCommand(arg);
						theParent.zoom(doubleArg);
					}else{
						thePrompt.updateScreen("Don't think so...no visualization is running!");
					}
				}else if(theCommand.equals("rotate")){
					if(theParent.isOpen){
						//System.out.println("rotate");
						String secondString = arg.substring(firstSpace+1);
						int secondSpace = secondString.indexOf(" ");
						String secondArg = secondString.substring(0, secondSpace);
						String thirdArg = secondString.substring(secondSpace+1);
						//System.out.println("thirdArg: " + thirdArg);
						double theta = Double.parseDouble(thirdArg);
						goodCommand(arg);
						theParent.rotateNumeric(theta, secondArg);
					}else{
						thePrompt.updateScreen("Don't think so...no visualization is running!");
					}
				}else if(theCommand.equals("pan")){
					if(theParent.isOpen){
						System.out.println("pan");
						String secondString = arg.substring(firstSpace+1);
						int secondSpace = secondString.indexOf(" ");
						String secondArg = secondString.substring(0, secondSpace);
						String thirdArg = secondString.substring(secondSpace+1);
						//System.out.println("thirdArg: " + thirdArg);
						double amount = Double.parseDouble(thirdArg);
						goodCommand(arg);
						theParent.panNumeric(amount, secondArg);
					}else{
						thePrompt.updateScreen("Don't think so...no visualization is running!");
					}
				}else if(theCommand.equals("recolor")){
					if(theParent.isOpen){
						System.out.println("Recolor manual called");
						String secondString = arg.substring(firstSpace+1);
						int secondSpace = secondString.indexOf(" ");
						String secondArg = secondString.substring(0, secondSpace);
						if(secondArg.equals("lin") || secondArg.equals("log")){
							String thirdString = secondString.substring(secondSpace+1);
							int thirdSpace = thirdString.indexOf(" ");
							String thirdArg = thirdString.substring(0, thirdSpace);		//the first double
							String fourthString = thirdString.substring(thirdSpace+1);	//the second double
							double firstDouble = Double.parseDouble(thirdArg);
							double secondDouble = Double.parseDouble(fourthString);
							goodCommand(arg);
							theParent.reColor(secondArg, firstDouble, secondDouble);
							theParent.reView();
						}else{
							badCommand("Check arguments: " + arg);
						}
					}else{
						thePrompt.updateScreen("Don't think so...no visualization is running!");
					}
				}else{
					badCommand("Unknown command: " + arg);
				}
			}catch(NumberFormatException ex){
				//System.out.println("number format exception");
				badCommand("illegal argument " + arg);
			}

		}


	}

	//***********************************************************************************************
	/*
	 * void badCommand
	 * used to indicate that the user has entered an unkown command
	 * @param barf the message to display on the associated CommandPrompt
	 */
	public void badCommand(String barf){
		thePrompt.updateScreen(barf);
	}
	
	//***********************************************************************************************
	/*
	 * void goodCommand
	 * used to indicate that the user has entered a valid command
	 * @param barf the message to display on the associated CommandPrompt
	 */
	public void goodCommand(String barf){
		thePrompt.updateScreen("Executing:");
		//thePrompt.updateScreen(barf);
		thePrompt.addCommand(barf);
	}

	private class ValueRange extends CcsThread.request{
		public ValueRange(){
			super("ValueRange", null);
		}

		public void handleReply(byte[] data){
			try{
				//System.out.println("ValueRange handleReply");
				DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));
				System.out.println("bytes available: " + stream.available());
				double first = stream.readDouble();
				double second = stream.readDouble();
				stream.close();
				//System.out.println("First: " + first);
				//System.out.println("Second: " + second);
				thePrompt.updateScreen("First: " + first);
				thePrompt.updateScreen("Second: " + second);
			}catch(Exception ex){
				System.out.println("Couldn't read data");
				ex.printStackTrace();
			}
		}
	}
	
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
				//refComPane.updateStatus("Simulation loaded: " + selectedSim);
				thePrompt.updateStatus(selectedSim);
				thePrompt.clearGroups();
				thePrompt.updateGroupList("All particles");

			}
		}
	}

}
