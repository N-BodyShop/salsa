//SimulationView.java

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.io.*;

public class SimulationView extends JLabel 
							implements ActionListener, 
									   MouseListener, 
									   ComponentListener {
	WindowManager windowManager;
	CcsThread ccs;
	Vector3D x, y, z, origin;
	double boxSize;
	double zoomFactor;
	int activeColoring = 0;
	int activeGroup = 0;
	int centeringMethod = 2;
	int radius = 0;
	
	int height, width;
	MemoryImageSource source;
	byte[] pixels;
	JLabel display;

	double angleLeft, angleCcw, angleUp;
	Rectangle rect;

	ColorModel colorModel;
	RightClickMenu rcm;
	
	EventListenerList listenerList = new EventListenerList();
	ViewEvent viewEvent = null;

	public SimulationView(WindowManager wm, ColorModel cm, int w, int h) {
		
		windowManager = wm;
		colorModel = cm;
		width = w;
		height = h;
		
		boxSize = windowManager.sim.boxSize;
		zoomFactor = 2;
		
		//a viewing window gets its own CcsThread, so the queues can operate independently
		ccs = new CcsThread(windowManager.ccs);
		
		setSize(width, height);
		
		pixels = new byte[width * height];
        source = new MemoryImageSource(width, height, colorModel, pixels, 0, width);
        source.setAnimated(true);
        setIcon(new ImageIcon(createImage(source)));
        
		xall();
		
        addMouseListener(this);
		addComponentListener(this);
		
		rcm = new RightClickMenu(windowManager, this);
	}
	
	public void redisplay(ColorModel cm) {
		colorModel = cm;
		source.newPixels(pixels, colorModel, 0, width);
	}
	
	public void getNewImage() {
		ccs.addRequest(new ImageRequest(), true);
	}
	
	public void getNewDepth() {
		ccs.addRequest(new CenterRequest());
	}
	
	public void zoom(double fac) {
		x = x.scalarMultiply(fac);
		y = y.scalarMultiply(fac);
		z = z.scalarMultiply(fac);
		getNewImage();
		getNewDepth();
	}
	
	public void mousePressed(MouseEvent e) {
		if(e.isPopupTrigger()) {
            //rcm.refresh();
            rcm.show(e.getComponent(), e.getX(), e.getY());
        }
	}
	
	public void mouseClicked(MouseEvent e) {
 		switch(e.getModifiers()) {
			case MouseEvent.BUTTON1_MASK:
				origin = origin.plus(x.scalarMultiply(2.0 * (e.getX() - (width - 1) / 2.0) / (width - 1))).plus(y.scalarMultiply(2.0 * ((height - 1) / 2.0 - e.getY()) / (height - 1)));
				zoom(1.0 / zoomFactor);
				break;
			case MouseEvent.BUTTON2_MASK:
				origin = origin.plus(x.scalarMultiply(2.0 * (e.getX() - (width - 1) / 2.0) / (width - 1))).plus(y.scalarMultiply(2.0 * ((height - 1) / 2.0 - e.getY()) / (height - 1)));
				zoom(zoomFactor);
				break;
			default:
				break;
		}
	}
			
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("refresh")) {
			getNewImage();
			getNewDepth();
		} else if(command.equals("xall"))
			xall();
		else if(command.equals("yall"))
			yall();
		else if(command.equals("zall"))
			zall();
		else if(command.equals("manageAttributes"))
			windowManager.attributeManager.setVisible(true);
		else if(command.equals("manageColoring"))
			windowManager.coloringManager.setVisible(true);
		else if(command.equals("manageGroups"))
			windowManager.groupManager.setVisible(true);
		else if(command.equals("screenCapture"))
			writePng();
	}
	
	//rotate the top half toward you, bottom away
    public void rotateUp(double theta) {
        y.rotate(-theta, x.unitVector());
        z = x.cross(y);
    }
	
	//rotate the right half toward you, left away
    public void rotateRight(double theta) {
        x.rotate(theta, y.unitVector());
        z = x.cross(y);
    }
	
	//rotate the axes clockwise
    public void rotateClock(double theta) {
        y.rotate(theta, z.unitVector());
        x.rotate(theta, z.unitVector());
    }
	
	public void rotationPerformed(RotationEvent e) {
		x.rotate(e.theta, e.rotationAxis);
		y.rotate(e.theta, e.rotationAxis);
		z = x.cross(y);
	}
	
    public void xall() {
		//invariant: must keep \delta = x.length() / width = y.length()  / height fixed to maintain aspect ratio
        origin = new Vector3D(windowManager.sim.origin);
		double delta = boxSize / (height < width ? height : width);
		x = new Vector3D(0, width * delta / 2.0, 0);
		y = new Vector3D(0, 0, height * delta / 2.0);
        z = x.cross(y);
		getNewImage();
		fireViewReset();
    }
	
    public void yall() {
        origin = new Vector3D(windowManager.sim.origin);
		double delta = boxSize / (height < width ? height : width);
        x = new Vector3D(-width * delta / 2.0, 0, 0);
        y = new Vector3D(0, 0, height * delta / 2.0);
        z = x.cross(y);
		getNewImage();
		fireViewReset();
    }
	
    public void zall() {
        origin = new Vector3D(windowManager.sim.origin);
		double delta = boxSize / (height < width ? height : width);
        x = new Vector3D(width * delta / 2.0, 0, 0);
        y = new Vector3D(0, height * delta / 2.0, 0);
        z = x.cross(y);
		getNewImage();
		fireViewReset();
    }
	
	public void componentResized(ComponentEvent e) {
		int newWidth = getWidth();
		int newHeight = getHeight();
		x = x.scalarMultiply((double) newWidth / width);
		y = y.scalarMultiply((double) newHeight / height);
		z = x.cross(y);
		width = newWidth;
		height = newHeight;
		getNewImage();
		if(pixels.length < width * height)
			pixels = new byte[width * height];
        source = new MemoryImageSource(width, height, colorModel, pixels, 0, width);
        source.setAnimated(true);
        setIcon(new ImageIcon(createImage(source)));
	}
	
	private class ImageRequest extends CcsThread.request {
		public ImageRequest() {
			super("lvImage", encodeRequest());
		}

		public void handleReply(byte[] data) {
			displayImage(data);
		}
	}
	
	private class CenterRequest extends CcsThread.request {
		public CenterRequest() {
			super("Center", encodeRequest());
		}

		public void handleReply(byte[] data) {
			try {
				double val = Double.parseDouble(new String(data));
				origin = origin.plus(z.unitVector().scalarMultiply(val));
			} catch(NumberFormatException e) {
				System.err.println("Problem decoding the z value.  String: " + (new String(data)));
			}
		}
	}
	
    private byte[] encodeRequest() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(136);
        try {
			DataOutputStream dos = new DataOutputStream(baos);
			// These fields go into the liveVizRequest
			dos.writeInt(1); /* version */
			dos.writeInt(1); /* code */
			dos.writeInt(width);
			dos.writeInt(height);
			
			// These fields go into the MyVizRequest
			dos.writeInt(activeColoring);
			dos.writeInt(radius);
			dos.writeInt(width); //encoded twice, for convenience
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
			dos.writeInt(centeringMethod);
			dos.writeInt(activeGroup);
			//System.out.println("x:"+x.toString()+" y:"+y.toString()+" z:"+z.toString()+" or:"+origin.toString());
        } catch(IOException e) {
            System.err.println("Couldn't encode request!");
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private void displayImage(byte[] data) {
        if(data.length != width * height) {
			//this could happen if we've resized between request and receipt of image
			getNewImage();
        } else {
            pixels = data;
            source.newPixels(pixels, colorModel, 0, width);
        }
    }
	
	public void addViewListener(ViewListener l) {
		listenerList.add(ViewListener.class, l);
	}

	public void removeViewListener(ViewListener l) {
		listenerList.remove(ViewListener.class, l);
	}
	
	protected void fireViewReset() {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for(int i = listeners.length - 2; i >= 0; i -= 2) {
			if(listeners[i] == ViewListener.class) {
				// Lazily create the event:
				if(viewEvent == null)
					viewEvent = new ViewEvent(this);
				((ViewListener) listeners[i + 1]).viewReset(viewEvent);
			}
		}
	}
	
    public void writePng() {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showSaveDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                FileOutputStream fos = new FileOutputStream(file);
                PngEncoder png = new PngEncoder(createImage(source), false);
                byte[] pngBytes = png.pngEncode();
                fos.write(pngBytes);
                fos.flush();
                fos.close();
            } catch(FileNotFoundException fnfe) {
				System.out.println(fnfe);
			} catch(IOException ioe) {
				System.out.println(ioe);
			}
        } else {
            System.out.println("Screen capture cancelled by user.");
        }
    }

	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }
	
}
