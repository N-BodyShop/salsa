//ViewingPanel.java

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
	private double zLoc;

	/****************************************************************************
	 * CONSTRUCTOR
	 */
	public ViewingPanel(String hostname, int port) {
		super(new BorderLayout());
		/*Date begin = new Date();
		System.out.println("Begin constructor: " + begin.getTime());*/
		// for mapping System.out.println("ViewingPanel: Constructor");
		ccs = new CcsThread(new Label(), hostname, port);

		wrbb = createWRBBColorModel(256);
		
		start_z = 0;
		width = 512;
		height = 512;
		
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
					double theta = Math.PI * (source.getValue() - source.getOldValue()) / 180.0;
					x.rotate(theta, z.unitVector());
					y.rotate(theta, z.unitVector());
					z = x.cross(y);
					ccs.addRequest(new ImageRequest(), true);
					source.setOldValue(source.getValue());

				}
			}
		});

		zIndicator = new JSliderOldValue(-50, 50, 0);
		zIndicator.setEnabled(false);

		Box box = Box.createHorizontalBox();

		Box b = Box.createVerticalBox();
		JButton button = new JButton("xall");
		button.setActionCommand("xall");
		button.addActionListener(this);
		b.add(button);
		button = new JButton("yall");
		button.setActionCommand("yall");
		button.addActionListener(this);
		b.add(button);
		button = new JButton("zall");
		button.setActionCommand("zall");
		button.addActionListener(this);
		b.add(button);
		box.add(b);
		box.add(Box.createHorizontalGlue());
		
		/*b = Box.createVerticalBox();
		lowValueField = new JTextField(String.valueOf(lowValue), 10);
		lowValueField.setActionCommand("lowValue");
		lowValueField.addActionListener(this);
		highValueField = new JTextField(String.valueOf(highValue), 10);
		highValueField.addActionListener(this);
		highValueField.setActionCommand("highValue");
		b.add(lowValueField);
		b.add(highValueField);
		box.add(b);*/
		
		b = Box.createHorizontalBox();
		Box b2 = Box.createVerticalBox();
		b2.add(new JLabel("Down"));
		b2.add(new JLabel("Left"));
		b2.add(new JLabel("Cntr"));
		b2.add(new JLabel("Out"));
		b.add(b2);
		b2 = Box.createVerticalBox();
		b2.add(updownSlider);
		b2.add(leftrightSlider);
		b2.add(clockwiseSlider);
		b2.add(zIndicator);
		b.add(b2);
		b2 = Box.createVerticalBox();
		b2.add(new JLabel("Up"));
		b2.add(new JLabel("Right"));
		b2.add(new JLabel("Clock"));
		b2.add(new JLabel("In"));
		b.add(b2);
		/* trying to implement a button to expand the control pane
		 * the problem is that the close operation of the CommandPane closes the visualization program as well
		b2 = Box.createVerticalBox();
		JButton but = new JButton("more>>");
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Button pushed");
				CommandPane pane = new CommandPane(400,400);
			}
		});
		b2.add(but);
		b.add(b2);
		*/
		box.add(b);
		
		add(box, BorderLayout.SOUTH);
		
		ccs.addRequest(new CcsConfigRequest());
		/*Date end = new Date();
		System.out.println("end constructor: " + end.getTime());
		System.out.println("Total constructor time: " + (end.getTime() - begin.getTime())/1000);*/
	}
	
	//********************************************************************************************
	/* origin, x, y, z, first set HERE
	 * class CcsConfigRequest
	 */

    private class CcsConfigRequest extends CcsThread.request {

		public CcsConfigRequest() {
	    		super("lvConfig", 0);
			// for mapping System.out.println("CcsConfigRequest: constructor");
		}
		
		public void handleReply(byte[] configData){
			// for mapping System.out.println("CcsConfigRequest: handleReply");
	  		try {
	        		config = new Config(new DataInputStream(new ByteArrayInputStream(configData)));
				origin = x = y = z = new Vector3D(0, 0, 0);
				start_z = 0;
				zLoc = 0.0;
				//zIndicator = new JSliderOldValue((new Double(config.min.z)).intValue(), (new Double(config.max.z)).intValue(), 0);
				System.out.println("Sliders max: " + zIndicator.getMaximum());
				System.out.println("Slider's min: " + zIndicator.getMinimum());
				System.out.println("Config values: color=" + config.isColor + " push=" + config.isPush + " 3d=" + config.is3d);
	    			System.out.println("Box bounds: {(" + config.min.x + " " + config.min.y + " " + config.min.z + "),(" + config.max.x + " " + config.max.y + " " + config.max.z + ")}");
				boxSize = config.max.x - config.min.x;
				System.out.println("Box depth is: " + (config.max.z-config.min.z));
				if((config.max.y - config.min.y != boxSize) || (config.max.z - config.min.z != boxSize)) {
					System.err.println("Box is not a cube!");
				}
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
		//System.out.println("Mouse was pressed: " + e.paramString());
		start_x = e.getX();
		start_y = e.getY();
		start_z = e.getY();

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
				Vector3D translationVector = (z.scalarMultiply(((double) start_z - e.getY()) / 522.0));

				if ((e.getY() - start_z) < 0) {
					//the translation request is INTO the screen, so translate by adding
					zLoc = zLoc + translationVector.length();
					if (zLoc > config.max.z) {
						zLoc = config.max.z;
					} else {
						origin = origin.plus(translationVector);
						zIndicator.setValue((new Double(zLoc*100)).intValue());
						System.out.println("Slider's value: " + zIndicator.getValue());
					}
				} else {
					//the translation request is OUT Of the screen, so translate by subtracting
					zLoc = zLoc - translationVector.length();
					if (zLoc < config.min.z) {
						zLoc = config.min.z;
					} else {
						origin = origin.plus(translationVector);
						zIndicator.setValue((new Double(zLoc*100)).intValue());
						System.out.println("Slider's value: " + zIndicator.getValue());
					}
				}
				break;
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
	 * void mouseDragged ADDED WITH NEW LISTENER
	 */

	public void mouseDragged(MouseEvent e) {
		if(e.getModifiers() == 16) {

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
		}

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
			//width = 256;
			//height = 256;
			//lowRes = true;
			origin = /*new Vector3D(0,0,0);*/ config.max.plus(config.min).scalarMultiply(0.5);
			//x = new Vector3D(0, config.max.x - config.min.x, 0);
			//y = new Vector3D(0, 0, config.max.y - config.min.y);
			x = new Vector3D(0, boxSize, 0);
			y = new Vector3D(0, 0, boxSize);
			z = new Vector3D(y.cross(x));

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
			//width = 500;
			//height = 500;
			origin = /*new Vector3D(0,0,0);*/ config.max.plus(config.min).scalarMultiply(0.5);
			//x = new Vector3D(-(config.max.x - config.min.x), 0, 0);
			//y = new Vector3D(0, 0, config.max.z - config.min.z);
			x = new Vector3D(boxSize, 0, 0);
			y = new Vector3D(0, 0, boxSize);
			z = new Vector3D(y.cross(x));
			
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
			lowRes = false;
			origin = /*new Vector3D(0,0,0);*/ config.max.plus(config.min).scalarMultiply(0.5);
			x = new Vector3D(boxSize,0,0);
			y = new Vector3D(0,boxSize,0);
			z = new Vector3D(y.cross(x));
			
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

			/* used in conjunction with CcsThread.startPoint for efficiency testing */
			Date end = new Date();
			System.out.println("Image Gen took: " + ((double) (end.getTime() - CcsThread.startPoint.getTime())/1000) + " seconds");

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

}//end ViewingPanel


