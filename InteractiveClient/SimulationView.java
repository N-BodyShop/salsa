/*
SimulationView.java

This is the central class of the client, and it:
	- Displays the current simulation
	- Queues, sends, and receives network render requests
	- Manages the coordinate axes
	- Handles the toolbar

Original version by U. Washington student Grame Lufkin, 2004
Bug fixes in 2004 by Tom Quinn.
Extensive changes in 2009 by Dain Harmon, UAF grad student.
Coordinate system fixes in 2010 by John Ruan, U. Washington and Orion Lawlor, UAF
*/
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

import javax.imageio.ImageIO;

// import com.sun.image.codec.jpeg.*;


public class SimulationView extends JPanel implements ActionListener, MouseInputListener, MouseMotionListener, MouseWheelListener, ComponentListener
{
	WindowManager windowManager;
	CcsThread ccs;
	
	/* Represents a 3D-to-2D transform.*/
	public class CoordinateSystem 
	{
		/*
		  Origin is the vector from the origin of the simulation to the 
	    	center of the screen (in simulation coordinates).
		*/
		public Vector3D origin;

		/*
		  Axes: gives orientation of our current screen, in simulation coordinates.
		  X points to the right onscreen; Y points *up* (like OpenGL); Z points out of screen (right handed).
		  These vectors *always* have length 1.0.
		  To get to onscreen pixels, the simulation axes are scaled to length 1.0/factor, 
		  then X and Y are scaled again by the window's aspect ratio.
		*/
		public Vector3D x, y, z; 

		/* zoom factor: higher values mean more zoomed in.
		*/
		public double factor;
		
		/* Default constructor */
		CoordinateSystem() {
			origin=new Vector3D(0,0,0); 
			x=new Vector3D(1,0,0); y=new Vector3D(0,1,0); z=new Vector3D(0,0,1);
			factor=1.0;
		}
		
		/* Deep copy constructor */
		CoordinateSystem(CoordinateSystem src) {
			origin=new Vector3D(src.origin); 
			x=new Vector3D(src.x); y=new Vector3D(src.y); z=new Vector3D(src.z);
			factor=src.factor;
		}
		/* Returns true if these are floating-point identical */
		final public boolean equals(CoordinateSystem src) {
			return origin.equals(src.origin) &&
				x.equals(src.x) && y.equals(src.y) && z.equals(src.z) &&
				factor==src.factor;
		}
		
		
		/* Make the x, y, and z axes mutually orthogonal, and have unit length. 
		   z gets computed first. */
		final public void orthoNormalize()
		{
		 // System.out.println("ORTHO: incoming vector lengths are "+x.length()+", "+y.length());
			 z=(x.cross(y)).unitVector();
			 y=(z.cross(x)).unitVector();
			 x=(y.cross(z)).unitVector();
		}
		
		
		//rotate the top half toward you, bottom away
		final public void rotateUp(double theta) {
    		y.rotate(theta, x.unitVector());
    		//z.rotate(-theta, x.unitVector());
    		orthoNormalize();
		}

		//rotate the right half toward you, left away
		final public void rotateRight(double theta) {
    		x.rotate(theta, y.unitVector());
    		z.rotate(theta, y.unitVector());
    		orthoNormalize();
		}

		//rotate the axes clockwise
		final public void rotateClock(double theta) {
    		y.rotate(theta, z.unitVector());
    		x.rotate(theta, z.unitVector());
    		orthoNormalize();
		}
	};
	
	/* The current onscreen coordinate system.  Updated by the UI. */	
	CoordinateSystem coord;
	
	/* Coordinate system for the last 2D render */
	CoordinateSystem coord2D;
	/* Coordinate system for the last 3D render */
	CoordinateSystem coord3D;
	
	double delta;// 1.0/smaller of width2D and height2D; used to maintain aspect ratio
	
	/* Amount that one zoomtool-click zooms */
	static final double clickZoomFactor=2.0;
	
	double delX, delY, delZ; //dimensions of simulation, in sim coords
	
	int activeColoring = 0;
	//int activeGroup = 0;
	String activeGroup = "All";
	int centeringMethod = 2;
	int radius = 0;
	
	double minMass = 0;
	double maxMass = 1;
	int doSplatter = 0; /* accessed by ToolBarPanel */
	boolean disable3D = false; /* accessed by ToolBarPanel */
	
	//Selection states: 0=none, 1=end of box xy, 2= in box z, 3=end box z, 4=sphere, 5=ruler,
	static final int selectState_none=0;
	static final int selectState_box_xy=1;
	static final int selectState_box_zstart=2;
	static final int selectState_box_zend=3;
	static final int selectState_sphere=4;
	static final int selectState_rulerstart=5;
	static final int selectState_rulerend=6;
	int selectState = selectState_none;	// State of selection
	
	// Simulation-space 3D click locations (for animating selection process)
	Vector3D selCur=new Vector3D(0,0,0), selOld=new Vector3D(0,0,0); 
	
	Vector3D selectCorner; // selection box: simulation space corner
	Vector3D selectEdge1, selectEdge2, selectEdge3; // selection box X, Y, Z (along original camera coords)
	
	double selectRadius;	// sphere radius

	byte[] pixels;

