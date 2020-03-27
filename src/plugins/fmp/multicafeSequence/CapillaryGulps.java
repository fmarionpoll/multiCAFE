package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class CapillaryGulps  implements XMLPersistent  {
	
	private final String ID_GULPS = "gulpsMC";
	public List<ROI2D> rois = null; 

	// -------------------------------
	
	public void copy(CapillaryGulps capG) {
		rois = new ArrayList <ROI2D> ();
		rois.addAll(capG.rois);		
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		boolean flag = false;
		rois = new ArrayList <ROI2D> ();
		final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
		if (nodeROIs != null) {
			flag = true;
			List<ROI> roislocal = ROI.loadROIsFromXML(nodeROIs);
			for (ROI roislocal_i : roislocal) {
        	   ROI2D roi = (ROI2D) roislocal_i;
        	   rois.add(roi);
           }
		}
        return flag;
	}

	@Override
	public boolean saveToXML(Node node) {
		boolean flag = false;
		final Node nodeROIs = XMLUtil.setElement(node, ID_GULPS);
        if (nodeROIs != null){
        	flag = true;
        	if (rois != null && rois.size() > 0) {
        		List<ROI> roislocal = new ArrayList<ROI> (rois.size());
        		for (ROI2D roi: rois)
        			roislocal.add((ROI) roi);
        		ROI.saveROIsToXML(nodeROIs, roislocal);
        	}
	    }
        return flag;
	}
	
	boolean isThereAnyMeasuresDone() {
		return (rois != null && rois.size() > 0);
	}
	
	List<Integer> getCumSumFromRoisArray(int npoints) {
		if (rois == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(npoints, 0));
		for (ROI roi: rois) {
			ROI2DUtilities.addROItoCumulatedSumArray((ROI2DPolyLine) roi, arrayInt);
		}
		return arrayInt;
	}

	public void transferROIsToMeasures(List<ROI> listRois) {	
		rois = new ArrayList<ROI2D>();
		for (ROI roi: listRois) {		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) {
				if (roiname .contains("gulp"))	
					rois.add( (ROI2D) roi);
			}
		}
	}

	public void addGulp(ROI2DPolyLine roi, int indexkymo, String name) {
		roi.setColor(Color.red);
		roi.setStroke(1);
		roi.setName(name);
		roi.setT(indexkymo);	
		rois.add(roi);
	}
	
	public List<ROI2D> addToROIs(List<ROI2D> listrois, int indexImage) {
		if (rois != null) 
			listrois.addAll(rois);
		return listrois;
	}
	
	public void removeROIsWithinInterval(int startPixel, int endPixel) {
		Iterator <ROI2D> iterator = rois.iterator();
		while (iterator.hasNext()) {
			ROI2D roi = iterator.next();
			// if roi.first >= startpixel && roi.first <= endpixel	
			Rectangle rect = ((ROI2D) roi).getBounds();
			if (rect.x >= startPixel && rect.x <= endPixel) {
				iterator.remove();
			}
		}
	}
}