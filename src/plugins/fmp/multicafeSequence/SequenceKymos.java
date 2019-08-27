package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.w3c.dom.Document;
import loci.formats.FormatException;
import ome.xml.meta.OMEXMLMetadata;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.MetaDataUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.fmp.multicafeTools.OverlayThreshold;
import plugins.fmp.multicafeTools.OverlayTrapMouse;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class SequenceKymos extends SequenceCamData  {	
	public Capillaries 		capillaries 			= new Capillaries();
	public 	boolean 		hasChanged 				= false;
	public 	boolean 		bStatusChanged 			= false;

	public 	LocalDateTime	startDate				= null;
	public 	LocalDateTime	endDate					= null;
	public 	long			minutesBetweenImages	= 1;
	public 	OverlayThreshold thresholdOverlay 		= null;
	public 	OverlayTrapMouse trapOverlay 			= null;
	
	public boolean isRunning_loadImages = false;
	public boolean isInterrupted_loadImages = false;
	
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
	
	public String getDecoratedImageNameFromCapillary(int t) {
		if (capillaries != null & capillaries.capillariesArrayList.size() > 0)
			return capillaries.capillariesArrayList.get(t).capillaryRoi.getName() + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
		return csFileName + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
	}

	// ----------------------------
	
	public void roisSaveEdits() {
		if (hasChanged) {
			validateRois();
			transferKymosRoisToMeasures();
			hasChanged = false;
		}
	}
	
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
			if (roi.getName().contains("derivative"))
				continue;
				
			// if gulp not found - add an index to it	
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp"+String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new MulticafeTools.ROI2DNameComparator());
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
		Collections.sort(listRois, new MulticafeTools.ROI2DNameComparator());
	}

	public void transferKymosRoisToMeasures() {
		List<ROI> allRois = seq.getROIs();
		for (int t=0; t< seq.getSizeT(); t++) {
			List<ROI> roisAtT = new ArrayList<ROI> ();
			for (ROI roi: allRois) {
				if (roi instanceof ROI2D) {
					if (((ROI2D)roi).getT() == t)
						roisAtT.add(roi);
				}
			}
			if (capillaries.capillariesArrayList.size() <= t) {
				capillaries.capillariesArrayList.add(new Capillary());
			}
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.filenameTIFF = getFileName(t);
			cap.transferROIsToMeasures(roisAtT);	
		}
	}
	
	public void transferMeasuresToKymosRois() {
		List<ROI> all = new ArrayList<ROI>();
		for (Capillary cap: capillaries.capillariesArrayList) {
			List<ROI> listOfRois = cap.transferMeasuresToROIs();
			all.addAll(listOfRois);
		}
		ROI2DUtilities.addROIsToSequenceIfNotAlreadyPresent(all, seq);
	}
	
	// ----------------------------
	
	public void getAnalysisParametersFromCamData (SequenceCamData vSequence) {		
		capillaries.desc.analysisStart = vSequence.analysisStart; 
		capillaries.desc.analysisEnd  = vSequence.analysisEnd;
		capillaries.desc.analysisStep = vSequence.analysisStep;
	}
	
	public void updateCapillariesFromCamData(SequenceCamData seqCam) {
		SequenceKymosUtils.transferCamDataROIStoKymo(seqCam, this);
		transferAnalysisParametersToCapillaries();
		return;
	}
		
	public void transferAnalysisParametersToCapillaries () {
		capillaries.desc.analysisStart = analysisStart;
		capillaries.desc.analysisEnd = analysisEnd;
		capillaries.desc.analysisStep = analysisStep;
	}
	
	private void transferCapillariesToAnalysisParameters () {
		analysisStart = capillaries.desc.analysisStart;
		analysisEnd = capillaries.desc.analysisEnd;
		analysisStep = capillaries.desc.analysisStep;
	}
	
	public List <String> loadListOfKymographsFromCapillaries(String dir) {
		isRunning_loadImages = true;
		String directoryFull = dir +File.separator +"results" + File.separator;	
		List<String> myListOfFileNames = new ArrayList<String>(capillaries.capillariesArrayList.size());
		Collections.sort(capillaries.capillariesArrayList, new MulticafeTools.CapillaryIndexImageComparator());
		for (Capillary cap: capillaries.capillariesArrayList) {
			if (isInterrupted_loadImages)
				break;
			String tempname = directoryFull+cap.getName()+ ".tiff";
			boolean found = isFileFound(tempname);
			if (!found) {
				tempname = directoryFull+cap.capillaryRoi.getName()+ ".tiff";
				found = isFileFound(tempname);
			}
			if (found) {
				cap.filenameTIFF = tempname;
				myListOfFileNames.add(tempname);
			}
		}
		isRunning_loadImages = false;
		return myListOfFileNames;
	}
	
	private boolean isFileFound(String tempname) {
		File tempfile = new File(tempname);
		return tempfile.exists(); 
	}
	
	// -------------------------
	
	public boolean loadImagesFromList(List <String> myListOfFileNames, boolean adjustImagesSize) {
		isRunning_loadImages = true;
		boolean flag = (myListOfFileNames.size() > 0);
		if (!flag)
			return flag;
		
		if (adjustImagesSize) {
			System.out.println("adjust images size");
			List <File> filesArray = new ArrayList<File> (myListOfFileNames.size());
			for (String name : myListOfFileNames)
				filesArray.add(new File(name));
			List<Rectangle> rectList = getMaxSizeofTiffFiles(filesArray);
			if (isInterrupted_loadImages) {
				isRunning_loadImages = false;
				return false;
			}
			adjustImagesToMaxSize(filesArray, rectList);
			if (isInterrupted_loadImages) {
				isRunning_loadImages = false;
				return false;
			}
		}
		
		System.out.println("load sequence of images");
		loadSequenceFromList(myListOfFileNames, true);
		if (isInterrupted_loadImages) {
			isRunning_loadImages = false;
			return false;
		}

		System.out.println("transfer measures to kymosgraphs as rois");
		setParentDirectoryAsFileName();
		status = EnumStatus.KYMOGRAPH;
		transferMeasuresToKymosRois();
		isRunning_loadImages = false;
		return flag;
	}
	
	List<Rectangle> getMaxSizeofTiffFiles(List<File> files) {
		int imageWidthMax = 0;
		int imageHeightMax = 0;
		List<Rectangle> rectList = new ArrayList<Rectangle>(files.size());
		
		ProgressFrame progress = new ProgressFrame("Read kymographs width and height");
		progress.setLength(files.size());
		
		for (int i= 0; i < files.size(); i++) {
			if (isInterrupted_loadImages) {
				return null;
			}

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
		Rectangle rect = new Rectangle(0, 0, imageWidthMax, imageHeightMax);
		rectList.add(rect);
		progress.close();
		return rectList;
	}
	
	void adjustImagesToMaxSize(List<File> files, List<Rectangle> rectList) {
		ProgressFrame progress = new ProgressFrame("Make kymographs the same width and height");
		progress.setLength(files.size());
		Rectangle rect = rectList.get(rectList.size()-1);
		int imageWidthMax = rect.width;
		int imageHeightMax = rect.height;
		
		for (int i= 0; i < files.size(); i++) {
			if (isInterrupted_loadImages) {
				return;
			}
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
        finally {
            result.releaseRaster(true);
        }
        result.dataChanged();
	}
	
	// ----------------------------------
	
	public boolean xmlLoadCapillaryTrack(String pathname) {
		File tempfile = new File(pathname);
		if (tempfile.isDirectory()) {
			pathname = pathname + File.separator + "capillarytrack.xml";
			tempfile = new File(pathname);
		}
		if (!tempfile.isFile())
			return false;
		boolean flag = capillaries.xmlLoadCapillaries(pathname);
		if (flag) {
			Path pathfilename = Paths.get(pathname);
			directory = pathfilename.getParent().toString();
			transferCapillariesToAnalysisParameters ();
			loadListOfKymographsFromCapillaries(getDirectory());
		}
		return flag;
	}
	
	public boolean xmlSaveCapillaryTrack(String pathname) {
		boolean flag = false;
		File tempfile = new File(pathname);
		if (tempfile.isDirectory()) {
			pathname = pathname + File.separator + "capillarytrack.xml";
		}
		flag = capillaries.xmlSaveCapillaries(pathname, this);
		return flag;
	}
	
	public boolean xmlReadRoiLineParameters(String pathname) {
		if (pathname != null)  {
			final Document doc = XMLUtil.loadDocument(pathname);
			if (doc != null) { 
				int version = capillaries.xmlReadCapillaryParametersv0(doc);
				return (version >= 0); 
			}
		}
		return false;
	}

	// ----------------------------

	public List<Integer> subtractTi(List<Integer > array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
			item0 = value;
		}
		return array;
	}
	
	public List<Integer> subtractT0 (List<Integer> array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public List<Integer> subtractT0AndAddConstant (List<Integer> array, int constant) {
		if (array == null)
			return null;
		int item0 = array.get(0) - constant;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public List<Integer> addConstant (List<Integer> array, int constant) {
		if (array == null)
			return null;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value + constant);
		}
		return array;
	}

	// ----------------------------
	
	public void setThresholdOverlay(boolean bActive) {
		if (bActive) {
			if (thresholdOverlay == null) 
				thresholdOverlay = new OverlayThreshold(this);
			if (!seq.contains(thresholdOverlay)) 
				seq.addOverlay(thresholdOverlay);
			thresholdOverlay.setSequence (this);
		}
		else {
			if (thresholdOverlay != null && seq.contains(thresholdOverlay) )
				seq.removeOverlay(thresholdOverlay);
			thresholdOverlay = null;
		}
	}
	
	public void setThresholdOverlayParametersSingle(TransformOp transf, int threshold) {
		thresholdOverlay.setTransform(transf);
		thresholdOverlay.setThresholdSingle(threshold);
		thresholdOverlay.painterChanged();
	}
	
	public void setThresholdOverlayParametersColors(TransformOp transf, ArrayList <Color> colorarray, int colordistancetype, int colorthreshold) {
		thresholdOverlay.setTransform(transf);
		thresholdOverlay.setThresholdColor(colorarray, colordistancetype, colorthreshold);
		thresholdOverlay.painterChanged();
	}

	public void setMouseTrapOverlay (boolean bActive, JButton pickColorButton, JComboBox<Color> colorPickCombo) {
		if (bActive) {
			if (trapOverlay == null)
				trapOverlay = new OverlayTrapMouse (pickColorButton, colorPickCombo);
			if (!seq.contains(trapOverlay))
				seq.addOverlay(trapOverlay);
		}
		else {
			if (trapOverlay != null && seq.contains(trapOverlay))
				seq.removeOverlay(trapOverlay);
			trapOverlay = null;
		}
	}
	
}
