//Manager.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class Manager extends javax.swing.JFrame {
	protected WindowManager windowManager;
	
	public Manager(String s, WindowManager wm) {
		super(s);
		windowManager = wm;
		
		Action newViewAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				windowManager.addView();
			}
		};
		Action closeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//find containing Manager
				Container c = ((JRootPane) e.getSource()).getParent();
				while(!(c instanceof Manager))
					c = c.getParent();
				//tell frame to close, the WindowManager will hear this event
				c.dispatchEvent(new WindowEvent((Manager) c, WindowEvent.WINDOW_CLOSING));
			}
		};
		Action quitAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				windowManager.quit();
			}
		};
		//need a JComponent to do Key Binding
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control N"), "newView");
		getRootPane().getActionMap().put("newView", newViewAction);
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control W"), "closeView");
		getRootPane().getActionMap().put("closeView", closeAction);
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Q"), "quit");
		getRootPane().getActionMap().put("quit", quitAction);
	}
}
