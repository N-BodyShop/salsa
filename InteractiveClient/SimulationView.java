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
import java.nio.ByteBuffer;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

import com.sun.opengl.util.Animator;
import com.sun.opengl.util.BufferUtil;

public class SimulationView extends JPanel implements ActionListener, MouseInputListener, MouseMotionListener, MouseWheelListener, ComponentListener, GLEventListener
{
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
	
	ColorBarPanel colorBar;
	
	/* OpenGL variables */
	int texture2D;
	int texture3D; /* textures used for 3D rendering (only) */
	int screen, colortable; 
	int framebuffer; /* framebuffer object */
	int texture[]=new int[1];
	boolean isNewImageData=true; /* need to regenerate texture3D */
	boolean mode3D=false;
	
	ByteBuffer b;
	ByteBuffer b2;
	ByteBuffer b3;
	int width3D, height3D, depth3D;
	int width2D, height2D;
	
	float factor=1.0f;
	float factorFor3D=1.0f;
	Point rotationPoint;
	int reget3DImageCounter=0;
	int reget3DImageLimit=6;
	
	boolean requestLock=false;
	CcsThread.request nextRequest=null;
	boolean requestLock3D=false;
	CcsThread.request nextRequest3D=null;
	
	Vector3D ox, oy, oz;
	
	int my_program;
	
	public SimulationView(WindowManager wm, int w, int h, ColorBarPanel cbp)
	{
		super(new BorderLayout());
		windowManager = wm;
		width = w;
		height = h;

		boxSize = windowManager.sim.boxSize;
		zoomFactor = 2;
		
		//a viewing window gets its own CcsThread, so the queues can operate independently
		ccs = new CcsThread(windowManager.ccs);
		
		this.setMaximumSize(new Dimension(w, h));
		this.setMinimumSize(new Dimension(w, h));
		
		colorBar=cbp;
		//Quick Hack
		width3D=height3D=depth3D=256;
		width2D=w;
		height2D=h;
		pixels = new byte[width2D*height2D];
		b=BufferUtil.newByteBuffer(width3D*height3D*depth3D);
		b2=BufferUtil.newByteBuffer(width2D*height2D*3);
		b3=BufferUtil.newByteBuffer(256*3);
		
		zall();
		ox=x.scalarMultiply(1);
		oy=y.scalarMultiply(1);
		oz=z.scalarMultiply(1);
		ccs.addRequest(new Image3DRequest(), true);
		
		GLCapabilities glcaps = new GLCapabilities();
	    glcaps.setDoubleBuffered(true);
	    glcaps.setHardwareAccelerated(true);
		GLJPanel glcanvas=new GLJPanel(glcaps);
		glcanvas.setPreferredSize(new Dimension(width,height));
	    glcanvas.addGLEventListener(this);
	    glcanvas.addMouseListener(this);
	    glcanvas.addMouseMotionListener(this);
	    glcanvas.addMouseWheelListener(this);
	    
	    /*GLCanvas glcanvas2=new GLCanvas(glcaps);
	    glcanvas2.setPreferredSize(new Dimension(width,height));
	    glcanvas2.setName("3D");
	    glcanvas2.addGLEventListener(this);
	    glcanvas2.addMouseListener(this);
	    glcanvas2.addMouseMotionListener(this);
	    glcanvas2.addMouseWheelListener(this);
	    this.add(glcanvas2, BorderLayout.EAST);*/
	    this.add(glcanvas, BorderLayout.WEST);
	    
        addComponentListener(this);
		
        rcm = new RightClickMenu(windowManager, this);
        gquery = new GroupQuery(this);
	}
	
	public void redisplay(ColorModel cm) {
		this.repaint();
	}

/* The ToolBarPanel calls this function when the user selects e.g. a new coloring */
	public void getNewImage() {
		request2D();
		request3D();
	}
	
	public void getNewDepth() {
		ccs.addRequest(new CenterRequest());
	}
	
	public void zoom(double fac) {
		//zall();
		x = x.scalarMultiply(fac);
		y = y.scalarMultiply(fac);
		z = z.scalarMultiply(fac);
		//getNewDepth();
	}
	
