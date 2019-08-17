package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.system.thread.ThreadUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;


public class MCCapillariesTab_File extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4019075448319252245L;
	
	private JButton		openButtonCapillaries	= new JButton("Load...");
	private JButton		saveButtonCapillaries	= new JButton("Save...");
	private JButton		openButtonKymos			= new JButton("Load...");
	private JButton		saveButtonKymos			= new JButton("Save...");
	private MultiCAFE 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		
		JLabel loadsaveText1 = new JLabel ("-> Capillaries (xml) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonCapillaries, saveButtonCapillaries));
		
		JLabel loadsaveText1b = new JLabel ("-> Kymographs (tiff) ", SwingConstants.RIGHT);
		loadsaveText1b.setFont(FontUtil.setStyle(loadsaveText1b.getFont(), Font.ITALIC));	
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1b, openButtonKymos, saveButtonKymos));
		
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {	
		openButtonCapillaries.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("CAPILLARIES_NEW", false, true);
		}}); 
		saveButtonCapillaries.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("CAP_ROIS_SAVE", false, true);
		}});	
		openButtonKymos.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			parent0.seqKymos.loadListOfKymographsFromCapillaries(parent0.seqCamData.getDirectory());
			firePropertyChange("KYMOS_OPEN", false, true);	
		}});
		saveButtonKymos.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			String path = parent0.seqCamData.getDirectory() + File.separator + "results";
			saveFiles(path);
			firePropertyChange("KYMOS_SAVE", false, true);
		}});
	}
	
	boolean loadCapillaryTrack() {	
		boolean flag = false;
		if (parent0.seqKymos == null)
			parent0.seqKymos = new SequenceKymos();
		flag = parent0.seqKymos.xmlLoadCapillaryTrack(parent0.seqCamData.getDirectory());
		if (flag) {
			SequenceKymosUtils.transferKymoCapillariesToCamData (parent0.seqCamData, parent0.seqKymos);
		}
		return flag;
	}
	
	boolean saveCapillaryTrack() {
		parent0.capillariesPane.unitsTab.getCapillariesInfosFromDialog(parent0.seqKymos.capillaries);
		parent0.sequencePane.infosTab.getCapillariesInfosFromDialog(parent0.seqKymos.capillaries);
		parent0.capillariesPane.buildarrayTab.getCapillariesInfosFromDialog(parent0.seqKymos.capillaries);
		parent0.sequencePane.browseTab.getAnalyzeFrameAndStep (parent0.seqCamData);
		if (parent0.seqKymos.capillaries.capillariesArrayList.size() == 0)
			parent0.seqKymos.updateCapillariesFromCamData(parent0.seqCamData);
		return parent0.seqKymos.xmlSaveCapillaryTrack(parent0.seqCamData.getDirectory());
	}

	void saveFiles(String directory) {
		ProgressFrame progress = new ProgressFrame("Save kymographs");
		if (directory == null) {
			directory = parent0.seqCamData.getDirectory()+ "\\results";
			}
		try {
			Files.createDirectories(Paths.get(directory));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String outputpath =  directory;
		JFileChooser f = new JFileChooser(outputpath);
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
		int returnedval = f.showSaveDialog(null);
		if (returnedval == JFileChooser.APPROVE_OPTION) { 
			outputpath = f.getSelectedFile().getAbsolutePath();		
			for (int t = 0; t < parent0.seqKymos.seq.getSizeT(); t++) {
				Capillary cap = parent0.seqKymos.capillaries.capillariesArrayList.get(t);
				progress.setMessage( "Save kymograph file : " + cap.getName());
				cap.filenameTIFF = outputpath + File.separator + cap.getName() + ".tiff";
				
				final File file = new File (cap.filenameTIFF);
				IcyBufferedImage image = parent0.seqKymos.seq.getImage(t, 0);
				
				ThreadUtil.bgRun( new Runnable() { @Override public void run() { 
					try {
						Saver.saveImage(image, file, true);
					} catch (FormatException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("File "+ cap.getName() + " saved " );
				}});
			}
			System.out.println("End of Kymograph saving process");
		}
		progress.close();
	}
	
	boolean loadDefaultKymos() {		
		boolean flag = false;

		SequenceKymos seqk = parent0.seqKymos;
		if (seqk == null || seqk.capillaries == null) {
			System.out.println("loadDefaultKymos: no parent sequence or no capillaries found");
			return flag;
		}
		
		if (seqk.isRunning_loadImages) {
			seqk.isInterrupted_loadImages = true;
			for (int i= 0; i < 10; i++) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!seqk.isRunning_loadImages)
					break;
			}
		}
		
		List<String> myList = seqk.loadListOfKymographsFromCapillaries(parent0.seqCamData.getDirectory());
		if (seqk.isInterrupted_loadImages) {
			seqk.isInterrupted_loadImages = false;
			return false;
		}
		
		flag = seqk.loadImagesFromList(myList, true);
		if (seqk.isInterrupted_loadImages) {
			seqk.isInterrupted_loadImages = false;
			return false;
		}

		if (flag) {
			SwingUtilities.invokeLater(new Runnable() {
			    public void run() {
	        	parent0.capillariesPane.optionsTab.transferCapillaryNamesToComboBox(seqk.capillaries.capillariesArrayList);
				parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
			    }
			});
		}
		return flag;
	}
}
