
//SelectionView

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class SelectionView extends JPanel implements MouseListener, MouseMotionListener, ActionListener, ComponentListener {

	private CcsThread ccs;
	private static ColorModel wrbb;
	private JPanel displayPanel;
	private JLabel display;
	private int width, lowResWidth, lowResHeight;
	public int height;
	private int start_x, start_y;
	private Vector3D x, y, z, origin;
	private double lowValue, highValue;
	private Config config;
	private ColorMapDisplay cmdisplay;
	private double boxSize;
	private MemoryImageSource source;
	private byte[] pixels;
	private boolean resizedImage;
	private boolean lowRes;
	private String h;
	private int p;
	private ViewingPanel refViewPanel;
	private boolean drawn;
	private JButton boxButton, sphereButton;
	private Rectangle drawRect = new Rectangle();
	private Rectangle finalRect = new Rectangle();
	private String typeOfSelection;


	/****************************************************************************
	 * CONSTRUCTOR
	 */
	public SelectionView(String hostname, int port, Vector3D newX, Vector3D newY, Vector3D newZ, Vector3D newO, ViewingPanel ref) {
		super(new BorderLayout());
		ccs = new CcsThread(new Label(), hostname, port);
		h = hostname;
		p = port;
		refViewPanel = ref;
		typeOfSelection = refViewPanel.getTypeOfSelection();

		wrbb = createWRBBColorModel(256);
		
		width = 512;
		height = 512;
		
		lowValue = 0;
		highValue = 1;
		drawn = false;

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

		JPanel b = new JPanel();
		boxButton = new JButton("Request Box Image");
		boxButton.setActionCommand("box");
		boxButton.addActionListener(this);
		boxButton.setEnabled(false);
		sphereButton = new JButton("Request sphere Image");
		sphereButton.setActionCommand("sphere");
		sphereButton.addActionListener(this);
		sphereButton.setEnabled(false);
		JButton clearScreen = new JButton("Clear Screen");
		clearScreen.setActionCommand("clear");
		clearScreen.addActionListener(this);
		b.add(boxButton);
		b.add(sphereButton);
		b.add(clearScreen);
		
		add(b, BorderLayout.SOUTH);

		ccs.addRequest(new CcsConfigRequest(newX, newY, newZ, newO));

	}


    /********************************************************************************************
     * void mousePressed
     */
	public void mousePressed(MouseEvent e) {
		//System.out.println("Mouse was pressed: " + e.paramString());
		start_x = e.getX();
		start_y = e.getY();

		if(refViewPanel.hasDrawn() && e.getModifiers()==18){
			//modifier 18 corresponds to ctrl+mask 1
			if(typeOfSelection.equals("box")){
				drawRect.setLocation(refViewPanel.getBoxConstraintOne(), start_y);
				drawRect.setSize(refViewPanel.getBoxConstraintTwo()-refViewPanel.getBoxConstraintOne(), 0);
				display.getGraphics().drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
				finalRect.setBounds(drawRect);
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
		Graphics g = display.getGraphics();
		g.setColor(Color.white);
		repaint();
		if(typeOfSelection.equals("box")){
			//draw box
			if((e.getY()-drawRect.y)>0){
				g.drawRect(drawRect.x, drawRect.y, drawRect.width, e.getY()-drawRect.y);
				finalRect.setBounds(drawRect.x, drawRect.y, drawRect.width, e.getY()-drawRect.y);
			}else{
				g.drawRect(drawRect.x, e.getY(), drawRect.width, drawRect.y-e.getY());
				finalRect.setBounds(drawRect.x, e.getY(), drawRect.width, drawRect.y-e.getY());
			}
		}
	}

	/****************************************************************************************
	 * void mouseReleased
	 */

	public void mouseReleased(MouseEvent e) {
		//System.out.println("Mouse was released: " + e.paramString());
		switch(e.getModifiers()) {
			case MouseEvent.BUTTON2_MASK:
				//pan...disabled for now
				//origin = origin.plus((x.scalarMultiply(((double) start_x - e.getX()) / width)).plus(y.scalarMultiply(((double) start_y - e.getY()) / height)).scalarMultiply(2.0));
				//ccs.addRequest(new ImageRequest(), true);
				break;
			case MouseEvent.BUTTON3_MASK:
				//zoom...disabled for now
				//int delta_y = e.getY() - start_y;
				//double zoom;
				//if(delta_y > 0) //zooming in
				//	zoom = 1.0 / (1.0 + (double) delta_y / (height - start_y));
				//else //zooming out
				//zoom = 1.0 - (double) delta_y / start_y;
				//x = x.scalarMultiply(zoom);
				//y = y.scalarMultiply(zoom);
				//ccs.addRequest(new ImageRequest(), true);
				break;
			case MouseEvent.BUTTON1_MASK | InputEvent.CTRL_MASK:
				Graphics g = display.getGraphics();
				g.setColor(Color.white);
				if(typeOfSelection.equals("box")){
					//draw the box selected
					g.drawRect(finalRect.x, finalRect.y, finalRect.width, finalRect.height);
					selectionBox(e.getX(), e.getY());
				}else{
					//draw a blob to indicate dot
					g.fillOval(refViewPanel.getBoxConstraintOne()-5, e.getY()-5, 10,10);
					selectionBox(e.getX(), e.getY());
				}
				break;
		}
	}

	/**************************************************************************************************/

	public void selectionBox(int currentX, int currentY){
		if(refViewPanel.hasDrawn() && !refViewPanel.hasRotated()){
			if(typeOfSelection.equals("box")){
				//user has drawn a box on the main window, and has not rotated the view, so allow calculations to continue
				int check = currentY-start_y;
				if(check>0){
					refViewPanel.setZSimPoints(4,5,6,7,refViewPanel.zSimMultiply(start_y));
					refViewPanel.setZSimPoints(0,1,2,3,refViewPanel.zSimMultiply(currentY));
				}else{
					refViewPanel.setZSimPoints(0,1,2,3,refViewPanel.zSimMultiply(start_y));
					refViewPanel.setZSimPoints(4,5,6,7,refViewPanel.zSimMultiply(currentY));
				}

				boxButton.setEnabled(true);	//enable the button to request an image

				refViewPanel.setPosVectors();	//call to ViewingPanel to set up and encode the data for the zoom box
			}else{
				//user has plotted a point
				refViewPanel.setZSimPoints(0,0,0,0,refViewPanel.zSimMultiply(currentY));//only gonna use the first index
				sphereButton.setEnabled(true);
				refViewPanel.setPosVectors();
			}
		}else{
			/*no box has been drawn on the main window, or they've rotated the view
			 *since this image was created...let 'em know about it*/
			JOptionPane.showMessageDialog(this, "Make sure you've drawn a box on, and have not rotated, the main image");
		}
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


	/****************************************************************************************
	/*
	 * ACTION PERFORMED
	 */

	public void actionPerformed(ActionEvent e) {
		// for mapping System.out.println("ViewingPanel: actionPerformed");
		System.out.println("Got action performed. Command: " + e.getActionCommand());
		if(e.getActionCommand().equals("box")){
			refViewPanel.requestBoxImage("box");
		}else if(e.getActionCommand().equals("sphere")){
			refViewPanel.requestBoxImage("sphere");
		}else if(e.getActionCommand().equals("clear")){
			System.out.println("Clearing screen");
			repaint();
		}else{
			System.out.println("Other action event!");
		}
	}

	/**************************************************************************************************/

	public void disableButton(){
		boxButton.setEnabled(false);
		sphereButton.setEnabled(false);
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

	//********************************************************************************************
	/* origin, x, y, z, first set HERE
	 * class CcsConfigRequest
	 */

	private class CcsConfigRequest extends CcsThread.request {
		private Vector3D xVex, yVex, zVex, oVex;

		public CcsConfigRequest(Vector3D xV, Vector3D yV, Vector3D zV, Vector3D oV) {
			super("lvConfig", 0);
			xVex = xV;
			yVex = yV;
			zVex = zV;
			oVex = oV;
			// for mapping System.out.println("CcsConfigRequest: constructor");
		}

		public void handleReply(byte[] configData){
			// for mapping System.out.println("CcsConfigRequest: handleReply");
			try {
	        		config = new Config(new DataInputStream(new ByteArrayInputStream(configData)));
				origin = x = y = z = new Vector3D(0, 0, 0);

				//System.out.println("Depth: " + depth);
				System.out.println("Config values: color=" + config.isColor + " push=" + config.isPush + " 3d=" + config.is3d);
				System.out.println("Box bounds: {(" + config.min.x + " " + config.min.y + " " + config.min.z + "),(" + config.max.x + " " + config.max.y + " " + config.max.z + ")}");
				boxSize = config.max.x - config.min.x;
				if((config.max.y - config.min.y != boxSize) || (config.max.z - config.min.z != boxSize)) {
					System.err.println("Box is not a cube!");
				}

				x = new Vector3D(xVex);
				y = new Vector3D(yVex);
				z = new Vector3D(zVex);
				origin = new Vector3D(oVex);
				y.rotate(-Math.PI/2, x.unitVector());
				ccs.addRequest(new ImageRequest(), true);

			} catch(IOException e) {
				System.err.println("Fatal: Couldn't obtain configuration information");
				e.printStackTrace();
			}
		}
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

		}
	}

	//*************************************************************************************************
	/* WRBB color map.
	This color map does linear interpolation between the set key colors
	black - blue - magenta - red - yellow - white.
	*/

	private static ColorModel createWRBBColorModel(int num_colors) {
		int cmap_size = num_colors;
		byte[] wrbb_red = new byte[cmap_size];
		byte[] wrbb_green = new byte[cmap_size];
		byte[] wrbb_blue = new byte[cmap_size];
    		int i;
		int nextColor = 0;
    		int chunk_size = (cmap_size - 1) / 5;

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

		return new IndexColorModel(8, cmap_size, wrbb_red, wrbb_green, wrbb_blue);
	}
	



}//end SelectionView




