import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class ParentPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener{
	private CcsThread ccs;
	public ColorModel wrbb;
	public ColorMapDisplay cmdisplay;
	private MainView mainView;
	private MainView auxView;
	private JSliderOldValue updownSlider,leftrightSlider,clockwiseSlider;
	private Config config;
	private double boxSize;
	//private int width, height;
	public String host;
	public int port;
	public String initialView;
	public JButton xButton, yButton, zButton;
	public int start_x, start_y;
	public String controller, typeOfSelection;
	public JPanel leftPanel, rightPanel, splitPanel, southPanel;
	//public int currentZLoc;
	private Rectangle drawRect = new Rectangle();
	private boolean mainDrawn, auxDrawn;
	private int xBoxConstraintOne, xBoxConstraintTwo;
	private double[] xSimNumbers, ySimNumbers, zSimNumbers;
	private Vector3D[] posVectors;
	private int num_points;
	private byte[] encoded;
	//private int oldValue;
	private byte[] wrbb_red, wrbb_green, wrbb_blue;
	private int color_start;
	private long firstClick;
	public boolean isOpen, mouseDragged;
	private String colorMapType;
	public ConfigPanel refConfigPanel;
	private CommandPane refComPane;
	public boolean resizeCall;
	private JPopupMenu rightClickMenu;

	/*
	 * constructor
	 * @param hostname the hostname, used to initialize the ccs thread
	 * @param portNumber the port number, used to initialize the ccs thread
	 * @param theView xall, yall, or zall
	 * @param cp the ConfigPanel object that instantiated this ParentPanel-->for accessor methods
	 * @param c the COmmandPane object controlling everything
	 */

	public ParentPanel(String hostname, int portNumber, String theView, ConfigPanel cp, CommandPane c){
		super(new BorderLayout());
		host = hostname;
		port = portNumber;
		ccs = new CcsThread(new Label(), host, port);
		initialView = theView;
		mainDrawn = false;
		auxDrawn = false;
		isOpen = true;
		refConfigPanel = cp;
		refComPane = c;
		resizeCall = false;

		/*
		 * setup color map for top of window
		 */
		JPanel northPanel = new JPanel(new GridLayout(2,1));
		wrbb = createWRBBColorModel();
		cmdisplay = new ColorMapDisplay(512*2, 10);
		cmdisplay.redisplay(wrbb);
		add(cmdisplay, BorderLayout.NORTH);

		cmdisplay.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent e){
				color_start = e.getX();
			}
			public void mouseReleased(MouseEvent e){}
			public void mouseExited(MouseEvent e){}
			public void mouseEntered(MouseEvent e){}
			public void mouseClicked(MouseEvent e){
				long click = System.currentTimeMillis();
				long clickTime = System.currentTimeMillis()-firstClick;
				if(clickTime<300){
					//Double clicked
					wrbb = invertWRBB();
					cmdisplay.redisplay(wrbb);
					mainView.messageHub("newColor", true);
					auxView.messageHub("newColor", true);
					firstClick = 0;

				}else{
					//Single click
					firstClick = click;
				}
			}
		});

		cmdisplay.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent e){
				wrbb = resetWRBB(e);
				cmdisplay.redisplay(wrbb);
				mainView.messageHub("newColor", true);
				auxView.messageHub("newColor", true);

			}

			public void mouseMoved(MouseEvent e){}
		});

		/* 
		 * add the MainView and AuxiliaryView to the parent
		 */
		splitPanel = new JPanel(new GridLayout(0,2));
		mainView = new MainView(this, "mainView");
		auxView = new MainView(this, "auxView");
		leftPanel = new JPanel();
		leftPanel.addComponentListener(mainView);
		leftPanel.addMouseListener(this);
		leftPanel.addMouseMotionListener(this);
		//leftPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		rightPanel = new JPanel();
		rightPanel.addComponentListener(auxView);
		rightPanel.addMouseListener(this);
		rightPanel.addMouseMotionListener(this);
		//rightPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		leftPanel.add(mainView);
		rightPanel.add(auxView);
		splitPanel.add(leftPanel);
		splitPanel.add(rightPanel);
		add(splitPanel, BorderLayout.CENTER);
		controller = "mainView";

		/*
		 * set up the bottom portion of the UI...buttons and sliders etc 
		 */
		southPanel = new JPanel();

		JPanel aux = new JPanel(new GridLayout(3,1));
		xButton = new JButton("xall");
		xButton.setActionCommand("xall");
		xButton.addActionListener(this);
		aux.add(xButton);
		yButton = new JButton("yall");
		yButton.setActionCommand("yall");
		yButton.addActionListener(this);
		aux.add(yButton);
		zButton = new JButton("zall");
		zButton.setActionCommand("zall");
		zButton.addActionListener(this);
		aux.add(zButton);
		southPanel.add(aux);

		aux = new JPanel(new GridLayout(3,1));
		JButton reColor = new JButton("ReColor");
		reColor.setActionCommand("recolor");
		reColor.addActionListener(this);
		aux.add(reColor);
		JButton newMap = new JButton("Switch Color Map");
		newMap.setActionCommand("switchmap");
		newMap.addActionListener(this);
		aux.add(newMap);
		JButton clear = new JButton("Clear Boxes");
		clear.setActionCommand("clear");
		clear.addActionListener(this);
		aux.add(clear);
		southPanel.add(aux);
		
		aux = new JPanel(new GridLayout(3,1));
		aux.add(new JLabel("Down"));
		aux.add(new JLabel("Left"));
		aux.add(new JLabel("Cntr"));
		southPanel.add(aux);

		aux = new JPanel(new GridLayout(3,1));

		updownSlider = new JSliderOldValue(0, 359, 180);
		updownSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSliderOldValue source = (JSliderOldValue) e.getSource();
				if(!source.getValueIsAdjusting() && (source.getValue() - source.getOldValue() != 0)) {
					mainDrawn = auxDrawn = false;
					//System.out.println("Adjusting up/down slider!");
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					mainView.drag(-1, 0, "x", theta);
					auxView.drag(-1,0,"x",theta);
					source.setOldValue(source.getValue());
					drawVex();
				}
			}
		});

		leftrightSlider = new JSliderOldValue(0, 359, 180);
		leftrightSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSliderOldValue source = (JSliderOldValue) e.getSource();
				if(!source.getValueIsAdjusting() && (source.getValue() - source.getOldValue() != 0)) {
					mainDrawn = auxDrawn = false;
					//System.out.println("Adjusting left/right slider!");
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					if(controller.equals("mainView")){
						mainView.drag(-1, 0, "y", theta);
						auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
					}else{
						auxView.drag(-1,0,"y", theta);
						mainView.update(auxView.x, auxView.y, auxView.z, auxView.origin, true);
					}
					source.setOldValue(source.getValue());
					drawVex();
				}
			}
		});

		clockwiseSlider = new JSliderOldValue(0, 359, 180);
		clockwiseSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSliderOldValue source = (JSliderOldValue) e.getSource();
				if(!source.getValueIsAdjusting() && (source.getValue() - source.getOldValue() != 0)) {
					mainDrawn = auxDrawn = false;
					//System.out.println("Adjusting clock/cntr slider!");
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					if(controller.equals("mainView")){
						mainView.drag(-1, 0, "z", theta);
						auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
					}else{
						auxView.drag(-1, 0, "z", theta);
						mainView.update(auxView.x, auxView.y, auxView.z, auxView.origin, true);
					}
					source.setOldValue(source.getValue());
					drawVex();
				}
			}
		});

		aux.add(updownSlider);
		aux.add(leftrightSlider);
		aux.add(clockwiseSlider);
		southPanel.add(aux);
		
		aux = new JPanel(new GridLayout(3,1));
		aux.add(new JLabel("Up"));
		aux.add(new JLabel("Right"));
		aux.add(new JLabel("Clock"));
		southPanel.add(aux);
		
		aux = new JPanel(new GridLayout(2,1));
		aux.setBorder(BorderFactory.createTitledBorder("Selecting with:"));
		ButtonGroup groupOfButtons = new ButtonGroup();
		JRadioButton rb = new JRadioButton("Boxes     ");
		rb.setActionCommand("boxes");
		rb.addActionListener(this);
		rb.setSelected(true);
		typeOfSelection = "boxes";
		groupOfButtons.add(rb);
		aux.add(rb);
		rb = new JRadioButton("Spheres     ");
		rb.setActionCommand("spheres");
		rb.addActionListener(this);
		groupOfButtons.add(rb);
		aux.add(rb);
		southPanel.add(aux);
		/*
		aux = new JPanel(new GridLayout(3,1));
		JButton png = new JButton("PNG capture");
		png.setActionCommand("png");
		png.addActionListener(this);
		aux.add(png);
		southPanel.add(aux);
		*/
		add(southPanel, BorderLayout.SOUTH);
		firstClick = 0;
		//end UI setup
		
		/* set up right click menu */
		rightClickMenu = new JPopupMenu();
		JMenuItem item;

		item = new JMenuItem("xall view");
		item.setActionCommand("xall");
		item.addActionListener(this);
		rightClickMenu.add(item);
		item = new JMenuItem("yall view");
		item.setActionCommand("yall");
		item.addActionListener(this);
		rightClickMenu.add(item);
		item = new JMenuItem("zall view");
		item.setActionCommand("zall");
		item.addActionListener(this);
		rightClickMenu.add(item);
		item = new JMenuItem("recolor image");
		item.setActionCommand("recolor");
		item.addActionListener(this);
		rightClickMenu.add(item);
		item = new JMenuItem("Save image as png...");
		item.setActionCommand("png");
		item.addActionListener(this);
		rightClickMenu.add(item);
		item = new JMenuItem("switch color map");
		item.setActionCommand("switchmap");
		item.addActionListener(this);
		rightClickMenu.add(item);
		item = new JMenuItem("clear boxes/spheres");
		item.setActionCommand("clear");
		item.addActionListener(this);
		rightClickMenu.add(item);
		//end right click menu setup

		mainView.messageHub("lvConfig", false);
		auxView.messageHub("lvConfig", true);

	}

	//*******************************************************************************************************//

	public void actionPerformed(ActionEvent e){
		String command = e.getActionCommand();
		if(command.equals("xall")){
			mainDrawn = auxDrawn = false;
			mainView.xAll(false);
			//auxView.xAll(true);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);

			updownSlider.setOldValue(180);
			updownSlider.setValue(180);
			leftrightSlider.setOldValue(180);
			leftrightSlider.setValue(180);
			clockwiseSlider.setOldValue(180);
			clockwiseSlider.setValue(180);

		}else if(command.equals("yall")){
			mainDrawn = auxDrawn = false;
			mainView.yAll(false);
			//auxView.yAll(true);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
			
			updownSlider.setOldValue(180);
			updownSlider.setValue(180);
			leftrightSlider.setOldValue(180);
			leftrightSlider.setValue(180);
			clockwiseSlider.setOldValue(180);
			clockwiseSlider.setValue(180);

		}else if(command.equals("zall")){
			mainDrawn = auxDrawn = false;
			mainView.zAll(false);
			//auxView.zAll(true);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);

			updownSlider.setOldValue(180);
			updownSlider.setValue(180);
			leftrightSlider.setOldValue(180);
			leftrightSlider.setValue(180);
			clockwiseSlider.setOldValue(180);
			clockwiseSlider.setValue(180);
			
		}else if(command.equals("clear")){
			ccs.addRequest(new ClearBoxes());
			ccs.addRequest(new ClearSpheres());
			refComPane.clearGroups();
			refComPane.updateGroupList("All Particles");
			reView();
		}else if(command.equals("boxes")){
			typeOfSelection = "boxes";
			mainDrawn = auxDrawn = false;
		}else if(command.equals("spheres")){
			typeOfSelection = "spheres";
			mainDrawn = auxDrawn = false;
		}else if(command.equals("recolor")){
			reColor(null,0,0);
		}else if(command.equals("switchmap")){
			if(colorMapType.equals("standard")){
				System.out.println("Rainbow color map coming right up!");
				wrbb = rainbowWRBB();
				cmdisplay.redisplay(wrbb);
				mainView.messageHub("newColor", true);
				auxView.messageHub("newColor", true);
			}else if(colorMapType.equals("rainbow")){
				System.out.println("Standard color map coming right up!");
				wrbb = createWRBBColorModel();
				cmdisplay.redisplay(wrbb);
				mainView.messageHub("newColor", true);
				auxView.messageHub("newColor", true);
			}
		}else if(command.equals("png")){
			encodePNG(true, null, null);
		}
	}
	
	//*******************************************************************************************************//
	/*
	 * void encodePNG
	 * encode the appropriate view to png file
	 * @param arg true: this function was called from a popup menu on the screen, false: this function was called from
	 *	CommandParser, via manual input
	 * @param view the view to save to png, either 'main' or 'aux'
	 * @param name the name of the png file
	 */
	public void encodePNG(boolean arg, String view, String name){
		System.out.println("FIRING UP THE PNG ENCODER!");
		if(arg){
			//called from actionPerformed
			String fileName = JOptionPane.showInputDialog("Enter file name");
			if(fileName==null){
				//do nothing...the cancel button was pressed
			}else if(fileName.equals("")){
				JOptionPane.showMessageDialog(this, "No filename Entered, terminating png encoder");
			}else{
				Image img;
				if(controller.equals("mainView")){
					img = createImage(mainView.source);
				}else{
					img = createImage(auxView.source);
				}
				byte[] pngBytes;
				PngEncoder png = new PngEncoder(img, false);
				pngBytes = png.pngEncode();
				try{
					FileOutputStream outfile = new FileOutputStream(fileName);
					outfile.write(pngBytes);
					outfile.flush();
					outfile.close();
				}catch(FileNotFoundException ex){
					System.out.println("File not found exception!");
				}catch(IOException ex){
					System.out.println("IO exception!");
				}
			}
		}else{
			//called from commandParser
			Image img;
			if(view.equals("main")){
				img = createImage(mainView.source);
			}else{
				img = createImage(auxView.source);
			}
			byte[] pngBytes;
			PngEncoder png = new PngEncoder(img, false);
			pngBytes = png.pngEncode();
			try{
				FileOutputStream outfile = new FileOutputStream(name);
				outfile.write(pngBytes);
				outfile.flush();
				outfile.close();
			}catch(FileNotFoundException ex){
				System.out.println("File not found exception!");
			}catch(IOException ex){
				System.out.println("IO exception!");
			}
		}
	}

	//*******************************************************************************************************//
	/*
	 * this function sends a request to the server to re-interpret the way the particle colors are defined/recognized
	 * @param argOne null: this function was called from actionPerformed, 'lin': linear coloring definition, 'log':
	 *	logarithmic color definition
	 * @param argTwo only valid when this function is called from CommandParser...the first double value that defines
	 *	redefining bounds to the particles
	 * @param argThree only valid when this function is called from CommandParser...the second double value that defines
	 *	redefining bounds to the particles
	 */

	public void reColor(String argOne, double argTwo, double argThree){
		if(argOne==null){
			//this block is used when the button is pressed, from actionPerformed
			String choice;
			int type;
			double one;
			double two;
			Object[] choices = {"linear", "logarithmic"};
			choice = (String)JOptionPane.showInputDialog(new JFrame(), "Select color method:", "Select", JOptionPane.PLAIN_MESSAGE, null, choices, "linear");
			if(choice==null){
				System.out.println("cancel was pressed");
			}else{
				System.out.println("Choice: " + choice);
				try{
					String numberOne = JOptionPane.showInputDialog("Enter first mysterious double value");
					one = Double.parseDouble(numberOne);
					String numberTwo = JOptionPane.showInputDialog("Enter second mysterious double value");
					two = Double.parseDouble(numberTwo);
					if(choice.equals("linear")){
						type = 0;
					}else{
						type = 1;
					}
					byte[] data = encodeNumbers(type, one, two);
					ccs.addRequest(new ReColor(data));
					reView();
				}catch(NullPointerException ex){
					System.out.println("Pressed cancel");
				}catch(NumberFormatException ex){
					System.out.println("Didn't enter a number");
					//ex.printStackTrace();
				}
			}
		}else{
			//this block is used when this method is called from CommandParser
			int type;
			if(argOne.equals("lin")){
				type = 0;
			}else{
				type = 1;
			}
			byte[] data = encodeNumbers(type, argTwo, argThree);
			ccs.addRequest(new ReColor(data));
			reView();
		}
	}
	
	//*******************************************************************************************************//
	/*
	 * invokes the ImageRequest server request in MainView, in order to update the image
	 */

	public void reView(){
		mainView.messageHub("review", true);
		auxView.messageHub("review", true);
	}

	//*******************************************************************************************************//
	/*
	 * this function is called indirectly from CommandParser to resize the window in response to the manual input
	 * command 'resize'...see CommandPrompt method instructions() for usage of 'resize'
	 */

	public void resizeMainView(int wid, int hei){

		mainView.resizedImage = true;
		mainView.width = wid;
		mainView.height = hei;
		double xRatio = mainView.x.length() / mainView.width;
		double yRatio = mainView.y.length() / mainView.height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			mainView.x = mainView.x.scalarMultiply(factor);
		}
		mainView.messageHub("review", true);
		
		auxView.resizedImage = true;
		auxView.width = wid;
		auxView.height = hei;
		xRatio = auxView.x.length() / auxView.width;
		yRatio = auxView.y.length() / auxView.height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			auxView.x = auxView.x.scalarMultiply(factor);
		}
		resizeCall = true;
		auxView.messageHub("review", true);
		cmdisplay.reset(wid*2, 10);
		cmdisplay.redisplay(wrbb);
		//leftPanel.resize(wid, hei);
		//rightPanel.resize(wid, hei);
		//splitPanel.resize(wid*2, hei);
		//resize(wid*2, getHeight());
		//southPanel.resize(wid*2, southPanel.getHeight());
		//refConfigPanel.packFrame();
	}

	//*******************************************************************************************************//
	/*
	 * this function is used by the reColor function to encode the arguments of the server call to binary
	 */

	private byte[] encodeNumbers(int type, double first, double second) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
		try {
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeInt(type);
			dos.writeDouble(first);
			dos.writeDouble(second);
		} catch(IOException e) {
			System.err.println("Couldn't encode request!");
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	//*******************************************************************************************************//

	public void mousePressed(MouseEvent e){
		mouseDragged = false;
		start_x = e.getX();
		start_y = e.getY();
		switch(e.getModifiers()){
			case MouseEvent.BUTTON1_MASK | InputEvent.CTRL_MASK:
				drawRect.setLocation(start_x, start_y);
				drawRect.setSize(0, 0);
				leftPanel.getGraphics().drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
				break;
		}

	}
	
	//*******************************************************************************************************//

	public void mouseDragged(MouseEvent e){
		switch(e.getModifiers()){
			case MouseEvent.BUTTON1_MASK:
				mainDrawn = auxDrawn = false;
				if(controller.equals("mainView")){
					mainView.drag(e.getX(), e.getY(), null,0.0);
					auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin,true);

				}else{
					auxView.drag(e.getX(), e.getY(), null,0.0);
					mainView.update(auxView.x, auxView.y, auxView.z, auxView.origin,true);
				}
				drawVex();
				start_x = e.getX();
				start_y = e.getY();
				break;
			case MouseEvent.BUTTON3_MASK:
				//zoom
				mouseDragged = true;
				mainDrawn = auxDrawn = false;
				if(controller.equals("mainView")){
					mainView.zoom(e);
					auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
				}else{
					auxView.zoom(e);
					mainView.update(auxView.x, auxView.y, auxView.z, auxView.origin, true);
				}
				start_x = e.getX();
				start_y = e.getY();
				break;
			case MouseEvent.BUTTON2_MASK:
				//pan
				mainDrawn = auxDrawn = false;
				Graphics gl = leftPanel.getGraphics();
				gl.setColor(Color.white);
				/* draw a corsshair to indicate the center of the simulation space */
				gl.drawLine((leftPanel.getWidth()/2)-30, (leftPanel.getHeight()/2),(leftPanel.getWidth()/2)+30,(leftPanel.getHeight()/2));
				gl.drawLine((leftPanel.getWidth()/2), (leftPanel.getHeight()/2)-30,(leftPanel.getWidth()/2),(leftPanel.getHeight()/2)+30);
				gl = rightPanel.getGraphics();
				gl.setColor(Color.white);
				gl.drawLine((rightPanel.getWidth()/2)-30, (rightPanel.getHeight()/2),(rightPanel.getWidth()/2)+30,(rightPanel.getHeight()/2));
				gl.drawLine((rightPanel.getWidth()/2), (rightPanel.getHeight()/2)-30,(rightPanel.getWidth()/2),(rightPanel.getHeight()/2)+30);
				if(controller.equals("mainView")){
					mainView.pan(e);
					auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
				}else{
					auxView.pan(e);
					mainView.update(auxView.x, auxView.y, auxView.z, auxView.origin, true);
				}
				start_x = e.getX();
				start_y = e.getY();
				break;
			case MouseEvent.BUTTON3_MASK | InputEvent.SHIFT_MASK:
				//translate origin in z direction
				mainDrawn = auxDrawn = false;
				if(controller.equals("mainView")){
					repaint();
					Graphics g = rightPanel.getGraphics();
					g.setColor(Color.white);
					g.drawLine((rightPanel.getWidth()/2) - 30, rightPanel.getHeight()/2, (rightPanel.getWidth()/2) + 30, rightPanel.getHeight()/2);

					mainView.zTranslate(e);
					auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);

				}else{	
					repaint();
					Graphics glx = leftPanel.getGraphics();
					glx.setColor(Color.white);
					glx.drawLine((leftPanel.getWidth()/2) - 30, leftPanel.getHeight()/2, (leftPanel.getWidth()/2) + 30, leftPanel.getHeight()/2);
					auxView.zTranslate(e);
					mainView.update(auxView.x, auxView.y, auxView.z, auxView.origin, true);
				}
				start_x = e.getX();
				start_y = e.getY();
				break;
			case MouseEvent.BUTTON1_MASK | InputEvent.CTRL_MASK:
				//BAND BOX
				if(controller.equals("mainView")){
					Graphics gBox = leftPanel.getGraphics();
					gBox.setColor(Color.white);
					if(typeOfSelection.equals("boxes")){
						if(auxDrawn){
							//we've drawn on auxView, so use the constraints specified there to draw the box on mainView
							if(e.getY()>start_y){
								repaint();
								drawRect.setBounds(xBoxConstraintOne, start_y, xBoxConstraintTwo-xBoxConstraintOne, e.getY()-start_y);
								//gBox.drawRect(xBoxConstraintOne, start_y, xBoxConstraintTwo-xBoxConstraintOne, e.getY()-start_y);
								gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
							}else{
								repaint();
								drawRect.setBounds(xBoxConstraintOne, e.getY(), xBoxConstraintTwo-xBoxConstraintOne,start_y-e.getY());
								//gBox.drawRect(xBoxConstraintOne, e.getY(), xBoxConstraintTwo-xBoxConstraintOne,start_y-e.getY());
								gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
							}
						}else{
							if(e.getX()>start_x){
								if(e.getY()>start_y){
									//being dragged from top left to bottom right
									drawRect.setBounds(start_x, start_y, e.getX()-start_x, e.getY()-start_y);
								}else{
									//being dragged from bottom left to top right
									drawRect.setBounds(start_x, e.getY(), e.getX()-start_x, start_y-e.getY());
								}
							}else{
								if(e.getY()>start_y){
									//being dragged from top right to bottom left
									drawRect.setBounds(e.getX(), start_y, start_x-e.getX(), e.getY()-start_y);
								}else{
									//being dragged from bottom righ to top left
									drawRect.setBounds(e.getX(), e.getY(), start_x-e.getX(), start_y-e.getY());
								}
							}
							repaint();
							gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
						}
					}else{
						//draw spheres
						if(mainDrawn || auxDrawn){
							//do nothing in dragging events
						}else{
							repaint();
							gBox.drawLine(start_x, start_y, e.getX(), e.getY());
						}
					}
				}else{
					Graphics gBox = rightPanel.getGraphics();
					gBox.setColor(Color.white);
					if(typeOfSelection.equals("boxes")){
						if(mainDrawn){
							if(e.getY()>start_y){
								repaint();
								drawRect.setBounds(xBoxConstraintOne, start_y, xBoxConstraintTwo-xBoxConstraintOne, e.getY()-start_y);
								//gBox.drawRect(xBoxConstraintOne, start_y, xBoxConstraintTwo-xBoxConstraintOne, e.getY()-start_y);
								gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
							}else{
								repaint();
								drawRect.setBounds(xBoxConstraintOne, e.getY(), xBoxConstraintTwo-xBoxConstraintOne,start_y-e.getY());
								//gBox.drawRect(xBoxConstraintOne, e.getY(), xBoxConstraintTwo-xBoxConstraintOne,start_y-e.getY());
								gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
							}
						}else{
							if(e.getX()>start_x){
								if(e.getY()>start_y){
									//being dragged from top left to bottom right
									drawRect.setBounds(start_x, start_y, e.getX()-start_x, e.getY()-start_y);
								}else{
									//being dragged from bottom left to top right
									drawRect.setBounds(start_x, e.getY(), e.getX()-start_x, start_y-e.getY());
								}
							}else{
								if(e.getY()>start_y){
									//being dragged from top right to bottom left
									drawRect.setBounds(e.getX(), start_y, start_x-e.getX(), e.getY()-start_y);
								}else{
									//being dragged from bottom righ to top left
									drawRect.setBounds(e.getX(), e.getY(), start_x-e.getX(), start_y-e.getY());
								}
							}
							repaint();
							gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
						}
					}else{
						//draw spheres
						if(mainDrawn || auxDrawn){
							//do nothing in dragging events
						}else{
							repaint();
							gBox.drawLine(start_x, start_y, e.getX(), e.getY());
						}
					}
				}
				break;
		}
	}

	//*******************************************************************************************************//

	public void mouseReleased(MouseEvent e){
		switch(e.getModifiers()){
			case MouseEvent.BUTTON2_MASK:
				break;
			case MouseEvent.BUTTON3_MASK:
				//popup menu
				if(!mouseDragged){
					if(controller.equals("mainView")){
						rightClickMenu.show(leftPanel, e.getX(), e.getY());
					}else{
						rightClickMenu.show(rightPanel, e.getX(), e.getY());
					}
				}
				break;
			case MouseEvent.BUTTON3_MASK | InputEvent.SHIFT_MASK:
				break;
			case MouseEvent.BUTTON1_MASK | InputEvent.CTRL_MASK:
				//draw
				if(controller.equals("mainView")){
					Graphics gBox = leftPanel.getGraphics();
					gBox.setColor(Color.white);
					if(typeOfSelection.equals("boxes")){
						gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
						if(start_x>e.getX()){	//set the constraints of the box to be drawn on the auxView
							xBoxConstraintOne = e.getX();
							xBoxConstraintTwo = start_x;
						}else{
							xBoxConstraintOne = start_x;
							xBoxConstraintTwo = e.getX();
						}
						setSimPoints(e.getX(), e.getY());
					}else{
						//set up for sphere
						if(auxDrawn){
							//draw blob
							gBox.fillOval(xBoxConstraintOne-10,e.getY()-10, 20, 20);
						}else{
							gBox.drawLine(start_x, start_y, e.getX(), e.getY());
							xBoxConstraintOne = start_x;
							xBoxConstraintTwo = e.getX();
						}
						setSimPoints(e.getX(), e.getY());
					}
					mainDrawn = true;
				}else{
					Graphics gBox = rightPanel.getGraphics();
					gBox.setColor(Color.white);
					if(typeOfSelection.equals("boxes")){
						gBox.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
						if(start_x>e.getX()){	//set the constraints of the box to be drawn on the mainView
							xBoxConstraintOne = e.getX();
							xBoxConstraintTwo = start_x;
						}else{
							xBoxConstraintOne = start_x;
							xBoxConstraintTwo = e.getX();
						}
						setSimPoints(e.getX(), e.getY());
					}else{
						//set up for sphere
						if(mainDrawn){
							//draw blob
							gBox.fillOval(xBoxConstraintOne-10,e.getY()-10, 20, 20);
						}else{
							gBox.drawLine(start_x, start_y, e.getX(), e.getY());
							xBoxConstraintOne = start_x;
							xBoxConstraintTwo = e.getX();
						}
						setSimPoints(e.getX(), e.getY());
					}
					auxDrawn = true;
				}
				if(mainDrawn && auxDrawn){	//both views have been drawn on, so request an image
					if(typeOfSelection.equals("boxes")){
						System.out.println("requesting box");
						setPosVectors();
						ccs.addRequest(new SpecifyBox(encoded));
					}else{
						System.out.println("requesting sphere");
						setPosVectors();
						ccs.addRequest(new SpecifySphere(encoded));
					}
					mainDrawn = false;
					auxDrawn = false;
				}
				break;

		}
		drawVex();

	}

	/**************************************************************************************************/
	/*
	 * This function converts pixel values on screen, specified by mouse actions, into simulation space units.
	 * The arrays xSimnNumbers, ySimNumbers, and zSimNumbers store the x, y, and z values of all points corresponding
	 * to the box or sphere being drawn.  The box points follow the illustration below
	 *
	 *			4---------5
	 *		       /|        /|
	 *		      / |       / |
	 *		     /  |      /  |
	 *		    0---------1   |
	 *		    |   7-----|---6
	 *		    |  /      |  /
	 *		    | /       | /
	 *		    |/        |/
	 *		    3---------2
	 *
	 * so xSimNumbers[0], ySimNumbers[0], and zSimNumbers[0] correspond to the x,y, and z sim points of point zero
	 */

	private void setSimPoints(int currentX, int currentY){
		if(typeOfSelection.equals("boxes")){
			if(controller.equals("mainView") && !auxDrawn){
				num_points = 8;
				xSimNumbers = new double[num_points];
				ySimNumbers = new double[num_points];

				if(currentX>start_x){
					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(start_x, mainView.x);
					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(currentX, mainView.x);
					if(currentY > start_y){
						//being dragged from top left to bottom right
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(start_y, mainView.y);
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(currentY, mainView.y);
					}else{
						//being dragged from bottom left to top right
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(start_y, mainView.y);
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(currentY, mainView.y);
					}
				}else{
					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(currentX, mainView.x);
					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(start_x, mainView.x);
					if(currentY > start_y){
						//being dragged from top right to bottom left
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(start_y, mainView.y);
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(currentY, mainView.y);

					}else{
						//being dragged from bottom righ to top left
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(start_y, mainView.y);
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(currentY, mainView.y);
					}
				}

			}else if(controller.equals("auxView") && !mainDrawn){
				num_points = 8;
				xSimNumbers = new double[num_points];
				ySimNumbers = new double[num_points];
				
				if(currentX>start_x){
					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(start_x, auxView.x);
					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(currentX, auxView.x);
					if(currentY > start_y){
						//being dragged from top left to bottom right
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(start_y, auxView.y);
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(currentY, auxView.y);
					}else{
						//being dragged from bottom left to top right
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(start_y, auxView.y);
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(currentY, auxView.y);
					}
				}else{
					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(currentX, mainView.x);
					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(start_x, mainView.x);
					if(currentY > start_y){
						//being dragged from top right to bottom left
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(start_y, auxView.y);
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(currentY, auxView.y);

					}else{
						//being dragged from bottom righ to top left
						ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(start_y, auxView.y);
						ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(currentY, auxView.y);
					}
				}

			}else if(controller.equals("mainView") && auxDrawn){
				num_points = 8;
				zSimNumbers = new double[num_points];
				if(currentY > start_y){
					//setting up zSimNumbers
					zSimNumbers[4] = zSimNumbers[5] = zSimNumbers[6] = zSimNumbers[7] = zSimMultiply(start_y, auxView.y);
					zSimNumbers[0] = zSimNumbers[1] = zSimNumbers[2] = zSimNumbers[3] = zSimMultiply(currentY, auxView.y);
				}else{
					zSimNumbers[4] = zSimNumbers[5] = zSimNumbers[6] = zSimNumbers[7] = zSimMultiply(currentY, auxView.y);
					zSimNumbers[0] = zSimNumbers[1] = zSimNumbers[2] = zSimNumbers[3] = zSimMultiply(start_y, auxView.y);
				}
			}else if(controller.equals("auxView") && mainDrawn){
				num_points = 8;
				zSimNumbers = new double[num_points];
				if(currentY > start_y){
					//setting up zSimNumbers
					zSimNumbers[4] = zSimNumbers[5] = zSimNumbers[6] = zSimNumbers[7] = zSimMultiply(start_y, mainView.y);
					zSimNumbers[0] = zSimNumbers[1] = zSimNumbers[2] = zSimNumbers[3] = zSimMultiply(currentY, mainView.y);
				}else{
					zSimNumbers[4] = zSimNumbers[5] = zSimNumbers[6] = zSimNumbers[7] = zSimMultiply(currentY, mainView.y);
					zSimNumbers[0] = zSimNumbers[1] = zSimNumbers[2] = zSimNumbers[3] = zSimMultiply(start_y, mainView.y);
				}
			}
		}else{
			if(controller.equals("mainView") && !auxDrawn){
				num_points = 2;
				xSimNumbers = new double[num_points];
				ySimNumbers = new double[num_points];

				xSimNumbers[0] = xSimMultiply(start_x, mainView.x);
				xSimNumbers[1] = xSimMultiply(currentX, mainView.x);
				ySimNumbers[0] = ySimMultiply(start_y, mainView.y);
				ySimNumbers[1] = ySimMultiply(currentY, mainView.y);

			}else if(controller.equals("auxView") && !mainDrawn){
				num_points = 2;
				xSimNumbers = new double[num_points];
				ySimNumbers = new double[num_points];

				xSimNumbers[0] = xSimMultiply(start_x, auxView.x);
				xSimNumbers[1] = xSimMultiply(currentX, auxView.x);
				ySimNumbers[0] = ySimMultiply(start_y, auxView.y);
				ySimNumbers[1] = ySimMultiply(currentY, auxView.y);
			
			}else if(controller.equals("mainView") && auxDrawn){
				num_points = 2;
				zSimNumbers = new double[num_points];

				zSimNumbers[0] = zSimMultiply(start_y ,auxView.y);

			}else if(controller.equals("auxView") && mainDrawn){
				num_points = 2;
				zSimNumbers = new double[num_points];

				zSimNumbers[0] = zSimMultiply(start_y ,mainView.y);
			}
		}
	}

	/**************************************************************************************************/
	/*
	 * used to convert pixel values to simulation space along the x-axis
	 */
	public double xSimMultiply(double mult, Vector3D respect){
		return ((2*(mult/mainView.width))-1)*respect.length();
	}

	/**************************************************************************************************/
	/*
	 * used to convert pixel values to simulation space along the y-axis
	 */
	public double ySimMultiply(double mult, Vector3D respect){
		return (1-(2*(mult/mainView.height)))*respect.length();
	}

	/**************************************************************************************************/
	/*
	 * used to convert pixel values to simulatino space along the z-axis
	 */
	public double zSimMultiply(double mult, Vector3D respect){
		return (1-(2*(mult/mainView.height)))*respect.length();
	}
	
	/**************************************************************************************************/
	/*
	 * this method uses the simNumbers in xSimNumbers, ySimNumbers, and zSimNumbers, in conjunction with
	 * the vector information of the two views to locate the points of a selection box or sphere in
	 * actual simulation space
	 */
	public void setPosVectors(){
		//System.out.println("Begin posVector process");
		if(typeOfSelection.equals("boxes")){
			num_points = 8;
			posVectors = new Vector3D[num_points];
			if(controller.equals("mainView")){
				Vector3D xHat = auxView.x.unitVector();
				Vector3D yHat = auxView.y.unitVector();
				Vector3D zHat = auxView.z.unitVector();//.scalarMultiply(-1);

				for(int x = 0; x < num_points; x++){
					posVectors[x] = auxView.origin.plus(xHat.scalarMultiply(xSimNumbers[x])).plus(yHat.scalarMultiply(ySimNumbers[x])).plus(zHat.scalarMultiply(zSimNumbers[x]));
					System.out.println("Vector " + x + " is at " + posVectors[x].toString());
				}
			}else{
				Vector3D xHat = mainView.x.unitVector();
				Vector3D yHat = mainView.y.unitVector();
				Vector3D zHat = mainView.z.unitVector().scalarMultiply(-1);

				for(int x = 0; x < num_points; x++){
					posVectors[x] = mainView.origin.plus(xHat.scalarMultiply(xSimNumbers[x])).plus(yHat.scalarMultiply(ySimNumbers[x])).plus(zHat.scalarMultiply(zSimNumbers[x]));
					System.out.println("Vector " + x + " is at " + posVectors[x].toString());
				}
			}
			encoded = encodeVectors();
		}else{
			posVectors = new Vector3D[1];
			if(controller.equals("mainView")){
				Vector3D xHat = auxView.x.unitVector();
				Vector3D yHat = auxView.y.unitVector();
				Vector3D zHat = auxView.z.unitVector();//.scalarMultiply(-1);

				posVectors[0] = auxView.origin.plus(xHat.scalarMultiply(xSimNumbers[0])).plus(yHat.scalarMultiply(ySimNumbers[0])).plus(zHat.scalarMultiply(zSimNumbers[0]));
			}else{
				Vector3D xHat = mainView.x.unitVector();
				Vector3D yHat = mainView.y.unitVector();
				Vector3D zHat = mainView.z.unitVector().scalarMultiply(-1);

				posVectors[0] = mainView.origin.plus(xHat.scalarMultiply(xSimNumbers[0])).plus(yHat.scalarMultiply(ySimNumbers[0])).plus(zHat.scalarMultiply(zSimNumbers[0]));
			}
			double xDist = xSimNumbers[1]-xSimNumbers[0];
			double yDist = ySimNumbers[1]-ySimNumbers[0];
			xDist = xDist*xDist;
			yDist = yDist*yDist;
			double radius = Math.sqrt(xDist+yDist);
			zSimNumbers[1]=radius;
			encoded = encodeVectors();
		}
	}
	
	/**************************************************************************************************/
	/*
	 * this method encodes the position vectors of a selection box or sphere into binary, for use by
	 * the server
	 */
	private byte[] encodeVectors() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
		try {
			DataOutputStream dos = new DataOutputStream(baos);
			if(typeOfSelection.equals("boxes")){
				for(int x = 0; x<num_points; x++){
					dos.writeDouble(posVectors[x].x);
					dos.writeDouble(posVectors[x].y);
					dos.writeDouble(posVectors[x].z);
				}
			}else{
				System.out.println("ENcoding sphere");
				System.out.println("Radius: " + zSimNumbers[1]);
				System.out.println("vec 1: " + posVectors[0].toString());
				dos.writeDouble(posVectors[0].x);
				dos.writeDouble(posVectors[0].y);
				dos.writeDouble(posVectors[0].z);
				dos.writeDouble(zSimNumbers[1]);
			}
		} catch(IOException e) {
			System.err.println("Couldn't encode request!");
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	//*******************************************************************************************************//

	public void mouseClicked(MouseEvent e){}

	//*******************************************************************************************************//
	/*
	 * the variables leftPanel and rightPanel are listening in on this method, which sets the variable
	 * 'controller' to the appropriate view, either "mainView" or "auxView" depending on which panel
	 * the mouse enters into
	 * TODO: this method should implement some sort of border or indicator of which panel has control
	 */
	public void mouseEntered(MouseEvent e){
		if(controller.equals((((MainView)((JPanel)e.getComponent()).getComponent(0)).getSignature()))){
			//do nothing
		}else{
			controller = (((MainView)((JPanel)e.getComponent()).getComponent(0)).getSignature());
			if(controller.equals("mainView")){
				//mainView.setBorder(BorderFactory.createRaisedBevelBorder());
				//auxView.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			}else{
				//auxView.setBorder(BorderFactory.createRaisedBevelBorder());
				//mainView.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			}
		}
	}

	//*******************************************************************************************************//

	public void mouseExited(MouseEvent e){}

	//*******************************************************************************************************//
	
	public void mouseMoved(MouseEvent e){}

	//*******************************************************************************************************//
	/*
	 * called from CommandParser to zoom the views
	 */
	public void zoom(double arg){
		mainView.zoom(arg);
		auxView.zoom(arg);
	}

	//*******************************************************************************************************//
	/*
	 * called from CommandParser to rotate the views
	 */
	public void rotateNumeric(double amount, String direction){
		mainDrawn = auxDrawn = false;
		double theta = amount * Math.PI / 180;
		if(direction.equals("left")){
			mainView.drag(-1, 0, "y", theta);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		}else if(direction.equals("right")){
			mainView.drag(-1, 0, "y", -theta);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		}else if(direction.equals("up")){
			mainView.drag(-1, 0, "x", theta);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		}else if(direction.equals("down")){
			mainView.drag(-1, 0, "x", -theta);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		}else if(direction.equals("clock")){
			mainView.drag(-1, 0, "z", theta);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		}else if(direction.equals("counter")){
			mainView.drag(-1, 0, "z", -theta);
			auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		}
		drawVex();
		//blabedy
	}

	//*******************************************************************************************************//
	/*
	 * called indirectly from CommandParser to pan the views
	 */
	public void panNumeric(double amount, String direction){
		mainDrawn = auxDrawn = false;
		mainView.numericPan(amount, direction);
		auxView.update(mainView.x, mainView.y, mainView.z, mainView.origin, true);
		Graphics gl = leftPanel.getGraphics();
		/* the bloody thing won't stay drawn on the screen
		gl.setColor(Color.white);
		gl.drawLine((leftPanel.getWidth()/2)-30, (leftPanel.getHeight()/2),(leftPanel.getWidth()/2)+30,(leftPanel.getHeight()/2));
		gl.drawLine((leftPanel.getWidth()/2), (leftPanel.getHeight()/2)-30,(leftPanel.getWidth()/2),(leftPanel.getHeight()/2)+30);
		gl = rightPanel.getGraphics();
		gl.setColor(Color.white);
		gl.drawLine((rightPanel.getWidth()/2)-30, (rightPanel.getHeight()/2),(rightPanel.getWidth()/2)+30,(rightPanel.getHeight()/2));
		gl.drawLine((rightPanel.getWidth()/2), (rightPanel.getHeight()/2)-30,(rightPanel.getWidth()/2),(rightPanel.getHeight()/2)+30);
		*/
	}

	//*******************************************************************************************************//
	/*
	 * draw a set of x, y, and z vectors corresponding to the x, y, and z vectors of both the mainView and auxView
	 */
	public void drawVex(){
		//need four xPixel values
		Double xPixOrigin, yPixOrigin, xPixDraw, yPixDraw;
		double xSimOne, ySimOne, xSimTwo, ySimTwo;
		Vector3D adder, r;
		Graphics g = mainView.display.getGraphics();
		Graphics ga = auxView.display.getGraphics();
		g.setColor(Color.white);
		ga.setColor(Color.white);
		int divisor = 3;	//use this to adjust a global property

		xSimOne = mainView.x.unitVector().dot(mainView.origin.minus(mainView.origin));
		ySimOne = mainView.y.unitVector().dot(mainView.origin.minus(mainView.origin));

		xPixOrigin = new Double((((xSimOne / mainView.x.length()) + 1) / 2) * mainView.width);
		yPixOrigin = new Double((((ySimOne / mainView.y.length()) + 1) / 2) * mainView.height);

		adder = new Vector3D(Math.sqrt((mainView.x.length()*mainView.x.length())+(mainView.y.length()*mainView.y.length()))/5, 0, 0);
		r = mainView.origin.plus(adder);

		xSimTwo = mainView.x.unitVector().dot(r.minus(mainView.origin));
		ySimTwo = mainView.y.unitVector().dot(r.minus(mainView.origin));

		xPixDraw = new Double((((xSimTwo / mainView.x.length()) + 1) / 2) * mainView.width);
		yPixDraw = new Double((((ySimTwo / mainView.y.length()) + 1) / 2) * mainView.height);

		g.drawLine(xPixOrigin.intValue() - mainView.width/divisor, yPixOrigin.intValue()+mainView.height/divisor, xPixDraw.intValue()- mainView.width/divisor, yPixDraw.intValue()+ mainView.height/divisor);
		g.drawString("x", xPixDraw.intValue()-mainView.width/divisor, yPixDraw.intValue()+mainView.height/divisor);

		//**************************************************************************************

		xSimOne = mainView.x.unitVector().dot(mainView.origin.minus(mainView.origin));
		ySimOne = mainView.y.unitVector().dot(mainView.origin.minus(mainView.origin));

		xPixOrigin = new Double((((xSimOne / mainView.x.length()) + 1) / 2) * mainView.width);
		yPixOrigin = new Double((((ySimOne / mainView.y.length()) + 1) / 2) * mainView.height);

		adder = new Vector3D(0 , Math.sqrt((mainView.x.length()*mainView.x.length())+(mainView.y.length()*mainView.y.length()))/5, 0);
		r = mainView.origin.plus(adder);

		xSimTwo = mainView.x.unitVector().dot(r.minus(mainView.origin));
		ySimTwo = mainView.y.unitVector().dot(r.minus(mainView.origin));

		xPixDraw = new Double((((xSimTwo / mainView.x.length()) + 1) / 2) * mainView.width);
		yPixDraw = new Double((((ySimTwo / mainView.y.length()) + 1) / 2) * mainView.height);

		g.drawLine(xPixOrigin.intValue() - mainView.width/divisor, yPixOrigin.intValue()+mainView.height/divisor, xPixDraw.intValue()- mainView.width/divisor, yPixDraw.intValue()+ mainView.height/divisor);
		g.drawString("y", xPixDraw.intValue()-mainView.width/divisor, yPixDraw.intValue()+mainView.height/divisor);

		//**************************************************************************************

		xSimOne = mainView.x.unitVector().dot(mainView.origin.minus(mainView.origin));
		ySimOne = mainView.y.unitVector().dot(mainView.origin.minus(mainView.origin));

		xPixOrigin = new Double((((xSimOne / mainView.x.length()) + 1) / 2) * mainView.width);
		yPixOrigin = new Double((((ySimOne / mainView.y.length()) + 1) / 2) * mainView.height);

		adder = new Vector3D(0 , 0, Math.sqrt((mainView.x.length()*mainView.x.length())+(mainView.y.length()*mainView.y.length()))/5);
		r = mainView.origin.plus(adder);

		xSimTwo = mainView.x.unitVector().dot(r.minus(mainView.origin));
		ySimTwo = mainView.y.unitVector().dot(r.minus(mainView.origin));

		xPixDraw = new Double((((xSimTwo / mainView.x.length()) + 1) / 2) * mainView.width);
		yPixDraw = new Double((((ySimTwo / mainView.y.length()) + 1) / 2) * mainView.height);

		g.drawLine(xPixOrigin.intValue() - mainView.width/divisor, yPixOrigin.intValue()+mainView.height/divisor, xPixDraw.intValue()- mainView.width/divisor, yPixDraw.intValue()+ mainView.height/divisor);
		g.drawString("z", xPixDraw.intValue()-mainView.width/divisor, yPixDraw.intValue()+mainView.height/divisor);
		
		//**************************************************************************************

		xSimOne = auxView.x.unitVector().dot(auxView.origin.minus(auxView.origin));
		ySimOne = auxView.y.unitVector().dot(auxView.origin.minus(auxView.origin));

		xPixOrigin = new Double((((xSimOne / auxView.x.length()) + 1) / 2) * auxView.width);
		yPixOrigin = new Double((((ySimOne / auxView.y.length()) + 1) / 2) * auxView.height);

		adder = new Vector3D(Math.sqrt((auxView.x.length()*auxView.x.length())+(auxView.y.length()*auxView.y.length()))/5 , 0, 0);
		r = auxView.origin.plus(adder);

		xSimTwo = auxView.x.unitVector().dot(r.minus(auxView.origin));
		ySimTwo = auxView.y.unitVector().dot(r.minus(auxView.origin));

		xPixDraw = new Double((((xSimTwo / auxView.x.length()) + 1) / 2) * auxView.width);
		yPixDraw = new Double((((ySimTwo / auxView.y.length()) + 1) / 2) * auxView.height);

		ga.drawLine(xPixOrigin.intValue() + auxView.width/divisor, yPixOrigin.intValue() + auxView.height/divisor, xPixDraw.intValue() + auxView.width/divisor, yPixDraw.intValue() + auxView.height/divisor);
		ga.drawString("x", xPixDraw.intValue()+auxView.width/divisor, yPixDraw.intValue()+auxView.height/divisor);
		
		//**************************************************************************************

		xSimOne = auxView.x.unitVector().dot(auxView.origin.minus(auxView.origin));
		ySimOne = auxView.y.unitVector().dot(auxView.origin.minus(auxView.origin));

		xPixOrigin = new Double((((xSimOne / auxView.x.length()) + 1) / 2) * auxView.width);
		yPixOrigin = new Double((((ySimOne / auxView.y.length()) + 1) / 2) * auxView.height);

		adder = new Vector3D(0 , Math.sqrt((auxView.x.length()*auxView.x.length())+(auxView.y.length()*auxView.y.length()))/5, 0);
		r = auxView.origin.plus(adder);

		xSimTwo = auxView.x.unitVector().dot(r.minus(auxView.origin));
		ySimTwo = auxView.y.unitVector().dot(r.minus(auxView.origin));

		xPixDraw = new Double((((xSimTwo / auxView.x.length()) + 1) / 2) * auxView.width);
		yPixDraw = new Double((((ySimTwo / auxView.y.length()) + 1) / 2) * auxView.height);

		ga.drawLine(xPixOrigin.intValue()+auxView.width/divisor, yPixOrigin.intValue()+auxView.height/divisor, xPixDraw.intValue()+auxView.width/divisor, yPixDraw.intValue()+auxView.height/divisor);
		ga.drawString("y", xPixDraw.intValue()+auxView.width/divisor, yPixDraw.intValue()+auxView.height/divisor);

		//**************************************************************************************

		xSimOne = auxView.x.unitVector().dot(auxView.origin.minus(auxView.origin));
		ySimOne = auxView.y.unitVector().dot(auxView.origin.minus(auxView.origin));

		xPixOrigin = new Double((((xSimOne / auxView.x.length()) + 1) / 2) * auxView.width);
		yPixOrigin = new Double((((ySimOne / auxView.y.length()) + 1) / 2) * auxView.height);

		adder = new Vector3D(0 , 0, Math.sqrt((auxView.x.length()*auxView.x.length())+(auxView.y.length()*auxView.y.length()))/5);
		r = auxView.origin.plus(adder);

		xSimTwo = auxView.x.unitVector().dot(r.minus(auxView.origin));
		ySimTwo = auxView.y.unitVector().dot(r.minus(auxView.origin));

		xPixDraw = new Double((((xSimTwo / auxView.x.length()) + 1) / 2) * auxView.width);
		yPixDraw = new Double((((ySimTwo / auxView.y.length()) + 1) / 2) * auxView.height);

		ga.drawLine(xPixOrigin.intValue()+auxView.width/divisor, yPixOrigin.intValue()+auxView.height/divisor, xPixDraw.intValue()+auxView.width/divisor, yPixDraw.intValue()+auxView.height/divisor);
		ga.drawString("z", xPixDraw.intValue()+auxView.width/divisor, yPixDraw.intValue()+auxView.height/divisor);
		/*
		System.out.println("Drawing");
		System.out.println("xSimOne: " + xSimOne);
		System.out.println("ySimOne: " + ySimOne);
		System.out.println("xSimTwo: " + xSimTwo);
		System.out.println("ySimTwo: " + ySimTwo);
		System.out.println("xPixOrigin: " + xPixOrigin);
		System.out.println("yPixorigin: " + yPixOrigin);
		System.out.println("xPixDraw: " + xPixDraw);
		System.out.println("yPixDraw: " + yPixDraw);
		*/

	}

	//*******************************************************************************************************//
	//*******************************************************************************************************//
	//													 //
	//                         SERVER REQUESTS APPEAR BELOW HERE		                                 //
	//													 //
	//*******************************************************************************************************//
	//*******************************************************************************************************//

	/**************************************************************************************************/

	private class SpecifyBox extends CcsThread.request {

		public SpecifyBox(byte[] data){
			super("SpecifyBox", data);
		}

		public void handleReply(byte[] data){
			String reply = new String(data);
			System.out.println("The box is: " + reply);
			refComPane.updateGroupList(reply);
		}
	}

	//*************************************************************************************************

	private class SpecifySphere extends CcsThread.request {

		public SpecifySphere(byte[] data){
			super("SpecifySphere", data);
		}

		public void handleReply(byte[] data){
			String reply = new String(data);
			System.out.println("The sphere is: " + reply);
			refComPane.updateGroupList(reply);
		}
	}
	
	//*************************************************************************************************

	private class ClearBoxes extends CcsThread.request {

		public ClearBoxes(){
			super("ClearBoxes", null);
		}

		public void handleReply(byte[] data){}
	}

	/**************************************************************************************************/
	
	private class ClearSpheres extends CcsThread.request {

		public ClearSpheres(){
			super("ClearSpheres", null);
		}
		public void handleReply(byte[] data){}
	}
	
	/**************************************************************************************************/

	private class ReColor extends CcsThread.request {

		public ReColor(byte[] data){
			super("Recolor", data);
		}
		public void handleReply(byte[] data){}
	}
	
	/**************************************************************************************************/
	//												  //
	//					Server requests end					  //
	//												  //
	//*************************************************************************************************/



	/**************************************************************************************************/
	/*												  */
	/*					Color mapping stuff below here				  */
	/*												  */
	/**************************************************************************************************/

	/*
	 * returns a standard color map
	 */
	private ColorModel createWRBBColorModel() {
		int cmap_size=254;
		wrbb_red = new byte[256];
		wrbb_green = new byte[256];
		wrbb_blue = new byte[256];
    		int i;
		int nextColor = 1;
    		int chunk_size = ((cmap_size - 1) / 5);

		for(i = 0; i < chunk_size; i++) {
        		wrbb_red[nextColor] = 0;
        		wrbb_green[nextColor] = 0;
        		wrbb_blue[nextColor++] = (byte) (255 * i / chunk_size);
    		}

		for(i = 0; i < chunk_size; i++) {
        		wrbb_red[nextColor] = (byte) (255 * i / chunk_size);
        		wrbb_green[nextColor] = 0;
        		wrbb_blue[nextColor++] = (byte) 255;
    		}

		for(i = 0; i < chunk_size; i++) {
         		wrbb_red[nextColor] = (byte) 255;
        		wrbb_green[nextColor] = 0;
        		wrbb_blue[nextColor++] = (byte) (255 - 255 * i / chunk_size);
    		}

		for(i = 0; i < chunk_size; i++) {
        		wrbb_red[nextColor] = (byte) 255;
        		wrbb_green[nextColor] = (byte) (255 * i / chunk_size);
        		wrbb_blue[nextColor++] = (byte) 0;
    		}

		for(i = 0; i < chunk_size; i++) {
         		wrbb_red[nextColor] = (byte) 255;
        		wrbb_green[nextColor] = (byte) 255;
        		wrbb_blue[nextColor++] = (byte) (255 * i / chunk_size);
    		}

		while(nextColor < cmap_size + 1) {
			wrbb_red[nextColor] = (byte) 255;
			wrbb_green[nextColor] = (byte) 255;
			wrbb_blue[nextColor++] = (byte) 255;
		}

		wrbb_red[0] = 0;
		wrbb_green[0] = 0;
		wrbb_blue[0] = 0;
		wrbb_red[255] = 0;
		wrbb_green[255] = (byte)255;
		wrbb_blue[255] = 0;

		colorMapType = "standard";
		return new IndexColorModel(8, 256, wrbb_red, wrbb_green, wrbb_blue);
	}

	/**************************************************************************************************/

	/*
	 * returns a colormap that is simply the current map translated according to mousedrags
	 */
	private ColorModel resetWRBB(MouseEvent e){
		//System.out.println("Resetting colors");
		System.out.println("color_start is: " + color_start);
		System.out.println("e.getX() is: " + e.getX());
		int diff = color_start - e.getX();
		diff = (int) (254.0 * diff / (mainView.width + auxView.width));
		color_start = e.getX();
		byte[] transferRed = new byte[254];
		byte[] transferGreen = new byte[254];
		byte[] transferBlue = new byte[254];

		for(int x = 0; x < 254; x++){
			transferRed[x] = wrbb_red[((diff+x+254)%254)+1];
			transferGreen[x] = wrbb_green[((diff+x+254)%254)+1];
			transferBlue[x] = wrbb_blue[((diff+x+254)%254)+1];
		}
		for(int x = 0; x < transferRed.length; x++){
			wrbb_red[x+1] = transferRed[x];
			wrbb_green[x+1] = transferGreen[x];
			wrbb_blue[x+1] = transferBlue[x];
		}

		return new IndexColorModel(8, 256, wrbb_red, wrbb_green, wrbb_blue);
	}

	/**************************************************************************************************/

	/*
	 * returns a color map that is simply the current color map inverted
	 */
	private ColorModel invertWRBB(){
		byte[] transferRed = new byte[254];
		byte[] transferGreen = new byte[254];
		byte[] transferBlue = new byte[254];
		for(int x = 0; x < 254; x++){
			transferRed[x] = wrbb_red[254-x];
			transferGreen[x] = wrbb_green[254-x];
			transferBlue[x] = wrbb_blue[254-x];
		}
		for(int x = 0; x < transferRed.length; x++){
			wrbb_red[x+1] = transferRed[x];
			wrbb_green[x+1] = transferGreen[x];
			wrbb_blue[x+1] = transferBlue[x];
		}
		return new IndexColorModel(8, 256, wrbb_red, wrbb_green, wrbb_blue);
	}
	
	/**************************************************************************************************/
	/*
	 * returns a new rainbow colormap for the particles to be colored with
	 */
	private ColorModel rainbowWRBB(){
		int i,j;
		double slope;
		double offset;
		byte[] rainbow_red = new byte[254];
		byte[] rainbow_green = new byte[254];
		byte[] rainbow_blue = new byte[254];

		slope = 205.0/42.0;
		for(i = 0; i < 43; i++){
			rainbow_red[i] = (byte)255;
			rainbow_green[i] = (byte)((int)(slope * (double)i + 50.0 + 0.5));
			rainbow_blue[i] = (byte)0;
		}
		slope = 205.0/21.0;
		for(i = 43; i < 64; i++){
			rainbow_red[i] = (byte)(255 - (int)(slope * (double)(i - 42) + 0.5));
			rainbow_green[i] = (byte)255;
			rainbow_blue[i] = (byte)0;
		}
		slope = 205.0/29.0;
		for(i = 64; i < 94; i++){
			rainbow_red[i] = (byte)0;
			rainbow_green[i] = (byte)255;
			rainbow_blue[i] = (byte)((int)(slope * (double)(i - 64) + 50.0 + 0.5));
		}
		slope = 255.0/31.0;
		for(i = 94; i < 125; i++){
			rainbow_red[i] = (byte)0;
			rainbow_green[i] = (byte)(255 - (int)(slope * (double)(i - 93) + 0.5));
			rainbow_blue[i] = (byte)255;
		}

		/*
		 * at this point, the rainbow color map only has 125 elements in it, so what this method does
		 * is, it sets two indexes in wrbb arrays for every one color in the 125 colors stored in the
		 * rainbow arrays, resulting in 250 indexes of wrbb filled...the last couple indexes at the end are
		 * taken care of in the while loop that follows
		 */
		for(i = 1, j = 1; i < 125; i++, j = j+2){
			wrbb_red[j] = rainbow_red[i];
			wrbb_red[j+1] = rainbow_red[i];
			wrbb_green[j] = rainbow_green[i];
			wrbb_green[j+1] = rainbow_green[i];
			wrbb_blue[j] = rainbow_blue[i];
			wrbb_blue[j+1] = rainbow_blue[i];
		}

		while(j<255){
			wrbb_red[j] = rainbow_red[124];
			wrbb_green[j] = rainbow_green[124];
			wrbb_blue[j] = rainbow_blue[124];
			j++;
		}
		colorMapType = "rainbow";
		return new IndexColorModel(8, 256, wrbb_red, wrbb_green, wrbb_blue);
	}


}
