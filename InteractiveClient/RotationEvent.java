
public class RotationEvent extends java.util.EventObject {
	double theta;
	Vector3D rotationAxis;
	
	public RotationEvent(Object source, double t, Vector3D axis) {
		super(source);
		theta = t;
		rotationAxis = new Vector3D(axis);
	}

}
