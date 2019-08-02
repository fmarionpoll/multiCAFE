package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafeTools.BuildKymographsThread;
import plugins.fmp.multicafeTools.EnumStatusComputation;


public class MCCapillaryTab_BuildKymos extends JPanel { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	
	JButton 						kymoStartComputationButton 	= new JButton("Start");
	JButton 						kymosStopComputationButton 	= new JButton("Stop");
	JSpinner 						diskRadiusSpinner 			= new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
	JCheckBox 						doRegistrationCheckBox 		= new JCheckBox("registration", false);
	
	EnumStatusComputation 			sComputation 				= EnumStatusComputation.START_COMPUTATION; 
	
	private MultiCAFE 				parent0						= null;
	private BuildKymographsThread 	buildKymographsThread 		= null;
	private Thread 					thread 						= null;


	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(
				kymoStartComputationButton, 
				kymosStopComputationButton));
		add(GuiUtil.besidesPanel(
				new JLabel("area around ROIs", SwingConstants.RIGHT), 
				diskRadiusSpinner, 
				new JLabel (" "), doRegistrationCheckBox
				));
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			kymosBuildStart();
		}});
		
		kymosStopComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			kymosBuildStop();
		}});	
	}
	
	private void setStartButton(boolean enableStart) {
		kymoStartComputationButton.setEnabled(enableStart );
		kymosStopComputationButton.setEnabled(!enableStart);
	}
	
	// -----------------------------------
	
	private void kymosBuildStart() {
		if (parent0.vSequence == null) 
			return;
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		parent0.sequencePane.browseTab.getBrowseItems (parent0.vSequence);
		parent0.vSequence.cleanUpBufferAndRestart();
		setStartButton(false);
		kymosBuildKymographs();	
		Viewer v = parent0.vSequence.getFirstViewer();
		v.toFront();
	}
	
	private void kymosBuildStop() {	
		if (thread != null && thread.isAlive()) {
			buildKymographsThread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		parent0.capillariesPane.optionsTab.displayViews (true);
	}
	
	private void resetUserInterface() {
		sComputation = EnumStatusComputation.START_COMPUTATION;
		firePropertyChange( "KYMOS_CREATE", false, true);
		setStartButton(true);
		firePropertyChange( "KYMOS_OK", false, true);
	}
	
	private void kymosBuildKymographs() {
		buildKymographsThread = null;
		if (parent0.vkymos != null) {
			parent0.vkymos.close();
		}
		
		// start building kymos in a separate thread
		buildKymographsThread = new BuildKymographsThread();
		buildKymographsThread.options.vSequence 	= parent0.vSequence;
		buildKymographsThread.options.analyzeStep 	= parent0.vSequence.analysisStep;
		buildKymographsThread.options.startFrame 	= (int) parent0.vSequence.analysisStart;
		buildKymographsThread.options.endFrame 		= (int) parent0.vSequence.analysisEnd;
		buildKymographsThread.options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		buildKymographsThread.options.doRegistration= doRegistrationCheckBox.isSelected();
		buildKymographsThread.vkymos 				= parent0.vkymos;

		thread = new Thread(null, buildKymographsThread, "buildkymos");
		thread.start();
		
		Thread waitcompletionThread = new Thread(null, new Runnable() {
			public void run() {
				try{ 
					thread.join();
					}
				catch(Exception e){;} 
				finally { 
					kymosBuildStop();
					resetUserInterface();
				}
			}}, "waitforcompletion");
		waitcompletionThread.start();
	}

}
