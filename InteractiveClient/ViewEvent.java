
public class ViewEvent extends java.util.EventObject {
	double theta = 0;
	Vector3D rotationAxis = null;
	
	public ViewEvent(Object source) {
		super(source);
	}
	
	public ViewEvent(Object source, double t, Vector3D axis) {
		super(source);
		theta = t;
		rotationAxis = new Vector3D(axis);
	}

}
