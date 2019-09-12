package plugins.fmp.multicafeBatch;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;


public class MulticafeBatch  extends PluginActionable implements ChangeListener, PropertyChangeListener {

	IcyFrame 		mainFrame 			= new IcyFrame("Build kymographs 27-07-06-2019", true, true, true, true);
	BuildKymosListFilesPane 	listFilesPane 		= new BuildKymosListFilesPane();
	BuildKymosPane	buildKymosPane		= new BuildKymosPane();

	@Override
	public void run() {
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
