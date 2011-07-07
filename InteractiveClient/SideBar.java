/*
 New 2009 right-hand sidebar.
 Written by Dain Harmon, from UAF.
 
 Calls out to a captive tabbed GroupManager, 
 ColoringManager, AttributeManager, ToolBarPanel.
*/
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class SideBar extends JPanel implements ActionListener
{
	int numTools=7;
	JButton toolbox []=new JButton[numTools];
	
	WindowManager windowManager;
	SimulationView view;
	
	public SideBar(WindowManager wm, SimulationView v)
	{
		windowManager = wm;
		view = v;
		
		//this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.setLayout(new BorderLayout());
		JPanel toolboxbox = new JPanel(new GridLayout(numTools, 1));
		String s="Rotate";
		toolbox[0]=new JButton(s);
		toolbox[0].setActionCommand(s);
		toolbox[0].addActionListener(this);
		toolboxbox.add(toolbox[0]);
		s="Zoom+";
		toolbox[1]=new JButton(s);
		toolbox[1].setActionCommand(s);
		toolbox[1].addActionListener(this);
		toolboxbox.add(toolbox[1]);
		s="Zoom-";
		toolbox[2]=new JButton(s);
		toolbox[2].setActionCommand(s);
		toolbox[2].addActionListener(this);
		toolboxbox.add(toolbox[2]);
		s="Select Sphere";
		toolbox[3]=new JButton(s);
		toolbox[3].setActionCommand(s);
		toolbox[3].addActionListener(this);
		toolboxbox.add(toolbox[3]);
		s="Select Box";
		toolbox[4]=new JButton(s);
		toolbox[4].setActionCommand(s);
		toolbox[4].addActionListener(this);
		toolboxbox.add(toolbox[4]);
		s="Ruler";
		toolbox[5]=new JButton(s);
		toolbox[5].setActionCommand(s);
		toolbox[5].addActionListener(this);
		toolboxbox.add(toolbox[5]);
		s="Refresh";
		toolbox[6]=new JButton(s);
		toolbox[6].setActionCommand(s);
		toolbox[6].addActionListener(this);
		toolboxbox.add(toolbox[6]);
		
		switchTool(0);
		
		this.add(toolboxbox, BorderLayout.WEST);
	
		JTabbedPane JTP=new JTabbedPane();
		JTP.addTab("Color", new ColoringManager(windowManager).getContentPane());
		JTP.addTab("Attributes", new AttributeManager(windowManager).getContentPane());
		JTP.addTab("Groups", new GroupManager(windowManager).getContentPane());
		JTP.addTab("Rendering", new ToolBarPanel(windowManager,v));
		this.add(JTP, BorderLayout.EAST);
	}
	
	public void switchTool(int tool)
    {
    	view.activeTool=tool;
		view.selectState=0;
		
		for (int i=0; i<numTools; i++)
			toolbox[i].setEnabled(true);
		toolbox[tool].setEnabled(false);
    }
	
	public void actionPerformed(ActionEvent e)
	{
		String command=e.getActionCommand();
		if (command.equals("Rotate"))
		{
			switchTool(0);
		}
		else if (command.equals("Zoom+"))
		{
			switchTool(1);
		}
		else if (command.equals("Zoom-"))
		{
			switchTool(2);
		}
		else if (command.equals("Select Sphere"))
		{
			switchTool(3);
		}
		else if (command.equals("Select Box"))
		{
			switchTool(4);
		}
		else if (command.equals("Ruler"))
		{
			switchTool(5);
		}
		else if (command.equals("Refresh"))
		{
			view.getNewDepth(true); // blocking, so we're really centered nicely
			view.getNewImage(true);
		}
	}
}