	public void orthoNormalizeZVector()
	{
		 z=(((x.cross(y)).unitVector()).scalarMultiply((x.length()+y.length())/2));
		 y=(((z.cross(x)).unitVector()).scalarMultiply((x.length()+z.length())/2));
		 x=(((y.cross(z)).unitVector()).scalarMultiply((y.length()+z.length())/2));
	}
	
	public void mousePressed(MouseEvent e) {
	    if(e.isPopupTrigger()) {
            //rcm.refresh();
		rcm.show(e.getComponent(), e.getX(), e.getY());
		return;
		}
	    /*if(e.getModifiers()
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
		}*/
	    rotationPoint=e.getPoint();
	    mode3D=true;
	    if((reget3DImageCounter>=reget3DImageLimit||reget3DImageCounter<=-reget3DImageLimit)&&mode3D)
		{
			if(!requestLock)
				ccs.addRequest(new Image3DRequest(), false);
			else
				nextRequest3D=new Image3DRequest();
		}
	    displayImage();
	}
	
    public void mouseReleased(MouseEvent e) {
	    /*switch(selectState) {
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
		}*/
    	mode3D=false;
    	ccs.doBlockingRequest(new ImageRequest());
    	displayImage();
    }

    Vector3D coordEvent(MouseEvent e) // get simulation coordinates of
				      // mouse click
    {
	return origin.plus(x.scalarMultiply(2.0 * (e.getX() - (width - 1) / 2.0) / (width - 1))).plus(y.scalarMultiply(2.0 * ((height - 1) / 2.0 - e.getY()) / (height - 1)));
    }
    
