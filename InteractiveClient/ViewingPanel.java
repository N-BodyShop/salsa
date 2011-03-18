//ViewingPanel.java

import java.awt.*;
import java.awt.event.*;

import javax.media.opengl.GLCapabilities;
import javax.swing.*;
import javax.swing.event.*;

public class ViewingPanel extends JFrame {
	WindowManager windowManager;
	ColorBarPanel colorBar;
	SimulationView view;
	SideBar side;
	
	public ViewingPanel(WindowManager wm) {
		super("Salsa: Simulation View");
		windowManager = wm;
                addWindowListener(wm);
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
		
		Dimension ss=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		// ss.width=800; ss.height=480; // <- for NetBook testing
		
		int width = ss.width/2;  /* basic width of 2D view panel */
		int height = ss.height-100;  /* leave room for window title bar, start menu, etc */
		
		colorBar = new ColorBarPanel(windowManager.sim, 20, height);
		view = new SimulationView(windowManager, width, height, colorBar);
		colorBar.setView(view);	
		side= new SideBar(windowManager, view);
		
		setJMenuBar(new MenuBar(windowManager,view));
		c.add(colorBar, BorderLayout.WEST);
		c.add(view, BorderLayout.CENTER);
		c.add(side, BorderLayout.EAST);
		
/*		Action newViewAction = new AbstractAction() {
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
		getActionMap().put("quit", quitAction);*/
		
		pack();
		setVisible(true);
	}
		
}
