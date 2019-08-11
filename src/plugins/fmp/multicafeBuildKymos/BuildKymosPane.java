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
	
	SequenceCamData 				vSequence 				= null;
	SequenceKymos					vkymos					= null;
	private ArrayList <SequenceKymos> kymographArrayList 	= new ArrayList <SequenceKymos> ();
	 
	private BuildKymographs 		buildKymographsThread 	= null;
	private Viewer 					viewer1 				= null;
	private Thread 					thread 					= null;
	private int						analyzeStep 			= 1; // TODO:  textbox? add checkbox for registration
	private int 					diskRadius 				= 5;
	


private BuildKymographsBatch 	parent0 	= null;
	
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
		
		if (((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getSize() == 0) 
			return; 
				
		parent0.listFilesPane.xmlFilesJList.setSelectedIndex(0);
		String oo = ((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getElementAt(0);
		boolean flag = loadSequence(oo);
		if (!flag) {
			System.out.println("sequence "+oo+ " could not be opened: skip record");
			return;
		}
		loadRois(oo);
		initInputSequenceViewer();
		startstopBufferingThread();
		
		vSequence.seq.setPositionT(0);
		kymosBuildKymographs();
	}
	
	private void kymosBuildKymographs() {
		buildKymographsThread = null;
		if (kymographArrayList.size() > 0) {
			for (SequenceKymos seq:kymographArrayList)
				seq.seq.close();
		}
		kymographArrayList.clear();
		for (Capillary cap:vkymos.capillaries.capillariesArrayList) {
			SequenceKymos kymographSeq = new SequenceKymos();	
			kymographSeq.seq.setName(cap.getName());
			kymographArrayList.add(kymographSeq);
		} 
		
		// build kymograph
		buildKymographsThread = new BuildKymographs();
		buildKymographsThread.options.vSequence  	= vSequence;
		buildKymographsThread.options.analyzeStep 	= analyzeStep;
		buildKymographsThread.options.startFrame 	= (int) vSequence.analysisStart;
		buildKymographsThread.options.endFrame 		= (int) vSequence.analysisEnd;
		buildKymographsThread.options.diskRadius 	= diskRadius;
		buildKymographsThread.options.doRegistration= false; // doRegistrationCheckBox.isSelected();
		buildKymographsThread.vkymos 				= vkymos;

		// change display status
		stopComputationButton.setEnabled(true);
		startComputationButton.setEnabled(false);
		
		thread = new Thread(buildKymographsThread);
		thread.start();

		Thread waitcompletionThread = new Thread(new Runnable(){ public void run()
		{
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
		
		vSequence = new SequenceCamData();
		vSequence.loadSequence(csdummy);
		vSequence.setFileName(csdummy);
		if (vSequence.status == EnumStatus.FAILURE) {
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			String lastUsedPath = guiPrefs.get("lastUsedPath", "");
			String path = vSequence.loadSequenceFromDialog(lastUsedPath);
			if (path.isEmpty())
				return false;
			vSequence.setFileName(path);
			guiPrefs.put("lastUsedPath", path);
			vSequence.loadSequence(vSequence.getFileName());
		}
		System.out.println("sequence openened: "+ vSequence.getFileName());

		return true;
	}

	private void loadRois(String oo) {
		System.out.println("read capillaries info for: "+ oo);
		vSequence.seq.removeAllROI();
		String path = vSequence.getDirectory();
		boolean flag = vkymos.xmlReadCapillaryTrack(path+"\\capillarytrack.xml");
		if (flag) 
			SequenceKymosUtils.transferROIStoCapillaries(vSequence, vkymos);
	}

	private void initInputSequenceViewer () {

		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				viewer1 = new Viewer(vSequence.seq, true);
			}
		}, true);
		
		if (viewer1 == null) {
			viewer1 = vSequence.seq.getFirstViewer(); 
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

		if (vSequence == null)
			return;

		vSequence.analysisStep = analyzeStep;
	}

	private void saveComputation() {
		
		Path dir = Paths.get(vSequence.getDirectory());
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

		// send some info
		ProgressFrame progress = new ProgressFrame("Save kymographs");

		// save capillarytrack.xml
		String name = vSequence.getDirectory()+ "\\capillarytrack.xml";
		vkymos.capillaries.xmlWriteROIsAndDataNoQuestion(name, vkymos);
		
		for (SequenceKymos seq: kymographArrayList) {
			progress.setMessage( "Save kymograph file : " + seq.seq.getName());

			String filename = directory + "\\" + seq.seq.getName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = seq.seq.getFirstImage();
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
		
		for (SequenceKymos seq:kymographArrayList)
			seq.seq.close();
		kymographArrayList.clear();
		vSequence.seq.close();
	}

	@Override	
	public void viewerChanged(ViewerEvent event)
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
            vSequence.currentFrame = event.getSource().getPositionT() ;  
	}

	@Override
	public void viewerClosed(Viewer viewer)
	{
		viewer.removeListener(this);
	}
}
