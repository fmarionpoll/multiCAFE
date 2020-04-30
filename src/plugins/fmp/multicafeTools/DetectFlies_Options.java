package plugins.fmp.multicafeTools;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Cages;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class DetectFlies_Options implements XMLPersistent {
	
	public int 		threshold 				= -1;
	public int		thresholdBckgnd			= 40;
	public int		thresholdDiff			= 100;
	public boolean 	btrackWhite 			= false;
	public boolean  blimitLow 				= false;
	public boolean  blimitUp 				= false;
	public int  	limitLow				= 0;
	public int  	limitUp					= 1;
	public int 		jitter 					= 10;
	
	public TransformOp transformop; 
	public int		videoChannel 			= 0;
	public boolean 	backgroundSubstraction 	= false;

	public Cages 	cages 					= null;
	public List<BooleanMask2D> 	cageMaskList = new ArrayList<BooleanMask2D>();
	
	public int		stepFrame 				= 1;
	public int 		startFrame				= 0;
	public int 		endFrame				= 1;
	public boolean	isFrameFixed			= false;
	public int 		nbframes				= 1;
	public Rectangle parent0Rect 			= null;

	public ExperimentList expList 			= null;
	public Rectangle 	rectangleAllCages 	= null;
	ROI2DRectangle [] 	tempRectROI;
	ROI [][] 			resultFlyPositionArrayList;
	
	// -----------------------------------------------------
	
	
	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "DetectFliesParameters");
		if (xmlVal == null) 
			return false;
		threshold =  XMLUtil.getElementIntValue(xmlVal, "threshold", -1);
		btrackWhite = XMLUtil.getElementBooleanValue(xmlVal, "btrackWhite", false);
		blimitLow = XMLUtil.getElementBooleanValue(xmlVal, "blimitLow",false);
		blimitUp = XMLUtil.getElementBooleanValue(xmlVal, "blimitUp", false);
		limitLow =  XMLUtil.getElementIntValue(xmlVal, "limitLow", -1);
		limitUp =  XMLUtil.getElementIntValue(xmlVal, "limitUp", -1);
		jitter =  XMLUtil.getElementIntValue(xmlVal, "jitter", 10); 
		String op1 = XMLUtil.getElementValue(xmlVal, "transformOp", null);
		transformop = TransformOp.findByText(op1);
		startFrame =  XMLUtil.getAttributeIntValue(xmlVal, "start", 0);
		endFrame = XMLUtil.getAttributeIntValue(xmlVal, "end", 0);
		stepFrame = XMLUtil.getAttributeIntValue(xmlVal, "step", 1);
		videoChannel = XMLUtil.getAttributeIntValue(xmlVal, "videoChannel", 0);
		return true;
	}
	
	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "DetectFliesParameters");
		XMLUtil.setElementIntValue(xmlVal, "threshold", threshold);
		XMLUtil.setElementBooleanValue(xmlVal, "btrackWhite", btrackWhite);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitLow", blimitLow);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitUp", blimitUp);
		XMLUtil.setElementIntValue(xmlVal, "limitLow", limitLow);
		XMLUtil.setElementIntValue(xmlVal, "limitUp", limitUp);
		XMLUtil.setElementIntValue(xmlVal, "jitter", jitter); 
		if (transformop != null) {
			String transform1 = transformop.toString();
			XMLUtil.setElementValue(xmlVal, "transformOp", transform1);
		}
		XMLUtil.setAttributeIntValue(xmlVal, "start", startFrame);
		XMLUtil.setAttributeIntValue(xmlVal, "end", endFrame); 
		XMLUtil.setAttributeIntValue(xmlVal, "step", stepFrame); 
		XMLUtil.setAttributeIntValue(xmlVal, "videoChannel", videoChannel);
		return true;
	}
	
	public BooleanMask2D findLargestComponent(ROI2DArea roiAll, int iroi) {
		ROI cageLimitROI = cages.cageList.get(iroi).roi;
		if ( cageLimitROI == null )
			return null;
		BooleanMask2D cageMask = cageMaskList.get(iroi);
		if (cageMask == null)
			return null;
		ROI2DArea roi = new ROI2DArea(roiAll.getBooleanMask( true ).getIntersection( cageMask ) );
		// find largest component in the threshold
		int max = 0;
		BooleanMask2D bestMask = null;
		for ( BooleanMask2D mask : roi.getBooleanMask( true ).getComponents() ) {
			int len = mask.getPoints().length;
			if (blimitLow && len < limitLow)
				len = 0;
			if (blimitUp && len > limitUp)
				len = 0;
			if ( len > max ) {
				bestMask = mask;
				max = len;
			}
		}
		return bestMask;
	}
	
	public ROI2DArea findFly(IcyBufferedImage img, int threshold) {
		if (img == null)
			return null;
		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];
		if (btrackWhite) {
			byte[] arrayRed 	= img.getDataXYAsByte( 0);
			byte[] arrayGreen 	= img.getDataXYAsByte( 1);
			byte[] arrayBlue 	= img.getDataXYAsByte( 2);
			for ( int i = 0 ; i < arrayRed.length ; i++ ) {
				float r = ( arrayRed[i] 	& 0xFF );
				float g = ( arrayGreen[i] 	& 0xFF );
				float b = ( arrayBlue[i] 	& 0xFF );
				float intensity = (r+g+b)/3f;
				mask[i] = ( intensity ) > threshold ;
			}
		}
		else {
			byte[] arrayChan = img.getDataXYAsByte( videoChannel);
			for ( int i = 0 ; i < arrayChan.length ; i++ ) {
				mask[i] = ( ((int) arrayChan[i] ) & 0xFF ) < threshold ;
			}
		}
		BooleanMask2D bmask = new BooleanMask2D( img.getBounds(), mask); 
		ROI2DArea roiResult = new ROI2DArea( bmask );
		return roiResult;
	}
	
	public void findFlies (IcyBufferedImage workimage, int t, int it) {
		ROI2DArea roiAll = findFly (workimage, threshold);
		for ( int icage = 0; icage < cages.cageList.size(); icage++ ) {		
			BooleanMask2D bestMask = findLargestComponent(roiAll, icage);
			ROI2DArea flyROI = null;
			Cage cage = cages.cageList.get(icage);
			if (cage.cageNFlies < 1)
				continue;
			if ( bestMask != null ) {
				flyROI = new ROI2DArea( bestMask );
				flyROI.setName("det"+cage.roi.getName() +"_" + t );
				flyROI.setT( t );
				resultFlyPositionArrayList[icage][it] = flyROI;
				Rectangle2D rect = flyROI.getBounds2D();
				tempRectROI[icage].setRectangle(rect);
				Point2D flyPosition = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
				int npoints = cage.flyPositions.pointsList.size();
				cage.flyPositions.add(flyPosition, t);
				if (it > 0 && npoints > 0) {
					Point2D prevPoint = cage.flyPositions.getValidPointAtOrBefore(npoints);
					if (prevPoint.getX() >= 0) {
						double distance = flyPosition.distance(prevPoint);
						if (distance > jitter)
							cage.flyPositions.lastTimeAlive = t;
					}
				}
			}
			else {
				Point2D flyPosition = new Point2D.Double(-1, -1);
				cage.flyPositions.add(flyPosition, t);
			}
		}
	}
	
	public void initTempRectROIs(Experiment exp, Sequence seq) {
		cages = exp.cages;
		int nbcages = cages.cageList.size();
		tempRectROI = new ROI2DRectangle [nbcages];
		for (int i=0; i < nbcages; i++) {
			tempRectROI[i] = new ROI2DRectangle(0, 0, 10, 10);
			tempRectROI[i].setName("fly_"+i);
			Cage cage = cages.cageList.get(i);
			XYTaSeries positions = new XYTaSeries();
			cage.flyPositions = positions;
			seq.addROI(tempRectROI[i]);	
		}
		// create array for the results - 1 point = 1 slice
		resultFlyPositionArrayList = new ROI[nbcages][nbframes];
	}
	
	public void initParametersForDetection(Experiment exp) {
		nbframes = (exp.endFrame - exp.startFrame +1)/stepFrame +1;
		exp.cages.clear();
//		exp.cages.cageList = exp.seqCamData.getCages();
		cages = exp.cages;
		cageMaskList = ROI2DUtilities.getMask2DFromROIs(cages.cageList);
		rectangleAllCages = null;
		for (Cage cage: cages.cageList) {
			Rectangle rect = cage.roi.getBounds();
			if (rectangleAllCages == null)
				rectangleAllCages = new Rectangle(rect);
			else
				rectangleAllCages.add(rect);
		}
	}
	
	public void removeTempRectROIs(Experiment exp) {
		for (int i=0; i < tempRectROI.length; i++)
			exp.seqCamData.seq.removeROI(tempRectROI[i]);
	}
	
	public void copyDetectedROIsToSequence(Experiment exp) {
		try {
//			exp.seqCamData.seq.beginUpdate();
//			exp.cages = cages;
			int ncages = cages.cageList.size();
			for (int icage=0; icage < ncages; icage++) {
				for ( int it = 0; it < resultFlyPositionArrayList[icage].length ; it++ ) {
					exp.seqCamData.seq.addROI( resultFlyPositionArrayList[icage][it] );
				}
			}
		}
		finally {
			exp.seqCamData.seq.endUpdate();
		}
	}
	
	public void copyDetectedROIsToCages(Experiment exp) {
//		exp.cages = cages;
		int ncages = cages.cageList.size();
		for (int icage=0; icage < ncages; icage++) {
			Cage cage = cages.cageList.get(icage);
			for ( int it = 0; it < resultFlyPositionArrayList[icage].length ; it++ ) {
				cage.detectedFliesList.add((ROI2D) resultFlyPositionArrayList[icage][it] );
			}
		}
	}
	
}
