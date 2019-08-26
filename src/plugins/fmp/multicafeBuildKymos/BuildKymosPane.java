package plugins.fmp.multicafeBuildKymos;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.image.IcyBufferedImage;
import icy.preferences.XMLPreferences;
import icy.sequence.DimensionId;
import icy.system.thread.ThreadUtil;
import loci.formats.FormatException;

import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.EnumStatus;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;
import plugins.fmp.multicafeSequence.SequenceCamData;

import plugins.fmp.multicafeTools.BuildKymographs;


public class BuildKymosPane  extends JPanel implements ActionListener, ViewerListener {
	/**
	 * 
	 */
	private static final long serialVersionUID 	= -1610357726091762089L;
	public JButton 					startComputationButton 	= new JButton("Start");
	public JButton 					stopComputationButton 	= new JButton("Stop");
	
	SequenceCamData 				seqCamData 				= null;
	SequenceKymos					seqKymos				= null;
	
	private BuildKymographs 		buildKymographsThread 	= null;
	private Viewer 					viewer1 				= null;
	private Thread 					thread 					= null;
	private int						analyzeStep 			= 1; 
	// TODO:  textbox? add checkbox for registration
	private int 					diskRadius 				= 5;
	private BuildKymographsBatch 	parent0 				= null;
	private List<String> 			discardedNames			= new ArrayList <String> ();
	
	
	public void init (JPanel mainPanel, String string, BuildKymographsBatch parent0) {
		this.parent0 = parent0;

		final JPanel kymographsPanel = GuiUtil.generatePanel("KYMOGRAPHS");
		mainPanel.add(GuiUtil.besidesPanel(kymographsPanel));
		kymographsPanel.add(GuiUtil.besidesPanel(startComputationButton, stopComputationButton));
		JLabel startLabel = new JLabel("start "); 
		startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel endLabel = new JLabel("end "); 
		endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		startComputationButton.addActionListener(this);
		stopComputationButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == startComputationButton) {		
			startComputation();
		}

		else if ( o == stopComputationButton ) {
			stopComputation();
		}
	}

	private void startComputation() {
		boolean foundstack = false;
		while (!foundstack) {
			int nstacks = ((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getSize();
			if (nstacks == 0) 
				return; 
	
			String oo = ((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getElementAt(0);
			boolean flag = loadSequence(oo);
			if (!flag) {
				System.out.println("sequence "+oo+ " could not be opened: skip record");
				discardedNames.add(oo);
				((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).removeElement(oo);
			}
			else {
			
				stopComputationButton.setEnabled(true);
				startComputationButton.setEnabled(false);
				
				loadRois(oo);
				if (seqKymos.capillaries.capillariesArrayList.size() >0)
					foundstack = true;
				else {
					System.out.println("sequence "+oo+ " with no capillaries found: skip record");
					discardedNames.add(oo);
					((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).removeElement(oo);
				}
			}
		}
		
		initInputSequenceViewer();
		startstopBufferingThread();
		seqCamData.seq.setPositionT(0);
		kymosBuildKymographs();		
	}
	
	private void kymosBuildKymographs() {
		// build kymograph
		buildKymographsThread = new BuildKymographs();
		buildKymographsThread.options.seqCamData  	= seqCamData;
		buildKymographsThread.options.analyzeStep 	= analyzeStep;
		buildKymographsThread.options.startFrame 	= (int) seqCamData.analysisStart;
		buildKymographsThread.options.endFrame 		= (int) seqCamData.analysisEnd;
		buildKymographsThread.options.diskRadius 	= diskRadius;
		buildKymographsThread.options.doRegistration= false; // doRegistrationCheckBox.isSelected();
		buildKymographsThread.options.seqKymos		= seqKymos;
		
		thread = new Thread(buildKymographsThread);
		thread.start();

		Thread waitcompletionThread = new Thread(new Runnable(){ public void run() {
			try { 
				thread.join();
			}
			catch(Exception e){;} 
			finally { 
				stopComputation();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						saveComputation();
						startComputationButton.setEnabled(true);
						String oo = ((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getElementAt(0);
						((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).removeElement(oo);
						if (!buildKymographsThread.stopFlag)
							startComputationButton.doClick();
					}});
			}
		}});
		waitcompletionThread.start();
	}
	
	private void stopComputation() {	
		if (thread != null && thread.isAlive()) {
			buildKymographsThread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		startComputationButton.setEnabled(true);
		stopComputationButton.setEnabled(false);
	}

	private boolean loadSequence(String oo) {
		// open sequence
		File oofile = new File(oo);
		String csdummy = oofile.getParentFile().getAbsolutePath();
		seqCamData = new SequenceCamData();
		seqCamData.loadSequence(csdummy);
		seqCamData.setFileName(csdummy);
		if (seqCamData.status == EnumStatus.FAILURE) {
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			String lastUsedPath = guiPrefs.get("lastUsedPath", "");
			String path = seqCamData.loadSequenceFromDialog(lastUsedPath);
			if (path.isEmpty())
				return false;
			seqCamData.setFileName(path);
			guiPrefs.put("lastUsedPath", path);
		}
		System.out.println("sequence openened: "+ seqCamData.getFileName());
		return true;
	}

	private void loadRois(String oo) {
		System.out.println("read capillaries info for: "+ oo);
		String filename = seqCamData.getFileName();
		if (filename != null) {
			seqKymos = new SequenceKymos();
			if (!oo .contains("capillarytrack") && oo.contains(".xml")) {
				seqCamData.xmlReadROIs(oo);
				seqKymos.xmlReadRoiLineParameters(oo);
			}
			SequenceKymosUtils.transferCamDataROIStoKymo(seqCamData, seqKymos);
		}
		seqKymos.xmlSaveCapillaryTrack(seqCamData.getDirectory());
	}

	private void initInputSequenceViewer () {
		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				viewer1 = new Viewer(seqCamData.seq, true);
			}
		}, true);
		if (viewer1 == null) {
			viewer1 = seqCamData.seq.getFirstViewer(); 
			if (!viewer1.isInitialized()) {
				try {
					Thread.sleep(1000);
					if (!viewer1.isInitialized())
						System.out.println("Viewer still not initialized after 1 s waiting");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Rectangle rectv = viewer1.getBoundsInternal();
		Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		viewer1.setBounds(rectv);
	}
	
	private void startstopBufferingThread() {
		if (seqCamData == null)
			return;
		seqCamData.analysisStep = analyzeStep;
	}

	private void saveComputation() {
		Path dir = Paths.get(seqCamData.getDirectory());
		dir = dir.resolve("results");
		String directory = dir.toAbsolutePath().toString();
		if (Files.notExists(dir))  {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return;
			}
		}

		ProgressFrame progress = new ProgressFrame("Save kymographs");		
		for (int t = 0; t < seqKymos.seq.getSizeT(); t++) {
			Capillary cap = seqKymos.capillaries.capillariesArrayList.get(t);
			progress.setMessage( "Save kymograph file : " + cap.getName());	
			String filename = directory + File.separator + cap.getName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = seqKymos.seq.getImage(t, 0);
			try {
				Saver.saveImage(image, file, true);
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		progress.close();
		closeSequence();
	}
	
	private void closeSequence() {	
		seqKymos.seq.removeAllROI();
		seqKymos.seq.close();
		seqCamData.seq.removeAllROI();
		seqCamData.seq.close();
		seqCamData = null;
		seqKymos = null;
	}

	@Override	
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
            seqCamData.currentFrame = event.getSource().getPositionT() ;  
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}
}
