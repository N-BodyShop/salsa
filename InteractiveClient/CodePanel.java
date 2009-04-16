//CodePanel.java

import charm.ccs.CcsServer;
import charm.ccs.CcsThread;
import charm.ccs.PythonAbstract;
import charm.ccs.PythonExecute;
import charm.ccs.PythonPrint;
import charm.ccs.PythonFinished;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class CodePanel extends JPanel /*implements ActionListener*/ {
	WindowManager windowManager;
	JTextArea codeText;
	JTextArea responseText;
	private int interpreterHandle;
	private int printRequest;
	
	public CodePanel(WindowManager wm) {
		super(new BorderLayout());
		
		windowManager = wm;
		interpreterHandle = 0;
		
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
			PythonExecute myrequest
			    = new PythonExecute(codeText.getText()+"\n",
						true,true,interpreterHandle);
			printRequest = 0;
			myrequest.setKeepPrint(true);
			myrequest.setWait(true);
			windowManager.ccs.addRequest(new ExecutePythonCode(myrequest.pack()));
			}
		};
		codeText.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift ENTER"), "execute");
		codeText.getActionMap().put("execute", executeAction);
		goButton.addActionListener(executeAction);		

	}

	private class ExecutePythonCode extends CcsThread.request {
		public ExecutePythonCode(byte[] s) {
			super("ExecutePythonCode", s);
		}
		
	    // ExecutePythonCode Replies:
	    // interpreterHandle: either handle of existing
	    // interpreter (if we asked for one), or handle of a new
	    // interpreter.  0xFFFFFFFF if we asked for an interpreter
	    // that is in use.
	    // From python print:
	    // 0 if print request on unknown interpreter
	    // string if valid print request
                public void handleReply(byte[] data) {
		    System.err.println("In handle Reply\n");
		    
		    if (printRequest == 0) {
		    // data will be 4 bytes... convert them to an int
			interpreterHandle = CcsServer.readInt(data, 0);
			printRequest = 1;
			System.err.println("Request print " + interpreterHandle);
			windowManager.ccs.addRequest(new ExecutePythonCode((new PythonPrint(interpreterHandle, false)).pack()));
			}
		    else {
                        String result = new String(data);
			if (result.length() > 0) {
			    if (result.length() == 4 && data[0]==0 && data[1]==0 && data[2]==0 && data[3]==0) responseText.append("No prints.\n");
			    else responseText.append(result+"\n");
			    }
                        //System.out.println("Return from code execution: \"" + result + "\"");
			}
                }
	}
}
