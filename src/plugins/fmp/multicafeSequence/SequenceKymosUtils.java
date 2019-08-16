package plugins.fmp.multicafeSequence;


import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.sequence.MetaDataUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import ome.xml.meta.OMEXMLMetadata;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class SequenceKymosUtils {
	
	public static boolean 	isInterrupted 			= false;
	public static boolean 	isRunning 				= false;
	
	private static int 		imageWidthMax 			= 0;
	private static int 		imageHeightMax 			= 0;
	private static List<Rectangle> rectList 	= null;
	static String 			extension 				= ".tiff";
	
	// -------------------------------------------------------

	static void getMaxSizeofTiffFiles(List<File> files) {
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
	
	static void adjustImagesToMaxSize(List<File> files) {
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
	        final int sizeY 		= source.getSizeY();
	        final int endC 			= source.getSizeC();
	        final int sourceSizeX 	= source.getSizeX();
	        final int destSizeX 	= result.getSizeX();
	        final DataType dataType = source.getDataType_();
	        final boolean signed 	= dataType.isSigned();

	        result.lockRaster();
	        try {
	            for (int ch = 0; ch < endC; ch++) {
	                final Object src = source.getDataXY(ch);
	                final Object dst = result.getDataXY(ch);

	                int srcOffset = 0;
	                int dstOffset = 0;

	                for (int curY = 0; curY < sizeY; curY++) {
	                    Array1DUtil.arrayToArray(src, srcOffset, dst, dstOffset, sourceSizeX, signed);
	                    result.setDataXY(ch, dst);
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
	
	public static void transferCamDataROIStoKymo (SequenceCamData seqCams, SequenceKymos seqKymos) {
		List<ROI2D> listROISCap = ROI2DUtilities.getListofCapillariesFromSequence(seqCams);
		if (seqKymos == null) {
			System.out.println("seqkymos null - return");
			return;
		}
		if (seqKymos.capillaries == null)
			seqKymos.capillaries = new Capillaries();
		
		// rois not in cap?
		for (ROI2D roi:listROISCap) {
			boolean found = false;
			for (Capillary cap: seqKymos.capillaries.capillariesArrayList) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seqKymos.capillaries.capillariesArrayList.add(new Capillary((ROI2DShape)roi));
		}
		
		// cap with no corresponding roi?
		for (Capillary cap: seqKymos.capillaries.capillariesArrayList) {
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seqKymos.capillaries.capillariesArrayList.remove(cap);
		}
	}
	
	public static void transferKymoCapillariesToCamData (SequenceCamData seqCams, SequenceKymos seqKymos) {
		if (seqKymos == null || seqKymos.capillaries == null)
			return;
		
		List<ROI2D> listROISCap = ROI2DUtilities.getListofCapillariesFromSequence(seqCams);
		
		// cap with no corresponding roi?
		for (Capillary cap: seqKymos.capillaries.capillariesArrayList) {
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seqCams.seq.addROI(cap.capillaryRoi);
		}
	}
}
