import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class MainView extends JPanel implements ComponentListener{
	private String signature;
	private ParentPanel theParent;
	private CcsThread ccs;
	public Vector3D x,y,z,origin;
	public int width, height;
	public MemoryImageSource source;
	private byte[] pixels;
	public JLabel display;
	public boolean resizedImage;
	private double boxSize;
	private Config config;

	public MainView(ParentPanel arg, String sign){
		super(new BorderLayout());
		theParent = arg;
		ccs = new CcsThread(new Label(), theParent.host, theParent.port);
		addComponentListener(this);
		signature = sign;

		width = 512;
		height = 512;

		pixels = new byte[width * height];
		source = new MemoryImageSource(width, height, theParent.wrbb, pixels, 0, width);
		source.setAnimated(true);
		display = new JLabel();
		display.setIcon(new ImageIcon(display.createImage(source)));

		add(display, BorderLayout.CENTER);
	}

	/**************************************************************************************************/
	/*
	 * rotates the view
	 */
	public void drag(int xN, int yN, String aboutThis, double angle){
		if(xN==-1){
			//this first block is used by the sliders
			if(aboutThis.equals("x")){
				//rotate by theta degrees about xunit
				y.rotate(-angle, x.unitVector());
				z = x.cross(y);
				ccs.addRequest(new ImageRequest(), true);
			}else if(aboutThis.equals("y")){
				//rotate by theta degrees about yunit
				x.rotate(angle, y.unitVector());
				z=x.cross(y);
				ccs.addRequest(new ImageRequest(), true);
			}else if(aboutThis.equals("z")){
				//rotate by theta degrees about zunit
				x.rotate(angle, z.unitVector());
				y.rotate(angle, z.unitVector());
				ccs.addRequest(new ImageRequest(), true);
			}
		}else{
			//this block is used by click-dragging on the screen
			int oldX = theParent.start_x;
			int oldY = theParent.start_y;

			double theta = Math.PI * (xN - oldX) / 180.0;
			x.rotate(theta, y.unitVector());

			theta = Math.PI * (oldY - yN) / 180.0;
			y.rotate(-theta, x.unitVector());
			z = x.cross(y);

			ccs.addRequest(new ImageRequest(), true);
		}
	}

	/**************************************************************************************************/
	/*
	 * This function updates this MainView object with respect to the vector args of another
	 * MainView object
	 */
	public void update(Vector3D newX, Vector3D newY, Vector3D newZ, Vector3D newO, boolean update){
		x = new Vector3D(newX);
		y = new Vector3D(newY);
		z = new Vector3D(newZ);
		origin = new Vector3D(newO);
		if(theParent.controller.equals("mainView")){
			y.rotate(-Math.PI/2, x.unitVector());
		}else{
			y.rotate(Math.PI/2, x.unitVector());
		}
		z = x.cross(y);
		if(update){
			ccs.addRequest(new ImageRequest(), true);
		}else{}
	}
	
	/**************************************************************************************************/
	/* pan, according to mousedrags in ParentPanel */
	public void pan(MouseEvent e){
		origin = origin.plus((x.scalarMultiply(((double) theParent.start_x - e.getX()) / (theParent.getWidth()/2))).plus(y.scalarMultiply(((double) e.getY() - theParent.start_y) / getHeight())).scalarMultiply(2.0));
		ccs.addRequest(new ImageRequest(), true);
	}

	/**************************************************************************************************/
	/*
	 * called indirectly from CommandParser to pan the view
	 */

	public void numericPan(double scalar, String direction){
		if(direction.equals("up")){
			double amount = y.length()*scalar*2;
			origin = origin.plus(y.unitVector().scalarMultiply(-amount));
		}else if(direction.equals("down")){
			double amount = y.length()*scalar*2;
			origin = origin.plus(y.unitVector().scalarMultiply(amount));
		}else if(direction.equals("left")){
			double amount = x.length()*scalar*2;
			origin = origin.plus(x.unitVector().scalarMultiply(amount));
		}else if(direction.equals("right")){
			double amount = x.length()*scalar*2;
			origin = origin.plus(x.unitVector().scalarMultiply(-amount));
		}else if(direction.equals("in")){
			z = x.cross(y);
			double amount = z.length()*scalar*2;
			origin = origin.plus(z.unitVector().scalarMultiply(amount));
		}else if(direction.equals("out")){
			z = x.cross(y);
			double amount = z.length()*scalar*2;
			origin = origin.plus(z.unitVector().scalarMultiply(-amount));
		}
		ccs.addRequest(new ImageRequest(), true);
	}
	
	/**************************************************************************************************/
	/*
	 * called from mousedragging in ParentPanel to zoom the view
	 */
	public void zoom(MouseEvent e){
		int delta_y = e.getY() - theParent.start_y;
		double zoom;
		if(delta_y > 0){ 
			//zooming in
			zoom = 1.0 / (1.0 + (double) delta_y / (theParent.getHeight() - theParent.start_y));
		}else{ 
			//zooming out
			zoom = 1.0 - (double) delta_y / theParent.start_y;
		}
		x = x.scalarMultiply(zoom);
		y = y.scalarMultiply(zoom);
		ccs.addRequest(new ImageRequest(), true);
	}
	
	/**************************************************************************************************/
	/*
	 * called indirectly from CommandParser to zoom
	 */
	public void zoom(double scalar){
		x = x.scalarMultiply(scalar);
		y = y.scalarMultiply(scalar);
		ccs.addRequest(new ImageRequest(), true);
	}
	
	/**************************************************************************************************/
	/*
	 * translate the z location of the origin in simulation space according to mousedrags
	 */
	public void zTranslate(MouseEvent e){
		z = x.cross(y);
		double zShift = ((2*((double)theParent.start_y - e.getY()))/theParent.getHeight())*y.length();
		Vector3D translationVector = z.unitVector().scalarMultiply(zShift);
		if ((e.getY() - theParent.start_y) < 0) {
			origin = origin.plus(translationVector);
		} else {
			origin = origin.plus(translationVector);
		}
	}

	/**************************************************************************************************/

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}

	/*
	 * resize the view.  This method resizes the vectors appropriately to adjust the view,
	 * ensuring it fits correctly in the given space on screen
	 */
	public void componentResized(ComponentEvent e) {
		width = e.getComponent().getWidth();
		height = e.getComponent().getHeight();
		resizedImage = true;
		double xRatio = x.length() / width;
		double yRatio = y.length() / height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			x = x.scalarMultiply(factor);
		}
		theParent.cmdisplay.reset(width*2, 10);
		theParent.cmdisplay.redisplay(theParent.wrbb);

		ccs.addRequest(new ImageRequest(), true);

	}

	//*******************************************************************************************************//

	public void xAll(boolean rotated){
		//set up xall view and addd request
		origin = config.max.plus(config.min).scalarMultiply(0.5);

		x = new Vector3D(0, boxSize, 0);
		y = new Vector3D(0, 0, boxSize);
		z = new Vector3D(x.cross(y));

		double xRatio = x.length() / width;
		double yRatio = y.length() / height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			x = x.scalarMultiply(factor);
		}

		if(rotated){
			y.rotate(-Math.PI/2, x.unitVector());
		}
		ccs.addRequest(new ImageRequest(), true);
	}
	//*******************************************************************************************************//

	public void yAll(boolean rotated){
		//set up yall view and add request
		origin = config.max.plus(config.min).scalarMultiply(0.5);
		x = new Vector3D(boxSize, 0, 0);
		y = new Vector3D(0, 0, boxSize);
		z = new Vector3D(x.cross(y));

		double xRatio = x.length() / width;
		double yRatio = y.length() / height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			x = x.scalarMultiply(factor);
		}

		if(rotated){
			y.rotate(-Math.PI/2, x.unitVector());
		}
		ccs.addRequest(new ImageRequest(), true);
	}

	//*******************************************************************************************************//

	public void zAll(boolean rotated){
		//set up zall view and add request
		origin = config.max.plus(config.min).scalarMultiply(0.5);
		x = new Vector3D(boxSize,0,0);
		y = new Vector3D(0,boxSize,0);
		z = new Vector3D(x.cross(y));

		double xRatio = x.length() / width;
		double yRatio = y.length() / height;
		if(xRatio != yRatio) {
			double factor = yRatio / xRatio;
			x = x.scalarMultiply(factor);
		}

		if(rotated){
			y.rotate(-Math.PI/2, x.unitVector());
		}
		ccs.addRequest(new ImageRequest(), true);
	}
	
	//*******************************************************************************************************//
	/*
	public Vector3D getXVec(){return x;}
	public Vector3D getYVec(){return y;}
	public Vector3D getZVec(){return z;}
	public Vector3D getOVec(){return origin;}
	*/
	public String getSignature(){return signature;}
	
	/**************************************************************************************************
	 * byte[] encodeRequest
	 * encode the vector positions of THIS MainView object into binary for use by the server
	 */
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

			System.out.println("w: " + width + " h: " + height);
			System.out.println("x: " + x.toString());
			System.out.println("y: " + y.toString());
			System.out.println("z: " + z.toString());
			System.out.println("o: " + origin.toString());

		} catch(IOException e) {
			System.err.println("Couldn't encode request!");
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	/**************************************************************************************************
	 * void displayImage
	 * takes the array of data provided by the server and displays them on screen
	 */

	private void displayImage(byte[] data) {
		try {
			if(resizedImage || data.length != width * height) {
				source = new MemoryImageSource(width, height, theParent.wrbb, data, 0, width);
				source.setAnimated(true);
				display.setIcon(new ImageIcon(display.createImage(source)));
				if(data.length == width * height)
				resizedImage = false;
				pixels = data;
			} else {
				source.newPixels(data, theParent.wrbb, 0, width);
				pixels = data;
			}
		} catch (ArrayIndexOutOfBoundsException bla) {
			bla.printStackTrace();
		}
	}

	//*******************************************************************************************************//
	//*******************************************************************************************************//
	//													 //
	//                         SERVER REQUESTS APPEAR BELOW HERE		                                 //
	//													 //
	//*******************************************************************************************************//
	//*******************************************************************************************************//

	public void messageHub(String arg, boolean arg2){
		if(arg.equals("lvConfig")){
			ccs.addRequest(new CcsConfigRequest(arg2));
		}else if(arg.equals("newColor")){
			source = new MemoryImageSource(width, height, theParent.wrbb, pixels, 0, width);
			source.setAnimated(true);
			display.setIcon(new ImageIcon(display.createImage(source)));
		}else if(arg.equals("review")){
			System.out.println("REVIEW CALLED!");
			ccs.addRequest(new ImageRequest(), true);
		}
	}
	
	//*******************************************************************************************************//

	private class CcsConfigRequest extends CcsThread.request {
		private boolean rotated;

		public CcsConfigRequest(boolean rot) {
	    		super("lvConfig", 0);
			rotated = rot;
		}

		public void handleReply(byte[] configData){
			// for mapping System.out.println("CcsConfigRequest: handleReply");
	  		try {
	        		config = new Config(new DataInputStream(new ByteArrayInputStream(configData)));
				origin = x = y = z = new Vector3D(0, 0, 0);

				System.out.println("Config values: color=" + config.isColor + " push=" + config.isPush + " 3d=" + config.is3d);
	    			System.out.println("Box bounds: {(" + config.min.x + " " + config.min.y + " " + config.min.z + "),(" + config.max.x + " " + config.max.y + " " + config.max.z + ")}");
				boxSize = config.max.x - config.min.x;
				if((config.max.y - config.min.y != boxSize) || (config.max.z - config.min.z != boxSize)) {
					System.err.println("Box is not a cube!");
				}

				if(theParent.initialView.equals("xall")){
					xAll(rotated);
				}else if(theParent.initialView.equals("yall")){
					yAll(rotated);
				}else if(theParent.initialView.equals("zall")){
					zAll(rotated);
				}
		
			} catch(IOException e) {
				System.err.println("Fatal: Couldn't obtain configuration information");
				e.printStackTrace();
			}
		}
    	}
	
	/*******************************************************************************************************
	 * class ImageRequest
	 */

	private class ImageRequest extends CcsThread.request {

		public ImageRequest() {
			super("lvImage", null);
			setData(encodeRequest());
		}

		public void handleReply(byte[] data) {
			pixels = data;
			displayImage(data);
			if(theParent.resizeCall){
				theParent.resizeCall = false;
				theParent.refConfigPanel.packFrame();
			}
		}
	}
}

