import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class LocalParticle extends JPanel
                        implements ActionListener {
    
    WindowManager windowManager;
    JTextField codeField;
    
    public LocalParticle(WindowManager wm) {
	super(new BorderLayout());
	windowManager = wm;

	Box b = new Box(BoxLayout.LINE_AXIS);
	b.add(new JLabel("LocalParticle Code: "));
	codeField = new JTextField(20);
	b.add(codeField);
	JButton button = new JButton("Execute");
	button.setActionCommand("new");
	button.addActionListener(this);
	b.add(button);
	add(b, BorderLayout.CENTER);
	}
    public void actionPerformed(ActionEvent e) {
	
	System.out.println("Executing: " + codeField.getText());
	//	windowManager.ccs.addRequest(new ExecuteLocalCode("import ck\ndef localparticle(p):\n\t" + codeField.getText() + "\n\0localparticle"));
	windowManager.ccs.addRequest(new ExecuteLocalCode("import ck\ndef localparticle(p):\n\t" + codeField.getText() + "\n"));
    }
    public class ExecuteLocalCode extends CcsThread.request {
	public ExecuteLocalCode(String code) {
	    super("LocalParticleCode", code);
	    }
	public void handleReply(byte[] data) {
	    String result = new String(data);
	    System.out.println("Return from code execution: \"" + result + "\"");
	    }
	
	}
}

	
