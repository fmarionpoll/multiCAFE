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
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;


public class MCKymos_File extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4381802490262298749L;
	private JButton		openButtonKymos			= new JButton("Load...");
	private JButton		saveButtonKymos			= new JButton("Save...");
	private MultiCAFE 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		
		JLabel loadsaveText1b = new JLabel ("-> Kymographs (tiff) ", SwingConstants.RIGHT);
		loadsaveText1b.setFont(FontUtil.setStyle(loadsaveText1b.getFont(), Font.ITALIC));	
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1b, openButtonKymos, saveButtonKymos));
		
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {	
		openButtonKymos.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null) {
				loadDefaultKymos(exp);
				firePropertyChange("KYMOS_OPEN", false, true);
			}
		}});
		saveButtonKymos.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null) {
				String path = exp.seqCamData.getDirectory() + File.separator + "results";
				saveKymographFiles(path);
				firePropertyChange("KYMOS_SAVE", false, true);
			}
		}});
	}

	void saveKymographFiles(String directory) {
		ProgressFrame progress = new ProgressFrame("Save kymographs");
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null)
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (directory == null) 
			directory = exp.seqCamData.getDirectory()+ File.separator+"results";
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
			for (int t = 0; t < seqKymos.seq.getSizeT(); t++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				progress.setMessage( "Save kymograph file : " + cap.getCapillaryName());
				cap.filenameTIFF = outputpath + File.separator + cap.getCapillaryName() + ".tiff";
				final File file = new File (cap.filenameTIFF);
				IcyBufferedImage image = seqKymos.seq.getImage(t, 0);
				ThreadUtil.bgRun( new Runnable() { @Override public void run() { 
						try {
							Saver.saveImage(image, file, true);
						} catch (FormatException | IOException e) {
							e.printStackTrace();
						}
					System.out.println("File "+ cap.getCapillaryName() + " saved " );
				}});
			}
		}
		progress.close();
	}
	
	boolean loadDefaultKymos(Experiment exp) {		
		boolean flag = false;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || exp.capillaries == null) {
			System.out.println("loadDefaultKymos: no parent sequence or no capillaries found");
			return flag;
		}
		
		if (seqKymos.isRunning_loadImages) {
			seqKymos.isInterrupted_loadImages = true;
			for (int i= 0; i < 10; i++) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!seqKymos.isRunning_loadImages)
					break;
			}
		}
		List<String> myList = exp.seqKymos.loadListOfKymographsFromCapillaries(exp.seqCamData.getDirectory(), exp.capillaries);
		if (seqKymos.isInterrupted_loadImages) {
			seqKymos.isInterrupted_loadImages = false;
			return false;
		}
	
		flag = seqKymos.loadImagesFromList(myList, true);
		if (seqKymos.isInterrupted_loadImages) {
			seqKymos.isInterrupted_loadImages = false;
			return false;
		}

		seqKymos.transferCapillariesToKymosRois(exp.capillaries);
		if (flag) {
			SwingUtilities.invokeLater(new Runnable() {
			    public void run() {
	        	parent0.paneKymos.tabDisplay.transferCapillaryNamesToComboBox(exp.capillaries.capillariesArrayList);
				}
			});
		}
		return flag;
	}
}
