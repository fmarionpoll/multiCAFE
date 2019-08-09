package plugins.fmp.multicafeBuildKymos;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;


public class BuildKymographsBatch  extends PluginActionable implements ChangeListener, PropertyChangeListener {

	IcyFrame 		mainFrame 			= new IcyFrame("Build kymographs 27-07-06-2019", true, true, true, true);
	ListFilesPane 	listFilesPane 		= new ListFilesPane();
	BuildKymosPane	buildKymosPane		= new BuildKymosPane();

	@Override
	public void run() {

		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(false);
		Icy.getMainInterface().getMainFrame().getInspector().imageCacheDisabled();

		// build and display the GUI
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		
		listFilesPane.init(mainPanel, "SOURCE", this);
		listFilesPane.addPropertyChangeListener(this);
		
		buildKymosPane.init(mainPanel, "KYMOGRAPHS", this);
		buildKymosPane.addPropertyChangeListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// ignore
		System.out.println("state change detected");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {	
	}

}
