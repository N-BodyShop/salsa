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
    byte [] imagebytes;
    int cmap_size, cbstartx, width;
    byte[] puke_red;
    byte[] puke_green;
    byte[] puke_blue;
    
    public ColorBarPanel( Simulation sim, int h, int w ){
        s = sim;
        width = w;
        initPukeCM();
        s.cm = createWRBBColorModel();
        imagebytes = new byte[w * h];
        byte value;
        for(int i = 0; i < w; i++) {
            value = (byte) (s.numberOfColors + (256.0 - s.numberOfColors) * i / w);
            for(int j = 0; j < h; j++) 
                imagebytes[j * w + i] = value;
        }
        
        cbpm = new CBPopupMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        JLabel cbl = new JLabel();
        cbsource = new MemoryImageSource(w, h, s.cm, imagebytes, 0, w);
        cbsource.setAnimated(true);
        cbl.setIcon(new ImageIcon(createImage(cbsource)));
        add(cbl);
    }
    
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
        cbstartx = e.getX();
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
/*        System.out.println("Translate: "+(e.getX() - cbstartx)*cmap_size/width+ "=("+(e.getX()-cbstartx)+")*"+cmap_size+"/"+width);
        s.cm = translateCM( (e.getX() - cbstartx)*cmap_size/width );
        cbsource.newPixels(imagebytes,s.cm,0,width);
        vp.redisplay();*/
    }

    public void mouseClicked(MouseEvent e) {
        maybeShowPopup(e);
        if ( e.getClickCount() == 2 ){ 
            invertCM();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) 
            cbpm.show(e.getComponent(), e.getX(), e.getY());
    }
    
    public void mouseDragged(MouseEvent e) {
        translateCM( (cbstartx - e.getX())*cmap_size/width );
        cbstartx = e.getX();
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        translateCM(e.getWheelRotation());
    }

    private class CBPopupMenu extends JPopupMenu implements ActionListener {
        
        public CBPopupMenu( ) {
            super();
            
            JMenuItem item;
            item = new JMenuItem("standard");
            item.setActionCommand("standard");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("invert");
            item.setActionCommand("invert");
            item.addActionListener(this);
            add(item);
            item = new JMenuItem("rainbow");
            item.setActionCommand("rainbow");
            item.addActionListener(this);
            add(item);
        }
        
        public void actionPerformed(ActionEvent e){
            String command =  e.getActionCommand();
            if ( command.equals("standard") ) { 
                s.cm = createWRBBColorModel();
            }
            else if (command.equals("invert"))  { 
                invertCM();
            }
            else if (command.equals("rainbow"))  { 
                s.cm = rainbowCM();
            }
            cbsource.newPixels(imagebytes,s.cm,0,width);
            vp.redisplay();
        }
        
    }

    private void initPukeCM() {
        puke_red = new byte[5];
        puke_green = new byte[5];
        puke_blue = new byte[5];

        puke_red[0] = 0;
        puke_green[0] = 0;
        puke_blue[0] = 0;
        puke_red[1] = 0;
        puke_green[1] = (byte) 255;
        puke_blue[1] = 0;
        puke_red[2] = (byte) 190;
        puke_green[2] = (byte) 200;
        puke_blue[2] = (byte) 255;
        puke_red[3] = (byte) 255;
        puke_green[3] = (byte) 63;
        puke_blue[3] = (byte) 63;
        puke_red[4] = (byte) 255;
        puke_green[4] = (byte) 255;
        puke_blue[4] = (byte) 140;
    }

    /*
    * returns a standard color map
    */
    private ColorModel createWRBBColorModel() {
        cmap_size = 256 - s.numberOfColors;
        cm_red = new byte[256];
        cm_green = new byte[256];
        cm_blue = new byte[256];
        
        int i;
        int nextColor = s.numberOfColors;
        int chunk_size = ((cmap_size - 1) / 5);

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

        while(nextColor < cmap_size + 1) {
                cm_red[nextColor] = (byte) 255;
                cm_green[nextColor] = (byte) 255;
                cm_blue[nextColor++] = (byte) 255;
        }

        for (i = 2; i < s.numberOfColors; i++) {
            cm_red[i] = puke_red[i];
            cm_green[i] = puke_green[i];
            cm_blue[i] = puke_blue[i];
        }
        return new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }
    
    /*
    * returns an inverted color map
    */
    private void invertCM(){
        byte temp;
        for(int i = 0; i < (255 - 5) / 2; ++i) {
            temp = cm_red[i + 5];
            cm_red[i + 5] = cm_red[255 - i];
            cm_red[255 - i] = temp;
            temp = cm_green[i + 5];
            cm_green[i + 5] = cm_green[255 - i];
            cm_green[255 - i] = temp;
            temp = cm_blue[i + 5];
            cm_blue[i + 5] = cm_blue[255 - i];
            cm_blue[255 - i] = temp;
        }
        s.cm = new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
        cbsource.newPixels(imagebytes,s.cm,0,width);
        vp.redisplay();
    }
    

    /*
    * returns the current color model translated according to mousedrags
    */
    private void translateCM( int diff ){
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
    System.out.println("Crash 5");
        cbsource.newPixels(imagebytes,s.cm,0,width);
    System.out.println("Crash 6");
        vp.redisplay();
    System.out.println("Crash 7");
    }


    /****************************************************************************/
    /*
    * returns a new rainbow colormap
    */
    private ColorModel rainbowCM(){
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
            * The rainbow color map only has 125 elements, so this method 
            * sets two indexes in wrbb arrays for every one color in the 125 colors stored in the
            * rainbow arrays, resulting in 250 indexes of wrbb filled...the last couple indexes at the end are
            * taken care of in the while loop that follows
            */
            for(i = 1, j = 1; i < 125; i++, j = j+2){
                    cm_red[j] = rainbow_red[i];
                    cm_red[j+1] = rainbow_red[i];
                    cm_green[j] = rainbow_green[i];
                    cm_green[j+1] = rainbow_green[i];
                    cm_blue[j] = rainbow_blue[i];
                    cm_blue[j+1] = rainbow_blue[i];
            }

            while(j<255){
                    cm_red[j] = rainbow_red[124];
                    cm_green[j] = rainbow_green[124];
                    cm_blue[j] = rainbow_blue[124];
                    j++;
            }
/*            colorMapType = "rainbow";*/
            return new IndexColorModel(8, 256, cm_red, cm_green, cm_blue);
    }
 
}
