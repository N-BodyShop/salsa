import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;

public class ColorMapDisplay extends JLabel {
	private int numColorsReserved;
	private int width, height;
	private byte[] image;
	private ColorModel cm;
	
	public ColorMapDisplay(int w, int h, int reserved) {
		numColorsReserved = reserved;
		reset(w, h);
	}
	
	public ColorMapDisplay(int w, int h) {
		this(w, h, 5);
	}
	
	public ColorMapDisplay() {
		this(256, 10);
	}
	
	public void redisplay(ColorModel cm) {
		setIcon(new ImageIcon(createImage(new MemoryImageSource(width, height, cm, image, 0, width))));
	}
	
	public void reset(int w, int h) {
		width = w;
		height = h;
		
		byte value = 0;
		image = new byte[width * height];
		double numColors = 256 - numColorsReserved;
		for(int i = 0; i < width; i++) {
			value = (byte) ((byte) (numColorsReserved) + (byte) (numColors * i / width));
			for(int j = 0; j < height; j++)
				image[j * width + i] = value;
		}
		setSize(width, height);
	}
}
