//SimulationView.java

import charm.ccs.CcsThread;
import charm.ccs.PythonAbstract;
import charm.ccs.PythonExecute;
import charm.ccs.PythonPrint;
import charm.ccs.PythonFinished;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.io.*;

public class SimulationView extends JLabel 
    implements ActionListener, 
	       MouseInputListener, 
	       ComponentListener {
	WindowManager windowManager;
	CcsThread ccs;
	Vector3D x, y, z, origin;  // Axes directions and origin of simulation
	double boxSize;
	double zoomFactor;
	int activeColoring = 0;
	//int activeGroup = 0;
	String activeGroup = "All";
	int centeringMethod = 2;
	int radius = 0;
	double minMass = 0;
	double maxMass = 1;
	int doSplatter = 0;
	int selectState = 0;	// State of box selection
	Vector3D selectCorner, selectEdge1, // Box vectors
	         selectEdge2, selectEdge3;
	double selectRadius;	// sphere radius
	int selStartX, selStartY;
	int oldCurrentX, oldCurrentY;
	
	int height, width;
	MemoryImageSource source;
	byte[] pixels;
	JLabel display;

	double angleLeft, angleCcw, angleUp;
	Rectangle rect;

	ColorModel colorModel;
	RightClickMenu rcm;
	GroupQuery gquery;
	
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
        
	zall();
		
        addMouseListener(this);
        addMouseMotionListener(this);
	addComponentListener(this);
		
	rcm = new RightClickMenu(windowManager, this);
	gquery = new GroupQuery(this);
	}
	
	public void redisplay(ColorModel cm) {
		colorModel = cm;
		source.newPixels(pixels, colorModel, 0, width);
		setIcon(new ImageIcon(createImage(source)));
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
		return;
		}
	    if(e.getModifiers()
	       == (MouseEvent.BUTTON1_MASK|InputEvent.SHIFT_MASK)) {
		// box selection
		    switch(selectState) {
		    case 0:	// starting box selection
			selectCorner = coordEvent(e);
			selStartX = e.getX();
			selStartY = e.getY();
			oldCurrentX = selStartX;
			oldCurrentY = selStartY;
			selectState = 1;
			System.out.println("Corner 1: " + selectCorner.toString());
			break;
		    case 2:
			// get 3rd component of box origin
			Vector3D dz = y.scalarMultiply(y.dot(coordEvent(e).minus(origin))
						       /y.lengthSquared());
			selectCorner = selectCorner.plus(dz);
			
			selectEdge3 = coordEvent(e);
			selectState = 3;
			selStartY = e.getY();
			oldCurrentY = selStartY;
			System.out.println("Corner 3: " + selectEdge3.toString());
			break;
		    default:
			System.out.println("Bad state in press: " + selectState);
			}
		return;
		}
	    if(e.getModifiers()
	       == (MouseEvent.BUTTON1_MASK|InputEvent.CTRL_MASK)) {
		// sphere selection
		    switch(selectState) {
		    case 0:
			selectCorner = coordEvent(e);
			selStartX = e.getX();
			selStartY = e.getY();
			oldCurrentX = selStartX;
			oldCurrentY = selStartY;
			selectState = 4;
			System.out.println("Center: " + selectCorner.toString());
			break;
		    default:
			System.out.println("Bad state in press: " + selectState);
			}
		}
	}
	
    public void mouseReleased(MouseEvent e) {
	    switch(selectState) {
		    case 0:
			break;
		
		    case 1:	// end of X-Y box selection
			selectEdge1 = coordEvent(e);
			selectState = 2;
			// Turn corners into direction vectors
			Vector3D d = selectEdge1.minus(selectCorner);
			selectEdge1 = x.scalarMultiply(x.dot(d)
							 /x.lengthSquared());
			selectEdge2 = y.scalarMultiply(y.dot(d)
							 /y.lengthSquared());
			
			System.out.println("Corner 2: " + selectEdge1.toString());
			System.out.println("Corner 3: " + selectEdge2.toString());
			rotateUp(-0.5*Math.PI);
			getNewImage();
			getNewDepth();
			selStartX = xCoord(selectCorner);
			selStartY = -1;
			oldCurrentX = xCoord(selectCorner.plus(selectEdge1));
			oldCurrentY = -1;
			break;
		    case 3:	// end of Z box selection
			selectEdge3 = y.scalarMultiply(y.dot(coordEvent(e).minus(selectEdge3))
						      /y.lengthSquared());
			System.out.println("Corner: " + selectCorner.toString());
			System.out.println("Dir 1: " + selectEdge1.toString());
			System.out.println("Dir 2: " + selectEdge2.toString());
			System.out.println("Dir 3: " + selectEdge3.toString());
			gquery.setVisible(true);
			break;
		    case 4:	// end of Sphere
			selectRadius = (coordEvent(e).minus(selectCorner)).length();
			System.out.println("Corner: " + selectCorner.toString());
			System.out.println("Radius: " + selectRadius);
			gquery.setVisible(true);
			break;
		    default:
			System.out.println("Bad state in release: " + selectState);
		}
    }

    Vector3D coordEvent(MouseEvent e) // get simulation coordinates of
				      // mouse click
    {
	return origin.plus(x.scalarMultiply(2.0 * (e.getX() - (width - 1) / 2.0) / (width - 1))).plus(y.scalarMultiply(2.0 * ((height - 1) / 2.0 - e.getY()) / (height - 1)));
    }
    
    public void mouseDragged(MouseEvent e) {
	if(selectState == 0) return;
	
	Graphics g = getGraphics();
	g.setXORMode(Color.green);

	if(selectState == 3) {	// selecting "z" bounds
	    int rectX = selStartX < oldCurrentX ? selStartX : oldCurrentX;
	    int deltaX = Math.abs(oldCurrentX - selStartX);
	    int rectY = selStartY < oldCurrentY ? selStartY : oldCurrentY;
	    int deltaY = Math.abs(oldCurrentY - selStartY);
	    g.drawRect(rectX, rectY, deltaX, deltaY);

	    int currentY = e.getY();
	    rectY = selStartY < currentY ? selStartY : currentY;
	    deltaY = Math.abs(currentY - selStartY);
	    g.drawRect(rectX, rectY, deltaX, deltaY);
	    oldCurrentY = currentY;
	    }
	else if(selectState == 1) { // selecting x and y bounds
	    int rectX = selStartX < oldCurrentX ? selStartX : oldCurrentX;
	    int deltaX = Math.abs(oldCurrentX - selStartX);
	    int rectY = selStartY < oldCurrentY ? selStartY : oldCurrentY;
	    int deltaY = Math.abs(oldCurrentY - selStartY);
	    g.drawRect(rectX, rectY, deltaX, deltaY);

	    int currentX = e.getX();
	    int currentY = e.getY();
	    rectX = selStartX < currentX ? selStartX : currentX;
	    deltaX = Math.abs(currentX - selStartX);
	    rectY = selStartY < currentY ? selStartY : currentY;
	    deltaY = Math.abs(currentY - selStartY);
	    g.drawRect(rectX, rectY, deltaX, deltaY);
	    oldCurrentX = currentX;
	    oldCurrentY = currentY;
	    }
	else { // selecting sphere
	    int size = (int) Math.sqrt((selStartX - oldCurrentX)
				 *(selStartX - oldCurrentX)
				 + (selStartY - oldCurrentY)
				 *(selStartY - oldCurrentY));
	    
	    g.drawArc(selStartX-size, selStartY-size, 2*size, 2*size, 0, 360);
	    int currentX = e.getX();
	    int currentY = e.getY();
	    size = (int) Math.sqrt((selStartX - currentX)
				 *(selStartX - currentX)
				 + (selStartY - currentY)
				 *(selStartY - currentY));
	    g.drawArc(selStartX-size, selStartY-size, 2*size, 2*size, 0, 360);
	    oldCurrentX = currentX;
	    oldCurrentY = currentY;
	    }
    }
    
    public void mouseMoved(MouseEvent e) { 
	if(selectState == 0) return;
	
	if(selectState == 2) {	// box is rotated, now draw a line
	    Graphics g = getGraphics();
	    g.setXORMode(Color.green);
	    g.drawLine(selStartX, oldCurrentY, oldCurrentX, oldCurrentY);
	    int currentY = e.getY();
	    g.drawLine(selStartX, currentY, oldCurrentX, currentY);
	    oldCurrentY = currentY;
	    
	    return;
	    }
    }

    int xCoord(Vector3D c) 	// Return x pixel value for a
				// simulation coordinate
    {
	double xval = x.dot(c.minus(origin))/x.lengthSquared();
	return (new Double(width*(xval + 1.0)*.5)).intValue();
    }
    int yCoord(Vector3D c) 	// Return a y pixel value for a
				// simulation coordinate
    {
	double yval = y.dot(c.minus(origin))/y.lengthSquared();
	return (new Double(width*(yval + 1.0)*.5)).intValue();
    }
	
    public void mouseClicked(MouseEvent e) {
 		switch(e.getModifiers()) {
		case MouseEvent.BUTTON1_MASK:  // Zoom in.
		    if(selectState == 1 || selectState == 3)
			return;
		    origin = coordEvent(e);
		    zoom(1.0 / zoomFactor);
		    if(selectState == 2) {
			selStartX = xCoord(selectCorner);
			selStartY = -1;
			oldCurrentX = xCoord(selectCorner.plus(selectEdge1));
			oldCurrentY = -1;
			}
		    break;
		case MouseEvent.BUTTON2_MASK:  // Zoom out
		    if(selectState == 1 || selectState == 3)
			return;
		    origin = coordEvent(e);
		    zoom(zoomFactor);
		    if(selectState == 2) {
			selStartX = xCoord(selectCorner);
			selStartY = -1;
			oldCurrentX = xCoord(selectCorner.plus(selectEdge1));
			oldCurrentY = -1;
			}
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
		else if(command.equals("executeCode"))
			windowManager.addCodeFrame();
		else if(command.equals("executeLocalCode"))
			windowManager.addLocalCodeFrame();
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
			// could be a while, lets wait
			super("lvImage", encodeRequest());
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}

		public void handleReply(byte[] data) {
			setCursor(Cursor.getDefaultCursor());
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
			//dos.writeInt(activeGroup);
			dos.writeInt(activeGroup.length());
			dos.writeBytes(activeGroup);
			dos.writeDouble(minMass);
			dos.writeDouble(maxMass);
			dos.writeInt(doSplatter);
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
	    setIcon(new ImageIcon(createImage(source)));
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

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }
    public void makeBox(String groupName) 
    {
	if(selectState == 3) {
	    PythonExecute code = new PythonExecute("charm.createGroupAttributeBox(\""
					   + groupName + "\", \"All\", \"position\","
					   + selectCorner.toPyString() + ","
					   + selectEdge1.toPyString() + ","
					   + selectEdge2.toPyString() + ","
					   + selectEdge3.toPyString() + ")\n",
						   false, true, 0);
	    windowManager.ccs.addRequest(new ExecutePythonCode(code.pack()));
	    }
	else if(selectState == 4) {
	    PythonExecute code = new PythonExecute("charm.createGroupAttributeSphere(\""
					   + groupName + "\", \"All\", \"position\","
					   + selectCorner.toPyString() + ","
					   + selectRadius + ")\n",
					       false, true, 0);
	    windowManager.ccs.addRequest(new ExecutePythonCode(code.pack()));
	    }
	selectState = 0;
    }
	private class ExecutePythonCode extends CcsThread.request {
		public ExecutePythonCode(byte[] s) {
			super("ExecutePythonCode", s);
		}
		
		public void handleReply(byte[] data) {
			String result = new String(data);
			System.out.println("Return from code execution: \"" + result + "\"");
		}
	}
}
