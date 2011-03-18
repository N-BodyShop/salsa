/*
  This class stores all the OpenGL-specific state for SimulationView.
  By moving GL into this separate class, SimulationView can be created
  even when jogl.jar is missing.
  
  Dr. Orion Sky Lawlor, lawlor@alaska.edu, 2011-03-17 (Public Domain)
*/
import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import java.awt.Dimension;
import com.sun.opengl.util.BufferUtil;
import com.sun.gluegen.runtime.BufferFactory;

class SimulationViewGL implements GLEventListener {

	private SimulationView sv;
	public GLJPanel glcanvas;
	
	public SimulationViewGL(SimulationView sv_,int width2D,int height2D) {
		sv=sv_;
		
		GLCapabilities glcaps = new GLCapabilities();
		glcaps.setDoubleBuffered(true);
		glcaps.setHardwareAccelerated(true);
		
		glcanvas=new GLJPanel(glcaps);
		glcanvas.addGLEventListener(this);
		
		sv.b=BufferUtil.newByteBuffer(sv.width3D*sv.height3D*sv.depth3D);
		sv.b2=BufferUtil.newByteBuffer(sv.width2D*sv.height2D*3);
		sv.b3=BufferUtil.newByteBuffer(256*3);
	}
	public javax.swing.JComponent getMainComponent() {
		return glcanvas;
	}
	public void remove() {
		sv.remove(glcanvas);
	}

	public void init(GLAutoDrawable arg0) { sv.init(arg0); }
	public void display(GLAutoDrawable arg0) { sv.display(arg0); }
	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2){}
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}

};
