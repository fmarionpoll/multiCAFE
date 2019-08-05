package plugins.fmp.multicafeSequence;


import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.MetaDataUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import ome.xml.meta.OMEXMLMetadata;


public class SequencePlusUtils {
	
	public static boolean isInterrupted = false;
	public static boolean isRunning = false;
	
	private static int imageWidthMax = 0;
	private static int imageHeightMax = 0;
	private static ArrayList<Rectangle> rectList = null;
	static String extension = ".tiff"; //".tiff"; //".jpg"; //
	
	// -------------------------------------------------------
	
	private static ArrayList<File> keepOnlyFilesMatchingCapillaries(File[] files, Capillaries capillaries) {

		ArrayList<File> filesArray = new ArrayList<File>();
		for (int i= 0; i < files.length; i++) {
			String filename = files[i].getAbsolutePath();
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
		
		ProgressFrame progress = new ProgressFrame("Read kymographs width and height");
		progress.setLength(files.size());
		
		for (int i= 0; i < files.size(); i++) {
			String path = files.get(i).getPath();
			OMEXMLMetadata metaData = null;
			try {
				metaData = Loader.getOMEXMLMetaData(path);
			} catch (UnsupportedFormatException | IOException e) {
				e.printStackTrace();
			}
			int imageWidth = MetaDataUtil.getSizeX(metaData, 0);
			int imageHeight= MetaDataUtil.getSizeY(metaData, 0);
			if (imageWidth > imageWidthMax)
				imageWidthMax = imageWidth;
			if (imageHeight > imageHeightMax)
				imageHeightMax = imageHeight;
			
			Rectangle rect = new Rectangle(0, 0, imageWidth, imageHeight);
			rectList.add(rect);
			progress.incPosition();
		}
		progress.close();
	}
	
	private static void adjustImagesToMaxSize(ArrayList<File> files) {

		ProgressFrame progress = new ProgressFrame("Make kymographs the same width and height");
		progress.setLength(files.size());

		for (int i= 0; i < files.size(); i++) {
			if (rectList.get(i).width == imageWidthMax && rectList.get(i).height == imageHeightMax)
				continue;
			
			progress.setMessage("adjust image "+files.get(i));
			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(files.get(i).getAbsolutePath());
			} catch (UnsupportedFormatException | IOException e) {
				e.printStackTrace();
			}
			
			IcyBufferedImage ibufImage2 = new IcyBufferedImage(imageWidthMax, imageHeightMax, ibufImage.getSizeC(), ibufImage.getDataType_());
			transferImage1To2(ibufImage, ibufImage2);
			try {
				Saver.saveImage(ibufImage2, files.get(i), true);
			} catch (FormatException | IOException e) {
				e.printStackTrace();
			}
			progress.incPosition();
		}
		progress.close();
	}
	
	private static void transferImage1To2(IcyBufferedImage source, IcyBufferedImage result) {

	        final int sizeY = source.getSizeY();
	        final int endC = source.getSizeC();
	        final int sourceSizeX 	= source.getSizeX();
	        final int destSizeX 	= result.getSizeX();
	        final DataType dataType = source.getDataType_();
	        final boolean signed = dataType.isSigned();

	        result.lockRaster();
	        try
	        {
	            for (int ch = 0; ch < endC; ch++)
	            {
	                final Object src = source.getDataXY(ch);
	                final Object dst = result.getDataXY(ch);

	                int srcOffset = 0;
	                int dstOffset = 0;

	                for (int curY = 0; curY < sizeY; curY++)
	                {
	                    Array1DUtil.arrayToArray(src, srcOffset, dst, dstOffset, sourceSizeX, signed);
	                    srcOffset += sourceSizeX;
	                    dstOffset += destSizeX;
	                }
	            }
	        }
	        finally
	        {
	            result.releaseRaster(true);
	        }
	        result.dataChanged();
	}
	
	public static SequencePlus openKymoFiles (String directory, Capillaries parent_capillaries) {
		
		isRunning = true;

		File dir = new File(directory);
		File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
		if (files == null)
			return null;
		
		ProgressFrame progress = new ProgressFrame("Read kymographs");
				
		ArrayList <File> filesArray = keepOnlyFilesMatchingCapillaries(files, parent_capillaries);
		getMaxSizeofTiffFiles(filesArray);
		adjustImagesToMaxSize(filesArray);

		progress.setMessage("create sequence");
		ArrayList<String> listFileNames = new ArrayList<String> (filesArray.size());
		for (File file: filesArray) {
			listFileNames.add(file.getPath());
		}
		
		SequencePlus kymographSeq = new SequencePlus(listFileNames);
		kymographSeq.analysisStep = 1;
		
		progress.setMessage("load measures for each capillary");
		kymographSeq.capillaries = new Capillaries();
		
		for (int i=0; i < listFileNames.size(); i++) {
			
			String filename = listFileNames.get(i);
			int index1 = filename.indexOf(extension);
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
	
	public static void saveKymosMeasures (SequencePlus vkymos, String directory) {
		
		isRunning = true;
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");
		progress.setLength(vkymos.seq.getSizeT());
		
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
		
		if (vkymos == null || vkymos.capillaries == null)
			return;
					
		vkymos.capillaries.analysisStart = vSequence.analysisStart; 
		vkymos.capillaries.analysisEnd  = vSequence.analysisEnd;
		vkymos.capillaries.analysisStep = vSequence.analysisStep;
		
	}
}
