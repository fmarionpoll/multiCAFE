package plugins.fmp.multicafeTools;


import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceVirtual;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class ROI2DUtilities  {
	
	public static ArrayList<ROI2D> getListofCagesFromSequence (SequenceVirtual vSequence) {
		if (vSequence == null)
			 return null;
		
		ArrayList<ROI2D> roiList = vSequence.seq.getROI2Ds();
		Collections.sort(roiList, new MulticafeTools.ROI2DNameComparator());
		ArrayList<ROI2D> cageLimitROIList		= new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList )
		{
			String csName = roi.getName();
			if (( csName.contains( "cage") 
				|| csName.contains("Polygon2D")) 
				&& ( roi instanceof ROI2DPolygon ))
				cageLimitROIList.add(roi);
		}
		return cageLimitROIList;
	}
	
	public static ArrayList<BooleanMask2D> getMask2DFromRoiList (ArrayList<ROI2D> roiList) {
		ArrayList<BooleanMask2D> cageMaskList = new ArrayList<BooleanMask2D>();
		for ( ROI2D roi : roiList ) {
			cageMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));
		}
		return cageMaskList;
	}
	
	public static void removeROIsFromSequence (SequenceVirtual vSequence, ArrayList<ROI2D> roiList) {
		for ( ROI2D roi : roiList ) {
			vSequence.seq.removeROI(roi);
		}
	}
	
	public static boolean interpolateMissingPointsAlongXAxis (ROI2DPolyLine roiLine) {
		// interpolate points so that each x step has a value	
		// assume that points are ordered along x
		Polyline2D line = roiLine.getPolyline2D();
		int roiLine_npoints = line.npoints;
		// exit if the length of the segment is the same
		int roiLine_nintervals =(int) line.xpoints[roiLine_npoints-1] - (int) line.xpoints[0] +1;  
		if (roiLine_npoints == roiLine_nintervals)
			return true;
		else if (roiLine_npoints > roiLine_nintervals)
			return false;
		
		List<Point2D> pts = new ArrayList <Point2D>(roiLine_npoints);
		double ylast = line.ypoints[roiLine_npoints-1];
		for (int i=1; i< roiLine_npoints; i++) {			
			int xfirst = (int) line.xpoints[i-1];
			int xlast = (int) line.xpoints[i];
			double yfirst = line.ypoints[i-1];
			ylast = line.ypoints[i];
			for (int j = xfirst; j< xlast; j++) {
				int val = (int) (yfirst + (ylast-yfirst)*(j-xfirst)/(xlast-xfirst));
				Point2D pt = new Point2D.Double(j, val);
				pts.add(pt);
			}
		}
		Point2D pt = new Point2D.Double(line.xpoints[roiLine_npoints-1], ylast);
		pts.add(pt);
		roiLine.setPoints(pts);
		return true;
	}

	public static ArrayList<Integer> transfertRoiYValuesToDataArray(ROI2DPolyLine roiLine) {
		Polyline2D line = roiLine.getPolyline2D();
		ArrayList<Integer> intArray = new ArrayList<Integer> (line.npoints);
		for (int i=0; i< line.npoints; i++) {
			intArray.add((int) line.ypoints[i]);
		}
		return intArray;
	}
	
	public static void validateRois(Sequence seq) {

		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine) roi);
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

	public static ArrayList<Integer> getArrayListFromRois (SequenceKymos seqK, EnumArrayListType option, int t) {	
		Capillary cap = seqK.capillaries.capillariesArrayList.get(t);
		ArrayList<ROI2D> listRois = seqK.seq.getROI2Ds();
		if (listRois == null)
			return null;
		ArrayList<Integer> datai = null;
		
		switch (option) {
		case derivedValues:
			datai = cap.derivedValuesArrayList;
			break;
		case cumSum:
			datai = new ArrayList<Integer>(Collections.nCopies(seqK.seq.getWidth(), 0));
			addRoisMatchingFilterToCumSumDataArray(seqK.seq, "gulp", datai);
			break;
		case bottomLevel:
			datai = copyFirstRoiMatchingFilterToDataArray(seqK.seq, "bottomlevel");
			break;
		case topLevel:
		default:
			datai = copyFirstRoiMatchingFilterToDataArray(seqK.seq, "toplevel");
			break;
		}
		return datai;
	}
	
	public static ArrayList<Integer> copyFirstRoiMatchingFilterToDataArray (Sequence seq, String filter) {
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) { 
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine)roi);
				return transfertRoiYValuesToDataArray((ROI2DPolyLine)roi);
			}
		}
		return null;
	}
	
	public static void addRoisMatchingFilterToCumSumDataArray (Sequence seq, String filter, ArrayList<Integer> cumSumArray) {
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) 
				addRoitoCumulatedSumArray((ROI2DPolyLine) roi, cumSumArray);
		}
		return ;
	}
	
	public static void addRoitoCumulatedSumArray(ROI2DPolyLine roi, ArrayList<Integer> sumArrayList) {
		interpolateMissingPointsAlongXAxis (roi);
		ArrayList<Integer> intArray = transfertRoiYValuesToDataArray(roi);
		Polyline2D line = roi.getPolyline2D();
		int jstart = (int) line.xpoints[0];

		int previousY = intArray.get(0);
		for (int i=0; i< intArray.size(); i++) {
			int val = intArray.get(i);
			int deltaY = val - previousY;
			previousY = val;
			for (int j = jstart+i; j< sumArrayList.size(); j++) {
				sumArrayList.set(j, sumArrayList.get(j) +deltaY);
			}
		}
	}

	public static ROI2D getROIFromIntArray(ArrayList<Integer> sumArrayList) {
		ROI2D roi = new ROI2DPolyLine();
		xx
		return roi;
	}
}
