//Vector3D.java

public class Vector3D {

	public double x, y, z;

	public Vector3D() {
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Vector3D(double a, double b, double c) {
		x = a;
		y = b;
		z = c;
	}

	public Vector3D(Vector3D v) {
		 x = v.x;
		 y = v.y;
		 z = v.z;
	}
	
	final public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	final public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	final public Vector3D plus(Vector3D v) {
		return new Vector3D(x + v.x, y + v.y, z + v.z);
	}

	final public Vector3D minus(Vector3D v) {
		return new Vector3D(x - v.x, y - v.y, z - v.z);
	}
	
	final public Vector3D scalarMultiply(double s) {
		return new Vector3D(s * x, s * y, s * z);
	}
			
	final public double dot(Vector3D v) {
		return x * v.x + y * v.y + z * v.z;
	}
	
	final public double costheta(Vector3D v) {
		double length_squared = dot(this);
		double length_squared_v = v.dot(v);
		if(length_squared * length_squared_v == 0)
			return 0;
		else
			return dot(v) / Math.sqrt(length_squared * length_squared_v);
	}
	
	final public Vector3D cross(Vector3D v) {
		return new Vector3D(y * v.z -z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
	}
	
	final public Vector3D unitVector() {
		return scalarMultiply(1 / length());
	}
	
	public String toString() {
		return "(" + x + "," + y + "," + z + ")";
	}
	
	public String toPyString() {
		return x + "," + y + "," + z;
	}
	
	final public void rotateUpDown(double theta) {
		double c = Math.cos(theta);
		double s = Math.sin(theta);
		double oldy = y;
		y = oldy * c + z * s;
		z = -oldy * s + z * c;
	}

	final public void rotateLeftRight(double theta) {
		double c = Math.cos(theta);
		double s = Math.sin(theta);
		double oldx = x;
		x = oldx * c - z * s;
		z = oldx * s + z * c;
	}

	final public void rotateClockCounter(double theta) {
		double c = Math.cos(theta);
		double s = Math.sin(theta);
		double oldx = x;
		x = oldx * c + y * s;
		y = -oldx * s + y * c;
	}
	
	private class Quaternion {
		
		public double w;
		public Vector3D v;
		
		public Quaternion(double w_, double x, double y, double z) {
			w = w_;
			v = new Vector3D(x, y, z);
		}
		
		public Quaternion(double w_, Vector3D v_) {
			w = w_;
			v = new Vector3D(v_);
		}
		
		public Quaternion prime() {
			return new Quaternion(w, -v.x, -v.y, -v.z);
		}
		
		public Quaternion multiply(Quaternion q) {
			return new Quaternion(w * q.w - v.dot(q.v), q.v.scalarMultiply(w).plus(v.scalarMultiply(q.w)).plus(v.cross(q.v)));
		}
		
		public double length() {
			return Math.sqrt(w * w + v.x * v.x + v.y * v.y + v.z * v.z);
		}
		
		public double lengthSquared() {
			return w * w + v.x * v.x + v.y * v.y + v.z * v.z;
		}
	}
	
	//rotate by theta around the axis given by the unit vector u (using quaternions)
	final public void rotate(double theta, Vector3D u) {
		Quaternion q = new Quaternion(Math.cos(theta / 2), u.scalarMultiply(Math.sin(theta / 2)));
		q = q.multiply(new Quaternion(0, x, y, z)).multiply(q.prime());
		x = q.v.x;
		y = q.v.y;
		z = q.v.z;
	}
};