	//double angleLeft, angleCcw, angleUp;
	Rectangle rect;

	RightClickMenu rcm;
	GroupQuery gquery;

	EventListenerList listenerList = new EventListenerList();
	ViewEvent viewEvent = null;

	public ColorBarPanel colorBar;

	/* Network-related variables */
	public boolean hasGL=true; // if true, we can run OpenGL code
	public boolean isNewImageData=true; /* need to regenerate texture3D */
	boolean networkBusy=false; // if true, the server is currently busy rendering already
	boolean want2D=false, wantZ=false, want3D=false; /* we need to send off a request of this type (2D render, depth estimate, 3D render) */
	boolean uptodate2D=false, uptodate3D=false; /* we have a good current image */
	
	public int width2D, height2D; //size of screen in pixel coords

	public Object bLock=new Object();
	public ByteBuffer b=null; // 3D greyscale volume texture upload buffer
	public int width3D, height3D, depth3D; // number of voxels in volume impostor image
	
	public Object b2Lock=new Object();
	//public ByteBuffer b2; // 2D RGB texture upload buffer
	//public ByteBuffer b3; // 1D colortable texture upload buffer
	public int cwidth2D, cheight2D;

	boolean maxMode=true;
	Point rotationPoint;
	
/* OpenGL mode: */
	//SimulationViewGL svGL; // stores our OpenGL data (if OpenGL enabled)
/* Non-OpenGL mode: */
	JLabel fallbackLabel;
	
	//0=No Compression, 1=JPEG, 2=RunLength
	static final int encoding_raw=0;
	static final int encoding_jpeg=1;
	static final int encoding_rle=2;
	int encoding2D=encoding_rle;
	int encoding3D=encoding_rle;
	int compression2D=0;
	int compression3D=60;

	//0=Rotate, 1=Zoom+, 2=Zoom-, 3=Select Sphere, 4=Select Box, 5=Ruler
	static final int tool_rotate=0;
	static final int tool_zoomin=1;
	static final int tool_zoomout=2;
	static final int tool_sphere=3;
	static final int tool_box=4;
	static final int tool_ruler=5;
	public int activeTool=tool_rotate;

	public SimulationView(WindowManager wm, int w, int h, ColorBarPanel cbp)
	{
		super(new BorderLayout());
		windowManager = wm;

		delX = windowManager.sim.maxX-windowManager.sim.minX;
		delY = windowManager.sim.maxY-windowManager.sim.minY;
		delZ = windowManager.sim.maxZ-windowManager.sim.minZ;
		System.out.println("Loading simulation: dimensions X="+(delX)+" Y="+(delY)+" Z="+(delZ));

		//a viewing window gets its own CcsThread, so the queues can operate independently
		ccs = new CcsThread(windowManager.ccs);

		//this.setMaximumSize(new Dimension(w, h));
		//this.setMinimumSize(new Dimension(w, h));

		colorBar=cbp;
		// FIXME: volume render dimensions should be autodetermined based on graphics card.
		//  512 is fine for high-end cards, but way too much for low-end!
		width3D=height3D=depth3D=512;
		width2D=w;
		height2D=h;
		delta = (double) 1.0 / (height2D < width2D ? height2D : width2D);
		pixels = new byte[width2D*height2D];

		coord=new CoordinateSystem();
		coord2D=new CoordinateSystem();
		coord3D=new CoordinateSystem();
		coord3D.x=new Vector3D(1,0,0);
		coord3D.y=new Vector3D(0,1,0);
		coord3D.z=new Vector3D(0,0,1);
		
		zall();

    	rcm = new RightClickMenu(windowManager, this);
    	gquery = new GroupQuery(this);
		
		initialJOGLsetup();
	}
	
	void initialJOGLsetup() {
		System.out.println("Trying to set up JOGL...");
		try {
			/* This will make the OpenGL glcanvas, ByteBuffers, etc;
			  or else it will fail, and we will fall back to AWT. */
			SimulationViewGL svGL=new SimulationViewGL(this,width2D,height2D);
			addMyListeners(svGL.getMainComponent());
		}
		catch (java.lang.NoClassDefFoundError e) {
			e.printStackTrace();
			fallbackSwingNow("Error initializing JOGL (missing .jar?).");
		}
	}
	
	private boolean fallbackNextTime=false;
	// Stupidly, missing or mismatched JOGL/OpenGL native libraries aren't detectable until paint time.
	public void paint(Graphics g) {
		if (fallbackNextTime) {
			fallbackNextTime=false;
			fallbackSwingNow("delayed fallback resumed");
		}
		try {
			super.paint(g);
		} catch (java.lang.UnsatisfiedLinkError e) {
			e.printStackTrace();
			System.out.println("If this crashes or hangs, remove 'jogl.jar' and restart Salsa.");
			fallbackSwingNow("Error loading JOGL native libraries (missing or mismatched .so?).");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("If this crashes or hangs, remove 'jogl.jar' and restart Salsa.");
			fallbackSwingNow("Exception loading JOGL.");
		}
	}
	
	// Fall back to swing, but not quite yet.
	//  This is needed because JOGL is amazingly stupid about *crashing* if you removeAll() at the wrong time.
	public void fallbackSwingDelayed(String fallbackReason)
	{
		System.out.println("--- "+fallbackReason + " ---\nWill fall back to plain 2D Swing rendering (eventually).\n");
		fallbackNextTime=true;
		hasGL=false;
		this.repaint();
	}
	
