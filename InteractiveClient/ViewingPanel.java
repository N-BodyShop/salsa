//ViewingPanel

/*
 * List of commands that can be used:
 * 1. hold ctrl, click and drag mouse button one to draw a selectino box on the window
 * 2. hold shift and right click and drag in the vertical direction to translate the z cooridantes of the
 * 	origin vector...if the translation slider does not seem to be updating, drag smaller increments
 * 	as you hold the shift button...also, try translating a small portion in the opposite direction to
 * 	get the UI to update
 * 3. right click and drag vertically to zoom in and out
 * 4. click and drag the roller to pan
 * 5. click button one while holding the shift and control buttons to open a 'rotated' view
 *	of the current image...this is used for selecting particular sections of simulated space
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class ViewingPanel extends JPanel implements MouseListener, MouseMotionListener, ChangeListener, ActionListener, ComponentListener {

	private CcsThread ccs;
	private static ColorModel wrbb;
	private JPanel displayPanel;
	private JLabel display;
	private int width, height, lowResWidth, lowResHeight;
	private int start_x, start_y, start_z;
	private Vector3D x, y, z, origin;
	private double lowValue, highValue;
	private Config config;
	private JSliderOldValue updownSlider, leftrightSlider, clockwiseSlider, zIndicator;
	private JTextField lowValueField, highValueField;
	private ColorMapDisplay cmdisplay;
	private double boxSize;
	private MemoryImageSource source;
	private byte[] pixels;
	private boolean resizedImage;
	private boolean lowRes;
	private double zLoc, depth;	//used to assist with origin's z translation
	private Box theBox;	//used to construct the ViewingPanel in the constructor, and again in CcsConfigRequest
	private int sliderMult;	//used in mouseReleased to accurately update zSlider according to z translation of origin
	private String h;	//the host name stored
	private int p;		//the port name stored
	private int NUM_POINTS;	//sets the size of xSimNumbers, ySimNumbers, zSimNumbers, and posVectors
	private double[] xSimNumbers, ySimNumbers, zSimNumbers;	//used to calculate translation vectors for subset selections
	private Vector3D[] posVectors;				//store the translation vectors mentioned above
	private int xBoxConstraintOne, xBoxConstraintTwo;	//set the constraints of the drawing range/point for SelectionView objects
	private boolean drawn, rotated;		//used to control whether or not u can draw on a SelectionView object
	private byte[] encoded;	//the array of bytes passed to the server for drawing subest boxes
	private SelectionView sv;	//the SelectionView object that this window opens
	private Rectangle drawRect = new Rectangle();	//used to draw rubber band boxes
	private CommandPane refComPane;	//reference CommandPane, to access methods in CommandPane class
	private String typeOfSelection;	//keep track of whether user is subsetting using a box or a sphere
	private JFrame parentFrame;
	private ParentPanel parent;

	/****************************************************************************
	 * CONSTRUCTOR
	 */
	public ViewingPanel(String hostname, int port, String cmd, CommandPane ref, JFrame frame, ParentPanel par) {
		super(new BorderLayout());
		ccs = new CcsThread(new Label(), hostname, port);
		h = hostname;
		p = port;
		refComPane = ref;
		parentFrame = frame;
		parent = par;

		wrbb = createWRBBColorModel();

		start_z = 0;
		width = 512;
		height = 512;
		drawn = rotated = false;
		typeOfSelection = "nothing";

		lowValue = 0;
		highValue = 1;

		cmdisplay = new ColorMapDisplay(512, 10);
		cmdisplay.redisplay(wrbb);

		add(cmdisplay, BorderLayout.NORTH);
		
		pixels = new byte[width * height];
		source = new MemoryImageSource(width, height, wrbb, pixels, 0, width);
		source.setAnimated(true);
		display = new JLabel();
		display.setIcon(new ImageIcon(display.createImage(source)));
		display.addMouseListener(this);
		display.addMouseMotionListener(this);

		lowRes = false;
		lowResWidth = 50;
		lowResHeight = 50;

		displayPanel = new JPanel();
		displayPanel.add(display);
		displayPanel.addComponentListener(this);
		add(displayPanel, BorderLayout.CENTER);
		
		updownSlider = new JSliderOldValue(0, 359, 180);
		updownSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSliderOldValue source = (JSliderOldValue) e.getSource();
				if(!source.getValueIsAdjusting() && (source.getValue() - source.getOldValue() != 0)) {
					//System.out.println("Adjusting up/down slider!");
					drawn = false;
					rotated = true;
					try{sv.disableButton();}catch(Exception le){}
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					y.rotate(theta, x.unitVector());
					z = x.cross(y);
					ccs.addRequest(new ImageRequest(), true);
					source.setOldValue(source.getValue());
				}
			}
		});
		leftrightSlider = new JSliderOldValue(0, 359, 180);
		leftrightSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSliderOldValue source = (JSliderOldValue) e.getSource();
				if(!source.getValueIsAdjusting() && (source.getValue() - source.getOldValue() != 0)) {
					//System.out.println("Adjusting left/right slider!");
					drawn = false;
					rotated = true;
					try{sv.disableButton();}catch(Exception le){}
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					x.rotate(theta, y.unitVector());
					z = x.cross(y);
					ccs.addRequest(new ImageRequest(), true);
					source.setOldValue(source.getValue());
				}
			}
		});
		clockwiseSlider = new JSliderOldValue(0, 359, 180);
		clockwiseSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSliderOldValue source = (JSliderOldValue) e.getSource();
				if(!source.getValueIsAdjusting() && (source.getValue() - source.getOldValue() != 0)) {
					//System.out.println("Adjusting clock/cntr slider!");
					drawn = false;
					rotated = true;
					try{sv.disableButton();}catch(Exception le){}
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					x.rotate(theta, z.unitVector());
					y.rotate(theta, z.unitVector());
					z = x.cross(y);
					ccs.addRequest(new ImageRequest(), true);
					source.setOldValue(source.getValue());

				}
			}
		});

		Box box = Box.createHorizontalBox();

		Box b = Box.createVerticalBox();
		JButton xButton = new JButton("xall");
		xButton.setActionCommand("xall");
		xButton.addActionListener(this);
		b.add(xButton);
		JButton yButton = new JButton("yall");
		yButton.setActionCommand("yall");
		yButton.addActionListener(this);
		b.add(yButton);
		JButton zButton = new JButton("zall");
		zButton.setActionCommand("zall");
		zButton.addActionListener(this);
		b.add(zButton);
		box.add(b);
		box.add(Box.createHorizontalGlue());
		
		b = Box.createVerticalBox();
		JButton duplicate = new JButton("Duplicate");
		duplicate.setActionCommand("duplicate");
		duplicate.addActionListener(this);
		b.add(duplicate);
		
		JButton clear = new JButton("Clear dots");
		clear.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try{
				if(typeOfSelection.equals("box")){
					ccs.addRequest(new ClearBoxes());
				}else{
					ccs.addRequest(new ClearSpheres());
				}
				}catch(Exception ex){}
			}
		});
		JButton saveSubset = new JButton("Save Subset");
		saveSubset.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				System.out.println("Save this configuration");
				String in = JOptionPane.showInputDialog("Enter the name of this view");
				if(in==null){
					//the user has pressed cancel, so do nothing
				}else{
					//store this string in the commandpane
					refComPane.updateGroupList(in);
				}
			}
		});

		b.add(clear);
		b.add(saveSubset);
		box.add(b);
		/*
		b = Box.createVerticalBox();
		lowValueField = new JTextField(String.valueOf(lowValue), 10);
		lowValueField.setActionCommand("lowValue");
		lowValueField.addActionListener(this);
		highValueField = new JTextField(String.valueOf(highValue), 10);
		highValueField.addActionListener(this);
		highValueField.setActionCommand("highValue");
		b.add(lowValueField);
		b.add(highValueField);
		box.add(b);
		*/
		b = Box.createHorizontalBox();
		Box b2 = Box.createVerticalBox();
		b2.add(new JLabel("Down"));
		b2.add(new JLabel("Left"));
		b2.add(new JLabel("Cntr"));
		b2.add(new JLabel("Out"));
		b.add(b2);
		theBox = Box.createVerticalBox();
		theBox.add(updownSlider);
		theBox.add(leftrightSlider);
		theBox.add(clockwiseSlider);
		//b2.add(zIndicator);
		b.add(theBox);
		b2 = Box.createVerticalBox();
		b2.add(new JLabel("Up"));
		b2.add(new JLabel("Right"));
		b2.add(new JLabel("Clock"));
		b2.add(new JLabel("In"));
		b.add(b2);

		box.add(b);

		add(box, BorderLayout.SOUTH);

		ccs.addRequest(new CcsConfigRequest(cmd, xButton, yButton, zButton));

	}
	
	//********************************************************************************************
	/* origin, x, y, z, first set HERE
	 * class CcsConfigRequest
	 */

    private class CcsConfigRequest extends CcsThread.request {
    		private String cmd;
		JButton xButton, yButton, zButton;

		public CcsConfigRequest(String a, JButton xB, JButton yB, JButton zB) {
	    		super("lvConfig", 0);
			cmd = a;
			xButton = xB;
			yButton = yB;
			zButton = zB;
			// for mapping System.out.println("CcsConfigRequest: constructor");
		}

		public void handleReply(byte[] configData){
			// for mapping System.out.println("CcsConfigRequest: handleReply");
	  		try {
	        		config = new Config(new DataInputStream(new ByteArrayInputStream(configData)));
				origin = x = y = z = new Vector3D(0, 0, 0);
				start_z = 0;
				zLoc = 0.0;
				depth = config.max.z - config.min.z;
				if(depth<=1){
					zIndicator = new JSliderOldValue(-50,50,0);
					sliderMult = 100;
				}else{
					zIndicator = new JSliderOldValue((new Double(config.min.z)).intValue(), (new Double(config.max.z)).intValue(), 0);
					sliderMult = 1;
				}
				zIndicator.setEnabled(false);
				theBox.add(zIndicator);

				//System.out.println("Depth: " + depth);
				System.out.println("Config values: color=" + config.isColor + " push=" + config.isPush + " 3d=" + config.is3d);
	    			System.out.println("Box bounds: {(" + config.min.x + " " + config.min.y + " " + config.min.z + "),(" + config.max.x + " " + config.max.y + " " + config.max.z + ")}");
				boxSize = config.max.x - config.min.x;
				if((config.max.y - config.min.y != boxSize) || (config.max.z - config.min.z != boxSize)) {
					System.err.println("Box is not a cube!");
				}

				if(cmd=="xall"){
					xButton.doClick();
				}else if(cmd=="yall"){
					yButton.doClick();
				}else if(cmd=="zall"){
					zButton.doClick();
				}else{}

			} catch(IOException e) {
				System.err.println("Fatal: Couldn't obtain configuration information");
				e.printStackTrace();
			}
		}
    	}
    /********************************************************************************************
     * void mousePressed
     */
	public void mousePressed(MouseEvent e) {
		System.out.println("Mouse was pressed: " + e.paramString());
		start_x = e.getX();
		start_y = e.getY();
		start_z = e.getY();

		if(e.getModifiers()==18){
			try{
				if(typeOfSelection.equals("box")){
					System.out.println("Drawing box");
					drawRect.setLocation(start_x, start_y);
					drawRect.setSize(0, 0);
					display.getGraphics().drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
				}else if(typeOfSelection.equals("sphere")){
					//draw sphere
					System.out.println("Drawing sphere");
				}else{
					/* if drawing hasn't been done before, ask the user what kind of drawing they want */
					Object[] choices = {"box", "sphere"};
					Object selectedValue = JOptionPane.showInputDialog(null,"Chose type of subsetting", "Input",JOptionPane.INFORMATION_MESSAGE, null,choices, choices[0]);
					typeOfSelection = (String)selectedValue;
					//System.out.println("Chose: " + (String)selectedValue);
				}
			}catch(NullPointerException ex){
				/* the user pressed the cancel button in the dialogue for chosing drawing type, so do nothing */
			}
		}


		/*
		switch(e.getModifiers()) {
			case MouseEvent.BUTTON1_MASK:
				System.out.println("button 1 pressed");
				break;
			case MouseEvent.BUTTON2_MASK:
				System.out.println("button 2 pressed");
				break;
			case MouseEvent.BUTTON3_MASK:
				System.out.println("button 3 pressed");
				break;
			case MouseEvent.BUTTON3_MASK | InputEvent.SHIFT_MASK:
				System.out.println("button 3 pressed with shift");
				break;
			case MouseEvent.BUTTON3_MASK | InputEvent.CTRL_MASK:
				System.out.println("button 3 pressed with control");
				break;
		}
		*/
	}
	
	/**************************************************************************************************
	 * void mouseDragged
	 */

	public void mouseDragged(MouseEvent e) {
		if(e.getModifiers() == 16) {
			//mod 16 corresponds to mask1
			drawn = false;
			rotated = true;
			try{sv.disableButton();}catch(Exception le){}
			int oldX = start_x;
			int oldY = start_y;
			start_x = e.getX();
			start_y = e.getY();

			double theta = Math.PI * (e.getX() - oldX) / 180.0;
			//Vector3D xprime = x;
			x.rotate(theta, y.unitVector());

			theta = Math.PI * (oldY - e.getY()) / 180.0;
			y.rotate(theta, x.unitVector());
			z = x.cross(y);

			ccs.addRequest(new ImageRequest(), true);

		}else if(e.getModifiers() == 18){
			//mod 18 corresponds to ctrl+mask1
			Graphics g = display.getGraphics();
			g.setColor(Color.white);
			if(typeOfSelection.equals("box")){
				//draw rubber band box
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
				g.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
			}else{
				//draw a line
				repaint();
				g.drawLine(start_x, start_y, e.getX(), e.getY());
			}
		}

	}

	/****************************************************************************************
	 * void mouseReleased
	 */

	public void mouseReleased(MouseEvent e) {
		//System.out.println("Mouse was released: " + e.paramString());
		switch(e.getModifiers()) {
			case MouseEvent.BUTTON1_MASK:
				//do nothing
				break;
			case MouseEvent.BUTTON2_MASK:
				//pan
				origin = origin.plus((x.scalarMultiply(((double) start_x - e.getX()) / width)).plus(y.scalarMultiply(((double) start_y - e.getY()) / height)).scalarMultiply(2.0));
				ccs.addRequest(new ImageRequest(), true);
				break;
			case MouseEvent.BUTTON3_MASK:
				//zoom
				int delta_y = e.getY() - start_y;
				double zoom;
				if(delta_y > 0) //zooming in
					zoom = 1.0 / (1.0 + (double) delta_y / (height - start_y));
				else //zooming out
				zoom = 1.0 - (double) delta_y / start_y;
				x = x.scalarMultiply(zoom);
				y = y.scalarMultiply(zoom);
				ccs.addRequest(new ImageRequest(), true);
				break;
			case MouseEvent.BUTTON3_MASK | InputEvent.SHIFT_MASK:
				// translate origin in z direction until config.min.z or config.max.z
				z = x.cross(y);
				double zShift = ((2*((double)start_z - e.getY()))/height)*y.length();
				Vector3D translationVector = z.unitVector().scalarMultiply(zShift);
				System.out.println("translation length: " + translationVector.length());

				if ((e.getY() - start_z) < 0) {
					//the translation request is INTO the screen, so translate by adding
					zLoc = zLoc + translationVector.length();
					//if (zLoc > config.max.z) {
					//	zLoc = config.max.z;
					//} else {
						origin = origin.plus(translationVector);
						zIndicator.setValue((new Double(zLoc*sliderMult)).intValue());
					//}
				} else {
					//the translation request is OUT Of the screen, so translate by subtracting
					zLoc = zLoc - translationVector.length();
					//if (zLoc < config.min.z) {
					//	zLoc = config.min.z;
					//} else {
						origin = origin.plus(translationVector);
						zIndicator.setValue((new Double(zLoc*sliderMult)).intValue());
					//}
				}
				break;
			case MouseEvent.BUTTON1_MASK | InputEvent.CTRL_MASK:
				//System.out.println("Drawing");

				if(typeOfSelection.equals("sphere")){
					//draw line
					Graphics g = display.getGraphics();
					g.setColor(Color.white);
					g.drawLine(start_x, start_y, e.getX(), e.getY());
					selectionBox(e.getX(), e.getY());
					drawn = true;
					selectWindow();
				}else{
					//draw box
					Graphics g = display.getGraphics();
					g.setColor(Color.white);
					g.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
					selectionBox(e.getX(), e.getY());
					drawn = true;
					selectWindow();
				}
				break;
			//case MouseEvent.BUTTON1_MASK | InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK:

		}
	}
	
	/**************************************************************************************************
	 * called from mouseReleased to open a SelectionView
	 */

	private void selectWindow(){
		/*
		JFrame f = new JFrame("Selection view");
		sv = new SelectionView(h,p,x,y,z,origin,this);
		f.getContentPane().add(sv);
		f.pack();
		f.setVisible(true);
		f.setSize(parentFrame.getWidth(), parentFrame.getHeight());
		rotated = false;	//set to false, to allow drawing on the new window
		*/
	}

	/**************************************************************************************************
	 * accessor method to let SelectionView objects know what kind of drawing selection is being used
	 */

	public String getTypeOfSelection(){
		return typeOfSelection;
	}

	/**************************************************************************************************
	 * void mouseMoved ADDED WITH NEW LISTENER
	 */

	public void mouseMoved(MouseEvent e) {

	}

	/**************************************************************************************************
	 * void mouseExited
	 */

	public void mouseExited(MouseEvent e) {
	}
	
	/**************************************************************************************************
	 * void mouseClicked
	 */

	public void mouseClicked(MouseEvent e) {
	}

	/**************************************************************************************************
	 * void mouseEntered
	 */

	public void mouseEntered(MouseEvent e) {
	}

	/**************************************************************************************************/
	/*
	 * selectionBox does the calculations of the scalar multiplier of each of the vertices of the drawn box
	 * or line, depending on the users drawing preference
	 */
	public void selectionBox(int currentX, int currentY){
		//g.setColor(Color.white);
		if(typeOfSelection.equals("box")){
			/* calculate vertices of box */
			NUM_POINTS = 8;
			xSimNumbers = new double[NUM_POINTS];
			ySimNumbers = new double[NUM_POINTS];
			zSimNumbers = new double[NUM_POINTS];
			posVectors = new Vector3D[NUM_POINTS];

			double w = currentX - start_x; //the width of the drawn box
			double h = currentY - start_y; //the height of the drawn box

			if(w>0){
				xBoxConstraintOne = start_x;
				xBoxConstraintTwo = currentX;
				if(h>0){
					//box is drawn from upper left to bottom right
					//g.drawRect(start_x, start_y, currentX-start_x, currentY-start_y);

					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(start_x);
					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(currentX);
					ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(start_y);
					ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(currentY);

				}else{
					//box is drawn from lower left to upper right
					//g.drawRect(start_x, currentY, currentX-start_x, start_y-currentY);

					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(start_x);
					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(currentX);
					ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(start_y);
					ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(currentY);
				}
			}else{
				xBoxConstraintOne = currentX;
				xBoxConstraintTwo = start_x;
				if(h>0){
					//box is drawn from top right to bottom left
					//g.drawRect(currentX, start_y, start_x-currentX, currentY-start_y);

					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(start_x);
					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(currentX);
					ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(start_y);
					ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(currentY);

				}else{
					//box is drawn from bottom right to top left
					//g.drawRect(currentX, currentY, start_x-currentX, start_y-currentY);

					xSimNumbers[1] = xSimNumbers[2] = xSimNumbers[5] = xSimNumbers[6] = xSimMultiply(start_x);
					xSimNumbers[0] = xSimNumbers[3] = xSimNumbers[4] = xSimNumbers[7] = xSimMultiply(currentX);
					ySimNumbers[2] = ySimNumbers[3] = ySimNumbers[6] = ySimNumbers[7] = ySimMultiply(start_y);
					ySimNumbers[0] = ySimNumbers[1] = ySimNumbers[4] = ySimNumbers[5] = ySimMultiply(currentY);
				}
			}
		}else{
			/* calculate vertices of line */
			NUM_POINTS = 2;
			xSimNumbers = new double[NUM_POINTS];
			ySimNumbers = new double[NUM_POINTS];
			zSimNumbers = new double[NUM_POINTS];
			posVectors = new Vector3D[NUM_POINTS];

			xBoxConstraintOne = start_x;
			xBoxConstraintTwo = currentX;

			xSimNumbers[0] = xSimMultiply(start_x);
			ySimNumbers[0] = ySimMultiply(start_y);
			xSimNumbers[1] = xSimMultiply(currentX);
			ySimNumbers[1] = ySimMultiply(currentY);
			
			double xDist = xSimNumbers[1]-xSimNumbers[0];
			double yDist = ySimNumbers[1]-ySimNumbers[0];
			xDist = xDist*xDist;
			yDist = yDist*yDist;
			double radius = Math.sqrt(xDist+yDist);
			zSimNumbers[1]=radius;
		}
	}

	/**************************************************************************************************/
	
	public boolean hasDrawn(){
		return drawn;
	}
	
	/**************************************************************************************************/
	
	public boolean hasRotated(){
		return rotated;
	}

	/**************************************************************************************************/

	public int getBoxConstraintOne(){
		return xBoxConstraintOne;
	}

	/**************************************************************************************************/

	public int getBoxConstraintTwo(){
		return xBoxConstraintTwo;
	}
	
	/**************************************************************************************************/

	public double xSimMultiply(double mult){
		return ((2*(mult/width))-1)*x.length();
	}

	/**************************************************************************************************/
	
	public double ySimMultiply(double mult){
		return ((2*(mult/height))-1)*y.length();
	}
	
	/**************************************************************************************************/
	
	public double zSimMultiply(double mult){
		return (1-(2*(mult/sv.height)))*y.length();
	}
	
	/**************************************************************************************************/

	public void setZSimPoints(int one, int two, int three, int four, double value){
		if(typeOfSelection.equals("sphere")){
			//setting up for a sphere
			zSimNumbers[one] = value;

		}else{
			//setting up for a box
			zSimNumbers[one] = zSimNumbers[two] = zSimNumbers[three] = zSimNumbers[four] = value;
		}
	}

	/**************************************************************************************************/

	public Vector3D getZ(){
		return z;
	}
	
	/**************************************************************************************************
	 * called from SelectionView after a box has been drawn to specify the depth of the box image to be
	 * requested
	 */

	public void setPosVectors(){
		//System.out.println("Begin posVector process");
		try{
			Vector3D xHat = x.unitVector();
			Vector3D yHat = y.unitVector();
			Vector3D zHat = z.unitVector();

			for(int x = 0; x < NUM_POINTS; x++){
				posVectors[x] = origin.plus(xHat.scalarMultiply(xSimNumbers[x])).plus(yHat.scalarMultiply(ySimNumbers[x])).plus(zHat.scalarMultiply(zSimNumbers[x]));
				System.out.println("Vector " + x + " is at " + posVectors[x].toString());
			}
			encoded = encodeVectors();

		}catch(Exception e){
			System.out.println("error in setPosVectors: " + e.toString());
			System.out.println("stack trace: " );
			e.printStackTrace();
		}

	}

	/**************************************************************************************************
	 * called from SelectionView when the Request Image button is pushed
	 */

	public void requestBoxImage(String arg){
		if(arg.equals("box")){
			System.out.println("Box image requested");
			ccs.addRequest(new SpecifyBox(encoded));
		}else{
			System.out.println("Sphere image requested");
			ccs.addRequest(new SpecifySphere(encoded));
		}
	}
	
	/**************************************************************************************************
	 * void stateChanged
	 */
	
	public void stateChanged(ChangeEvent e) {
		//for mapping System.out.println("ViewingPanel: stateChanged");
		JSlider source = (JSlider) e.getSource();
        	if(!source.getValueIsAdjusting()) {
			System.out.println("Slider event: " + source.getValue());
		}
	}
	
	/****************************************************************************************
	/*
	 * ACTION PERFORMED
	 */

	public void actionPerformed(ActionEvent e) {
		// for mapping System.out.println("ViewingPanel: actionPerformed");
		System.out.println("Got action performed. Command: " + e.getActionCommand());

		if(e.getActionCommand().equals("xall")) {
			rotated = true;
			//width = 256;
			//height = 256;
			//lowRes = true;

			origin = /*new Vector3D(0,0,0);*/ config.max.plus(config.min).scalarMultiply(0.5);
			//x = new Vector3D(0, config.max.x - config.min.x, 0);
			//y = new Vector3D(0, 0, config.max.y - config.min.y);
			x = new Vector3D(0, boxSize, 0);
			y = new Vector3D(0, 0, boxSize);
			z = new Vector3D(x.cross(y));

			double xRatio = x.length() / width;
			double yRatio = y.length() / height;
			if(xRatio != yRatio) {
				double factor = yRatio / xRatio;
				x = x.scalarMultiply(factor);
			}

			updownSlider.setOldValue(180);
			updownSlider.setValue(180);
			leftrightSlider.setOldValue(180);
			leftrightSlider.setValue(180);
			clockwiseSlider.setOldValue(180);
			clockwiseSlider.setValue(180);
			zIndicator.setOldValue(0);
			zIndicator.setValue(0);
			zLoc = 0;

			ccs.addRequest(new ImageRequest(), true);

		} else if(e.getActionCommand().equals("yall")) {
			rotated = true;
			//width = 500;
			//height = 500;
			origin = /*new Vector3D(0,0,0);*/ config.max.plus(config.min).scalarMultiply(0.5);
			//x = new Vector3D(-(config.max.x - config.min.x), 0, 0);
			//y = new Vector3D(0, 0, config.max.z - config.min.z);
			x = new Vector3D(boxSize, 0, 0);
			y = new Vector3D(0, 0, boxSize);
			z = new Vector3D(x.cross(y));

			double xRatio = x.length() / width;
			double yRatio = y.length() / height;
			if(xRatio != yRatio) {
				double factor = yRatio / xRatio;
				x = x.scalarMultiply(factor);
			}

			updownSlider.setOldValue(180);
			updownSlider.setValue(180);
			leftrightSlider.setOldValue(180);
			leftrightSlider.setValue(180);
			clockwiseSlider.setOldValue(180);
			clockwiseSlider.setValue(180);
			zIndicator.setOldValue(0);
			zIndicator.setValue(0);
			zLoc = 0;

			ccs.addRequest(new ImageRequest(), true);

		} else if(e.getActionCommand().equals("zall")) {
			rotated = true;
			lowRes = false;
			origin = /*new Vector3D(0,0,0);*/ config.max.plus(config.min).scalarMultiply(0.5);
			x = new Vector3D(boxSize,0,0);
			y = new Vector3D(0,boxSize,0);
			z = new Vector3D(x.cross(y));

			double xRatio = x.length() / width;
			double yRatio = y.length() / height;
			if(xRatio != yRatio) {
				double factor = yRatio / xRatio;
				x = x.scalarMultiply(factor);
			}

			updownSlider.setOldValue(180);
			updownSlider.setValue(180);
			leftrightSlider.setOldValue(180);
			leftrightSlider.setValue(180);
			clockwiseSlider.setOldValue(180);
			clockwiseSlider.setValue(180);
			zIndicator.setOldValue(0);
			zIndicator.setValue(0);
			zLoc = 0;

			ccs.addRequest(new ImageRequest(), true);

		} else if(e.getActionCommand().equals("lowValue")) {
			try {
				lowValue = Double.parseDouble(lowValueField.getText());
			} catch(NumberFormatException ex) {
				System.err.println("Bad low value!");
			}
		} else if(e.getActionCommand().equals("highValue")) {
			try {
				highValue = Double.parseDouble(highValueField.getText());
			} catch(NumberFormatException ex) {
				System.err.println("Bad high value!");
			}
		} else if(e.getActionCommand().equals("duplicate")) {
			System.out.println("opening duplicate view");
			String in = JOptionPane.showInputDialog("Enter the name of Duplicate view");
			if(in==null){
				//the user has pressed cancel, so do nothing
			}else{
				JFrame f = new JFrame("Duplicate: " + in);
				f.getContentPane().add(new DuplicateView(h, p, x, y, z, origin));
				f.pack();
				f.setSize(450,500);
				f.setVisible(true);
			}
		} else {
			System.out.println("Other action event!");
		}
	}

	/**************************************************************************************************
	 * void componentHidden
	 */

	public void componentHidden(ComponentEvent e) {
	}

	/**************************************************************************************************
	 * void componentMoved
	 */

	public void componentMoved(ComponentEvent e) {
	}

	/**************************************************************************************************
	 * void componentShown
	 */

	public void componentShown(ComponentEvent e) {
	}
	
	/**************************************************************************************************
	 * void componentResized
	 */

	public void componentResized(ComponentEvent e) {

		//System.out.println("ViewingPanel: componentResized");

		width = e.getComponent().getWidth();
		height = e.getComponent().getHeight();
		resizedImage = true;
		double xRatio = x.length() / width;
		double yRatio = y.length() / height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			x = x.scalarMultiply(factor);
		}
		cmdisplay.reset(width, 10);
		//cmdisplay = new ColorMapDisplay(width, 10);
		cmdisplay.redisplay(wrbb);
		//add(cmdisplay, BorderLayout.NORTH);
		System.out.println("New width: " + width);
		System.out.println("New height: " + height);
		ccs.addRequest(new ImageRequest(), true);
	}
	
	/**************************************************************************************************
	 * void displayImage
	 */
	
	private void displayImage(byte[] data) {
		try {
		// for mapping System.out.println("ViewingPanel: displayImage");
		if(resizedImage || data.length != width * height) {
			source = new MemoryImageSource(width, height, wrbb, data, 0, width);
			source.setAnimated(true);
			display.setIcon(new ImageIcon(display.createImage(source)));
			if(data.length == width * height)
			    resizedImage = false;
			pixels = data;
		} else if(lowRes) {
			//fiddle with pixels to resize the image
			int x2, y2 = 0, extraPixels, extraRows = 0;
			int q1 = width / lowResWidth;
			int r1 = width % lowResWidth;
			int q2 = height / lowResHeight;
			int r2 = height % lowResHeight;
			int d1 = lowResWidth / r1;
			int d2 = lowResHeight / r2;
			byte[] extraRow = new byte[width];
			for(int y = 0; y < lowResHeight; ++y) {
				x2 = 0;
				extraPixels = 0;
				//stretch this row
				for(int x = 0; x < lowResWidth; ++x) {
					for(int i = 0; i < q1; ++i)
						extraRow[x2++] = data[x + y * lowResWidth];
					if(extraPixels < r1 && x % d1 == 0) {
						extraRow[x2++] = data[x + y * lowResWidth];
						extraPixels++;
					}
				}
				//duplicate rows
				for(int i = 0; i < q2; ++i) {
					for(int j = 0; j < width; ++j)
						pixels[j + y2 * width] = extraRow[j];
					y2++;
				}
				if(extraRows < r2 && y % d2 == 0) {
					//make extra copy
					for(int j = 0; j < width; ++j)
						pixels[j + y2 * width] = extraRow[j];
					extraRows++;
					y2++;
				}
			}
			source.newPixels();
		} else {
			source.newPixels(data, wrbb, 0, width);
			pixels = data;
		}
		} catch (ArrayIndexOutOfBoundsException bla) {
			bla.printStackTrace();
		}
	}

	/**************************************************************************************************/

	private byte[] encodeVectors() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
		try {
			DataOutputStream dos = new DataOutputStream(baos);
			if(typeOfSelection.equals("box")){
				for(int x = 0; x<NUM_POINTS; x++){
					dos.writeDouble(posVectors[x].x);
					dos.writeDouble(posVectors[x].y);
					dos.writeDouble(posVectors[x].z);
				}
			}else{
				System.out.println("ENcoding sphere");
				System.out.println("Radius: " + zSimNumbers[1]);
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

	/**************************************************************************************************
	 * byte[] encodeRequest
	 */
	private byte[] encodeRequest() {
		// for mapping System.out.println("ViewingPanel: encodeRequest");
		ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
		try {
			DataOutputStream dos = new DataOutputStream(baos);

			dos.writeInt(1); /*Client version*/
			dos.writeInt(1); /*Request type*/
			if(lowRes) {
				dos.writeInt(lowResWidth);
				dos.writeInt(lowResHeight);
			} else {
				dos.writeInt(width);
				dos.writeInt(height);
			}
			dos.writeDouble(x.x);
			dos.writeDouble(x.y);
			dos.writeDouble(x.z);
			dos.writeDouble(y.x);
			dos.writeDouble(y.y);
			dos.writeDouble(y.z);
			dos.writeDouble(z.x);
			dos.writeDouble(z.y);
			dos.writeDouble(z.z);
			dos.writeDouble(origin.x);
			dos.writeDouble(origin.y);
			dos.writeDouble(origin.z);
			
			dos.writeDouble(lowValue);
			dos.writeDouble(highValue);

			System.out.println("w: " + width + " h: " + height);
			System.out.println("x: " + x.toString());
			System.out.println("y: " + y.toString());
			System.out.println("z: " + (x.cross(y)).toString());
			System.out.println("o: " + origin.toString());

		} catch(IOException e) {
			System.err.println("Couldn't encode request!");
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	
	/**************************************************************************************************/

	private class SpecifyBox extends CcsThread.request {

		public SpecifyBox(byte[] data){
			super("SpecifyBox", data);
		}

		public void handleReply(byte[] data){
			//do something
		}
	}
	
	private class SpecifySphere extends CcsThread.request {
	
		public SpecifySphere(byte[] data){
			super("SpecifySphere", data);
		}
		
		public void handleReply(byte[] data){}
	}

	/**************************************************************************************************
	 * class ImageRequest
	 */

	private class ImageRequest extends CcsThread.request {

		public ImageRequest() {
			super("lvImage", null);
			// for mapping System.out.println("ImageRequest: constructor");
			setData(encodeRequest());

		}

		public void handleReply(byte[] data) {
			// for mapping System.out.println("ImageRequest: handleReply");
			displayImage(data);

			/* used in conjunction with CcsThread.startPoint for efficiency testing
			Date end = new Date();
			System.out.println("Image Gen took: " + ((double) (end.getTime() - CcsThread.startPoint.getTime())/1000) + " seconds");*/

		}
	}
	
	//*************************************************************************************************

	private class ClearBoxes extends CcsThread.request {
	
		public ClearBoxes(){
			super("ClearBoxes", null);
		}
		
		public void handleReply(byte[] data){}
	}
	
	private class ClearSpheres extends CcsThread.request {
		
		public ClearSpheres(){
			super("ClearSpheres", null);
		}
		public void handleReply(byte[] data){}
	}

	//*************************************************************************************************
	/* WRBB color map.
	This color map does linear interpolation between the set key colors
	black - blue - magenta - red - yellow - white.
	*/

	private static ColorModel createWRBBColorModel() {
		int cmap_size=254;
		byte[] wrbb_red = new byte[256];
		byte[] wrbb_green = new byte[256];
		byte[] wrbb_blue = new byte[256];
    		int i;
		int nextColor = 0;
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

		while(nextColor < cmap_size) {
			wrbb_red[nextColor] = (byte) 255;
			wrbb_green[nextColor] = (byte) 255;
			wrbb_blue[nextColor++] = (byte) 255;
		}

		wrbb_red[254] = 0;
		wrbb_green[254] = 0;
		wrbb_blue[254] = 0;
		wrbb_red[255] = 0;
		wrbb_green[255] = (byte)255;
		wrbb_blue[255] = 0;

		return new IndexColorModel(8, 256, wrbb_red, wrbb_green, wrbb_blue);
	}

	/*************************************************************************************
	 * dispaly vector lengths
	 */
	private void showLengths() {
		System.out.println("xLength: " + x.length());
		System.out.println("yLength: " + y.length());
		System.out.println("zLength: " + z.length());
	}
	
	/*************************************************************************************
	 * dispaly ratios of x.length / width and y.length / height
	 */
	private void showRatios() {
		System.out.println("x/width ratio: " + x.length() / width);
		System.out.println("y/height ratio: " + y.length() / height);
	}
	
	public Vector3D getXVector(){return x;}
	public Vector3D getYVector(){return y;}
	public Vector3D getZVector(){return z;}
	public Vector3D getOriginVector(){return origin;}

}//end ViewingPanel


