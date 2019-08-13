package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;

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
			return capillaries.capillariesArrayList.get(t).roi.getName() + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
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
		for (Capillary cap: capillaries.capillariesArrayList) {
			List<ROI> listOfRois = cap.transferMeasuresToROIs();
			seq.addROIs(listOfRois, false);
		}
	}
	
	// ----------------------------
	
	public void getAnalysisParametersFromCamData (SequenceCamData vSequence) {		
		capillaries.analysisStart = vSequence.analysisStart; 
		capillaries.analysisEnd  = vSequence.analysisEnd;
		capillaries.analysisStep = vSequence.analysisStep;
	}
	
	public void updateCapillariesFromCamData(SequenceCamData seqCam) {
		SequenceKymosUtils.transferCamDataROIStoKymo(seqCam, this);
		transferAnalysisParametersToCapillaries();
		return;
	}
	
	public void transferAnalysisParametersToCapillaries () {
		capillaries.analysisStart = analysisStart;
		capillaries.analysisEnd = analysisEnd;
		capillaries.analysisStep = analysisStep;
	}
	
	private void transferCapillariesToAnalysisParameters () {
		analysisStart = capillaries.analysisStart;
		analysisEnd = capillaries.analysisEnd;
		analysisStep = capillaries.analysisStep;
	}
	
	public List <String> loadListOfKymographsFromCapillaries(String dir) {
		String directoryFull = dir +File.separator +"results" + File.separator;	
		List<String> myListOfFileNames = new ArrayList<String>(capillaries.capillariesArrayList.size());
		Collections.sort(capillaries.capillariesArrayList, new MulticafeTools.CapillaryIndexImageComparator());
		for (Capillary cap: capillaries.capillariesArrayList) {
			// is tiff file name defined but not in the right directory
			if (cap.filenameTIFF != null && !cap.filenameTIFF.isEmpty()) {
				if (!cap.filenameTIFF .contains(directoryFull)) {
					Path oldpath = Paths.get(cap.filenameTIFF);
					cap.filenameTIFF = directoryFull + oldpath.getFileName();
				}
				myListOfFileNames.add(cap.filenameTIFF);
			}
			// name not defined but tiff file present?
			else {
				String tempname = directoryFull+cap.getName()+ ".tiff";
				File tempfile = new File(tempname);
				if (tempfile.exists() ) {
					cap.filenameTIFF = tempname;
					myListOfFileNames.add(cap.filenameTIFF);
				}
			}
		}
		return myListOfFileNames;
	}
	public boolean loadImagesFromList(List <String> myListOfFileNames, boolean adjustImagesSize) {
		boolean flag = (myListOfFileNames.size() > 0);
		if (!flag)
			return flag;

		if (adjustImagesSize) {
			List <File> filesArray = new ArrayList<File> (myListOfFileNames.size());
			for (String name : myListOfFileNames)
				filesArray.add(new File(name));
			SequenceKymosUtils.getMaxSizeofTiffFiles(filesArray);
			SequenceKymosUtils.adjustImagesToMaxSize(filesArray);
		}
		
		loadSequenceFromList(myListOfFileNames, true);
		setParentDirectoryAsFileName();
		status = EnumStatus.KYMOGRAPH;
		transferMeasuresToKymosRois();

		return flag;
	}
	
	public boolean xmlReadCapillaryTrack(String pathname) {
		File tempfile = new File(pathname);
		if (tempfile.isDirectory()) {
			pathname = pathname + File.separator + "capillarytrack.xml";
			tempfile = new File(pathname);
		}
		if (!tempfile.isFile())
			return false;
		
		boolean flag = capillaries.xmlReadROIsAndData(pathname, this);
		if (flag) {
			Path pathfilename = Paths.get(pathname);
			directory = pathfilename.getParent().toString();
			transferCapillariesToAnalysisParameters ();
			loadListOfKymographsFromCapillaries(getDirectory());	
		}
		return flag;
	}
	
	public boolean xmlWriteCapillaryTrack(String pathname) {
		boolean flag = false;
		File tempfile = new File(pathname);
		if (tempfile.isDirectory()) {
			pathname = pathname + File.separator + "capillarytrack.xml";
		}
		flag = capillaries.xmlWriteROIsAndDataNoQuestion(pathname, this);
		return flag;
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
