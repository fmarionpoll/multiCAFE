package plugins.fmp.multicafeSequence;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;



public class SequencePlusUtils {
	public static boolean isInterrupted = false;
	public static boolean isRunning = false;
	
	public static SequencePlus openFiles (String directory) {
		
		isRunning = true;
		String[] list = (new File(directory)).list();
		if (list == null)
			return null;
		
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(list.length);
		
		SequencePlus kymographSeq = new SequencePlus();
		int t= 0;
		
		for (String filename: list) {
			if (!filename.contains(".tiff"))
				continue;
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			final String name =  directory + "\\" + filename;
			progress.setMessage( "Load "+filename);
			
			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(name);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.addImage(t, ibufImage);
			
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf("\\")+1;
			String title = filename.substring(index0, index1);
			Capillary cap = new Capillary();
			cap.indexImage = t;
			cap.name = title;
			
			kymographSeq.loadXMLKymographAnalysis(cap, directory);
			
			t++;
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return kymographSeq;
	}
	
	public static SequencePlus openKymoFiles (String directory, Capillaries caps) {
		
		isRunning = true;
		SequencePlus kymos = new SequencePlus ();	

		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(caps.capillariesArrayList.size());
		
		int t=0;
		for (Capillary cop: caps.capillariesArrayList) {
			
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			final String name =  directory + "\\" + cop.roi.getName() + ".tiff";
			progress.setMessage( "Load "+name);
	
			try {
				IcyBufferedImage ibufImage = Loader.loadImage(name);
				if (t != 0 && (ibufImage.getWidth() != kymos.getWidth() || ibufImage.getHeight() != kymos.getHeight())) {
					Rectangle rect = new Rectangle(0, 0, kymos.getWidth(), kymos.getHeight() );
					ibufImage = IcyBufferedImageUtil.getSubImage(ibufImage, rect );
				}
				kymos.addImage(t, ibufImage);
			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			t++;
			kymos.loadXMLKymographAnalysis(cop, directory);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return kymos;
	}
	
	public static ArrayList<SequencePlus> openFiles (String directory, Capillaries capillaries) {
		
		isRunning = true;
		ArrayList<SequencePlus> arrayKymos = new ArrayList<SequencePlus> ();	

		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(capillaries.capillariesArrayList.size());
		
		for (Capillary cap: capillaries.capillariesArrayList) {
			
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			SequencePlus kymographSeq = new SequencePlus();
			final String name =  directory + "\\" + cap.roi.getName() + ".tiff";
			progress.setMessage( "Load "+name);
			
			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(name);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.addImage(0, ibufImage);
			
			String title = cap.roi.getName();
			kymographSeq.setName(title);
			kymographSeq.loadXMLKymographAnalysis(cap, directory);
			arrayKymos.add(kymographSeq);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return arrayKymos;
	}
	
	public static void saveKymosMeasures (SequencePlus vkymos, String directory) {
		
		isRunning = true;
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");
		progress.setLength(vkymos.getSizeT());
		
		for (Capillary cap : vkymos.capillaries.capillariesArrayList) {
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
			}

			if (!vkymos.saveXMLKymographAnalysis(cap, directory))
				System.out.println(" -> failed - in directory: " + directory);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
	}

	public static void transferSequenceInfoToKymos (SequencePlus vkymos, SequenceVirtual vSequence) {
		
		if (vkymos.capillaries == null)
			return;
					
		vkymos.capillaries.analysisStart = vSequence.analysisStart; 
		vkymos.capillaries.analysisEnd  = vSequence.analysisEnd;
		vkymos.capillaries.analysisStep = vSequence.analysisStep;
		
	}
}
