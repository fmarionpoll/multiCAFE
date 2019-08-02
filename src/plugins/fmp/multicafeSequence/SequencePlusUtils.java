package plugins.fmp.multicafeSequence;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import loci.formats.FormatException;



public class SequencePlusUtils {
	
	public static boolean isInterrupted = false;
	public static boolean isRunning = false;
	
	private static int imageWidthMax = 0;
	private static int imageHeightMax = 0;
	private static ArrayList<Rectangle> rectList = null;
	
	// -------------------------------------------------------
	
	private static ArrayList<File> keepOnlyFilesMatchingCapillaries(File[] files, Capillaries capillaries) {
		ArrayList<File> filesArray= new ArrayList<File> ();
		for (int i= 0; i < files.length; i++) {
			String filename = files[i].getName();
			for (Capillary cap: capillaries.capillariesArrayList) {
				if (filename.contains(cap.roi.getName())) {
					filesArray.add(files[i]);
					break;
				}
			}
		}
		return filesArray;
	}
	
	private static void getMaxSizeofTiffFiles(ArrayList<File> files) {

		imageWidthMax = 0;
		imageHeightMax = 0;
		rectList = new ArrayList<Rectangle>(files.size());
		
		for (int i= 0; i < files.size(); i++) {
			File filetest = files.get(i);
			ImageInputStream imageStream = null;
			try {
				imageStream = ImageIO.createImageInputStream(filetest);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
			ImageReader reader = null;
			if(readers.hasNext()) {
				reader = readers.next();
			}else {
				try {
					imageStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//can't read image format... 
				continue;
			}
			
			reader.setInput(imageStream,true,true);
			int imageWidth = 0;
			int imageHeight= 0;
			try {
				imageWidth = reader.getWidth(0);
				if (imageWidth > imageWidthMax)
					imageWidthMax = imageWidth;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				imageHeight = reader.getHeight(0);
				if (imageHeight > imageHeightMax)
					imageHeightMax = imageHeight;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			reader.dispose();
			try {
				imageStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			rectList.get(i).width = imageWidth;
			rectList.get(i).height = imageHeight;
		}
	}
	
	private static void adjustImagesToMaxSize(ArrayList<File> files) {

		Rectangle rectRef = new Rectangle(0, 0, imageWidthMax, imageHeightMax );
		
		for (int i= 0; i < files.size(); i++) {
			if (rectList.get(i).width == imageWidthMax && rectList.get(i).height == imageHeightMax)
				continue;
			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(files.get(i).getPath());
			} catch (UnsupportedFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ibufImage = IcyBufferedImageUtil.getSubImage(ibufImage, rectRef );
			try {
				Saver.saveImage(ibufImage, files.get(i), true);
			} catch (FormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static void adjustImagesToSizeOfFirst(ArrayList<File> files) {

		IcyBufferedImage ibufImage = null;
		try {
			ibufImage = Loader.loadImage(files.get(0).getPath());
		} catch (UnsupportedFormatException | IOException e) {
			e.printStackTrace();
		}
		imageWidthMax = ibufImage.getWidth();
		imageHeightMax = ibufImage.getHeight();
		Rectangle rectRef = new Rectangle(0, 0, imageWidthMax, imageHeightMax );
		
		for (int i= 1; i < files.size(); i++) {
//			if (rectList.get(i).width == imageWidthMax && rectList.get(i).height == imageHeightMax)
//				continue;
			ibufImage = null;
			try {
				ibufImage = Loader.loadImage(files.get(i).getPath());
			} catch (UnsupportedFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (ibufImage.getWidth() == imageWidthMax && ibufImage.getHeight() == imageHeightMax)
				continue;
			ibufImage = IcyBufferedImageUtil.getSubImage(ibufImage, rectRef );
			try {
				Saver.saveImage(ibufImage, files.get(i), true);
			} catch (FormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static SequencePlus openKymoFiles (String directory, Capillaries parent_capillaries) {
		
		isRunning = true;
		File dir = new File(directory);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".tiff"));
		if (files == null)
			return null;
		
		ProgressFrame progress = new ProgressFrame("Read kymographs length");
		progress.setLength(files.length);
		
		ArrayList<File> filesArray = keepOnlyFilesMatchingCapillaries(files, parent_capillaries);
//		progress.setMessage("get size of tiff images");
//		getMaxSizeofTiffFiles(filesArray);
//		progress.setMessage("adjust size of tiff images");
//		adjustImagesToMaxSize(filesArray);
		
		adjustImagesToSizeOfFirst(filesArray);
		progress.setMessage("create kymographs sequence");
		String [] filenames = new String [filesArray.size()];
		for (int i=0; i< filesArray.size(); i++) {
			filenames[i] = filesArray.get(i).getName();
		}
		SequencePlus kymographSeq = new SequencePlus(filenames, directory);
		
		progress.setMessage("load measures for each capillary");
		kymographSeq.capillaries = new Capillaries();
		String [] listFiles = kymographSeq.getListofFiles();
		for (int i=0; i < listFiles.length; i++) {
			
			String filename = listFiles[i];
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf("\\")+1;
			String title = filename.substring(index0, index1);
			Capillary cap = new Capillary();
			cap.indexImage = i;
			cap.name = title;
			kymographSeq.loadXMLKymographAnalysis(cap, directory);
			kymographSeq.capillaries.capillariesArrayList.add(cap);
		}
		
		progress.close();
		isRunning = false;
		return kymographSeq;
	}
	
	/*
	public static SequencePlus openKymoFiles (String directory, Capillaries capillaries) {
		
		isRunning = true;
		SequencePlus kymos = new SequencePlus ();	

		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(capillaries.capillariesArrayList.size());
		
		int t=0;
		for (Capillary cap: capillaries.capillariesArrayList) {
			
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			final String name =  directory + "\\" + cap.roi.getName() + ".tiff";
			progress.setMessage( "Load "+name);
	
			try {
				IcyBufferedImage ibufImage = Loader.loadImage(name);
				if (t != 0 && (ibufImage.getWidth() != kymos.getWidth() || ibufImage.getHeight() != kymos.getHeight())) {
					Rectangle rect = new Rectangle(0, 0, kymos.getWidth(), kymos.getHeight() );
					ibufImage = IcyBufferedImageUtil.getSubImage(ibufImage, rect );
				}
				int it = cap.getCapillaryIndexFromCapillaryName(cap.roi.getName());
				if (it < 0)
					it = t;
				kymos.setImage(it, 0, ibufImage);
				cap.indexImage = it;
				
			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (cap.indexImage == -1)
				cap.indexImage = t;
			kymos.loadXMLKymographAnalysis(cap, directory);
			
			t++;
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return kymos;
	}
	*/
	
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