    public void mouseDragged(MouseEvent e)
    {
    	double yDis=(Math.PI/height)*(rotationPoint.getY()-e.getPoint().getY());
    	double xDis=(Math.PI/height)*(rotationPoint.getX()-e.getPoint().getX());
    	double xLength=x.length();
    	double yLength=y.length();
        x=x.minus(z.scalarMultiply(xDis)).unitVector().scalarMultiply(xLength);
        y=y.plus(z.scalarMultiply(yDis)).unitVector().scalarMultiply(yLength);
        orthoNormalizeZVector();
    	rotationPoint=e.getPoint();
    	displayImage();
	/*if(selectState == 0) return;
	
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
	    }*/
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
	
    public void mouseClicked(MouseEvent e)
    {
 		/*switch(e.getModifiers()) {
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
		}*/
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
        //z.rotate(-theta, x.unitVector());
        orthoNormalizeZVector();
    }
	
	//rotate the right half toward you, left away
    public void rotateRight(double theta) {
        x.rotate(theta, y.unitVector());
        z.rotate(theta, y.unitVector());
        orthoNormalizeZVector();
    }
	
	//rotate the axes clockwise
    public void rotateClock(double theta) {
        y.rotate(theta, z.unitVector());
        x.rotate(theta, z.unitVector());
        orthoNormalizeZVector();
    }
	
	public void rotationPerformed(RotationEvent e) {
		x.rotate(e.theta, e.rotationAxis);
		y.rotate(e.theta, e.rotationAxis);
		orthoNormalizeZVector();
	}
	
    public void xall() {
		//invariant: must keep \delta = x.length() / width = y.length()  / height fixed to maintain aspect ratio
        origin = new Vector3D(windowManager.sim.origin);
		double delta = boxSize / (height < width ? height : width);
		x = new Vector3D(0, width * delta / 2.0, 0);
		y = new Vector3D(0, 0, height * delta / 2.0);
        z = x.cross(y);
		request2D();
		fireViewReset();
    }
	
    public void yall() {
        origin = new Vector3D(windowManager.sim.origin);
		double delta = boxSize / (height < width ? height : width);
        x = new Vector3D(-width * delta / 2.0, 0, 0);
        y = new Vector3D(0, 0, height * delta / 2.0);
        z = x.cross(y);
		request2D();
		fireViewReset();
    }
	
    public void zall() {
        origin = new Vector3D(windowManager.sim.origin);
		double delta = boxSize / (height2D < width2D ? height2D : width2D);
        x = new Vector3D(width2D * delta / 2.0, 0, 0);
        y = new Vector3D(0, height2D * delta / 2.0, 0);
        //z = x.cross(y);
        orthoNormalizeZVector();
		request2D();
		fireViewReset();
    }
	
	public void componentResized(ComponentEvent e) {
		int newWidth = getWidth();
		int newHeight = getHeight();
		/*x = x.scalarMultiply((double) newWidth / width);
		y = y.scalarMultiply((double) newHeight / height);
		z = x.cross(y);*/
		width = newWidth;
		height = newHeight;
		if(!mode3D)
			getNewImage();
		//b=BufferUtil.newByteBuffer(width*height*height);
		this.repaint();
	}
	
	private class ImageRequest extends CcsThread.request {
		public ImageRequest() {
			// could be a while, lets wait
			super("lvImage", encodeRequest2D());
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			requestLock=true;
		}

		public void handleReply(byte[] data) {
			setCursor(Cursor.getDefaultCursor());
			pixels=data;
			displayImage();
			if (nextRequest!=null)
			{
				ccs.addRequest(nextRequest, false);
				nextRequest=null;
			}
			else
			{
				requestLock=false;
				if (nextRequest3D!=null)
				{
					ccs.addRequest(nextRequest3D, false);
					nextRequest3D=null;
				}
			}
		}
	}
	
	private class Image3DRequest extends CcsThread.request {
		float factorStore=0.0f;
		public Image3DRequest() {
			// could be a while, lets wait
			super("lvImage", encodeRequest3D());
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			reget3DImageCounter=0;
			factorStore=factor;
			requestLock=true;
		}

		public void handleReply(byte[] data) {
			setCursor(Cursor.getDefaultCursor());
			b.clear();
			b.put(data);
			b.flip();
			factorFor3D=factorStore;
			isNewImageData=true;
			if (nextRequest!=null)
			{
				ccs.addRequest(nextRequest, false);
				nextRequest=null;
			}
			else
			{
				requestLock=false;
				if (nextRequest3D!=null)
				{
					ccs.addRequest(nextRequest3D, false);
					nextRequest3D=null;
				}
			}
		}
	}
	
	private class CenterRequest extends CcsThread.request {
		public CenterRequest() {
			super("Center", encodeRequest2D());
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
	
    private byte[] encodeRequest2D() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(136);
        try {
			DataOutputStream dos = new DataOutputStream(baos);
			// These fields go into the liveVizRequest
			dos.writeInt(1); /* version */
			dos.writeInt(1); /* code */
			dos.writeInt(width2D);
			dos.writeInt(height2D);
			
			// These fields go into the MyVizRequest
			dos.writeInt(activeColoring);
			dos.writeInt(radius);
			dos.writeInt(width2D); //encoded twice, for convenience
			dos.writeInt(height2D);
			Vector3D tx = x.scalarMultiply(1/factor);
			Vector3D ty = y.scalarMultiply(1/factor);
			Vector3D tz = z.scalarMultiply(1/factor);
			dos.writeDouble(tx.x);
			dos.writeDouble(tx.y);
			dos.writeDouble(tx.z);
			dos.writeDouble(ty.x);
			dos.writeDouble(ty.y);
			dos.writeDouble(ty.z);
			dos.writeDouble(tz.x);
			dos.writeDouble(tz.y);
			dos.writeDouble(tz.z);
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
    
    private byte[] encodeRequest3D() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(136);
        try {
			DataOutputStream dos = new DataOutputStream(baos);
			// These fields go into the liveVizRequest
			dos.writeInt(1); /* version */
			dos.writeInt(1); /* code */
			dos.writeInt(width3D);
			dos.writeInt(height3D*height3D);
			
			// These fields go into the MyVizRequest
			dos.writeInt(activeColoring);
			dos.writeInt(radius);
			dos.writeInt(width3D); //encoded twice, for convenience
			dos.writeInt(height3D*height3D);
			Vector3D tx = ox.scalarMultiply(1/factor);
			Vector3D ty = oy.scalarMultiply(1/factor);
			Vector3D tz = oz.scalarMultiply(1/factor);
			dos.writeDouble(tx.x);
			dos.writeDouble(tx.y);
			dos.writeDouble(tx.z);
			dos.writeDouble(ty.x);
			dos.writeDouble(ty.y);
			dos.writeDouble(ty.z);
			dos.writeDouble(tz.x);
			dos.writeDouble(tz.y);
			dos.writeDouble(tz.z);
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

    private void displayImage()
    {
        Component comp []=this.getComponents();
        for(int i=0; i<comp.length; i++)
        	comp[i].repaint();
    }
    
    
/* Post a new network image request */
	void request2D() {
		if(!requestLock)
			ccs.addRequest(new ImageRequest(), true);
		else
			nextRequest=new ImageRequest();
		displayImage();
	}
	
	void request3D() {
		if(!requestLock)
			ccs.addRequest(new Image3DRequest(), false);
		else
			nextRequest3D=new Image3DRequest();
		displayImage();
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
	
	private boolean hasShaders=false;
	public void init(GLAutoDrawable arg0)
	{
		GL gl = arg0.getGL();
		gl.glClearColor(0, 0, 0.0f, 0);
		
		if (gl.isFunctionAvailable("glCompileShaderARB"))
			hasShaders=true;
		
		if (hasShaders) {
			String shaderCode []=new String [1];
			shaderCode[0]="varying vec2 texture_coordinate;" +
							"void main(){gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;" +
							"texture_coordinate = vec2(gl_MultiTexCoord0);}";
			String fragmentCode []=new String [1];
			fragmentCode[0]="varying vec2 texture_coordinate; uniform sampler2D my_color_texture; uniform sampler2D my_screen_texture;"+
							"void main()" +
							"{ vec4 screenpix=texture2D(my_screen_texture, texture_coordinate);" +
							"gl_FragColor = texture2D(my_color_texture, vec2(screenpix)+vec2(0.5/255,0));}";
			int codeLength[]=new int[1];

			int my_vertex_shader;
			int my_fragment_shader;

			// Create Shader And Program Objects
			my_program = gl.glCreateProgramObjectARB();
			my_vertex_shader = gl.glCreateShaderObjectARB(GL.GL_VERTEX_SHADER_ARB);
			my_fragment_shader = gl.glCreateShaderObjectARB(GL.GL_FRAGMENT_SHADER_ARB);

			// Load Shader Sources
			codeLength[0]=shaderCode[0].length();
			gl.glShaderSourceARB(my_vertex_shader, 1, shaderCode, (int [])null, 0);
			gl.glShaderSourceARB(my_fragment_shader, 1, fragmentCode, (int [])null, 0);

			// Compile The Shaders
			gl.glCompileShaderARB(my_vertex_shader);
			gl.glCompileShaderARB(my_fragment_shader);

			// Attach The Shader Objects To The Program Object
			gl.glAttachObjectARB(my_program, my_vertex_shader);
			gl.glAttachObjectARB(my_program, my_fragment_shader);

			// Link The Program Object
			gl.glLinkProgramARB(my_program);

			int size=10000;
			byte log[]=new byte[size];
			int one[]=new int[1];
			gl.glGetInfoLogARB(my_vertex_shader, size, one, 0, log, 0);
			gl.glGetInfoLogARB(my_fragment_shader, size, one, 0, log, 0);
			/*for(byte bytes: log)
				System.out.print((char)bytes);
			System.out.println();*/
		}
		
		gl.glGenTextures(1, texture, 0);
		screen=texture[0];
		gl.glGenTextures(1, texture, 0);
		colortable=texture[0];
		gl.glGenFramebuffersEXT(1, texture, 0);
		framebuffer=texture[0];
		gl.glGenTextures(1, texture, 0);
		texture2D=texture[0];
		gl.glGenTextures(1, texture, 0);
		texture3D=texture[0];
	}

	public void display(GLAutoDrawable arg0)
	{
		GL gl = arg0.getGL();
		int j=gl.glGetError();
		if(j!=0)
			System.out.println("Beginning of display: " + j);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();						// Reset The View
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);			// Really Nice Perspective Calculation
		boolean store=mode3D;
		if(((GLJPanel)(arg0)).getName()=="3D")
		{
			store=mode3D;
			mode3D=true;
		}
		if (mode3D)
		{
			gl.glBindTexture(GL.GL_TEXTURE_2D, screen);
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, width2D, height2D, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
			
			if (hasShaders) {
				gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, framebuffer);
				gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, screen, 0);
			}
			
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_BORDER);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_BORDER);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
			gl.glViewport(0,0,width2D,height2D);
			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glLoadIdentity();
			
			
			if(isNewImageData)
			{
				texture[0]=texture3D;
				gl.glDeleteTextures(1, texture, 0);
				gl.glGenTextures(1, texture, 0);
				texture3D=texture[0];
				gl.glBindTexture(GL.GL_TEXTURE_3D, texture3D);
				gl.glTexImage3D(GL.GL_TEXTURE_3D, 0, GL.GL_LUMINANCE, width3D, height3D, height3D, 0, GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE, b);
				gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
				gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
				gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_BORDER);
				gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_BORDER);
				gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_BORDER);
				isNewImageData=false;
			}
			else
				gl.glBindTexture(GL.GL_TEXTURE_3D, texture3D);
			