	// This is the non-OpenGL fallback case: a plain Java Swing panel.
	public void fallbackSwingNow(String fallbackReason)
	{
		System.out.println("--- "+fallbackReason + " ---\nFalling back to plain 2D Swing rendering.\n");
		removeAll(); // don't try to paint that OpenGL crap again (this kills ancient-X machines)
		
		fallbackLabel=new JLabel("OpenGL disabled.  Now waiting for the network.");
		addMyListeners(fallbackLabel);
		
		hasGL=false;
		disable3D=true;
		
		getNewImage(false); // pull a fresh image to get Swing image updated
	}
	/*
	  We need to get mouse clicks and such: listen to them on this panel.
	*/
	private void addMyListeners(JComponent comp)
	{
		comp.setSize(new Dimension(width2D,height2D));
		comp.setPreferredSize(new Dimension(width2D,height2D));
		comp.addMouseListener(this);
		comp.addMouseMotionListener(this);
		comp.addMouseWheelListener(this);
		comp.addComponentListener(this);
		add(comp);
	}
	public void redisplay(ColorModel cm) {
		this.repaint();
	}
	private void displayImage()
	{
    	Component comp []=this.getComponents();
    	for(int i=0; i<comp.length; i++)
        	comp[i].repaint();
	}

/*********************** Coordinate System *************************/

	/* Get 3D simulation coordinates of a 2D onscreen mouse click.
	  Used during zooming, selection, etc.
	*/
	Vector3D coordEvent(MouseEvent e)
	{
		Vector3D ret=coord.origin.plus(
			coord.x.scalarMultiply(delta/coord.factor*2.0*(e.getX() - width2D*0.5)).plus(
			coord.y.scalarMultiply(delta/coord.factor*2.0*(height2D*0.5 - e.getY()))
		));
		
		/*
		Point back=screenFm3D(ret); // round-trip coordinate transform check: works!
		System.out.println("2D "+e.getX()+","+e.getY()+" to 3D "+ret+" to 2D "+back);
		*/
		
		return ret;
	}
	
	/* Get 2D screen coordinates of a simulation-coordinates 3D location.
	   Used for onscreen feedback during selection process. */
	Point screenFm3D(Vector3D sim) {
		sim=sim.minus(coord.origin);
		Point ret=new Point(
			(int)(coord.x.dot(sim)/(delta/coord.factor*2.0)+width2D*0.5),
			(int)(height2D*0.5-coord.y.dot(sim)/(delta/coord.factor*2.0))
		);
		return ret;
	}

	public void xall() { // down X axis: +Y and +Z
    	coord.origin = new Vector3D(windowManager.sim.origin);
		coord.x = new Vector3D(0, 1, 0);
		coord.y = new Vector3D(0, 0, 1);
		
		/* Set initial zoom factor based on whether the simulation in X-Y view 
		   has no extra space along the X or Y dimension, calculate the sim coord to pixel ratio*/  
		if ((delY)/(delZ) < width2D/height2D){
			coord.factor = 2.0/(delY);
		}
		else{
			coord.factor = 2.0/(delZ);
		}
	    coord.orthoNormalize();
		fireViewReset();
	}

	public void yall() { // down Y axis: -X and +Z
    	coord.origin = new Vector3D(windowManager.sim.origin);
		coord.x = new Vector3D(-1, 0, 0);
		coord.y = new Vector3D(0, 0, 1);
		if ((delX)/(delZ)< width2D/height2D){
			coord.factor = 2.0/(delX);
		}
		else{
			coord.factor = 2.0/(delZ);
		}
    	coord.orthoNormalize();
		fireViewReset();
	}

	public void zall() { // down Z axis: +X and +Y
		coord.origin = new Vector3D(windowManager.sim.origin);
		coord.x = new Vector3D(1, 0, 0);
		coord.y = new Vector3D(0, 1, 0);
		if ((delX)/(delY) < width2D/height2D){
			coord.factor = 2.0/(delX);
		}
		else{
			coord.factor = 2.0/(delY);
		}
    	coord.orthoNormalize();
		fireViewReset();
	}
	
/*********************** Selection ***********************/
	/* Draw a line between these two simulation-space locations */
	private void drawLine(Graphics g,Vector3D start,Vector3D end) {
		Point s=screenFm3D(start), e=screenFm3D(end);
		g.drawLine(s.x,s.y, e.x,e.y);
	}
	/* Draw a 2D box with corners at these two simulation-space locations */
	private void drawBox(Graphics g,Vector3D start,Vector3D end) {
		Point s=screenFm3D(start), e=screenFm3D(end);
		g.drawLine(s.x,s.y, s.x,e.y);
		g.drawLine(s.x,e.y, e.x,e.y);
		g.drawLine(e.x,e.y, e.x,s.y);
		g.drawLine(e.x,s.y, s.x,s.y);
	}

