//ViewingPanel.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ViewingPanel extends JPanel {
	WindowManager windowManager;
	SimulationView view;
	ColorBarPanel colorBar;
	ToolBarPanel tools;
	
	public ViewingPanel(WindowManager wm) {
		super(new BorderLayout());
		
		windowManager = wm;
		
		int width = 512;
		
		colorBar = new ColorBarPanel(windowManager.sim, width, 20);
		view = new SimulationView(windowManager, colorBar.colorModel, width, width);
		colorBar.setView(view);		
		tools = new ToolBarPanel(windowManager, view);
		
		add(colorBar, BorderLayout.NORTH);
		add(view, BorderLayout.CENTER);
		add(tools, BorderLayout.SOUTH);
		
		Action newViewAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				windowManager.addView();
			}
		};
		Action closeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//find containing JFrame
				Container c = ((ViewingPanel) e.getSource()).getParent();
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
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control N"), "newView");
		getActionMap().put("newView", newViewAction);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control W"), "closeView");
		getActionMap().put("closeView", closeAction);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Q"), "quit");
		getActionMap().put("quit", quitAction);
		
	}
		
}
