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
import java.io.*;
import java.net.UnknownHostException;

public class ColorBarPanel extends JPanel 
                    implements MouseListener, MouseMotionListener,
                    MouseWheelListener
{
    Simulation s;
    public ViewPanel vp;
    CBPopupMenu cbpm;
    MemoryImageSource cbsource;
    byte[] cm_red;
    byte[] cm_green;
    byte[] cm_blue;
    byte[] imagebytes;
    int cmap_size, cbstartx = 0, width, height;
    byte[] puke_red;
    byte[] puke_green;
    byte[] puke_blue;
    
    public ColorBarPanel(Simulation sim, int h, int w) {
        s = sim;
        width = w;
		height = h;
		
		cm_red = new byte[256];
		cm_green = new byte[256];
		cm_blue = new byte[256];
		puke_red = new byte[256];
		puke_green = new byte[256];
		puke_blue = new byte[256];
		
		cmap_size = 256 - s.numberOfColors;
		
        initPukeCM();
        setColorModelWRBB();
        //setColorModelRainbow();
 		
        cbpm = new CBPopupMenu();
		
        imagebytes = new byte[width * height];
        byte value;
		for(int i = 0; i < width; i++) {
            value = (byte) (s.numberOfColors + java.lang.Math.floor((255.0 - s.numberOfColors) * i / (w - 1.0)));
			for(int j = 0; j < height; j++) 
                imagebytes[j * width + i] = value;
        }
        
        JLabel cbl = new JLabel();
		cbsource = new MemoryImageSource(width, height, s.cm, imagebytes, 0, width);
        cbsource.setAnimated(true);
        cbl.setIcon(new ImageIcon(createImage(cbsource)));
        cbl.addMouseListener(this);
        cbl.addMouseMotionListener(this);
        cbl.addMouseWheelListener(this);
		
        add(cbl);
    }
    
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
        cbstartx = e.getX();
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    public void mouseClicked(MouseEvent e) {
        maybeShowPopup(e);
        if(e.getClickCount() == 2) { 
            invertCM();
			cbsource.newPixels(imagebytes, s.cm, 0, width);
            vp.redisplay();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    private void maybeShowPopup(MouseEvent e) {
        if(e.isPopupTrigger()) 
            cbpm.show(e.getComponent(), e.getX(), e.getY());
    }
    
    public void mouseDragged(MouseEvent e) {
        translateCM((cbstartx - e.getX()) * cmap_size / width );
        cbsource.newPixels(imagebytes, s.cm, 0, width);
        vp.redisplay();
        cbstartx = e.getX();
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        translateCM(e.getWheelRotation());
        cbsource.newPixels(imagebytes, s.cm, 0, width);
        vp.redisplay();
    }

    private class CBPopupMenu extends JPopupMenu implements ActionListener {
        
        public CBPopupMenu( ) {
            super();
            
            JMenuItem item;
            item = new JMenuItem("Standard Gradient");
            item.setActionCommand("standard");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("Invert");
            item.setActionCommand("invert");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("Rainbow gradient");
            item.setActionCommand("rainbow");
            item.addActionListener(this);
            add(item);
        }
        
        public void actionPerformed(ActionEvent e){
            String command = e.getActionCommand();
            if(command.equals("standard")) { 
                setColorModelWRBB();
            } else if(command.equals("invert")) { 
                invertCM();
            } else if(command.equals("rainbow")) { 
                setColorModelRainbow();
            }
            cbsource.newPixels(imagebytes, s.cm, 0, width);
            vp.redisplay();
        }
        
    }

    /*
    * inverts the current color map
    */
    private void invertCM() {
        byte temp;
        for(int i = 0; i < (255 - s.numberOfColors) / 2; ++i) {
            temp = cm_red[i + s.numberOfColors];
            cm_red[i + s.numberOfColors] = cm_red[255 - i];
            cm_red[255 - i] = temp;
            temp = cm_green[i + s.numberOfColors];
            cm_green[i + s.numberOfColors] = cm_green[255 - i];
            cm_green[255 - i] = temp;
            temp = cm_blue[i + s.numberOfColors];
            cm_blue[i + s.numberOfColors] = cm_blue[255 - i];
            cm_blue[255 - i] = temp;
        }
        s.cm = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }
    

    /*
    * translates the color map a specified number of colors left or right
    */
    private void translateCM(int diff) {
    System.out.println("Crash 1: "+diff);
        byte[] transferRed = new byte[cmap_size];
        byte[] transferGreen = new byte[cmap_size];
        byte[] transferBlue = new byte[cmap_size];
    System.out.println("Crash 2");
        for(int i = 0; i < cmap_size; ++i) {
            transferRed[i] = cm_red[s.numberOfColors + (i + diff + cmap_size) % cmap_size];
            transferGreen[i] = cm_green[s.numberOfColors + (i + diff + cmap_size) % cmap_size];
            transferBlue[i] = cm_blue[s.numberOfColors + (i + diff + cmap_size) % cmap_size];
        }
    System.out.println("Crash 3");
        for(int i = 0; i < cmap_size; ++i) {
            cm_red[i + s.numberOfColors] = transferRed[i];
            cm_green[i + s.numberOfColors] = transferGreen[i];
            cm_blue[i + s.numberOfColors] = transferBlue[i];
        }
    System.out.println("Crash 4");

        s.cm = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
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
		
		//the standard tipsy colors for gas, dark, star
        puke_red[2] = (byte) 190;
        puke_green[2] = (byte) 200;
        puke_blue[2] = (byte) 255;
        puke_red[3] = (byte) 255;
        puke_green[3] = (byte) 63;
        puke_blue[3] = (byte) 63;
        puke_red[4] = (byte) 255;
        puke_green[4] = (byte) 255;
        puke_blue[4] = (byte) 140;
		
		//put more puke colors here
    }
    
    /*
    * sets the colormap to WRBB
    */
    private void setColorModelWRBB() {
        int i;
        int nextColor = s.numberOfColors;
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
        for (i = 0; i < s.numberOfColors; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }
		
		//make and set the new color model
        s.cm = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }

    /*
    * sets the colormap to Rainbow
    */
    private void setColorModelRainbow() {
		float maxHue = 0.8f;
		float hue = 0;
		float delta = maxHue / cmap_size;
		int rainbowColor;
		for(int i = s.numberOfColors; i < 256; ++i, hue += delta) {
			rainbowColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
			cm_red[i] = (byte) ((rainbowColor >> 16) & 0xff);
			cm_green[i] = (byte) ((rainbowColor >> 8) & 0xff);
			cm_blue[i] = (byte) ((rainbowColor >> 0) & 0xff);
		}
		
		//copy in the default colors
        for (int i = 0; i < s.numberOfColors; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }

		s.cm = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }
 
}
