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
        if(e.isPopupTrigger()) 
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
            item = new JMenuItem("Puke map");
            item.setActionCommand("puke");
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
            else if(command.equals("puke"))
                setColorModelPuke();
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
	for(Enumeration e = sim.families.elements(); e.hasMoreElements(); ) {
		    Simulation.Family family = (Simulation.Family) e.nextElement();
		    int color = family.defaultColor;
		    if(2 + family.index >= startColor)
			    System.err.println("Color index messed up!");
		    puke_red[2 + family.index] = (byte) ((color >> 16) & 0xFF);
		    puke_green[2 + family.index] = (byte) ((color >> 8) & 0xFF);
		    puke_blue[2 + family.index] = (byte) ((color >> 0) & 0xFF);
	    }
		
	    //put more puke colors here
	    // The following has been cut, massaged, and pasted
	    // from Tipsy.

	    puke_red[2] = (byte) 190 ;
	    puke_green[2] = (byte) 200 ;
	    puke_blue[2] = (byte) 255 ;

	    puke_red[3] = (byte) 255 ;
	    puke_green[3] = (byte) 63 ;
	    puke_blue[3] = (byte) 63 ;

	    puke_red[4] = (byte) 255 ;
	    puke_green[4] = (byte) 255 ;
	    puke_blue[4] = (byte) 140 ;

	// Beyond family colors 
	    puke_red[5] = (byte) 127 ;
	    puke_green[5] = (byte) 255 ;
	    puke_blue[5] = (byte) 212 ;

	    puke_red[6] = (byte) 255 ;
	    puke_green[6] = (byte) 160 ;
	    puke_blue[6] = (byte) 122 ;

	    puke_red[7] = (byte) 238 ;
	    puke_green[7] = (byte) 224 ;
	    puke_blue[7] = (byte) 229 ;

	    puke_red[8] = (byte) 255 ;
	    puke_green[8] = (byte) 239 ;
	    puke_blue[8] = (byte) 213 ;

	    puke_red[9] = (byte) 238 ;
	    puke_green[9] = (byte) 233 ;
	    puke_blue[9] = (byte) 191 ;

	    puke_red[10] = (byte) 72 ;
	    puke_green[10] = (byte) 118 ;
	    puke_blue[10] = (byte) 255 ;

	    puke_red[11] = (byte) 192 ;
	    puke_green[11] = (byte) 192 ;
	    puke_blue[11] = (byte) 192 ;

	    puke_red[12] = (byte) 255 ;
	    puke_green[12] = (byte) 231 ;
	    puke_blue[12] = (byte) 186 ;

	    puke_red[13] = (byte) 0 ;
	    puke_green[13] = (byte) 154 ;
	    puke_blue[13] = (byte) 205 ;

	    puke_red[14] = (byte) 102 ;
	    puke_green[14] = (byte) 205 ;
	    puke_blue[14] = (byte) 0 ;

	    puke_red[15] = (byte) 240 ;
	    puke_green[15] = (byte) 255 ;
	    puke_blue[15] = (byte) 240 ;

	    puke_red[16] = (byte) 180 ;
	    puke_green[16] = (byte) 205 ;
	    puke_blue[16] = (byte) 205 ;

	    puke_red[17] = (byte) 255 ;
	    puke_green[17] = (byte) 245 ;
	    puke_blue[17] = (byte) 238 ;

	    puke_red[18] = (byte) 205 ;
	    puke_green[18] = (byte) 133 ;
	    puke_blue[18] = (byte) 63 ;

	    puke_red[19] = (byte) 108 ;
	    puke_green[19] = (byte) 133 ;
	    puke_blue[19] = (byte) 139 ;

	    puke_red[20] = (byte) 171 ;
	    puke_green[20] = (byte) 130 ;
	    puke_blue[20] = (byte) 255 ;

	    puke_red[21] = (byte) 255 ;
	    puke_green[21] = (byte) 228 ;
	    puke_blue[21] = (byte) 196 ;

	    puke_red[22] = (byte) 238 ;
	    puke_green[22] = (byte) 92 ;
	    puke_blue[22] = (byte) 56 ;

	    puke_red[23] = (byte) 0 ;
	    puke_green[23] = (byte) 205  ;
	    puke_blue[23] = (byte) 205 ;

	    puke_red[24] = (byte) 178 ;
	    puke_green[24] = (byte) 34 ;
	    puke_blue[24] = (byte) 34 ;

	    puke_red[25] = (byte) 139 ;
	    puke_green[25] = (byte) 95 ;
	    puke_blue[25] = (byte) 101 ;

	    puke_red[26] = (byte) 255 ;
	    puke_green[26] = (byte) 228 ;
	    puke_blue[26] = (byte) 225 ;

	    puke_red[27] = (byte) 238 ;
	    puke_green[27] = (byte) 232 ;
	    puke_blue[27] = (byte) 170 ;

	    puke_red[28] = (byte) 239 ;
	    puke_green[28] = (byte) 0 ;
	    puke_blue[28] = (byte) 239 ;

	    puke_red[29] = (byte) 179 ;
	    puke_green[29] = (byte) 238 ;
	    puke_blue[29] = (byte) 58 ;

	    puke_red[30] = (byte) 121 ;
	    puke_green[30] = (byte) 160 ;
	    puke_blue[30] = (byte) 221 ;

	    puke_red[31] = (byte) 255 ;
	    puke_green[31] = (byte) 0 ;
	    puke_blue[31] = (byte) 0 ;

	    puke_red[32] = (byte) 205 ;
	    puke_green[32] = (byte) 198 ;
	    puke_blue[32] = (byte) 115 ;

	    puke_red[33] = (byte) 255 ;
	    puke_green[33] = (byte) 215 ;
	    puke_blue[33] = (byte) 185 ;

	    puke_red[34] = (byte) 238 ;
	    puke_green[34] = (byte) 54 ;
	    puke_blue[34] = (byte) 0 ;

	    puke_red[35] = (byte) 155 ;
	    puke_green[35] = (byte) 205 ;
	    puke_blue[35] = (byte) 155 ;

	    puke_red[36] = (byte) 173 ;
	    puke_green[36] = (byte) 255 ;
	    puke_blue[36] = (byte) 47 ;

	    puke_red[37] = (byte) 238 ;
	    puke_green[37] = (byte) 122 ;
	    puke_blue[37] = (byte) 233 ;

	    puke_red[38] = (byte) 0 ;
	    puke_green[38] = (byte) 0 ;
	    puke_blue[38] = (byte) 128 ;

	    puke_red[39] = (byte) 222 ;
	    puke_green[39] = (byte) 184 ;
	    puke_blue[39] = (byte) 135 ;

	    puke_red[40] = (byte) 205 ;
	    puke_green[40] = (byte) 41 ;
	    puke_blue[40] = (byte) 144 ;

	    puke_red[41] = (byte) 245 ;
	    puke_green[41] = (byte) 255 ;
	    puke_blue[41] = (byte) 250 ;

	    puke_red[42] = (byte) 138 ;
	    puke_green[42] = (byte) 43 ;
	    puke_blue[42] = (byte) 226 ;

	    puke_red[43] = (byte) 125 ;
	    puke_green[43] = (byte) 38 ;
	    puke_blue[43] = (byte) 205 ;

	    puke_red[44] = (byte) 0 ;
	    puke_green[44] = (byte) 139 ;
	    puke_blue[44] = (byte) 69 ;

	    puke_red[45] = (byte) 34 ;
	    puke_green[45] = (byte) 139 ;
	    puke_blue[45] = (byte) 34 ;

	    puke_red[46] = (byte) 238 ;
	    puke_green[46] = (byte) 58 ;
	    puke_blue[46] = (byte) 140 ;

	    puke_red[47] = (byte) 47 ;
	    puke_green[47] = (byte) 79 ;
	    puke_blue[47] = (byte) 79 ;

	    puke_red[48] = (byte) 188 ;
	    puke_green[48] = (byte) 143 ;
	    puke_blue[48] = (byte) 143 ;

	    puke_red[49] = (byte) 124 ;
	    puke_green[49] = (byte) 252 ;
	    puke_blue[49] = (byte) 0 ;

	    puke_red[50] = (byte) 255 ;
	    puke_green[50] = (byte) 228 ;
	    puke_blue[50] = (byte) 181 ;

	    puke_red[51] = (byte) 239 ;
	    puke_green[51] = (byte) 237 ;
	    puke_blue[51] = (byte) 161 ;

	    puke_red[52] = (byte) 65 ;
	    puke_green[52] = (byte) 105 ;
	    puke_blue[52] = (byte) 225 ;

	    puke_red[53] = (byte) 255;
	    puke_green[53] = (byte) 238 ;
	    puke_blue[53] = (byte) 104 ;

	    puke_red[54] = (byte) 238 ;
	    puke_green[54] = (byte) 106 ;
	    puke_blue[54] = (byte) 80 ;

	    puke_red[55] = (byte) 0 ;
	    puke_green[55] = (byte) 100 ;
	    puke_blue[55] = (byte) 0 ;

	    puke_red[56] = (byte) 204 ;
	    puke_green[56] = (byte) 190 ;
	    puke_blue[56] = (byte) 112 ;

	    puke_red[57] = (byte) 205 ;
	    puke_green[57] = (byte) 200 ;
	    puke_blue[57] = (byte) 177 ;

	    puke_red[58] = (byte) 0 ;
	    puke_green[58] = (byte) 255 ;
	    puke_blue[58] = (byte) 127 ;

	    puke_red[59] = (byte) 240 ;
	    puke_green[59] = (byte) 255 ;
	    puke_blue[59] = (byte) 255 ;

	    puke_red[60] = (byte) 160 ;
	    puke_green[60] = (byte) 82 ;
	    puke_blue[60] = (byte) 45 ;

	    puke_red[61] = (byte) 46 ;
	    puke_green[61] = (byte) 139 ;
	    puke_blue[61] = (byte) 87 ;

	    puke_red[62] = (byte) 0 ;
	    puke_green[62] = (byte) 197 ;
	    puke_blue[62] = (byte) 205 ;

	    puke_red[63] = (byte) 205 ;
	    puke_green[63] = (byte) 102 ;
	    puke_blue[63] = (byte) 0 ;

	    puke_red[64] = (byte) 205 ;
	    puke_green[64] = (byte) 96 ;
	    puke_blue[64] = (byte) 144 ;

	    puke_red[65] = (byte) 122 ;
	    puke_green[65] = (byte) 197 ;
	    puke_blue[65] = (byte) 205 ;

	    puke_red[66] = (byte) 176 ;
	    puke_green[66] = (byte) 48 ;
	    puke_blue[66] = (byte) 96 ;

	    puke_red[67] = (byte) 209 ;
	    puke_green[67] = (byte) 95 ;
	    puke_blue[67] = (byte) 238 ;

	    puke_red[68] = (byte) 255 ;
	    puke_green[68] = (byte) 240 ;
	    puke_blue[68] = (byte) 245 ;

	    puke_red[69] = (byte) 205 ;
	    puke_green[69] = (byte) 102 ;
	    puke_blue[69] = (byte) 29 ;

	    puke_red[70] = (byte) 79 ;
	    puke_green[70] = (byte) 148 ;
	    puke_blue[70] = (byte) 205 ;

	    puke_red[71] = (byte) 255 ;
	    puke_green[71] = (byte) 255 ;
	    puke_blue[71] = (byte) 240 ;

	    puke_red[72] = (byte) 208 ;
	    puke_green[72] = (byte) 32 ;
	    puke_blue[72] = (byte) 144 ;

	    puke_red[73] = (byte) 0 ;
	    puke_green[73] = (byte) 206 ;
	    puke_blue[73] = (byte) 209 ;

	    puke_red[74] = (byte) 141 ;
	    puke_green[74] = (byte) 238 ;
	    puke_blue[74] = (byte) 238 ;

	    puke_red[75] = (byte) 205 ;
	    puke_green[75] = (byte) 201 ;
	    puke_blue[75] = (byte) 201;

	    puke_red[76] = (byte) 143 ;
	    puke_green[76] = (byte) 188;
	    puke_blue[76] = (byte) 143 ;

	    puke_red[77] = (byte) 240 ;
	    puke_green[77] = (byte) 248 ;
	    puke_blue[77] = (byte) 255 ;

	    puke_red[78] = (byte) 255 ;
	    puke_green[78] = (byte) 255 ;
	    puke_blue[78] = (byte) 0 ;

	    puke_red[79] = (byte) 135 ;
	    puke_green[79] = (byte) 206 ;
	    puke_blue[79] = (byte) 235 ;

	    puke_red[80] = (byte) 220 ;
	    puke_green[80] = (byte) 220 ;
	    puke_blue[80] = (byte) 220 ;

	    puke_red[81] = (byte) 238 ;
	    puke_green[81] = (byte) 99 ;
	    puke_blue[81] = (byte) 99 ;

	    puke_red[82] = (byte) 100 ;
	    puke_green[82] = (byte) 149 ;
	    puke_blue[82] = (byte) 237 ;

	    puke_red[83] = (byte) 255 ;
	    puke_green[83] = (byte) 235 ;
	    puke_blue[83] = (byte) 205 ;

	    puke_red[84] = (byte) 238 ;
	    puke_green[84] = (byte) 153 ;
	    puke_blue[84] = (byte) 73 ;

	    puke_red[85] = (byte) 238 ;
	    puke_green[85] = (byte) 18 ;
	    puke_blue[85] = (byte) 137 ;

	    puke_red[86] = (byte) 25 ;
	    puke_green[86] = (byte) 25 ;
	    puke_blue[86] = (byte) 112 ;

	    puke_red[87] = (byte) 205 ;
	    puke_green[87] = (byte) 112 ;
	    puke_blue[87] = (byte) 84 ;

	    puke_red[88] = (byte) 60 ;
	    puke_green[88] = (byte) 179 ;
	    puke_blue[88] = (byte) 113 ;

	    puke_red[89] = (byte) 255 ;
	    puke_green[89] = (byte) 250 ;
	    puke_blue[89] = (byte) 205 ;

	    puke_red[90] = (byte) 139 ;
	    puke_green[90] = (byte) 26 ;
	    puke_blue[90] = (byte) 26 ;

	    puke_red[91] = (byte) 173 ;
	    puke_green[91] = (byte) 216 ;
	    puke_blue[91] = (byte) 230 ;

	    puke_red[92] = (byte) 211 ;
	    puke_green[92] = (byte) 211 ;
	    puke_blue[92] = (byte) 211 ;

	    puke_red[93] = (byte) 205 ;
	    puke_green[93] = (byte) 192 ;
	    puke_blue[93] = (byte) 176 ;

	    puke_red[94] = (byte) 238 ;
	    puke_green[94] = (byte) 213 ;
	    puke_blue[94] = (byte) 210 ;

	    puke_red[95] = (byte) 110 ;
	    puke_green[95] = (byte) 139 ;
	    puke_blue[95] = (byte) 61 ;

	    puke_red[96] = (byte) 139 ;
	    puke_green[96] = (byte) 134;
	    puke_blue[96] = (byte) 130 ;

	    puke_red[97] = (byte) 102 ;
	    puke_green[97] = (byte) 205 ;
	    puke_blue[97] = (byte) 170 ;

	    puke_red[98] = (byte) 205 ;
	    puke_green[98] = (byte) 205 ;
	    puke_blue[98] = (byte) 180 ;

	    puke_red[99]   = (byte) 205 ;
	    puke_green[99] = (byte) 145 ;
	    puke_blue[99]  = (byte) 115 ;

	    puke_red[100]   = (byte) 0 ;
	    puke_green[100] = (byte) 229 ;
	    puke_blue[100]  = (byte) 238 ;

	    puke_red[101]   = (byte) 205 ;
	    puke_green[101] = (byte) 104 ;
	    puke_blue[101]  = (byte) 57 ;

	    puke_red[102]   = (byte) 32 ;
	    puke_green[102] = (byte) 178 ;
	    puke_blue[102]  = (byte) 170 ;

	    puke_red[103]   = (byte) 137 ;
	    puke_green[103] = (byte) 104 ;
	    puke_blue[103]  = (byte) 205 ;

	    puke_red[104]   = (byte) 139 ;
	    puke_green[104] = (byte) 69 ;
	    puke_blue[104]  = (byte) 19 ;

	    puke_red[105]   = (byte) 132 ;
	    puke_green[105] = (byte) 112 ;
	    puke_blue[105]  = (byte) 255 ;

	    puke_red[106]   = (byte) 255 ;
	    puke_green[106] = (byte) 246 ;
	    puke_blue[106]  = (byte) 143 ;

	    puke_red[107]   = (byte) 216 ;
	    puke_green[107] = (byte) 191 ;
	    puke_blue[107]  = (byte) 216 ;

	    puke_red[108]   = (byte) 154 ;
	    puke_green[108] = (byte) 50 ;
	    puke_blue[108]  = (byte) 205 ;

	    puke_red[109]   = (byte) 253 ;
	    puke_green[109] = (byte) 245 ;
	    puke_blue[109]  = (byte) 230 ;

	    puke_red[110]   = (byte) 239 ;
	    puke_green[110] = (byte) 87 ;
	    puke_blue[110]  = (byte) 66 ;

	    puke_red[111]   = (byte) 205 ;
	    puke_green[111] = (byte) 104 ;
	    puke_blue[111]  = (byte) 137 ;

	    puke_red[112]   = (byte) 102 ;
	    puke_green[112] = (byte) 205 ;
	    puke_blue[112]  = (byte) 170 ;

	    puke_red[113]   = (byte) 205 ;
	    puke_green[113] = (byte) 133 ;
	    puke_blue[113]  = (byte) 0 ;

	    puke_red[114]   = (byte) 239 ;
	    puke_green[114] = (byte) 62 ;
	    puke_blue[114]  = (byte) 47 ;

	    puke_red[115]   = (byte) 105 ;
	    puke_green[115] = (byte) 105 ;
	    puke_blue[115]  = (byte) 105 ;

	    puke_red[116]   = (byte) 139 ;
	    puke_green[116] = (byte) 125 ;
	    puke_blue[116]  = (byte) 107 ;

	    puke_red[117]   = (byte) 0 ;
	    puke_green[117] = (byte) 0 ;
	    puke_blue[117]  = (byte) 205 ;

	    puke_red[118]   = (byte) 105 ;
	    puke_green[118] = (byte) 139 ;
	    puke_blue[118]  = (byte) 34 ;

	    puke_red[119]   = (byte) 184 ;
	    puke_green[119] = (byte) 134 ;
	    puke_blue[119]  = (byte) 11 ;

	    puke_red[120]   = (byte) 70 ;
	    puke_green[120] = (byte) 130 ;
	    puke_blue[120]  = (byte) 180 ;

	    puke_red[121]   = (byte) 84 ;
	    puke_green[121] = (byte) 139 ;
	    puke_blue[121]  = (byte) 84 ;

	    puke_red[122]   = (byte) 255 ;
	    puke_green[122] = (byte) 48 ;
	    puke_blue[122]  = (byte) 48 ;

	    puke_red[123]   = (byte) 238 ;
	    puke_green[123] = (byte) 174 ;
	    puke_blue[123]  = (byte) 238 ;

	    puke_red[124]   = (byte) 112 ;
	    puke_green[124] = (byte) 128 ;
	    puke_blue[124]  = (byte) 144 ;

	    puke_red[125]   = (byte) 255 ;
	    puke_green[125] = (byte) 69 ;
	    puke_blue[125]  = (byte) 0 ;

	    puke_red[126]   = (byte) 154 ;
	    puke_green[126] = (byte) 192 ;
	    puke_blue[126]  = (byte) 205 ;

	// Repeat the puke sequence until 255.  Tipsy only had 126 colors.
	    int iMore = (byte) 125;  // This helps me do a simple string replace from the
			      // Tipsy code.

	    puke_red[iMore+2] = (byte) 190 ;
	    puke_green[iMore+2] = (byte) 200 ;
	    puke_blue[iMore+2] = (byte) 255 ;

	    puke_red[iMore+3] = (byte) 255 ;
	    puke_green[iMore+3] = (byte) 63 ;
	    puke_blue[iMore+3] = (byte) 63 ;

	    puke_red[iMore+4] = (byte) 255 ;
	    puke_green[iMore+4] = (byte) 255 ;
	    puke_blue[iMore+4] = (byte) 140 ;

	// Beyond family colors 
	    puke_red[iMore+5] = (byte) 127 ;
	    puke_green[iMore+5] = (byte) 255 ;
	    puke_blue[iMore+5] = (byte) 212 ;

	    puke_red[iMore+6] = (byte) 255 ;
	    puke_green[iMore+6] = (byte) 160 ;
	    puke_blue[iMore+6] = (byte) 122 ;

	    puke_red[iMore+7] = (byte) 238 ;
	    puke_green[iMore+7] = (byte) 224 ;
	    puke_blue[iMore+7] = (byte) 229 ;

	    puke_red[iMore+8] = (byte) 255 ;
	    puke_green[iMore+8] = (byte) 239 ;
	    puke_blue[iMore+8] = (byte) 213 ;

	    puke_red[iMore+9] = (byte) 238 ;
	    puke_green[iMore+9] = (byte) 233 ;
	    puke_blue[iMore+9] = (byte) 191 ;

	    puke_red[iMore+10] = (byte) 72 ;
	    puke_green[iMore+10] = (byte) 118 ;
	    puke_blue[iMore+10] = (byte) 255 ;

	    puke_red[iMore+11] = (byte) 192 ;
	    puke_green[iMore+11] = (byte) 192 ;
	    puke_blue[iMore+11] = (byte) 192 ;

	    puke_red[iMore+12] = (byte) 255 ;
	    puke_green[iMore+12] = (byte) 231 ;
	    puke_blue[iMore+12] = (byte) 186 ;

	    puke_red[iMore+13] = (byte) 0 ;
	    puke_green[iMore+13] = (byte) 154 ;
	    puke_blue[iMore+13] = (byte) 205 ;

	    puke_red[iMore+14] = (byte) 102 ;
	    puke_green[iMore+14] = (byte) 205 ;
	    puke_blue[iMore+14] = (byte) 0 ;

	    puke_red[iMore+15] = (byte) 240 ;
	    puke_green[iMore+15] = (byte) 255 ;
	    puke_blue[iMore+15] = (byte) 240 ;

	    puke_red[iMore+16] = (byte) 180 ;
	    puke_green[iMore+16] = (byte) 205 ;
	    puke_blue[iMore+16] = (byte) 205 ;

	    puke_red[iMore+17] = (byte) 255 ;
	    puke_green[iMore+17] = (byte) 245 ;
	    puke_blue[iMore+17] = (byte) 238 ;

	    puke_red[iMore+18] = (byte) 205 ;
	    puke_green[iMore+18] = (byte) 133 ;
	    puke_blue[iMore+18] = (byte) 63 ;

	    puke_red[iMore+19] = (byte) 108 ;
	    puke_green[iMore+19] = (byte) 133 ;
	    puke_blue[iMore+19] = (byte) 139 ;

	    puke_red[iMore+20] = (byte) 171 ;
	    puke_green[iMore+20] = (byte) 130 ;
	    puke_blue[iMore+20] = (byte) 255 ;

	    puke_red[iMore+21] = (byte) 255 ;
	    puke_green[iMore+21] = (byte) 228 ;
	    puke_blue[iMore+21] = (byte) 196 ;

	    puke_red[iMore+22] = (byte) 238 ;
	    puke_green[iMore+22] = (byte) 92 ;
	    puke_blue[iMore+22] = (byte) 56 ;

	    puke_red[iMore+23] = (byte) 0 ;
	    puke_green[iMore+23] = (byte) 205  ;
	    puke_blue[iMore+23] = (byte) 205 ;

	    puke_red[iMore+24] = (byte) 178 ;
	    puke_green[iMore+24] = (byte) 34 ;
	    puke_blue[iMore+24] = (byte) 34 ;

	    puke_red[iMore+25] = (byte) 139 ;
	    puke_green[iMore+25] = (byte) 95 ;
	    puke_blue[iMore+25] = (byte) 101 ;

	    puke_red[iMore+26] = (byte) 255 ;
	    puke_green[iMore+26] = (byte) 228 ;
	    puke_blue[iMore+26] = (byte) 225 ;

	    puke_red[iMore+27] = (byte) 238 ;
	    puke_green[iMore+27] = (byte) 232 ;
	    puke_blue[iMore+27] = (byte) 170 ;

	    puke_red[iMore+28] = (byte) 239 ;
	    puke_green[iMore+28] = (byte) 0 ;
	    puke_blue[iMore+28] = (byte) 239 ;

	    puke_red[iMore+29] = (byte) 179 ;
	    puke_green[iMore+29] = (byte) 238 ;
	    puke_blue[iMore+29] = (byte) 58 ;

	    puke_red[iMore+30] = (byte) 121 ;
	    puke_green[iMore+30] = (byte) 160 ;
	    puke_blue[iMore+30] = (byte) 221 ;

	    puke_red[iMore+31] = (byte) 255 ;
	    puke_green[iMore+31] = (byte) 0 ;
	    puke_blue[iMore+31] = (byte) 0 ;

	    puke_red[iMore+32] = (byte) 205 ;
	    puke_green[iMore+32] = (byte) 198 ;
	    puke_blue[iMore+32] = (byte) 115 ;

	    puke_red[iMore+33] = (byte) 255 ;
	    puke_green[iMore+33] = (byte) 215 ;
	    puke_blue[iMore+33] = (byte) 185 ;

	    puke_red[iMore+34] = (byte) 238 ;
	    puke_green[iMore+34] = (byte) 54 ;
	    puke_blue[iMore+34] = (byte) 0 ;

	    puke_red[iMore+35] = (byte) 155 ;
	    puke_green[iMore+35] = (byte) 205 ;
	    puke_blue[iMore+35] = (byte) 155 ;

	    puke_red[iMore+36] = (byte) 173 ;
	    puke_green[iMore+36] = (byte) 255 ;
	    puke_blue[iMore+36] = (byte) 47 ;

	    puke_red[iMore+37] = (byte) 238 ;
	    puke_green[iMore+37] = (byte) 122 ;
	    puke_blue[iMore+37] = (byte) 233 ;

	    puke_red[iMore+38] = (byte) 0 ;
	    puke_green[iMore+38] = (byte) 0 ;
	    puke_blue[iMore+38] = (byte) 128 ;

	    puke_red[iMore+39] = (byte) 222 ;
	    puke_green[iMore+39] = (byte) 184 ;
	    puke_blue[iMore+39] = (byte) 135 ;

	    puke_red[iMore+40] = (byte) 205 ;
	    puke_green[iMore+40] = (byte) 41 ;
	    puke_blue[iMore+40] = (byte) 144 ;

	    puke_red[iMore+41] = (byte) 245 ;
	    puke_green[iMore+41] = (byte) 255 ;
	    puke_blue[iMore+41] = (byte) 250 ;

	    puke_red[iMore+42] = (byte) 138 ;
	    puke_green[iMore+42] = (byte) 43 ;
	    puke_blue[iMore+42] = (byte) 226 ;

	    puke_red[iMore+43] = (byte) 125 ;
	    puke_green[iMore+43] = (byte) 38 ;
	    puke_blue[iMore+43] = (byte) 205 ;

	    puke_red[iMore+44] = (byte) 0 ;
	    puke_green[iMore+44] = (byte) 139 ;
	    puke_blue[iMore+44] = (byte) 69 ;

	    puke_red[iMore+45] = (byte) 34 ;
	    puke_green[iMore+45] = (byte) 139 ;
	    puke_blue[iMore+45] = (byte) 34 ;

	    puke_red[iMore+46] = (byte) 238 ;
	    puke_green[iMore+46] = (byte) 58 ;
	    puke_blue[iMore+46] = (byte) 140 ;

	    puke_red[iMore+47] = (byte) 47 ;
	    puke_green[iMore+47] = (byte) 79 ;
	    puke_blue[iMore+47] = (byte) 79 ;

	    puke_red[iMore+48] = (byte) 188 ;
	    puke_green[iMore+48] = (byte) 143 ;
	    puke_blue[iMore+48] = (byte) 143 ;

	    puke_red[iMore+49] = (byte) 124 ;
	    puke_green[iMore+49] = (byte) 252 ;
	    puke_blue[iMore+49] = (byte) 0 ;

	    puke_red[iMore+50] = (byte) 255 ;
	    puke_green[iMore+50] = (byte) 228 ;
	    puke_blue[iMore+50] = (byte) 181 ;

	    puke_red[iMore+51] = (byte) 239 ;
	    puke_green[iMore+51] = (byte) 237 ;
	    puke_blue[iMore+51] = (byte) 161 ;

	    puke_red[iMore+52] = (byte) 65 ;
	    puke_green[iMore+52] = (byte) 105 ;
	    puke_blue[iMore+52] = (byte) 225 ;

	    puke_red[iMore+53] = (byte) 255;
	    puke_green[iMore+53] = (byte) 238 ;
	    puke_blue[iMore+53] = (byte) 104 ;

	    puke_red[iMore+54] = (byte) 238 ;
	    puke_green[iMore+54] = (byte) 106 ;
	    puke_blue[iMore+54] = (byte) 80 ;

	    puke_red[iMore+55] = (byte) 0 ;
	    puke_green[iMore+55] = (byte) 100 ;
	    puke_blue[iMore+55] = (byte) 0 ;

	    puke_red[iMore+56] = (byte) 204 ;
	    puke_green[iMore+56] = (byte) 190 ;
	    puke_blue[iMore+56] = (byte) 112 ;

	    puke_red[iMore+57] = (byte) 205 ;
	    puke_green[iMore+57] = (byte) 200 ;
	    puke_blue[iMore+57] = (byte) 177 ;

	    puke_red[iMore+58] = (byte) 0 ;
	    puke_green[iMore+58] = (byte) 255 ;
	    puke_blue[iMore+58] = (byte) 127 ;

	    puke_red[iMore+59] = (byte) 240 ;
	    puke_green[iMore+59] = (byte) 255 ;
	    puke_blue[iMore+59] = (byte) 255 ;

	    puke_red[iMore+60] = (byte) 160 ;
	    puke_green[iMore+60] = (byte) 82 ;
	    puke_blue[iMore+60] = (byte) 45 ;

	    puke_red[iMore+61] = (byte) 46 ;
	    puke_green[iMore+61] = (byte) 139 ;
	    puke_blue[iMore+61] = (byte) 87 ;

	    puke_red[iMore+62] = (byte) 0 ;
	    puke_green[iMore+62] = (byte) 197 ;
	    puke_blue[iMore+62] = (byte) 205 ;

	    puke_red[iMore+63] = (byte) 205 ;
	    puke_green[iMore+63] = (byte) 102 ;
	    puke_blue[iMore+63] = (byte) 0 ;

	    puke_red[iMore+64] = (byte) 205 ;
	    puke_green[iMore+64] = (byte) 96 ;
	    puke_blue[iMore+64] = (byte) 144 ;

	    puke_red[iMore+65] = (byte) 122 ;
	    puke_green[iMore+65] = (byte) 197 ;
	    puke_blue[iMore+65] = (byte) 205 ;

	    puke_red[iMore+66] = (byte) 176 ;
	    puke_green[iMore+66] = (byte) 48 ;
	    puke_blue[iMore+66] = (byte) 96 ;

	    puke_red[iMore+67] = (byte) 209 ;
	    puke_green[iMore+67] = (byte) 95 ;
	    puke_blue[iMore+67] = (byte) 238 ;

	    puke_red[iMore+68] = (byte) 255 ;
	    puke_green[iMore+68] = (byte) 240 ;
	    puke_blue[iMore+68] = (byte) 245 ;

	    puke_red[iMore+69] = (byte) 205 ;
	    puke_green[iMore+69] = (byte) 102 ;
	    puke_blue[iMore+69] = (byte) 29 ;

	    puke_red[iMore+70] = (byte) 79 ;
	    puke_green[iMore+70] = (byte) 148 ;
	    puke_blue[iMore+70] = (byte) 205 ;

	    puke_red[iMore+71] = (byte) 255 ;
	    puke_green[iMore+71] = (byte) 255 ;
	    puke_blue[iMore+71] = (byte) 240 ;

	    puke_red[iMore+72] = (byte) 208 ;
	    puke_green[iMore+72] = (byte) 32 ;
	    puke_blue[iMore+72] = (byte) 144 ;

	    puke_red[iMore+73] = (byte) 0 ;
	    puke_green[iMore+73] = (byte) 206 ;
	    puke_blue[iMore+73] = (byte) 209 ;

	    puke_red[iMore+74] = (byte) 141 ;
	    puke_green[iMore+74] = (byte) 238 ;
	    puke_blue[iMore+74] = (byte) 238 ;

	    puke_red[iMore+75] = (byte) 205 ;
	    puke_green[iMore+75] = (byte) 201 ;
	    puke_blue[iMore+75] = (byte) 201;

	    puke_red[iMore+76] = (byte) 143 ;
	    puke_green[iMore+76] = (byte) 188;
	    puke_blue[iMore+76] = (byte) 143 ;

	    puke_red[iMore+77] = (byte) 240 ;
	    puke_green[iMore+77] = (byte) 248 ;
	    puke_blue[iMore+77] = (byte) 255 ;

	    puke_red[iMore+78] = (byte) 255 ;
	    puke_green[iMore+78] = (byte) 255 ;
	    puke_blue[iMore+78] = (byte) 0 ;

	    puke_red[iMore+79] = (byte) 135 ;
	    puke_green[iMore+79] = (byte) 206 ;
	    puke_blue[iMore+79] = (byte) 235 ;

	    puke_red[iMore+80] = (byte) 220 ;
	    puke_green[iMore+80] = (byte) 220 ;
	    puke_blue[iMore+80] = (byte) 220 ;

	    puke_red[iMore+81] = (byte) 238 ;
	    puke_green[iMore+81] = (byte) 99 ;
	    puke_blue[iMore+81] = (byte) 99 ;

	    puke_red[iMore+82] = (byte) 100 ;
	    puke_green[iMore+82] = (byte) 149 ;
	    puke_blue[iMore+82] = (byte) 237 ;

	    puke_red[iMore+83] = (byte) 255 ;
	    puke_green[iMore+83] = (byte) 235 ;
	    puke_blue[iMore+83] = (byte) 205 ;

	    puke_red[iMore+84] = (byte) 238 ;
	    puke_green[iMore+84] = (byte) 153 ;
	    puke_blue[iMore+84] = (byte) 73 ;

	    puke_red[iMore+85] = (byte) 238 ;
	    puke_green[iMore+85] = (byte) 18 ;
	    puke_blue[iMore+85] = (byte) 137 ;

	    puke_red[iMore+86] = (byte) 25 ;
	    puke_green[iMore+86] = (byte) 25 ;
	    puke_blue[iMore+86] = (byte) 112 ;

	    puke_red[iMore+87] = (byte) 205 ;
	    puke_green[iMore+87] = (byte) 112 ;
	    puke_blue[iMore+87] = (byte) 84 ;

	    puke_red[iMore+88] = (byte) 60 ;
	    puke_green[iMore+88] = (byte) 179 ;
	    puke_blue[iMore+88] = (byte) 113 ;

	    puke_red[iMore+89] = (byte) 255 ;
	    puke_green[iMore+89] = (byte) 250 ;
	    puke_blue[iMore+89] = (byte) 205 ;

	    puke_red[iMore+90] = (byte) 139 ;
	    puke_green[iMore+90] = (byte) 26 ;
	    puke_blue[iMore+90] = (byte) 26 ;

	    puke_red[iMore+91] = (byte) 173 ;
	    puke_green[iMore+91] = (byte) 216 ;
	    puke_blue[iMore+91] = (byte) 230 ;

	    puke_red[iMore+92] = (byte) 211 ;
	    puke_green[iMore+92] = (byte) 211 ;
	    puke_blue[iMore+92] = (byte) 211 ;

	    puke_red[iMore+93] = (byte) 205 ;
	    puke_green[iMore+93] = (byte) 192 ;
	    puke_blue[iMore+93] = (byte) 176 ;

	    puke_red[iMore+94] = (byte) 238 ;
	    puke_green[iMore+94] = (byte) 213 ;
	    puke_blue[iMore+94] = (byte) 210 ;

	    puke_red[iMore+95] = (byte) 110 ;
	    puke_green[iMore+95] = (byte) 139 ;
	    puke_blue[iMore+95] = (byte) 61 ;

	    puke_red[iMore+96] = (byte) 139 ;
	    puke_green[iMore+96] = (byte) 134;
	    puke_blue[iMore+96] = (byte) 130 ;

	    puke_red[iMore+97] = (byte) 102 ;
	    puke_green[iMore+97] = (byte) 205 ;
	    puke_blue[iMore+97] = (byte) 170 ;

	    puke_red[iMore+98] = (byte) 205 ;
	    puke_green[iMore+98] = (byte) 205 ;
	    puke_blue[iMore+98] = (byte) 180 ;

	    puke_red[iMore+99]   = (byte) 205 ;
	    puke_green[iMore+99] = (byte) 145 ;
	    puke_blue[iMore+99]  = (byte) 115 ;

	    puke_red[iMore+100]   = (byte) 0 ;
	    puke_green[iMore+100] = (byte) 229 ;
	    puke_blue[iMore+100]  = (byte) 238 ;

	    puke_red[iMore+101]   = (byte) 205 ;
	    puke_green[iMore+101] = (byte) 104 ;
	    puke_blue[iMore+101]  = (byte) 57 ;

	    puke_red[iMore+102]   = (byte) 32 ;
	    puke_green[iMore+102] = (byte) 178 ;
	    puke_blue[iMore+102]  = (byte) 170 ;

	    puke_red[iMore+103]   = (byte) 137 ;
	    puke_green[iMore+103] = (byte) 104 ;
	    puke_blue[iMore+103]  = (byte) 205 ;

	    puke_red[iMore+104]   = (byte) 139 ;
	    puke_green[iMore+104] = (byte) 69 ;
	    puke_blue[iMore+104]  = (byte) 19 ;

	    puke_red[iMore+105]   = (byte) 132 ;
	    puke_green[iMore+105] = (byte) 112 ;
	    puke_blue[iMore+105]  = (byte) 255 ;

	    puke_red[iMore+106]   = (byte) 255 ;
	    puke_green[iMore+106] = (byte) 246 ;
	    puke_blue[iMore+106]  = (byte) 143 ;

	    puke_red[iMore+107]   = (byte) 216 ;
	    puke_green[iMore+107] = (byte) 191 ;
	    puke_blue[iMore+107]  = (byte) 216 ;

	    puke_red[iMore+108]   = (byte) 154 ;
	    puke_green[iMore+108] = (byte) 50 ;
	    puke_blue[iMore+108]  = (byte) 205 ;

	    puke_red[iMore+109]   = (byte) 253 ;
	    puke_green[iMore+109] = (byte) 245 ;
	    puke_blue[iMore+109]  = (byte) 230 ;

	    puke_red[iMore+110]   = (byte) 239 ;
	    puke_green[iMore+110] = (byte) 87 ;
	    puke_blue[iMore+110]  = (byte) 66 ;

	    puke_red[iMore+111]   = (byte) 205 ;
	    puke_green[iMore+111] = (byte) 104 ;
	    puke_blue[iMore+111]  = (byte) 137 ;

	    puke_red[iMore+112]   = (byte) 102 ;
	    puke_green[iMore+112] = (byte) 205 ;
	    puke_blue[iMore+112]  = (byte) 170 ;

	    puke_red[iMore+113]   = (byte) 205 ;
	    puke_green[iMore+113] = (byte) 133 ;
	    puke_blue[iMore+113]  = (byte) 0 ;

	    puke_red[iMore+114]   = (byte) 239 ;
	    puke_green[iMore+114] = (byte) 62 ;
	    puke_blue[iMore+114]  = (byte) 47 ;

	    puke_red[iMore+115]   = (byte) 105 ;
	    puke_green[iMore+115] = (byte) 105 ;
	    puke_blue[iMore+115]  = (byte) 105 ;

	    puke_red[iMore+116]   = (byte) 139 ;
	    puke_green[iMore+116] = (byte) 125 ;
	    puke_blue[iMore+116]  = (byte) 107 ;

	    puke_red[iMore+117]   = (byte) 0 ;
	    puke_green[iMore+117] = (byte) 0 ;
	    puke_blue[iMore+117]  = (byte) 205 ;

	    puke_red[iMore+118]   = (byte) 105 ;
	    puke_green[iMore+118] = (byte) 139 ;
	    puke_blue[iMore+118]  = (byte) 34 ;

	    puke_red[iMore+119]   = (byte) 184 ;
	    puke_green[iMore+119] = (byte) 134 ;
	    puke_blue[iMore+119]  = (byte) 11 ;

	    puke_red[iMore+120]   = (byte) 70 ;
	    puke_green[iMore+120] = (byte) 130 ;
	    puke_blue[iMore+120]  = (byte) 180 ;

	    puke_red[iMore+121]   = (byte) 84 ;
	    puke_green[iMore+121] = (byte) 139 ;
	    puke_blue[iMore+121]  = (byte) 84 ;

	    puke_red[iMore+122]   = (byte) 255 ;
	    puke_green[iMore+122] = (byte) 48 ;
	    puke_blue[iMore+122]  = (byte) 48 ;

	    puke_red[iMore+123]   = (byte) 238 ;
	    puke_green[iMore+123] = (byte) 174 ;
	    puke_blue[iMore+123]  = (byte) 238 ;

	    puke_red[iMore+124]   = (byte) 112 ;
	    puke_green[iMore+124] = (byte) 128 ;
	    puke_blue[iMore+124]  = (byte) 144 ;

	    puke_red[iMore+125]   = (byte) 255 ;
	    puke_green[iMore+125] = (byte) 69 ;
	    puke_blue[iMore+125]  = (byte) 0 ;

	    puke_red[iMore+126]   = (byte) 154 ;
	    puke_green[iMore+126] = (byte) 192 ;
	    puke_blue[iMore+126]  = (byte) 205 ;

	    iMore = (byte) 125 + 126 - 2;  // This helps me do a
					   // simple string replace
					   // from the Tipsy code

	    puke_red[iMore+2] = (byte) 190 ;
	    puke_green[iMore+2] = (byte) 200 ;
	    puke_blue[iMore+2] = (byte) 255 ;

	    puke_red[iMore+3] = (byte) 255 ;
	    puke_green[iMore+3] = (byte) 63 ;
	    puke_blue[iMore+3] = (byte) 63 ;

	    puke_red[iMore+4] = (byte) 255 ;
	    puke_green[iMore+4] = (byte) 255 ;
	    puke_blue[iMore+4] = (byte) 140 ;

	// Beyond family colors 
	    puke_red[iMore+5] = (byte) 127 ;
	    puke_green[iMore+5] = (byte) 255 ;
	    puke_blue[iMore+5] = (byte) 212 ;

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
	
    /*
     * Sets the colormap to "Puke"  (default in Tipsy)
     */
    private void setColorModelPuke() {
	//copy in the puke colors
        for (int i = 0; i < 256; i++) {
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
