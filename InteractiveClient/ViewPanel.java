//
//  ViewPanel.java
//  
//
//  Created by Greg Stinson on Thu Oct 16 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.net.UnknownHostException;

public class ViewPanel extends JPanel implements MouseListener, MouseMotionListener {

    Simulation s;
    Config config;
    RightClickMenu rcm;
    Vector3D x, y, z, origin;
    int height, width;
    double boxSize;
    MemoryImageSource source;
    byte [] pixels;
    JLabel display;
    double angleLeft, angleCcw, angleUp;
    Rectangle rect;
    
    public ViewPanel( Simulation sim, int w, int h,double bs, Vector3D oh ){
        s = sim;
        boxSize = bs;
        x = new Vector3D(0, boxSize*0.5, 0);
        y = new Vector3D(0, 0, boxSize*0.5);
        z = new Vector3D(x.cross(y));
        origin = oh;
        width = w;
        height = h;

        pixels = new byte[width * height];
        display = new JLabel();
        source = new MemoryImageSource(width, height, s.cm, pixels, 0, width);
        source.setAnimated(true);
        display.setIcon(new ImageIcon(display.createImage(source)));
        add(display, BorderLayout.CENTER);
        addMouseListener(this);
        addMouseMotionListener(this);

        s.ccs.addRequest(new ActivateGroup( "All", this) );
        rcm = new RightClickMenu(s, this);
    }
    
    public void zoom( double zoomFactor ){
  //      System.out.println("zoom by: "+zoomFactor);
        x = x.scalarMultiply(zoomFactor);
        y = y.scalarMultiply(zoomFactor);
        z = z.scalarMultiply(zoomFactor);
        getNewImage();
        s.ccs.addRequest( new Center() );
    }
    
    public void center(){
        s.ccs.addRequest( new Center() );
    }
    
    public void rotateLeft( double theta ){
        x.rotate(theta,y.unitVector());
        z = x.cross(y);
        getNewImage();
    }

    public void rotateCcw( double theta ){
        y.rotate(theta,z.unitVector());
        x.rotate(theta,z.unitVector());
        getNewImage();
    }

    public void rotateUp( double theta ){
        y.rotate(theta,x.unitVector());
        z = x.cross(y);
        getNewImage();
    }

    public void redisplay () {
        source.newPixels(pixels,s.cm,0,width);
    }

    public void getNewImage () {
        s.ccs.addRequest(new ImageRequest(), true);
    }

    public void writePng () {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                FileOutputStream fos = new FileOutputStream(file);
                PngEncoder png = new PngEncoder(display.createImage(source), false);
                byte[] pngBytes = png.pngEncode();
                fos.write(pngBytes);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException fnfe){System.out.println(fnfe);}
            catch ( IOException ioe) { System.out.println(ioe); }
        } else {
            System.out.println("Save command cancelled by user.");
        }
    }

    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
        if (s.groupSelecting){
            rect = new Rectangle(e.getX(),e.getY(),0,0);
            System.out.println("Rectangle created: "+rect.x+" "+rect.y+" "+rect.width+" "+rect.height);
        }
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
/*        if (s.groupSelecting && 
            (e.getModifiers() == (MouseEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK) ) ) {
       //     repaint();
            Graphics graph = this.getGraphics();
            graph.setColor(Color.green);
            double x = e.getX()-rect.getX();
            double y = e.getY()-rect.getY();
            rect.setSize((int)x, (int)y);
            graph.drawRect(rect.x,rect.y,rect.width,rect.height);
            System.out.println("Draw rectangle: "+rect.x+" "+rect.y+" "+rect.width+" "+rect.height);
        }*/
    }

    public void mouseClicked(MouseEvent e) {
        maybeShowPopup(e);
        switch ( e.getModifiers() ) {
            case MouseEvent.BUTTON1_MASK:
                origin = origin.plus(x.scalarMultiply(2.0*(e.getX()-(getWidth()*0.5))/getWidth()));
                origin = origin.plus(y.scalarMultiply(2.0*((getHeight()*0.5)-e.getY())/getHeight()));
                zoom(1.0/(e.getClickCount()+1.0));
                break;
            case MouseEvent.BUTTON2_MASK:
                origin = origin.plus(x.scalarMultiply(2.0*(e.getX()-(getWidth()*0.5))/getWidth()));
                origin = origin.plus(y.scalarMultiply(2.0*((getHeight()*0.5)-e.getY())/getHeight()));
                zoom(e.getClickCount()+1.0);
                break;
            default:
                break;
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (s.groupSelecting && 
            (e.getModifiers() == (MouseEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK) ) ) {
            Graphics graph = this.getGraphics();
            graph.setColor(Color.green);
            double x = e.getX()-rect.getX();
            double y = e.getY()-rect.getY();
            rect.setSize((int)x, (int)y);
            graph.drawRect(rect.x,rect.y,rect.width,rect.height);
            repaint();
            System.out.println("Draw rectangle: "+rect.x+" "+rect.y+" "+rect.width+" "+rect.height);
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
        // needed if menu is updated, but I'm thinking it won't be now
            rcm.refresh();
            rcm.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    private class ImageRequest extends CcsThread.request {

        public ImageRequest() {
                super("lvImage", null);
     //           System.out.println("requesting new image");
                setData(encodeRequest());
        }

        public void handleReply(byte[] data) {
                displayImage(data);
        }
    }

    private class Center extends CcsThread.request {

        public Center() {
            super("Center", null);
            setData(encodeCenterRequest());
        }

        public void handleReply(byte[] data) {
            DataInputStream dis = new DataInputStream( new ByteArrayInputStream(data));
            try {
                double m = dis.readDouble();
                origin = origin.plus(z.unitVector().scalarMultiply( m ));
   //             System.out.println("Server response: "+m+"  New origin for rotation: "+origin);
            } catch (IOException ioe) {System.err.println("ioexception:"+ioe);}
        }
    }

    private byte[] encodeCenterRequest() {
        // for mapping System.out.println("ViewingPanel: encodeRequest");
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        try {
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(s.centerMethod); /*Client version*/
            dos.writeInt(1); /*Request type*/
            dos.writeInt(width);
            dos.writeInt(height);
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
   //         System.out.println("x:"+x.toString()+" y:"+y.toString()+" z:"+z.toString()+" or:"+origin.toString());
        } catch(IOException e) {
            System.err.println("Couldn't encode request!");
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
    private byte[] encodeRequest() {
        // for mapping System.out.println("ViewingPanel: encodeRequest");
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        try {
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(1); /*Client version*/
            dos.writeInt(1); /*Request type*/
            dos.writeInt(width);
            dos.writeInt(height);
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
  //          System.out.println("x:"+x.toString()+" y:"+y.toString()+" z:"+z.toString()+" or:"+origin.toString());
        } catch(IOException e) {
            System.err.println("Couldn't encode request!");
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private void displayImage(byte[] data) {
        try {
            if(/*resizedImage ||*/ data.length != width * height) {
                source = new MemoryImageSource(width, height, s.cm, data, 0, width);
                source.setAnimated(true);
                display.setIcon(new ImageIcon(display.createImage(source)));
//                if(data.length == width * height)
//                resizedImage = false;
                pixels = data;
            } else {
                source.newPixels(data, s.cm, 0, width);
                pixels = data;
            }
        } catch (ArrayIndexOutOfBoundsException bla) {
                bla.printStackTrace();
        }
    }
}
