//CodePanel.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class CodePanel extends JPanel /*implements ActionListener*/ {
	WindowManager windowManager;
	JTextArea codeText;
	JTextArea responseText;
	
	public CodePanel(WindowManager wm) {
		super(new BorderLayout());
		
		windowManager = wm;
		
		codeText = new JTextArea(15, 40);
		codeText.setTabSize(4);
		codeText.setFont(new Font("Monospaced", Font.PLAIN, 12));
		
		responseText = new JTextArea(10, 40);
		responseText.setEditable(false);
		responseText.setTabSize(4);
		responseText.setFont(new Font("Monospaced", Font.PLAIN, 12));
		responseText.setLineWrap(true);
		
		JButton goButton = new JButton("Execute code on server");
		
		Box b = new Box(BoxLayout.PAGE_AXIS);
		b.add(new JScrollPane(codeText));
		b.add(new JScrollPane(responseText));
		b.add(goButton);
		add(b, BorderLayout.CENTER);
		
		Action closeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//find containing JFrame
				Container c = ((CodePanel) e.getSource()).getParent();
				while(!(c instanceof JFrame))
					c = c.getParent();
				//tell frame to close, the WindowManager will hear this event
				c.dispatchEvent(new WindowEvent((JFrame) c, WindowEvent.WINDOW_CLOSING));
			}
		};
		Action quitAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				windowManager.quit();
			}
		};
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control W"), "closeView");
		getActionMap().put("closeView", closeAction);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Q"), "quit");
		getActionMap().put("quit", quitAction);
		
		Action executeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				windowManager.ccs.addRequest(new ExecutePythonCode(codeText.getText() + "\n"));
			}
		};
		codeText.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift ENTER"), "execute");
		codeText.getActionMap().put("execute", executeAction);
		goButton.addActionListener(executeAction);		

	}
	/*
	public void actionPerformed(ActionEvent e) {
		//System.out.println("Sending code: \"" + codeText.getText() + "\"");
		windowManager.ccs.addRequest(new ExecutePythonCode(codeText.getText() + "\n"));
	}
	*/
	private class ExecutePythonCode extends CcsThread.request {
		public ExecutePythonCode(String s) {
			super("ExecutePythonCode", s);
		}
		
		public void handleReply(byte[] data) {
			String result = new String(data);
			responseText.append(result);
			//System.out.println("Return from code execution: \"" + result + "\"");
		}
	}
}
