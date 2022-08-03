package plugins.fmp.multicafe2.experiment;


import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;




public class XYTaSeriesArrayList implements XMLPersistent 
{	
	public Double 			moveThreshold 		= 50.;
	public int				sleepThreshold		= 5;
	public int 				lastTimeAlive 		= 0;
	public int 				lastIntervalAlive 	= 0;
	public ArrayList<XYTaValue> xytArrayList	= new ArrayList<XYTaValue>();
	
	public String			name 				= null;
	public EnumXLSExportType exportType 		= null;
	public int				binsize				= 1;
	public Point2D			origin				= new Point2D.Double(0, 0);
	public double			pixelsize			= 1.;
	public int				nflies				= 1;
	
	private String ID_NBITEMS 			= "nb_items";
	private String ID_POSITIONSLIST 	= "PositionsList";
	private String ID_LASTIMEITMOVED 	= "lastTimeItMoved";
	private String ID_TLAST				= "tlast";
	private String ID_ILAST				= "ilast";

	
	public XYTaSeriesArrayList() 
	{
	}
	
	public XYTaSeriesArrayList(String name, EnumXLSExportType exportType, int nFrames, int binsize) 
	{
		this.name = name;
		this.exportType = exportType;
		this.binsize = binsize;
		xytArrayList = new ArrayList<XYTaValue>(nFrames);
		for (int i = 0; i< nFrames; i++) 
			xytArrayList.add(new XYTaValue(i));
	}
	
	public void clear() 
	{
		xytArrayList.clear();
	}
	
	public void ensureCapacity(int nFrames) 
	{
		xytArrayList.ensureCapacity(nFrames);
//		initArray(nFrames);
	}
	
	void initArray(int nFrames) 
	{
		for (int i = 0; i < nFrames; i++) {
			XYTaValue value = new XYTaValue(i);
			xytArrayList.add(value);
		}
	}
	
	public Point2D getPoint(int i) 
	{
		return xytArrayList.get(i).xyPoint;
	}
	
	public Point2D getValidPointAtOrBefore(int index) 
	{
		Point2D point = new Point2D.Double(-1, -1);
		for (int i = index; i>= 0; i--) 
		{
			XYTaValue xyVal = xytArrayList.get(i);
			if (xyVal.xyPoint.getX() >= 0 && xyVal.xyPoint.getY() >= 0) {
				point = xyVal.xyPoint;
				break;
			}	
		}
		return point;
	}
	
	public int getTime(int i) 
	{
		return xytArrayList.get(i).indexT;
	}

	public void addPosition (int frame, Point2D point, Rectangle2D rectangle) 
	{
		XYTaValue pos = new XYTaValue(frame, point, rectangle);
		xytArrayList.add(pos);
	}
	
	public void copyXYTaSeries (XYTaSeriesArrayList xySer) 
	{
		moveThreshold = xySer.moveThreshold;
		sleepThreshold = xySer.sleepThreshold;
		lastTimeAlive = xySer.lastIntervalAlive;
		xytArrayList = new ArrayList<XYTaValue>(xySer.xytArrayList.size());
		xytArrayList.addAll(xytArrayList);
		name = xySer.name;
		exportType = xySer.exportType;
		binsize = xySer.binsize;
	}

	@Override
	public boolean loadFromXML(Node node) 
	{
		if (node == null)
			return false;
		
		Element node_lastime = XMLUtil.getElement(node, ID_LASTIMEITMOVED);
		lastTimeAlive = XMLUtil.getAttributeIntValue(node_lastime, ID_TLAST, -1);
		lastIntervalAlive = XMLUtil.getAttributeIntValue(node_lastime, ID_ILAST, -1);

		Element node_position_list = XMLUtil.getElement(node, ID_POSITIONSLIST);
		if (node_position_list == null) 
			return false;
		
		xytArrayList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(node_position_list, ID_NBITEMS, 0);
		xytArrayList.ensureCapacity(nb_items);
		for (int i = 0; i< nb_items; i++) 
			xytArrayList.add(new XYTaValue(i));
		boolean bAdded = false;
		
		for (int i=0; i< nb_items; i++) 
		{
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			XYTaValue pos = new XYTaValue();
			pos.loadFromXML(node_position_i);
			if (pos.indexT < nb_items) 
				xytArrayList.set(pos.indexT, pos);
			else 
			{
				xytArrayList.add(pos);
				bAdded = true;
			}
		}
		
		if (bAdded)
			Collections.sort(xytArrayList, new Comparators.XYTaValue_Tindex_Comparator());
		return true;
	}