			gl.glDisable(GL.GL_ALPHA_TEST); /* too agressive--loses too many points */
			gl.glDisable(GL.GL_DEPTH_TEST); /* don't do Z buffer (screws up overlaps, esp. w/blending) */
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			gl.glBlendEquation(GL.GL_MAX);

			//scale and rotate
			float scale=factor/factorFor3D;
			double matrix []=new double[16];
			matrix[0]=x.x;		 matrix[4]=x.y;		  matrix[8]=x.z;		matrix[12]=0;
			matrix[1]=y.x;		 matrix[5]=y.y; 	  matrix[9]=y.z;		matrix[13]=0;
			matrix[2]=z.x;		 matrix[6]=z.y;		  matrix[10]=z.z;		matrix[14]=0;
			matrix[3]=origin.x;	 matrix[7]=origin.y; matrix[11]=origin.z;  matrix[15]=1;
			gl.glScalef(scale, scale, scale);
			gl.glScalef(2, 2, 2);
			gl.glMultMatrixd(matrix, 0);

			gl.glEnable(GL.GL_TEXTURE_3D);
			double intensity=1.0;
			switch (getFacing())
			{
				case 1:
					gl.glScalef(1, 1, 2);
					gl.glTranslatef(0, 0, -1.0f/2);
					gl.glBegin(GL.GL_QUADS);
					gl.glColor4d(intensity,intensity,intensity,intensity);
					for (int slice=0;slice<depth3D;slice++)
					{
						float z=((slice)/((float)depth3D)*1.0f);
						gl.glTexCoord3f(0.0f, 0.0f, z/1.0f);
						gl.glVertex3f(-1.0f, -1.0f, z);
						gl.glTexCoord3f(1.0f, 0.0f, z/1.0f);
						gl.glVertex3f(1.0f, -1.0f, z);
						gl.glTexCoord3f(1.0f, 1.0f, z/1.0f);
						gl.glVertex3f(1.0f, 1.0f, z);
						gl.glTexCoord3f(0.0f, 1.0f, z/1.0f);
						gl.glVertex3f(-1.0f, 1.0f, z);
					}
					gl.glEnd();
					break;
				case 2:
					gl.glScalef(1, 2, 1);
					gl.glTranslatef(0, -1.0f/2, 0);
					gl.glBegin(GL.GL_QUADS);
					gl.glColor4d(intensity,intensity,intensity,intensity);
					for (int slice=0;slice<depth3D;slice++)
					{
						float y=(slice)/((float)depth3D)*1.0f;
						gl.glTexCoord3f(0.0f, y/1.0f, 0.0f);
						gl.glVertex3f(-1.0f, y, -1.0f);
						gl.glTexCoord3f(1.0f, y/1.0f, 0);
						gl.glVertex3f(1.0f, y, -1.0f);
						gl.glTexCoord3f(1.0f, y/1.0f, 1.0f);
						gl.glVertex3f(1.0f, y, 1.0f);
						gl.glTexCoord3f(0.0f, y/1.0f, 1.0f);
						gl.glVertex3f(-1.0f, y, 1.0f);
					}
					gl.glEnd();
					break;
				case 3:
					gl.glScalef(2, 1, 1);
					gl.glTranslatef(-1.0f/2, 0 ,0);
					gl.glBegin(GL.GL_QUADS);
					gl.glColor4d(intensity,intensity,intensity,intensity);
					for (int slice=0;slice<depth3D;slice++)
					{
						float x=(slice)/((float)depth3D)*1.0f;
						gl.glTexCoord3f(x/1.0f, 0.0f, 0.0f);
						gl.glVertex3f(x, -1.0f, -1.0f);
						gl.glTexCoord3f(x/1.0f, 1.0f, 0);
						gl.glVertex3f(x, 1.0f, -1.0f);
						gl.glTexCoord3f(x/1.0f, 1.0f, 1.0f);
						gl.glVertex3f(x, 1.0f, 1.0f);
						gl.glTexCoord3f(x/1.0f, 0.0f, 1.0f);
						gl.glVertex3f(x, -1.0f, 1.0f);
					}
					gl.glEnd();
					break;
				default:
					System.out.println("Facing Error");
			}
			gl.glDisable(GL.GL_TEXTURE_3D);
			
