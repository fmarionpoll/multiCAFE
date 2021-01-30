package plugins.fmp.multicafe.sequence;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import loci.formats.FormatException;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;

import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class SequenceKymos extends SequenceCamData  {	
	public boolean 	isRunning_loadImages 		= false;
	public boolean 	isInterrupted_loadImages 	= false;
	public int 		imageWidthMax 				= 0;
	public int 		imageHeightMax 				= 0;
	
	// -----------------------------------------------------
	
	public SequenceKymos() {
		super ();
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos(String name, IcyBufferedImage image) {
		super (name, image);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos (String [] list, String directory) {
		super(list, directory);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos (List<String> listFullPaths) {
		super(listFullPaths, true);
		status = EnumStatus.KYMOGRAPH;
	}
	
	// ----------------------------
	
	public void validateRoisAtT(int t) {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
		for (ROI2D roi: listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == -1)
				roi.setT(t);
			if (roi.getT() != t)
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis ((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("deriv"))
				continue;
			// if gulp not found - add an index to it	
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp"+String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new Comparators.ROI2D_Name_Comparator());
	}
	
	public void removeROIsPolylineAtT(int t) {
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == t)
				seq.removeROI(roi);
		}
		//Collections.sort(listRois, new Comparators.ROI2DNameComparator());
	}
	
	public void updateROIFromCapillaryMeasure(Capillary cap, CapillaryLimit caplimits) {
		int t = cap.indexImage;
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() != t)
				continue;
			if (!roi.getName().contains(caplimits.typename))
				continue;
			((ROI2DPolyLine) roi).setPolyline2D(caplimits.polylineLimit);
			roi.setName(caplimits.name);
			break;
		}
	}
	
	public void validateRois() {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
		for (ROI2D roi: listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis ((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("derivative"))
				continue;
			// if gulp not found - add an index to it	
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp"+String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new Comparators.ROI2D_Name_Comparator());
	}

	public boolean transferKymosRoisToCapillaries(Capillaries capillaries) {
		List<ROI> allRois = seq.getROIs();
		if (allRois.size() < 1)
			return false;
		for (int kymo=0; kymo< seq.getSizeT(); kymo++) {
			List<ROI> roisAtT = new ArrayList<ROI> ();
			for (ROI roi: allRois) {
				if (roi instanceof ROI2D) {
					if (((ROI2D)roi).getT() == kymo)
						roisAtT.add(roi);
				}
			}
			if (capillaries.capillariesArrayList.size() <= kymo) 
				capillaries.capillariesArrayList.add(new Capillary());
			Capillary cap = capillaries.capillariesArrayList.get(kymo);
			cap.filenameTIFF = getFileName(kymo);
			cap.indexImage = kymo;
			cap.transferROIsToMeasures(roisAtT);	
		}
		return true;
	}
	
	public void transferCapillariesMeasuresToKymos(Capillaries capillaries) {
		List<ROI2D> seqRoisList = seq.getROI2Ds(false);
		ROI2DUtilities.removeROIsMissingChar(seqRoisList, '_');
		List<ROI2D> newRoisList = new ArrayList<ROI2D>();
		int ncapillaries = capillaries.capillariesArrayList.size();
		for (int i=0; i < ncapillaries; i++) {
			List<ROI2D> listOfRois = capillaries.capillariesArrayList.get(i).transferMeasuresToROIs();
			newRoisList.addAll(listOfRois);
		}
		ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
		seq.removeAllROI();
		seq.addROIs(seqRoisList, false);
	}
	
	public void saveKymosCurvesToCapillariesMeasures(Experiment exp) {
		exp.seqKymos.validateRois();
		exp.seqKymos.transferKymosRoisToCapillaries(exp.capillaries);
		exp.xmlSaveMCcapillaries();
	}

	// ----------------------------

	public List <FileProperties> loadListOfPotentialKymographsFromCapillaries(String dir, Capillaries capillaries) {
		String directoryFull = dir +File.separator ;	
		int ncapillaries = capillaries.capillariesArrayList.size();
		List<FileProperties> myListOfFiles = new ArrayList<FileProperties>(ncapillaries);
		for (int i=0; i< ncapillaries; i++) {
			FileProperties temp = new FileProperties();
			temp.fileName  = directoryFull+ capillaries.capillariesArrayList.get(i).getCapillaryName()+ ".tiff";
			myListOfFiles.add(temp);
		}
		return myListOfFiles;
	}
	
	// -------------------------
	
	public boolean loadImagesFromList(List <FileProperties> myListOfFileProperties, boolean adjustImagesSize) {
		isRunning_loadImages = true;
		boolean flag = (myListOfFileProperties.size() > 0);
		if (!flag)
			return flag;
		if (adjustImagesSize) {
			adjustImagesToMaxSize(myListOfFileProperties, getMaxSizeofTiffFiles(myListOfFileProperties));
		}
		List <String> myList = new ArrayList <String> ();
		for (FileProperties prop: myListOfFileProperties) {
			if (prop.exists)
				myList.add(prop.fileName);
		}
		if (myList.size() > 0) {
			loadSequenceOfImagesFromList(myList, true);
			setParentDirectoryAsCSCamFileName();
			status = EnumStatus.KYMOGRAPH;
		}
		isRunning_loadImages = false;
		return flag;
	}
	
	Rectangle getMaxSizeofTiffFiles(List<FileProperties> files) {
		imageWidthMax = 0;
		imageHeightMax = 0;
		for (int i= 0; i < files.size(); i++) {
			FileProperties fileProp = files.get(i);
			if (!fileProp.exists)
				continue;
			getImageDim(fileProp);
			if (fileProp.imageWidth > imageWidthMax)
				imageWidthMax = fileProp.imageWidth;
			if (fileProp.imageHeight > imageHeightMax)
				imageHeightMax = fileProp.imageHeight;
		}
		return new Rectangle(0, 0, imageWidthMax, imageHeightMax);
	}
	
	private FileProperties getImageDim(final FileProperties path) {
	    String suffix = this.getFileSuffix(path.fileName);
	    Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
	    if (iter.hasNext()) {
	        ImageReader reader = iter.next();
	        try {
	            ImageInputStream stream = new FileImageInputStream(path.file);
	            reader.setInput(stream);
	            path.imageWidth = reader.getWidth(reader.getMinIndex());
	            path.imageHeight = reader.getHeight(reader.getMinIndex());
	        } catch (IOException e) {
	            System.out.println(e.getMessage());
	        } finally {
	            reader.dispose();
	        }
	    } else {
	    	System.out.println("No reader found for given format: " + suffix);
	    }
	    return path;
	}
	
	private String getFileSuffix(final String path) {
	    String result = null;
	    if (path != null) {
	        result = "";
	        if (path.lastIndexOf('.') != -1) {
	            result = path.substring(path.lastIndexOf('.'));
	            if (result.startsWith(".")) {
	                result = result.substring(1);
	            }
	        }
	    }
	    return result;
	}
	
	void adjustImagesToMaxSize(List<FileProperties> files, Rectangle rect) {
		ProgressFrame progress = new ProgressFrame("Make kymographs the same width and height");
		progress.setLength(files.size());
		
		for (int i= 0; i < files.size(); i++) {
			FileProperties fileProp = files.get(i);
			if (!fileProp.exists)
				continue;
			if (fileProp.imageWidth == rect.width && fileProp.imageHeight == rect.height)
				continue;
			
			progress.setMessage("adjust image "+fileProp.fileName);
			System.out.print("adjust image "+fileProp.fileName);
			IcyBufferedImage ibufImage1 = null;
			try {
				ibufImage1 = Loader.loadImage(fileProp.fileName);
			} catch (UnsupportedFormatException | IOException e) {
				e.printStackTrace();
			}
			
			IcyBufferedImage ibufImage2 = new IcyBufferedImage(imageWidthMax, imageHeightMax, ibufImage1.getSizeC(), ibufImage1.getDataType_());
			transferImage1To2(ibufImage1, ibufImage2);
			try {
				Saver.saveImage(ibufImage2, fileProp.file, true);
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
        finally {
            result.releaseRaster(true);
        }
        result.dataChanged();
	}
		
	// ----------------------------

	public List<Integer> subtractTi(List<Integer > array) {
		if (array == null || array.size() < 1)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
			item0 = value;
		}
		return array;
	}
		
/*	public List<Integer> subtractDeltaT(List<Integer > array, int arrayStep, int deltaT) {
		if (array == null)
			return null;
		for (int index=0; index < array.size(); index++) {
			int value = 0;
			int timeIndex = index * arrayStep + deltaT;
			int indexDelta = timeIndex/arrayStep;
			if (indexDelta < array.size()) 
				value = array.get(indexDelta) - array.get(index);
			array.set(index, value);
		}
		return array;
	}
*/	
	
	public List<Integer> subtractT0 (List<Integer> array) {
		if (array == null || array.size() < 1)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public List<Integer> subtractT0AndAddConstant (List<Integer> array, int constant) {
		if (array == null || array.size() < 1)
			return null;
		int item0 = array.get(0) - constant;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public List<Integer> addConstant (List<Integer> array, int constant) {
		if (array == null || array.size() < 1)
			return null;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value + constant);
		}
		return array;
	}

	
}
