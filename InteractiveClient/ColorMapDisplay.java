import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;

public class ColorMapDisplay extends JLabel {
	private int width, height;
	private byte[] image;
	
	public ColorMapDisplay(int w, int h) {
		//System.out.println("Colormap called with width of " + w);
		width = w;
		height = h;
		image = new byte[width * height];
		byte value;
		for(int i = 0; i < width; i++) {
			value = (byte) (1 + 254.0 * i / width);
			for(int j = 0; j < height; j++)
				image[j * width + i] = value;
		}
		setSize(width, height);
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
		for(int i = 0; i < width; i++) {
		    value = (byte) (1 + 254.0 * i / width);
			for(int j = 0; j < height; j++)
				image[j * width + i] = value;
		}
		setSize(width, height);

	}
}
