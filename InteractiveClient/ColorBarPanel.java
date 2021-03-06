/*
 ColorBarPanel.java

Sets up and manipulates standard color map:

[0] background entry
[1...25x] attribute colors
[25x..254] family colors
[255] green, for marked data

 Created by Greg Stinson on Thu Oct 02 2003.
 Copyright (c) 2003 __MyCompanyName__. All rights reserved.
*/

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class ColorBarPanel extends JLabel implements MouseInputListener, MouseWheelListener, ComponentListener {
    Simulation sim;
	SimulationView view = null;
	public ColorModel colorModel;
    CBPopupMenu cbpm;
    MemoryImageSource cbsource;
    byte[] cm_red;
    byte[] cm_green;
    byte[] cm_blue;
	final int tableSize=256; // length of above arrays
	
    byte[] pixels = new byte[0];
    int cbstarty = 0, width, height; // onscreen pixel coordinates
	
	int startColor; // first color map index for attributes
	int endColor; // last color map index for attributes 
	int attrSize; // number of *attribute* entries in color map
	
	int startFamily; // color map index of first particle family (==endColor+1)
	
    byte[] puke_red;
    byte[] puke_green;
    byte[] puke_blue;
    
    public ColorBarPanel(Simulation s, int w, int h) {
        sim = s;
        width = w;
		height = h;
		
		cm_red = new byte[tableSize];
		cm_green = new byte[tableSize];
		cm_blue = new byte[tableSize];
		puke_red = new byte[tableSize];
		puke_green = new byte[tableSize];
		puke_blue = new byte[tableSize];
		
		startColor = 1;
		endColor = 255-1- sim.families.size();
		attrSize = endColor - startColor + 1;
		startFamily=endColor+1;
		
        initPukeCM();
        setColorModelWRBB();
 		
        cbpm = new CBPopupMenu();
		
		setSize(width, height);
		
		updateColors();
		
		addComponentListener(this);
    }
	
	public void setView(SimulationView v) {
		view = v;
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
	}
	
	// Push colorModel back out to AWT
	private void updateColors() {
	    if(pixels.length < width * height)
        	pixels = new byte[width * height];
	    byte value;

	    // Draw the colorbar, pixel by pixel.
	    	for(int i = 0; i < height; i++) {
			//value = (byte)(i*((float)tableSize)/(height)); // shows whole table
			value =(byte) (startColor + (endColor+1 - startColor) * i / height);
			for(int j = 0; j < width; j++) 
		    	pixels[i * width + j] = value;
		}
	    cbsource = new MemoryImageSource(width, height, colorModel, pixels, 
			width*(height-1), -width); /* <- flips image upside down */
	    setIcon(new ImageIcon(createImage(cbsource)));
		
		if (view!=null) view.redisplay(colorModel);
	}
    
	public void componentResized(ComponentEvent e) {
		width = getWidth();
		height = getHeight();
		updateColors();
	}
	
    public void mousePressed(MouseEvent e) {
        if(e.getButton()==e.BUTTON3) 
            cbpm.show(e.getComponent(), e.getX(), e.getY());
        cbstarty = e.getY();
    }
	
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2)
            invertCM();
	    updateColors();
    }
	
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseMoved(MouseEvent e) { }
	
    public void mouseDragged(MouseEvent e) {
        translateCM((e.getY()-cbstarty) * attrSize / height );
       	updateColors();
        cbstarty = e.getY();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        translateCM(e.getWheelRotation());
       	updateColors();
    }

    private class CBPopupMenu extends JPopupMenu implements ActionListener { 
        public CBPopupMenu( ) {
            JMenuItem item;
            item = new JMenuItem("Standard Gradient");
            item.setActionCommand("standard");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("Invert map");
            item.setActionCommand("invert");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("Rainbow gradient");
            item.setActionCommand("rainbow");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("Greyscale gradient");
            item.setActionCommand("grey");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("Swap Background Black/White");
            item.setActionCommand("swapBackground");
            item.addActionListener(this);
            add(item);
        }
        
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if(command.equals("standard")) 
                setColorModelWRBB();
            else if(command.equals("invert"))
                invertCM();
            else if(command.equals("rainbow"))
                setColorModelRainbow();
            else if(command.equals("grey"))
                setColorModelGrey();
            else if(command.equals("swapBackground"))
                swapBackground();

            updateColors();
        }
    }

    /*
    * inverts the current color map
    */
    private void invertCM() {
        byte temp;
        for(int i = 0; i < (endColor - startColor) / 2; ++i) {
            temp = cm_red[i + startColor];
            cm_red[i + startColor] = cm_red[endColor - i];
            cm_red[endColor - i] = temp;
            temp = cm_green[i + startColor];
            cm_green[i + startColor] = cm_green[endColor - i];
            cm_green[endColor - i] = temp;
            temp = cm_blue[i + startColor];
            cm_blue[i + startColor] = cm_blue[endColor - i];
            cm_blue[endColor - i] = temp;
        }
        colorModel = new IndexColorModel(8, tableSize, cm_red, cm_green, cm_blue);
    }
    

    /*
    * translates the color map a specified number of colors left or right
    */
    private void translateCM(int diff) {
        byte[] transferRed = new byte[attrSize];
        byte[] transferGreen = new byte[attrSize];
        byte[] transferBlue = new byte[attrSize];
        for(int i = 0; i < attrSize; ++i) {
            transferRed[i] = cm_red[startColor + (i + diff + attrSize) % attrSize];
            transferGreen[i] = cm_green[startColor + (i + diff + attrSize) % attrSize];
            transferBlue[i] = cm_blue[startColor + (i + diff + attrSize) % attrSize];
        }
        for(int i = 0; i < attrSize; ++i) {
            cm_red[i + startColor] = transferRed[i];
            cm_green[i + startColor] = transferGreen[i];
            cm_blue[i + startColor] = transferBlue[i];
        }

        colorModel = new IndexColorModel(8, tableSize, cm_red, cm_green, cm_blue);
    }


    private void initPukeCM() {
		//always have black as the first color
        puke_red[0] = 0;
        puke_green[0] = 0;
        puke_blue[0] = 0;
		//bright green is reserved for special/marked particles
        puke_red[255] = 0;
        puke_green[255] = (byte) 255;
        puke_blue[255] = 0;
		
		//set default colors for particle families
		for(Enumeration e = sim.families.elements(); e.hasMoreElements(); )
		{
			Simulation.Family family = (Simulation.Family) e.nextElement();
			int color = family.defaultColor;
			if(startFamily + family.index >= 255)
				System.err.println("Color index messed up!");
			puke_red[startFamily + family.index] = (byte) ((color >> 16) & 0xFF);
			puke_green[startFamily + family.index] = (byte) ((color >> 8) & 0xFF);
			puke_blue[startFamily + family.index] = (byte) ((color >> 0) & 0xFF);
		}
		
		//put more puke colors here
    }
    
    /*
    * sets the colormap to WRBB
	Note that bytes are signed in Java.  So when we use values from 0-255, Java
	automatically converts them to the range -128-127.  If you treat bytes as unsigned
	unifromly, the conversion should produce no side effects.  Just be aware.
    */
    private void setColorModelWRBB() {
        int i;
		//copy in the default colors
        for (i = 0; i < tableSize; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }
		
        int nextColor = startColor;
        int chunk_size = (attrSize - 1) / 5;

        for(i = 0; i < chunk_size; i++) {
                cm_red[nextColor] = 0;
                cm_green[nextColor] = 0;
                cm_blue[nextColor++] = (byte) (255 * i / chunk_size);
        }
        for(i = 0; i < chunk_size; i++) {
                cm_red[nextColor] = (byte) (255 * i / chunk_size);
                cm_green[nextColor] = 0;
                cm_blue[nextColor++] = (byte) 255;
        }
        for(i = 0; i < chunk_size; i++) {
                cm_red[nextColor] = (byte) 255;
                cm_green[nextColor] = 0;
                cm_blue[nextColor++] = (byte) (255 - 255 * i / chunk_size);
        }
        for(i = 0; i < chunk_size; i++) {
                cm_red[nextColor] = (byte) 255;
                cm_green[nextColor] = (byte) (255 * i / chunk_size);
                cm_blue[nextColor++] = (byte) 0;
        }
        for(i = 0; i < chunk_size; i++) {
                cm_red[nextColor] = (byte) 255;
                cm_green[nextColor] = (byte) 255;
                cm_blue[nextColor++] = (byte) (255 * i / chunk_size);
        }
		//make the remaining colors (at least one) be full white
        for(; nextColor <= endColor; ++nextColor) {
                cm_red[nextColor] = (byte) 255;
                cm_green[nextColor] = (byte) 255;
                cm_blue[nextColor] = (byte) 255;
        }
		
		//make and set the new color model
        colorModel = new IndexColorModel(8, tableSize, cm_red, cm_green, cm_blue);
    }

    /*
    * sets the colormap to Rainbow
    */
    private void setColorModelRainbow() {
		// Copy in the default colors
        for (int i = 0; i < tableSize; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }
		
		// Set rainbow for attribute colors
		float maxHue = 0.8f;
		float hue = 0;
		float delta = maxHue / attrSize;
		int rainbowColor;
		for(int i = startColor; i <= endColor; ++i, hue += delta) {
			rainbowColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
			cm_red[i] = (byte) ((rainbowColor >> 16) & 0xff);
			cm_green[i] = (byte) ((rainbowColor >> 8) & 0xff);
			cm_blue[i] = (byte) ((rainbowColor >> 0) & 0xff);
		}
		

	colorModel = new IndexColorModel(8, tableSize, cm_red, cm_green, cm_blue);
    }

    /*
    * sets the colormap to Greyscale
    */
    private void setColorModelGrey() {
		// Copy in the default colors
        for (int i = 0; i < tableSize; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }

        float hue = 0;
        float delta = 255.0f / attrSize;
        for(int i = startColor; i <= endColor; ++i, hue += delta) {
                cm_red[i] = (byte) (hue);
                cm_green[i] = (byte) (hue);
                cm_blue[i] = (byte) (hue);
        }
		
	colorModel = new IndexColorModel(8, tableSize, cm_red, cm_green, cm_blue);
    }
	
	/** Switch the background color between black and white.
	 A white background is useful for screenshots that will be printed.
	 */
    private void swapBackground() {
		if(cm_red[0] == 0) { //background is black
			cm_red[0] = cm_green[0] = cm_blue[0] = (byte) 255;
			puke_red[0] = puke_green[0] = puke_blue[0] = (byte) 255;
		} else {//background is white
			cm_red[0] = cm_green[0] = cm_blue[0] = 0;
			puke_red[0] = puke_green[0] = puke_blue[0] = 0;
		}
		
		colorModel = new IndexColorModel(8, tableSize, cm_red, cm_green, cm_blue);
    }
	
	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }
 
}
