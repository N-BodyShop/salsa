//InteractiveWindow.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class InteractiveWindow extends JFrame implements ActionListener {
	
	private JTextArea functionInput;
	private JButton button;
	private CcsThread ccs;
	
	public InteractiveWindow(CcsThread c) {
		super("NChilada Function Definition");
		ccs = c;
		
		Box b = Box.createVerticalBox();
		
		b.add(new JLabel("Define a per-particle function:"));

		b.add(new JLabel("void perParticle(FullParticle& p) {"));
		
		getContentPane().add(b, BorderLayout.NORTH);
		
		functionInput = new JTextArea(12, 50);
		
		getContentPane().add(new JScrollPane(functionInput));
		
		b = Box.createVerticalBox();
		b.add(new JLabel("}"));
		
		button = new JButton("Apply function");
		button.addActionListener(this);
		b.add(button);
		
		getContentPane().add(b, BorderLayout.SOUTH);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		System.out.println("Button pressed, text contents: \"" + functionInput.getText() + "\"");
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(bytesOut);
		try {
			dataOut.writeInt(functionInput.getText().getBytes().length);
			dataOut.writeBytes(functionInput.getText());
		} catch(IOException ex) { }
		ccs.addRequest(new CcsThread.request("applyPerParticleHandler", bytesOut.toByteArray()));
	}
	
}
