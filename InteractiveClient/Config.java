/*
Configuration of the CCS server.  This class should match the 
"liveVizConfig" class in liveViz0.h.
*/
import java.io.*;

public class Config {
    public int version; //Version of CCS server's getImageConfig command
    public boolean isColor;
    public boolean isPush;
    public boolean is3d;
    public Vector3D min, max;
   
    private Vector3D readVector3D(DataInputStream is) throws IOException {
		return new Vector3D(is.readDouble(), is.readDouble(), is.readDouble());
    }
	
    public Config(DataInputStream is) throws IOException {
		version = is.readInt();
		isColor = (is.readInt() != 0);
		isPush = (is.readInt() != 0);
		is3d = (is.readInt() != 0);
		if (is3d) {
			min = readVector3D(is);
			max = readVector3D(is);
		} else {
			min = new Vector3D();
			max = new Vector3D();
		}
    }
}
