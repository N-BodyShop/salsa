/**
 * class Storage is used by InputPanel and ConfigPanel to store and retrieve (respectively) the hostname and port
 * name and number
 */
public class Storage{
	private static int port;	//the port number inputted in InputPanel
	private static String host;	//the host String inputted in InputPanel
	
	public static void setHost(String s){
		host = s;
	}
	
	public static void setPort(String s){
		port = Integer.parseInt(s);
	}
	
	public static String getHost(){
		return host;
	}
	
	public static int getPort(){
		return port;
	}
	
}
