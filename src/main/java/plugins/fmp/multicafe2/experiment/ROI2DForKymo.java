package plugins.fmp.multicafe2.experiment;

import java.util.ArrayList;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;

public class ROI2DForKymo implements XMLPersistent 
{
	private int		index			= 0;
	private ROI2D 	roi 			= null;	
	private long 	start 			= 0;
	private long 	end 			= -1;
	private ArrayList<ArrayList<int[]>> masksList = null;
	
	private final String 	ID_META 	= "metaT";
	private final String 	ID_INDEX	= "indexT";
	private final String 	ID_START 	= "startT";
	private final String 	ID_END 		= "endT";
	
	
	public ROI2DForKymo(long start, long end, ROI2D roi) {
		setRoi(roi);
		this.start = start;
		this.end = end;
	}
	
	public ROI2DForKymo() {
	}

	public long getStart() {
		return start;
	}
	public long getEnd() {
		return end;
	}
	public ROI2D getRoi() {
		return roi;
	}
	
	public ArrayList<ArrayList<int[]>> getMasksList() {
		return masksList;
	}
	
	public void setStart(long start) {
		this.start = start;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public void setRoi(ROI2D roi) {
		this.roi = (ROI2D) roi.getCopy();
	}
	
	public void setMasksList(ArrayList<ArrayList<int[]>> masksList) {
		this.masksList = masksList;
	}

	@Override
	public boolean loadFromXML(Node node) {
		final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    if (nodeMeta == null)
	    	return false;
	    
    	index 	= XMLUtil.getElementIntValue(nodeMeta, ID_INDEX, 0);
        start 	= XMLUtil.getElementLongValue(nodeMeta, ID_START, 0);
        end 	= XMLUtil.getElementLongValue(nodeMeta, ID_END, -1);
        roi 	= ROI2DUtilities.loadFromXML_ROI(nodeMeta);
        return true;    
	}

	@Override
	public boolean saveToXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta == null) 
	    	return false;
	    XMLUtil.setElementIntValue(nodeMeta, ID_INDEX, index);
        XMLUtil.setElementLongValue(nodeMeta, ID_START, start);
        XMLUtil.setElementLongValue(nodeMeta, ID_END, end);
        ROI2DUtilities.saveToXML_ROI(nodeMeta, roi);
		return true;
	}
}