	/* Draw the currently selected region onscreen.
	   The XOR mode allows us to *erase* the currently selected region using this same function call!
	*/
	private void drawSelection() 
	{
		Graphics g = /*glcanvas.*/ getGraphics();
		g.setXORMode(Color.green);
		Point cur=screenFm3D(selCur), old=screenFm3D(selOld);
		switch(selectState)
		{
		case selectState_box_xy: 
			drawBox(g,selectCorner,selectCorner.plus(selectEdge1).plus(selectEdge2));
			break;
		case selectState_box_zstart:
			drawLine(g,selCur,selCur.plus(selectEdge1).plus(selectEdge2)); 
			//^ Line, because flat XOR box will erase itself!
			break;
		case selectState_box_zend: 
			drawBox(g,selectCorner,selectCorner.plus(selectEdge1).plus(selectEdge2).plus(selectEdge3));
			break;
		
		case selectState_sphere: { /* draw a circle */
			int size = (int) Math.sqrt((cur.x - old.x) // cur determines radius
					 *(cur.x - old.x)
					 + (cur.y - old.y)
					 *(cur.y - old.y));
			g.drawArc(old.x-size, old.y-size, 2*size, 2*size, 0, 360); // old determines origin
			} break;
		
		case selectState_rulerend: 
			drawLine(g,selCur,selOld);
			g.drawString(new java.text.DecimalFormat("####.####").format(selCur.minus(selOld).length()),
				(cur.x+old.x)/2,(cur.y+old.y)/2);
			break;
		default: // e.g., selectState_none
			break;
		};
	}
	/* Update the current selection state to reflect this new mouse event. 
	  This is called repeatedly as the mouse is dragged or moved around.
	*/
	private void updateSelection(MouseEvent e) {
		selCur=coordEvent(e);
		switch (selectState) {
		case selectState_box_xy:
			selectEdge1 = selCur;
			// Turn corners into direction vectors
			Vector3D d = selectEdge1.minus(selectCorner);
			selectEdge1 = coord.x.scalarMultiply(coord.x.dot(d));
			selectEdge2 = coord.y.scalarMultiply(coord.y.dot(d));
			break;
		case selectState_box_zstart:
			selCur=selectCorner.plus(coord.y.scalarMultiply(coord.y.dot(selCur.minus(selectCorner))));
			break;
		case selectState_box_zend: 
			selectEdge3 = coord.y.scalarMultiply(coord.y.dot(selCur.minus(selOld)));
			break;
		};
	}
	
	/*
	 Box selection needs a "corner" and three "edges" (X, Y, Z axes of box)
	1.) Mouse pressed.  Sets selectCorner.
	2.) Mouse dragged.  Updates selectEdge1 and Edge2 (X and Y axes)
	3.) Mouse released.  View rotates 90 degrees.
	4.) Mouse hovers.  Shows selectCorner with new Z (onscreen Y)
	5.) Mouse pressed.  Updates selectCorner.
	6.) Mouse dragged.  Updates Edge3.
	7.) Mouse released.  Give the new box a name in the dialog.
	Whew!
	*/
	private void boxSelectStart(MouseEvent e) // mouse going down during box select
	{
    	// box selection
		switch(selectState)
		{
			case selectState_none:	// starting box selection
				selCur = selOld = coordEvent(e);
				selectCorner = selCur; selectEdge1=selectEdge2=selectEdge3=new Vector3D(0,0,0);
				selectState = selectState_box_xy;
				System.out.println("Corner 1: " + selectCorner.toString());
				break;
			case selectState_box_zstart:
				drawSelection(); /* erase original line */
				updateSelection(e);
				selectCorner = selOld = selCur;
				selectState = selectState_box_zend;
				break;
			default:
		    	System.out.println("Bad state in press: " + selectState);
		}
		drawSelection(); /* so we've got one to start with! */
	}

	private void sphereSelectStart(MouseEvent e) // mouse going down during sphere select
	{
    	// sphere selection
		switch(selectState)
		{
			case selectState_none:
				selCur = selOld = coordEvent(e);
				selectState = selectState_sphere;
				drawSelection();
				break;
			default:
		    	System.out.println("Bad state in press: " + selectState);
		}
	}
	
	private void rulerSelectStart(MouseEvent e) // mouse going down during sphere select
	{
		drawSelection(); // erases any old selection
		selCur=coordEvent(e); 
		if (selectState==selectState_rulerend) {
			drawSelection(); // draws final selection
			System.out.println("Distance is " +selCur.minus(selOld).length());
			selectState=selectState_rulerstart;
		} else {
			selOld=selCur;
			selectState=selectState_rulerend;
			drawSelection(); // draws initial selection
		}
	}
	
	
/****************************** User Interface ************************************/
	private void mouseDebug(String what,MouseEvent e) {
		//System.out.println("------ "+what+" ----- at "+e.getX()+","+e.getY());
	}
	
	/* Begin rotating at this mouse click */
	private void rotateStart(MouseEvent e)
	{
    	rotationPoint=e.getPoint();
		uptodate2D=false; /* we're rotating, so 2D is now out of date */
		displayImage();
	}
	
