import java.util.EventListener;

public interface ViewListener extends EventListener {

    public void rotationPerformed(ViewEvent e);
	
	public void viewReset(ViewEvent e);

}
