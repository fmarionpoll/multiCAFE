package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
			String directory = parent0.vSequence.getDirectory()+ "\\results";
			parent0.vkymos = SequenceKymosUtils.openKymoFiles(directory, parent0.vSequence.capillaries); 
			firePropertyChange("KYMOS_OPEN", false, true);	
		}});
		saveButtonKymos.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			String path = parent0.vSequence.getDirectory() + "\\results";
			saveFiles(path);
			firePropertyChange("KYMOS_SAVE", false, true);
		}});
	}
	
	boolean loadCapillaryTrack(String csFileName) {	
		boolean flag = false;
		if (csFileName == null)
			flag = parent0.vSequence.xmlReadCapillaryTrackDefault();
		else
			flag = parent0.vSequence.xmlReadCapillaryTrack(csFileName);
		return flag;
	}
	
	boolean saveCapillaryTrack() {
		parent0.sequencePane.browseTab.getBrowseItems (parent0.vSequence);
		return parent0.vSequence.xmlWriteCapillaryTrackDefault();
	}

	void saveFiles(String directory) {
		ProgressFrame progress = new ProgressFrame("Save kymographs");
		if (directory == null) {
			directory = parent0.vSequence.getDirectory()+ "\\results";
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
			for (int i = 0; i < parent0.vkymos.seq.getSizeT(); i++) {
				Capillary cap = parent0.vkymos.capillaries.capillariesArrayList.get(i);
				progress.setMessage( "Save kymograph file : " + cap.getName());
				String filename = outputpath + "\\" + cap.getName() + ".tiff";
				final File file = new File (filename);
				IcyBufferedImage image = parent0.vkymos.seq.getImage(i, 0);
				
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
		if (SequenceKymosUtils.isRunning)
			SequenceKymosUtils.isInterrupted = true;
		
		if (parent0.vSequence== null || parent0.vSequence.capillaries == null) {
			System.out.println("loadDefaultKymos: no parent sequence or no capillaries found");
			return flag;
		}
		
		String directory = parent0.vSequence.getDirectory()+"\\results";
		parent0.vkymos = SequenceKymosUtils.openKymoFiles(directory, parent0.vSequence.capillaries);

		if (parent0.vkymos != null) {
			flag = true;
			SwingUtilities.invokeLater(new Runnable() {
			    public void run() {
	        	parent0.capillariesPane.optionsTab.transferCapillaryNamesToComboBox(parent0.vSequence.capillaries.capillariesArrayList);
				parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
			    }
			});
		}
		return flag;
	}
}