			if (hasShaders) {
				gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
				gl.glMatrixMode(GL.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glLoadIdentity();
				gl.glBindTexture(GL.GL_TEXTURE_2D, screen);

				b3.clear();
				for(int i=0; i<256; i++)
				{
					b3.put(colorBar.cm_red[i]);
					b3.put(colorBar.cm_green[i]);
					b3.put(colorBar.cm_blue[i]);
				}
				b3.flip();
				gl.glActiveTexture(GL.GL_TEXTURE1);
				gl.glBindTexture(GL.GL_TEXTURE_2D, colortable);
				gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, colorBar.cmap_size, 1, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, b3);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
				gl.glActiveTexture(GL.GL_TEXTURE0);


				// Use The Program Object Instead Of Fixed Function OpenGL
				gl.glUseProgramObjectARB(my_program);
				gl.glUniform1i(gl.glGetUniformLocationARB(my_program, "my_color_texture"), 1);
				gl.glBegin(GL.GL_QUADS);
					gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f,  1.0f, 0.0f);
					gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f,  1.0f, 0.0f);
					gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 0.0f);
					gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 0.0f);
				gl.glEnd();

				gl.glUseProgramObjectARB(0);
			}
		}
		else
		{
			b2.clear();
			for(int i=0;i<pixels.length;i++)
			{
				int b=0xff&(int)pixels[i];
				b2.put(colorBar.cm_red[b]);
				b2.put(colorBar.cm_green[b]);
				b2.put(colorBar.cm_blue[b]);
			}
			b2.flip();
			gl.glBindTexture(GL.GL_TEXTURE_2D, texture2D);
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, width2D, height2D, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, b2);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_BORDER);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_BORDER);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_BORDER);
			gl.glDisable(GL.GL_ALPHA_TEST); /* too agressive--loses too many points */
			gl.glDisable(GL.GL_BLEND);
			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glColor4f(1f, 1f, 1f, 1f);
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glBegin(GL.GL_QUADS);
				gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f,  1.0f, 0.0f);
				gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f,  1.0f, 0.0f);
				gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 0.0f);
				gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 0.0f);
			gl.glEnd();
			gl.glDisable(GL.GL_TEXTURE_2D);
		}
		j=gl.glGetError();
		if(j!=0)
			System.out.println("End of display: " + j);
		if(((GLJPanel)(arg0)).getName()=="3D")
			mode3D=store;
	}//end display
	
	//Pre: X, Y, and Z vectors are orthogonal to each other
	//Post: Return 1 if the z axis is most parallel with camera, 2 if the Y axis is most parallel with camera, and 3 if the x axis is most parallel with camera, failure to compute returns a -1
	public int getFacing()
	{
		if (Math.abs(z.z)>=Math.abs(z.x)&&Math.abs(z.z)>=Math.abs(z.y))
			return 1;
		else
			if (Math.abs(z.y)>=Math.abs(z.x)&&Math.abs(z.y)>=Math.abs(z.z))
				return 2;
			else
				if (Math.abs(z.x)>=Math.abs(z.z)&&Math.abs(z.x)>=Math.abs(z.y))
					return 3;
		return -1;
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2){}

	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}


	public void mouseWheelMoved(MouseWheelEvent e)
	{
		factor-=((float)e.getWheelRotation())/16.0f;
		if (factor <1.0f/16f)
				factor=1.0f/16f;
		reget3DImageCounter-=e.getWheelRotation();
		displayImage();
		//System.out.println("reget3DImageCounter: " + reget3DImageCounter + "/" +reget3DImageLimit);
		if(!mode3D)
		{
			request2D();
		}
		if((reget3DImageCounter>=reget3DImageLimit||reget3DImageCounter<=-reget3DImageLimit)&&mode3D)
		{
			request3D();
		}
	}

	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
