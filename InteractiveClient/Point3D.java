import java.awt.geom.*;

public class Point3D extends Point2D {
	private double x,y,z;

	public Point3D(){
		x=y=z=0;
	}
	
	public Point3D(double xLoc, double yLoc, double zLoc){
		x = xLoc; y = yLoc; z = zLoc;
	}
	
	public Point3D(Vector3D v){
		x=v.x;
		y=v.y;
		z=v.z;
	}

	public void setLocation(double x, double y){}

	public double getX(){return x;}
	public double getY(){return y;}
	public double getZ(){return z;}
	public void setX(double newX){x=newX;}
	public void setY(double newY){y=newY;}
	public void setZ(double newZ){z=newZ;}
	
	public String toString(){
		return ("(" + x + "," + y + "," + z + ")");
	}

}