	/* Zoom at this mouse location */
	private void zoomBy(MouseEvent e,double zoomFactor)
	{
		coord.origin = coordEvent(e);
		coord.factor*=zoomFactor;
		request2D(true); /* need a 2D image for new viewpoint */
		getNewDepth(false); /* eventually, pick up new depth */
		request3D(false); /* eventually, pick up new 3D too */
		displayImage(); /* draw new viewpoint */
	}
	/* Zoom in at this mouse location */
	private void zoomIn(MouseEvent e)
	{
		zoomBy(e,clickZoomFactor);
	}
	/* Zoom out at this mouse location */
	private void zoomOut(MouseEvent e)
	{
		zoomBy(e,1.0/clickZoomFactor);
	}

	
	public void mousePressed(MouseEvent e) {
		mouseDebug("PRESS",e);
		if(e.isPopupTrigger()) {
        	//rcm.refresh();
		rcm.show(e.getComponent(), e.getX(), e.getY());
		return;
		}
		switch (e.getModifiers()) {
		case (MouseEvent.BUTTON1_MASK|InputEvent.ALT_MASK):
	    	rotateStart(e);
		case (MouseEvent.BUTTON1_MASK|InputEvent.ALT_MASK|InputEvent.CTRL_MASK):  // Zoom in.
			zoomIn(e);
			break;
		case (MouseEvent.BUTTON3_MASK|InputEvent.ALT_MASK|InputEvent.CTRL_MASK):  // Zoom out
			zoomOut(e);
			break;
		case (MouseEvent.BUTTON1_MASK|InputEvent.SHIFT_MASK):
			boxSelectStart(e);
			break;
		case (MouseEvent.BUTTON1_MASK|InputEvent.CTRL_MASK):
	    	sphereSelectStart(e);
			break;
		default: /* no modifiers--check the active tool */
	    	switch(activeTool)
	    	{
	    	case tool_rotate:
	    		rotateStart(e);
	    		break;
			case tool_zoomin:
				zoomIn(e);
				break;  
			case tool_zoomout:
				zoomOut(e);
				break;
	    	case tool_sphere:
	    		sphereSelectStart(e);
	    		break;
	    	case tool_box:
	    		boxSelectStart(e);
	    		break;
	    	case tool_ruler:
				rulerSelectStart(e);
	    		break;
	    	}
		}
	}

	public void mouseDragged(MouseEvent e)
	{
		mouseDebug("DRAG",e);
    	if(activeTool==tool_rotate)
    	{
		/* Rotation is a "spaceball" controller in 3D: to rotate, just
		   nudge the onscreen x and y axes around by z, and re-orthonormalize. */
	    	double yDis=(Math.PI/height2D)*(rotationPoint.getY()-e.getPoint().getY());
	    	double xDis=(Math.PI/height2D)*(rotationPoint.getX()-e.getPoint().getX());
	    	double xLength=coord.x.length();
	    	double yLength=coord.y.length();
	    	coord.x=coord.x.minus(coord.z.scalarMultiply(xDis)).unitVector().scalarMultiply(xLength);
	    	coord.y=coord.y.plus(coord.z.scalarMultiply(yDis)).unitVector().scalarMultiply(yLength);
	    	coord.orthoNormalize();
	    	rotationPoint=e.getPoint();
			if (disable3D) request2D(true); /* gotta request series of 2D frames if we don't have impostors */
	    	displayImage();
    	}

		if(selectState == selectState_none) return;
		
		drawSelection(); // erases old selection
		updateSelection(e);
		drawSelection(); // draws new selection
	}

