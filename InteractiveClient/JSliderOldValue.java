import javax.swing.JSlider;

public class JSliderOldValue extends JSlider {
	private int oldValue;
	
	public JSliderOldValue(int min, int max, int value) {
		super(min, max, value);
		oldValue = value;
	}
	
	public int getOldValue() {
		return oldValue;
	}
	
	public void setOldValue(int value) {
		oldValue = value;
	}
}
