package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class CapillaryGulps  implements XMLPersistent  {
	
	private final String ID_GULPS = "gulpsMC";
	public List<ROI> rois = null; 

	
	public void copy(Capillary cap) {
		rois = new ArrayList <ROI> ();
		rois.addAll(cap.gulpsRois.rois);		
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		boolean flag = false;
		final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
        rois = new ArrayList <ROI> ();
        if (nodeROIs != null) {
        	flag = true;
        	rois = ROI.loadROIsFromXML(nodeROIs);
	    }
        return flag;
	}

	@Override
	public boolean saveToXML(Node node) {
		boolean flag = false;
		final Node nodeROIs = XMLUtil.setElement(node, ID_GULPS);
        if (nodeROIs != null){
        	flag = true;
	        ROI.saveROIsToXML(nodeROIs, rois);
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
		rois = new ArrayList<ROI>();
		for (ROI roi: listRois) {		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) {
				//((ROI2DPolyLine) roi).setT(indexImage);
				if (roiname .contains("gulp"))	
					rois.add(roi);
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
}