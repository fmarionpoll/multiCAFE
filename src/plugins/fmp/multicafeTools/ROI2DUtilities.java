package plugins.fmp.multicafeTools;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class ROI2DUtilities  {
	
	public static List<ROI2D> getCapillariesFromSequence (SequenceCamData seqCamData) {
		if (seqCamData == null)
			 return null;
		
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		Collections.sort(roiList, new MulticafeTools.ROI2DNameComparator());
		List<ROI2D> capillaryRois = new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList ) {
			if (!(roi instanceof ROI2DShape) || !roi.getName().contains("line")) 
				continue;
			if (roi instanceof ROI2DLine || roi instanceof ROI2DPolyLine)
				capillaryRois.add(roi);
		}
		return capillaryRois;
	}
	
	public static List<ROI2D> getGulpsFromSequence (SequenceCamData seqCamData) {
		if (seqCamData == null)
			 return null;
		
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		Collections.sort(roiList, new MulticafeTools.ROI2DNameComparator());
		List<ROI2D> gulpRois = new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList ) {
			if (!(roi instanceof ROI2DShape) || !roi.getName().contains("gulp")) 
				continue;
			if (roi instanceof ROI2DLine || roi instanceof ROI2DPolyLine)
				gulpRois.add(roi);
		}
		return gulpRois;
	}
	
	public static List<ROI2D> getCagesFromSequence (SequenceCamData seqCamData) {
		if (seqCamData == null)
			 return null;
		
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		Collections.sort(roiList, new MulticafeTools.ROI2DNameComparator());
		List<ROI2D> cageLimitROIList = new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList ) {
			String csName = roi.getName();
			if (( csName.contains( "cage") 
				|| csName.contains("Polygon2D")) 
				&& ( roi instanceof ROI2DPolygon ))
				cageLimitROIList.add(roi);
		}
		return cageLimitROIList;
	}
	
	public static List<BooleanMask2D> getMask2DFromROIs (List<ROI2D> roiList) {
		List<BooleanMask2D> cageMaskList = new ArrayList<BooleanMask2D>();
		for ( ROI2D roi : roiList ) {
			cageMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));
		}
		return cageMaskList;
	}
	
	public static boolean interpolateMissingPointsAlongXAxis (ROI2DPolyLine roiLine, int roiLine_nintervals) {
		if (roiLine_nintervals <= 0)
			return false;
		// interpolate points so that each x step has a value	
		// assume that points are ordered along x
		Polyline2D line = roiLine.getPolyline2D();
		int roiLine_npoints = line.npoints;
		
		// exit if the length of the segment is the same 
		if (roiLine_npoints == roiLine_nintervals)
			return true;
		// clip extra points
		if (roiLine_npoints > roiLine_nintervals)
			roiLine_npoints = roiLine_nintervals;
		
		List<Point2D> pts = new ArrayList <Point2D>(roiLine_npoints);
		double ylast = line.ypoints[roiLine_npoints-1];
		for (int i=1; i< roiLine_npoints; i++) {			
			int xfirst = (int) line.xpoints[i-1];
			if (xfirst < 0)
				xfirst = 0;
			int xlast = (int) line.xpoints[i];
			if (xlast > roiLine_nintervals -1)
				xlast = roiLine_nintervals -1;
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

	private static List<Integer> transferROIToDataArray(ROI2DPolyLine roiLine) {
		Polyline2D line = roiLine.getPolyline2D();
		List<Integer> intArray = new ArrayList<Integer> (line.npoints);
		for (int i=0; i< line.npoints; i++) {
			intArray.add((int) line.ypoints[i]);
		}
		return intArray;
	}
	
	public static void addROIsToSequenceNoDuplicate(List<ROI> listRois, Sequence seq) {
		List<ROI2D> seqList = seq.getROI2Ds(false);
		for (ROI2D seqRoi: seqList) {
			Iterator <ROI> iterator = listRois.iterator();
			while(iterator.hasNext()) {
				ROI roi = iterator.next();
				if (roi instanceof ROI2D) {
					if (seqRoi == roi)
						iterator.remove();
					else if (seqRoi.getName().equals (roi.getName() )) {
						seqRoi.copyFrom(roi);
						iterator.remove();
					}
				}
			}
		}
		seq.addROIs(listRois, false);
	}

	public static ROI2DPolyLine transfertDataArrayToROI(List<Integer> intArray) {
		Polyline2D line = new Polyline2D();
		for (int i =0; i< intArray.size(); i++) {
			Point2D pt = new Point2D.Double(i, intArray.get(i));
			line.addPoint(pt);
		}
		return new ROI2DPolyLine(line);
	}
	
	public static List<Integer> copyFirstROIMatchingFilterToDataArray (SequenceKymos seqKymos, String filter) {
		List<ROI2D> listRois = seqKymos.seq.getROI2Ds();
		int width = seqKymos.seq.getWidth();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) { 
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine)roi, width);
				return transferROIToDataArray((ROI2DPolyLine)roi);
			}
		}
		return null;
	}
	
	public static void addROIsMatchingFilterToCumSumDataArray (Sequence seq, String filter, List<Integer> cumSumArray) {
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) 
				addROItoCumulatedSumArray((ROI2DPolyLine) roi, cumSumArray);
		}
		return ;
	}
	
	public static void addROItoCumulatedSumArray(ROI2DPolyLine roi, List<Integer> sumArrayList) {
		Polyline2D roiline = roi.getPolyline2D();
		int width =(int) roiline.xpoints[roiline.npoints-1] - (int) roiline.xpoints[0] +1; 
		if (!interpolateMissingPointsAlongXAxis (roi, width))
			return;
		List<Integer> intArray = transferROIToDataArray(roi);
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

	public static void removeROIsWithinPixelInterval(List<ROI> gulpsRois, int startPixel, int endPixel) {
		Iterator <ROI> iterator = gulpsRois.iterator();
		while (iterator.hasNext()) {
			ROI roi = iterator.next();
			// if roi.first >= startpixel && roi.first <= endpixel
			if (roi instanceof ROI2D) {
				Rectangle rect = ((ROI2D) roi).getBounds();
				if (rect.x >= startPixel && rect.x <= endPixel) {
					iterator.remove();
				}
			}
		}
	}

}