	public void mouseReleased(MouseEvent e) {
		mouseDebug("RELEASE",e);
		updateSelection(e); /* update positions to this new position */
		switch(selectState) {
		case selectState_none:
			break;

		case selectState_box_xy:	// end of X-Y box selection
			selectState = selectState_box_zstart;
			System.out.println("Corner 2: " + selectEdge1.toString());
			System.out.println("Corner 3: " + selectEdge2.toString());
			coord.rotateUp(-0.5*Math.PI); /* rotate view by 90 degrees, so we can see box's Z axis */
			request2D(true);
			getNewDepth(false);
			break;
		case selectState_box_zend:	// end of Z box selection
			System.out.println("Corner: " + selectCorner.toString());
			System.out.println("Dir 1: " + selectEdge1.toString());
			System.out.println("Dir 2: " + selectEdge2.toString());
			System.out.println("Dir 3: " + selectEdge3.toString());
			gquery.setVisible(true);
			break;
		case selectState_sphere:	// end of Sphere
			selectRadius = (selCur.minus(selOld)).length();
			System.out.println("Center: " + selOld);
			System.out.println("Edge: " + selCur);
			System.out.println("Radius: " + selectRadius);
			gquery.setVisible(true); // see "makeBox"
			break;
			
		case selectState_rulerstart:
		case selectState_rulerend:
			/* nothing to do here */
		    break;
		default:
			System.out.println("Bad state in release: " + selectState);
		}
		
		if (activeTool==tool_rotate) 
		{ // New viewpoint--need new 2D image if possible...
			request2D(true); 
		}
	}
	
	
	public void mouseMoved(MouseEvent e)
	{ 
		mouseDebug("MOVE",e);
		if(selectState == selectState_none) return;

		if (selectState == selectState_rulerend || selectState == selectState_box_zstart)
		{
			drawSelection(); // erases old selection
			updateSelection(e); // updates new selection
			drawSelection();
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		mouseDebug("CLICK",e);
	}
	
    /* The mousewheel zooms in and out */
	public void mouseWheelMoved(MouseWheelEvent e)
	{

		try /* slowly shift mouse pointer closer to center of screen */
		{
			Robot r = new Robot();
			Point p=getLocationOnScreen();
			r.mouseMove((int)(p.getX()+e.getX()+.1*(width2D/2.0-e.getX())),(int)(p.getY()+e.getY()+.1*(height2D/2.0-e.getY())));
		} catch(Exception ex) {}

		coord.factor*=(16.0f-(float)e.getWheelRotation())/16.0f;
		/*if (factor <1.0f/16f)
				factor=1.0f/16f;*/
		coord.origin=coord.origin.plus((coordEvent(e).minus(coord.origin)).scalarMultiply((1+1.0/16.0)*1.1-1));
		displayImage();
		
		request2D(true); // fire off requests; they may not actually finish in time, but they're good to have... 
		request3D(false);
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("refresh")) {
			getNewDepth(true); // blocking, so we're really centered nicely
			getNewImage(true);
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

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

/********************************* Network *************************/
	/* Called by GroupQuery when the user gives the new group a name */
	public void makeGroup(String groupName) 
	{
		if(selectState == selectState_box_zend) {
			PythonExecute code = new PythonExecute("charm.createGroupAttributeBox(\""
			  + groupName + "\", \"All\", \"position\","
			  + selectCorner.toPyString() + ","
			  + selectEdge1.toPyString() + ","
			  + selectEdge2.toPyString() + ","
			  + selectEdge3.toPyString() + ")\n",
							   false, true, 0);
			windowManager.ccs.addRequest(new ExecutePythonCode(code.pack()));
			}
		else if(selectState == selectState_sphere) {
			PythonExecute code = new PythonExecute("charm.createGroupAttributeSphere(\""
			  + groupName + "\", \"All\", \"position\","
			  + selOld.toPyString() + ","
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

	/* Various toolbars call this function when the user selects e.g. a new coloring, family, attribute, sliders, ... */
	public void getNewImage(boolean flushOld3D) {
		request2D(true);
		request3D(flushOld3D);
		if (!flushOld3D) displayImage();
	}
	
	/* Post a new network image request */
	void request2D(boolean flushOldImage) {
		if (flushOldImage) uptodate2D=false;
		want2D=true;
		networkPoll();
	}

	void request3D(boolean flushOldImage) {
		if (flushOldImage) uptodate3D=false;
		want3D=true;
		networkPoll();
	}


	/* Send out any pending image requests */
	void networkPoll() {
		debugNetwork("poll");
		if (networkBusy) return; /* don't bother me right now */
		if (want2D) {
			ccs.addRequest(new ImageRequest(), false);
		}
		else if (wantZ) {
			ccs.addRequest(new CenterRequest(), false);
		}
		else if (want3D && !disable3D) {
			ccs.addRequest(new Image3DRequest(), false);
		}
		else {
				networkBusy=false;
		}
	}

	public final void debugNetwork(String doingWhat) {
	/*
		System.out.println("Network "+doingWhat+" (busy="+networkBusy
		  +" want="+(want2D?"2D ":"")+(wantZ?"Z ":"")+(want3D?"3D":"")
		  +" uptodate="+(uptodate2D?"2D ":"")+(uptodate3D?"3D ":"")+")");
	*/
	}

	private class ImageRequest extends CcsThread.request {
		CoordinateSystem mycoord;	
		int w, h;
		long reqStartTime=0;
		public ImageRequest() {
			super("lvImage", encodeRequest2D(coord.factor));
			mycoord=new CoordinateSystem(coord);	
			reqStartTime=System.currentTimeMillis();
			w=width2D;
			h=height2D;
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			networkBusy=true;
			want2D=false;
			debugNetwork("outgoing 2D image request");
		}

		public void handleReply(byte[] data) {
			//System.out.println("Request Time 2D (ms): "+(System.currentTimeMillis()-reqStartTime));
			setCursor(Cursor.getDefaultCursor());
			synchronized(b2Lock)
			{
				cwidth2D=w;
				cheight2D=h;
				byte[] bytes=new byte [cwidth2D*cheight2D];
				long startTime=0;
				switch(encoding2D)
				{
					case encoding_raw:
						pixels=data;
						break;
					case encoding_jpeg:
						startTime=System.currentTimeMillis();
						DataBuffer db=null;
						try {
							// JPEGImageDecoder dec=JPEGCodec.createJPEGDecoder(new ByteArrayInputStream(data));
							// db=dec.decodeAsRaster().getDataBuffer();
							BufferedImage imio = ImageIO.read(new ByteArrayInputStream(data));
							db = imio.getData().getDataBuffer();
						} catch (Exception e) {
							System.out.println("Cannot read Image");
						}
						for (int i=0; i<cwidth2D*cheight2D; i++)
						{
							bytes[i]=((byte) ((db.getElem(i)) & 0xFF));
						}
						pixels=(byte[])bytes;
						System.out.println("Decompress time (ms) JPEG2D: "+(System.currentTimeMillis()-startTime));
						System.out.println("JPEG Compression 2D: " +((double)data.length)/db.getSize()*100+"%\n Bytes: "+data.length);
						break;
					case encoding_rle:
						startTime=System.currentTimeMillis();
						if(data.length%2==0)
						{
							for(int i=0, k=0; i<data.length; i+=2)
							{
								int reps=0;
								reps=((int)data[i]&0xff);
								for(int j=0; j<reps; j++, k++)
									bytes[k]=data[i+1];
							}
							pixels=(byte[])bytes;
						}
						else
							System.err.println("Compression failure");
				//		System.out.println("Decompress time (ms) RL2D: "+(System.currentTimeMillis()-startTime));
				//		System.out.println("RunLength Compression 2D: " +((double)data.length)/(cwidth2D*cheight2D)*100+"%\n Bytes: "+data.length);
						break;
					default:
						pixels=data;
						break;

				}
			}
			
			if (!hasGL && fallbackLabel!=null) { /* trivial AWT fallback rendering */
				MemoryImageSource source = new MemoryImageSource(w, h, colorBar.colorModel, pixels, 
					// 0, w   //<- right side up: start at beginning, ascend
					w*(h-1),-w //<- upside down (OpenGL orientation): start at end, descend
				);
				source.setAnimated(true);
				fallbackLabel.setIcon(new ImageIcon(createImage(source)));
			}
			
			coord2D=mycoord;
			uptodate2D=true;
			networkBusy=false;
			networkPoll();
			displayImage();
			debugNetwork("incoming 2D image");
		}
	}

	private class Image3DRequest extends CcsThread.request {
		CoordinateSystem mycoord;
		long reqStartTime=0;
		public Image3DRequest() {
			super("lvImage", encodeRequest3D());
			mycoord=new CoordinateSystem(coord);			
			reqStartTime=System.currentTimeMillis();
			networkBusy=true;
			want3D=false;
			debugNetwork("outgoing 3D image request");
		}

		public void handleReply(byte[] data)
		{
			debugNetwork("incoming 3D image response");
			//System.out.println("Request Time 3D (ms): "+(System.currentTimeMillis()-reqStartTime));
			setCursor(Cursor.getDefaultCursor());
			long startTime=0;
			if (b!=null && hasGL && !disable3D) synchronized(bLock)
			{
				switch(encoding3D)
				{
					case encoding_raw:
						b.put(data);
						break;
					case encoding_jpeg:
						startTime=System.currentTimeMillis();
						DataBuffer db=null;
						try {
							// JPEGImageDecoder dec=JPEGCodec.createJPEGDecoder(new ByteArrayInputStream(data));
							// db=dec.decodeAsRaster().getDataBuffer();
							BufferedImage imio = ImageIO.read(new ByteArrayInputStream(data));
							db = imio.getData().getDataBuffer();
						} catch (Exception e) {
							System.out.println("Cannot read Image");
						}
						b.clear();
						for (int i=0; i<db.getSize(); i++)
						{
							b.put((byte) ((db.getElem(i)) & 0xFF));
						}
						System.out.println("Decompress time (ms) JPEG3D: "+(System.currentTimeMillis()-startTime));
						System.out.println("JPEG Compression 3D: " +((double)data.length)/db.getSize()*100+"%\n Bytes: "+data.length);
						break;

					case encoding_rle:
						startTime=System.currentTimeMillis();
						if(data.length%2==0)
						{
							for(int i=0, k=0; i<data.length; i+=2)
							{
								int reps=0;
								reps=((int)data[i]&0xff);
								for(int j=0; j<reps; j++, k++)
									b.put(data[i+1]);
							}

						}
						else
							System.err.println("Compression failure");
						//System.out.println("Decompress time (ms) RL3D: "+(System.currentTimeMillis()-startTime));
						//System.out.println("RunLength Compression 3D: " +((double)data.length)/b.capacity()*100+"%\n Bytes: "+data.length);
						break;

					default:
						b.put(data);
						break;
				}
				b.flip();
			}
			coord3D=mycoord;
			isNewImageData=true;
			uptodate3D=true;
			networkBusy=false;
			networkPoll();
			displayImage();
		}
	}


	public void getNewDepth(boolean beBlocking) {
		if (beBlocking) {
			ccs.doBlockingRequest(new CenterRequest());
		} else { /* non-blocking */
			wantZ=true;
			networkPoll();
		}
	}
	/**
	  A CenterRequest calls Worker::calculateDepth, to find the Z value
	  that the simulation should rotate and zoom around.
	*/
	private class CenterRequest extends CcsThread.request {
		// Our coordinate system is same as 2D render, but we act zoomed-in first--
		//  this picks out the Z for the object in the middle of the screen, 
		//  not some higher-potential object off on the side of the screen...
	    static final double trimCenterFactor=2.0;
		// Stash old coordinate system, in case we're rendered obsolete...
		private CoordinateSystem mycoord;
		public CenterRequest() {
			super("Center", encodeRequest2D(coord.factor*trimCenterFactor));
			mycoord=new CoordinateSystem(coord);
			mycoord.factor*=trimCenterFactor;
			networkBusy=true;
			wantZ=false;
			debugNetwork("outgoing depth request");
		}

		public void handleReply(byte[] data) {
			try {
				double val = Double.parseDouble(new String(data));
			//	System.out.println("Server says z should shift by "+val);
				/* FIXME: what happens if "coord" is drastically changed/reset while we're on the network?
				   This line might cause popping... */
				coord.origin = mycoord.origin.plus(mycoord.z.scalarMultiply(val/mycoord.factor));
			} catch(NumberFormatException e) {
				System.err.println("Problem decoding the z value.  String: " + (new String(data)));
			}
			networkBusy=false;
			networkPoll();
			debugNetwork("incoming depth request");
		}
	}

	/* Set up a CCS request structure ("MyVizRequest" on server) for a 2D render */
	private byte[] encodeRequest2D(double renderzoomfactor) {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
    	try {
			DataOutputStream dos = new DataOutputStream(baos);
			// These fields go into the liveVizRequest
			dos.writeInt(2); /* version */
			dos.writeInt(0); /* code */
			dos.writeInt(width2D);
			dos.writeInt(height2D);
			dos.writeInt(encoding2D);
			dos.writeInt(compression2D);

			// These fields go into the MyVizRequest
			dos.writeInt(activeColoring);
			dos.writeInt(radius);
			dos.writeInt(width2D); //encoded twice, for convenience
			dos.writeInt(height2D);
			Vector3D tx = coord.x.scalarMultiply(width2D*delta/renderzoomfactor);
			Vector3D ty = coord.y.scalarMultiply(height2D*delta/renderzoomfactor);
			Vector3D tz = coord.z.scalarMultiply(1/renderzoomfactor);
			dos.writeDouble(tx.x);
			dos.writeDouble(tx.y);
			dos.writeDouble(tx.z);
			dos.writeDouble(ty.x);
			dos.writeDouble(ty.y);
			dos.writeDouble(ty.z);
			dos.writeDouble(tz.x);
			dos.writeDouble(tz.y);
			dos.writeDouble(tz.z);
			dos.writeDouble(coord.origin.x);
			dos.writeDouble(coord.origin.y);
			dos.writeDouble(coord.origin.z);
			dos.writeInt(centeringMethod);
			//dos.writeInt(activeGroup);
			dos.writeLong(activeGroup.length());
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
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
    	try {
			DataOutputStream dos = new DataOutputStream(baos);
			// These fields go into the liveVizRequest
			dos.writeInt(2); /* version */
			dos.writeInt(0); /* code */
			dos.writeInt(width3D);
			dos.writeInt(height3D*height3D);
			dos.writeInt(encoding3D);
			dos.writeInt(compression3D);

			// These fields go into the MyVizRequest
			dos.writeInt(activeColoring);
			dos.writeInt(radius);
			dos.writeInt(width3D); //encoded twice, for convenience
			dos.writeInt(height3D*height3D);
			Vector3D tx = new Vector3D(1/coord.factor,0,0); /* always use axis-aligned rendering for voxels (limitation of current 3D display code) */
			Vector3D ty = new Vector3D(0,1/coord.factor,0);
			Vector3D tz = new Vector3D(0,0,1/coord.factor);
			dos.writeDouble(tx.x);
			dos.writeDouble(tx.y);
			dos.writeDouble(tx.z);
			dos.writeDouble(ty.x);
			dos.writeDouble(ty.y);
			dos.writeDouble(ty.z);
			dos.writeDouble(tz.x);
			dos.writeDouble(tz.y);
			dos.writeDouble(tz.z);
			dos.writeDouble(coord.origin.x);
			dos.writeDouble(coord.origin.y);
			dos.writeDouble(coord.origin.z);
			dos.writeInt(centeringMethod);
			//dos.writeInt(activeGroup);
			dos.writeLong(activeGroup.length());
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

	public void addViewListener(ViewListener l) {
		listenerList.add(ViewListener.class, l);
	}

	public void removeViewListener(ViewListener l) {
		listenerList.remove(ViewListener.class, l);
	}

    /* Called by xall, yall, zall */
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
		request2D(true);
		request3D(false);
	}

/*************************** Display **********************************/
	public void componentResized(ComponentEvent e) {
		width2D = getWidth();
		height2D = getHeight();
		delta = (double) 1.0 / (height2D < width2D ? height2D : width2D);
		request2D(true); 
		//b=BufferUtil.newByteBuffer(width2D*height2D*height2D);
		//glcanvas.setPreferredSize(new Dimension(width2D,height2D));
		//glcanvas.setSize(new Dimension(width2D, height2D));
		//System.out.println("SizeWin: "+width2D+", "+height2D);
		displayImage();
	}

    /* Save current 2D image to a PNG file, name chosen by user */
	public void writePng() {
    	JFileChooser fc = new JFileChooser();
    	int returnVal = fc.showSaveDialog(this);
    	if(returnVal == JFileChooser.APPROVE_OPTION) {
        	File file = fc.getSelectedFile();
        	try {
            	FileOutputStream fos = new FileOutputStream(file);

            	int pix[]=new int [pixels.length];
            	for(int i=0;i<pixels.length;i++)
				{
					int index=0xff&(int)pixels[i];
					pix[i]=((0xff&colorBar.cm_red[index])<<16)+((0xff&colorBar.cm_green[index])<<8)+((0xff&colorBar.cm_blue[index])<<0);
				}
            	PngEncoder png = new PngEncoder(createImage(new MemoryImageSource(width2D, height2D, pix, 0, width2D)), false);
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
}
