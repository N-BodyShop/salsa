//
//  ColorBarPanel.java
//  
//
//  Created by Greg Stinson on Thu Oct 02 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

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
    byte[] pixels = new byte[0];
    int cmap_size, cbstartx = 0, width, height, startColor;
    byte[] puke_red;
    byte[] puke_green;
    byte[] puke_blue;
    
    public ColorBarPanel(Simulation s, int w, int h) {
        sim = s;
        width = w;
		height = h;
		
		cm_red = new byte[256];
		cm_green = new byte[256];
		cm_blue = new byte[256];
		puke_red = new byte[256];
		puke_green = new byte[256];
		puke_blue = new byte[256];
		
		startColor = 2 + sim.families.size();
		cmap_size = 256 - startColor;
		
        initPukeCM();
        setColorModelWRBB();
 		
        cbpm = new CBPopupMenu();
		
		setSize(width, height);
		
		resize();
		
		addComponentListener(this);
    }
	
	public void setView(SimulationView v) {
		view = v;
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
	}
			
	public void resize() {
	    if(pixels.length < width * height)
        	pixels = new byte[width * height];
	    byte value;

	    // Draw the colorbar, pixel by pixel.
	    for(int i = 0; i < width; i++) {
		value = (byte) (startColor + java.lang.Math.floor((255.0 - startColor) * i / (width - 1.0)));
		for(int j = 0; j < height; j++) 
		    pixels[j * width + i] = value;
		}
	    cbsource = new MemoryImageSource(width, height, colorModel, pixels, 0, width);
	    setIcon(new ImageIcon(createImage(cbsource)));
	}
      
	public void componentResized(ComponentEvent e) {
		width = getWidth();
		height = getHeight();
		resize();
	}
	
    public void mousePressed(MouseEvent e) {
        if(e.getButton()==e.BUTTON3) 
            cbpm.show(e.getComponent(), e.getX(), e.getY());
        cbstartx = e.getX();
    }
	
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2) { 
            invertCM();
	    cbsource.newPixels(pixels, colorModel, 0, width);
	    setIcon(new ImageIcon(createImage(cbsource)));
            view.redisplay(colorModel);
        }
    }
	
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseMoved(MouseEvent e) { }
	
    public void mouseDragged(MouseEvent e) {
        translateCM((cbstartx - e.getX()) * cmap_size / width );
        cbsource.newPixels(pixels, colorModel, 0, width);
        setIcon(new ImageIcon(createImage(cbsource)));
        view.redisplay(colorModel);
        cbstartx = e.getX();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        translateCM(e.getWheelRotation());
        cbsource.newPixels(pixels, colorModel, 0, width);
        setIcon(new ImageIcon(createImage(cbsource)));
        view.redisplay(colorModel);
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
            else if(command.equals("swapBackground"))
                swapBackground();
            cbsource.newPixels(pixels, colorModel, 0, width);
	    setIcon(new ImageIcon(createImage(cbsource)));
            view.redisplay(colorModel);
        }
    }

    /*
    * inverts the current color map
    */
    private void invertCM() {
        byte temp;
        for(int i = 0; i < (255 - startColor) / 2; ++i) {
            temp = cm_red[i + startColor];
            cm_red[i + startColor] = cm_red[255 - i];
            cm_red[255 - i] = temp;
            temp = cm_green[i + startColor];
            cm_green[i + startColor] = cm_green[255 - i];
            cm_green[255 - i] = temp;
            temp = cm_blue[i + startColor];
            cm_blue[i + startColor] = cm_blue[255 - i];
            cm_blue[255 - i] = temp;
        }
        colorModel = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }
    

    /*
    * translates the color map a specified number of colors left or right
    */
    private void translateCM(int diff) {
        byte[] transferRed = new byte[cmap_size];
        byte[] transferGreen = new byte[cmap_size];
        byte[] transferBlue = new byte[cmap_size];
        for(int i = 0; i < cmap_size; ++i) {
            transferRed[i] = cm_red[startColor + (i + diff + cmap_size) % cmap_size];
            transferGreen[i] = cm_green[startColor + (i + diff + cmap_size) % cmap_size];
            transferBlue[i] = cm_blue[startColor + (i + diff + cmap_size) % cmap_size];
        }
        for(int i = 0; i < cmap_size; ++i) {
            cm_red[i + startColor] = transferRed[i];
            cm_green[i + startColor] = transferGreen[i];
            cm_blue[i + startColor] = transferBlue[i];
        }

        colorModel = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }


    private void initPukeCM() {
		//always have black as the first color
        puke_red[0] = 0;
        puke_green[0] = 0;
        puke_blue[0] = 0;
		//bright green is reserved for special particles
        puke_red[1] = 0;
        puke_green[1] = (byte) 255;
        puke_blue[1] = 0;
		
		//set default colors for particle families
		for(Enumeration e = sim.families.elements(); e.hasMoreElements(); )
		{
			Simulation.Family family = (Simulation.Family) e.nextElement();
			int color = family.defaultColor;
			if(2 + family.index >= startColor)
				System.err.println("Color index messed up!");
			puke_red[2 + family.index] = (byte) ((color >> 16) & 0xFF);
			puke_green[2 + family.index] = (byte) ((color >> 8) & 0xFF);
			puke_blue[2 + family.index] = (byte) ((color >> 0) & 0xFF);
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
        int nextColor = startColor;
        int chunk_size = (cmap_size - 1) / 5;

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
        for(; nextColor < 256; ++nextColor) {
                cm_red[nextColor] = (byte) 255;
                cm_green[nextColor] = (byte) 255;
                cm_blue[nextColor] = (byte) 255;
        }
		
		//copy in the default colors
        for (i = 0; i < startColor; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }
		
		//make and set the new color model
        colorModel = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }

    /*
    * sets the colormap to Rainbow
    */
    private void setColorModelRainbow() {
		float maxHue = 0.8f;
		float hue = 0;
		float delta = maxHue / cmap_size;
		int rainbowColor;
		for(int i = startColor; i < 256; ++i, hue += delta) {
			rainbowColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
			cm_red[i] = (byte) ((rainbowColor >> 16) & 0xff);
			cm_green[i] = (byte) ((rainbowColor >> 8) & 0xff);
			cm_blue[i] = (byte) ((rainbowColor >> 0) & 0xff);
		}
		
		//copy in the default colors
        for (int i = 0; i < startColor; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }

	colorModel = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
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
		
		colorModel = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }
	
	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }
 
}