	@Override
	public boolean saveToXML(Node node) 
	{
		if (node == null)
			return false;
		
		Element node_lastime = XMLUtil.addElement(node, ID_LASTIMEITMOVED);
		XMLUtil.setAttributeIntValue(node_lastime, ID_TLAST, lastTimeAlive);
		lastIntervalAlive = getLastIntervalAlive();
		XMLUtil.setAttributeIntValue(node_lastime, ID_ILAST, lastIntervalAlive);
		
		Element node_position_list = XMLUtil.addElement(node, ID_POSITIONSLIST);
		XMLUtil.setAttributeIntValue(node_position_list, ID_NBITEMS, xytArrayList.size());
		
		int i = 0;
		for (XYTaValue pos: xytArrayList) 
		{
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.addElement(node_position_list, elementi);
			pos.saveToXML(node_position_i);
			i++;
		}
		return true;
	}
	
	public int computeLastIntervalAlive() 
	{
		computeIsAlive();
		return lastIntervalAlive;
	}
	
	public void computeIsAlive() 
	{
		computeDistanceBetweenPoints();
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= xytArrayList.size() - 1; i >= 0; i--) 
		{
			XYTaValue pos = xytArrayList.get(i);
			if (pos.distance > moveThreshold && !isalive) 
			{
				lastIntervalAlive = i;
				lastTimeAlive = pos.indexT;
				isalive = true;				
			}
			pos.bAlive = isalive;
		}
	}
	
	public void checkIsAliveFromAliveArray() 
	{
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= xytArrayList.size() - 1; i >= 0; i--) 
		{
			XYTaValue pos = xytArrayList.get(i);
			if (!isalive && pos.bAlive) 
			{
				lastIntervalAlive = i;
				lastTimeAlive = pos.indexT;
				isalive = true;				
			}
			pos.bAlive = isalive;
		}
	}

	public void computeDistanceBetweenPoints() 
	{
		if (xytArrayList.size() > 0) 
		{
			Point2D previous = new Point2D.Double();
			previous = xytArrayList.get(0).xyPoint;
			for (XYTaValue pos: xytArrayList) 
			{
				pos.distance = pos.xyPoint.distance(previous);
				if (previous.getX() < 0 || pos.xyPoint.getX() < 0)
					pos.distance = Double.NaN;
				previous = pos.xyPoint;
			}
		}
	}
	
	// -----------------------------------------------------------
	
	public void computeDistanceBetweenPoints(XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		if (flyPositions.xytArrayList.size() > 0) 
		{
			int it_start = 0;
			int it_end = flyPositions.xytArrayList.size() * stepMs;
			Point2D previous = new Point2D.Double();
			previous = xytArrayList.get(it_start).xyPoint;
			int it_out = 0;
			for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
			{
				XYTaValue pos = xytArrayList.get(it_out);
				int index = it/stepMs;
				pos.copy(flyPositions.xytArrayList.get(index));
				pos.distance = pos.xyPoint.distance(previous);
				if (previous.getX() < 0 || pos.xyPoint.getX() < 0)
					pos.distance = Double.NaN;
				previous = pos.xyPoint;
			}
		}
	}
	
	public void computeIsAlive(XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		flyPositions.computeIsAlive();
		int it_start = 0;
		int it_end = flyPositions.xytArrayList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			XYTaValue pos = xytArrayList.get(it_out);
			pos.bAlive = flyPositions.xytArrayList.get(index).bAlive;
		}
	}
	
	public void computeSleep(XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		flyPositions.computeSleep();
		int it_start = 0;
		int it_end = flyPositions.xytArrayList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			XYTaValue pos = xytArrayList.get(it_out);
			pos.bSleep = flyPositions.xytArrayList.get(index).bSleep;
		}
	}
	
	public void computeNewPointsOrigin(Point2D newOrigin, XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		int it_start = 0;
		int it_end = flyPositions.xytArrayList.size()  * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			XYTaValue pos_from = flyPositions.xytArrayList.get(index);
			XYTaValue pos_to = xytArrayList.get(it_out);
			pos_to.copy(pos_from);
			pos_to.xyPoint.setLocation( pos_to.xyPoint.getX()-deltaX, pos_to.xyPoint.getY()-deltaY);
		}
	}
	
	// ------------------------------------------------------------
	
	public List<Double> getIsAliveAsDoubleArray() 
	{
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (XYTaValue pos: xytArrayList) 
			dataArray.add(pos.bAlive ? 1.0: 0.0);
		return dataArray;
	}
	
	public List<Integer> getIsAliveAsIntegerArray() 
	{
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (XYTaValue pos: xytArrayList) 
		{
			dataArray.add(pos.bAlive ? 1: 0);
		}
		return dataArray;
	}
		
	public int getLastIntervalAlive() 
	{
		if (lastIntervalAlive >= 0)
			return lastIntervalAlive;
		return computeLastIntervalAlive();
	}
	
	public int getTimeBinSize () 
	{
		return xytArrayList.get(1).indexT - xytArrayList.get(0).indexT;
	}
	
	public Point2D getPointAt (int timeIndex) 
	{
		if (xytArrayList.size() < 1)
			return null;	
		int index = timeIndex / getTimeBinSize();
		return xytArrayList.get(index).xyPoint;
	}
		
	public Double getDistanceBetween2Points(int firstTimeIndex, int secondTimeIndex) 
	{
		if (xytArrayList.size() < 2)
			return Double.NaN;
		int firstIndex = firstTimeIndex / getTimeBinSize();
		int secondIndex = secondTimeIndex / getTimeBinSize();
		if (firstIndex < 0 || secondIndex < 0 || firstIndex >= xytArrayList.size() || secondIndex >= xytArrayList.size())
			return Double.NaN;
		XYTaValue pos1 = xytArrayList.get(firstIndex);
		XYTaValue pos2 = xytArrayList.get(secondIndex);
		if (pos1.xyPoint.getX() < 0 || pos2.xyPoint.getX()  < 0)
			return Double.NaN;
		Double distance = pos2.xyPoint.distance(pos1.xyPoint); 
		return distance;
	}
	
	public int isAliveAtTimeIndex(int timeIndex) 
	{
		if (xytArrayList.size() < 2)
			return 0;
		getLastIntervalAlive();
		int index = timeIndex / getTimeBinSize();
		XYTaValue pos = xytArrayList.get(index);
		return (pos.bAlive ? 1: 0); 
	}

	private List<Integer> getDistanceAsMoveOrNot() 
	{
		computeDistanceBetweenPoints();
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (int i= 0; i< xytArrayList.size(); i++) 
			dataArray.add(xytArrayList.get(i).distance < moveThreshold ? 1: 0);
		return dataArray;
	}
	
	public void computeSleep() 
	{
		if (xytArrayList.size() < 1)
			return;
		List <Integer> datai = getDistanceAsMoveOrNot();
		int timeBinSize = getTimeBinSize() ;
		int j = 0;
		for (XYTaValue pos: xytArrayList) 
		{
			int isleep = 1;
			int k = 0;
			for (int i= 0; i < sleepThreshold; i+= timeBinSize) 
			{
				if ((k+j) >= datai.size())
					break;
				isleep = datai.get(k+j) * isleep;
				if (isleep == 0)
					break;
				k++;
			}
			pos.bSleep = (isleep == 1);
			j++;
		}
	}
	
	public List<Double> getSleepAsDoubleArray() 
	{
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (XYTaValue pos: xytArrayList) 
			dataArray.add(pos.bSleep ? 1.0: 0.0);
		return dataArray;
	}
	
	public int isAsleepAtTimeIndex(int timeIndex) 
	{
		if (xytArrayList.size() < 2)
			return -1;
		int index = timeIndex / getTimeBinSize();
		if (index >= xytArrayList.size())
			return -1;
		return (xytArrayList.get(index).bSleep ? 1: 0); 
	}

	public void computeNewPointsOrigin(Point2D newOrigin) 
	{
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		for (XYTaValue pos : xytArrayList) 
			pos.xyPoint.setLocation(pos.xyPoint.getX()-deltaX, pos.xyPoint.getY()-deltaY);
	}
	
	public void changePixelSize(double newpixelSize) 
	{
		if (newpixelSize == pixelsize)
			return;
		double ratio = 1/pixelsize*newpixelSize;
		for (XYTaValue pos : xytArrayList) 
			pos.xyPoint.setLocation(pos.xyPoint.getX()*ratio, pos.xyPoint.getY()*ratio);
		pixelsize = newpixelSize;
		origin.setLocation(origin.getX()*ratio, origin.getY()*ratio);
	}

	
	public void clearValues(int fromIndex) 
	{
		int toIndex = xytArrayList.size();
		if (fromIndex > 0 && fromIndex < toIndex) 
			xytArrayList.subList(fromIndex, toIndex).clear();
		
	}

}


