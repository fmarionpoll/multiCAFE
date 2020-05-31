package plugins.fmp.multicafeSequence;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.EnumXLSExportType;




public class XYTaSeries implements XMLPersistent {
	
	public Double 			moveThreshold 		= 50.;
	public int				sleepThreshold		= 5;
	public int 				lastTimeAlive 		= 0;
	public int 				lastIntervalAlive 	= 0;
	public ArrayList<XYTaValue> pointsList  	= new ArrayList<XYTaValue>();
	
	public String			name 				= null;
	public EnumXLSExportType exportType 		= null;
	public int				binsize				= 1;
	public Point2D			origin				= new Point2D.Double(0, 0);
	public double			pixelsize			= 1.;
	public int				nflies				= 1;
	


	public void ensureCapacity(int minCapacity) {
		pointsList.ensureCapacity(minCapacity);
	}
	
	public XYTaSeries() {
	}
	
	public XYTaSeries(String name, EnumXLSExportType exportType, int nFrames, int binsize) {
		this.name = name;
		this.exportType = exportType;
		this.binsize = binsize;
		pointsList 	= new ArrayList<XYTaValue>(nFrames);
		for (int i = 0; i< nFrames; i++) {
			pointsList.add(new XYTaValue(i*binsize));
		}
	}
	
	public XYTaValue getAt(int indexData) {
		XYTaValue val = null;
		if (indexData < pointsList.size())
			val = pointsList.get(indexData);
		return val;
	}
	
	public XYTaValue getLast() {			
		XYTaValue val = null;
		if (pointsList.size()>0) 
			val = pointsList.get(pointsList.size()-1);
		return val;
	}
	
	public void clear() {
		pointsList.clear();
	}
	
	public Point2D getPoint(int i) {
		return pointsList.get(i).point;
	}
	
	public Point2D getValidPointAtOrBefore(int index) {
		Point2D point = new Point2D.Double(-1, -1);
		for (int i = index; i>= 0; i--) {
			XYTaValue xyVal = pointsList.get(i);
			if (xyVal.point.getX() >= 0 && xyVal.point.getY() >= 0) {
				point = xyVal.point;
				break;
			}	
		}
		return point;
	}
	
	public int getTime(int i) {
		return pointsList.get(i).time;
	}

	public void add(Point2D point, int frame) {
		XYTaValue pos = new XYTaValue(point, frame);
		pointsList.add(pos);
	}
	
	public void copy (XYTaSeries xySer) {
		moveThreshold = xySer.moveThreshold;
		sleepThreshold = xySer.sleepThreshold;
		lastTimeAlive = xySer.lastIntervalAlive;
		for (XYTaValue aVal: xySer.pointsList) {
			XYTaValue newVal = new XYTaValue();
			newVal.copy(aVal);
			pointsList.add(newVal);
		}
		name = xySer.name;
		exportType = xySer.exportType;
		binsize = xySer.binsize;
	}

	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		
		Element node_lastime = XMLUtil.getElement(node, "lastTimeItMoved");
		lastTimeAlive = XMLUtil.getAttributeIntValue(node_lastime, "tlast", -1);
		lastIntervalAlive = XMLUtil.getAttributeIntValue(node_lastime, "ilast", -1);

		Element node_position_list = XMLUtil.getElement(node, "PositionsList");
		if (node_position_list == null) 
			return false;
		
		pointsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(node_position_list, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			XYTaValue pos = new XYTaValue();
			pos.loadFromXML(node_position_i);
			pointsList.add(pos);
		}
		return true;
	}

	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		
		Element node_lastime = XMLUtil.addElement(node, "lastTimeAlive");
		XMLUtil.setAttributeIntValue(node_lastime, "tlast", lastTimeAlive);
		lastIntervalAlive = getLastIntervalAlive();
		XMLUtil.setAttributeIntValue(node_lastime, "ilast", lastIntervalAlive);
		
		Element node_position_list = XMLUtil.addElement(node, "PositionsList");
		XMLUtil.setAttributeIntValue(node_position_list, "nb_items", pointsList.size());
		
		int i = 0;
		for (XYTaValue pos: pointsList) {
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.addElement(node_position_list, elementi);
			pos.saveToXML(node_position_i);
			i++;
		}
		return true;
	}
	
	public int computeLastIntervalAlive() {
		computeIsAlive();
		return lastIntervalAlive;
	}
	
	public void computeIsAlive() {
		computeDistanceBetweenPoints();
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= pointsList.size() - 1; i >= 0; i--) {
			XYTaValue pos = pointsList.get(i);
			if (pos.distance > moveThreshold && !isalive) {
				lastIntervalAlive = i;
				lastTimeAlive = pos.time;
				isalive = true;				
			}
			pos.alive = isalive;
		}
	}

	public void computeDistanceBetweenPoints() {
		if (pointsList.size() > 0) {
			Point2D previous = new Point2D.Double();
			previous = pointsList.get(0).point;
			for (XYTaValue pos: pointsList) {
				pos.distance = pos.point.distance(previous);
				if (previous.getX() < 0 || pos.point.getX() < 0)
					pos.distance = Double.NaN;
				previous = pos.point;
			}
		}
	}
	
	public List<Double> getIsAliveAsDoubleArray() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size());
		for (XYTaValue pos: pointsList) {
			dataArray.add(pos.alive ? 1.0: 0.0);
		}
		return dataArray;
	}
	
	public List<Integer> getIsAliveAsIntegerArray() {
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(pointsList.size());
		for (XYTaValue pos: pointsList) {
			dataArray.add(pos.alive ? 1: 0);
		}
		return dataArray;
	}
		
	public int getLastIntervalAlive() {
		if (lastIntervalAlive >= 0)
			return lastIntervalAlive;
		return computeLastIntervalAlive();
	}
	
	public int getTimeBinSize () {
		return pointsList.get(1).time - pointsList.get(0).time;
	}
	
	public Point2D getPointAt (int timeIndex) {
		if (pointsList.size() < 1)
			return null;
		
		int index = timeIndex / getTimeBinSize();
		return pointsList.get(index).point;
	}
		
	public Double getDistanceBetween2Points(int firstTimeIndex, int secondTimeIndex) {
		if (pointsList.size() < 2)
			return Double.NaN;
		int firstIndex = firstTimeIndex / getTimeBinSize();
		int secondIndex = secondTimeIndex / getTimeBinSize();
		if (firstIndex < 0 || secondIndex < 0 || firstIndex >= pointsList.size() || secondIndex >= pointsList.size())
			return Double.NaN;
		XYTaValue pos1 = pointsList.get(firstIndex);
		XYTaValue pos2 = pointsList.get(secondIndex);
		if (pos1.point.getX() < 0 || pos2.point.getX()  < 0)
			return Double.NaN;
		Double distance = pos2.point.distance(pos1.point); 
		return distance;
	}
	
	public int isAliveAtTimeIndex(int timeIndex) {
		if (pointsList.size() < 2)
			return 0;
		getLastIntervalAlive();
		int index = timeIndex / getTimeBinSize();
		XYTaValue pos = pointsList.get(index);
		return (pos.alive ? 1: 0); 
	}

	private List<Integer> getDistanceAsMoveOrNot() {
		computeDistanceBetweenPoints();
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(pointsList.size());
		for (int i= 0; i< pointsList.size(); i++) {
			dataArray.add(pointsList.get(i).distance < moveThreshold ? 1: 0);
		}
		return dataArray;
	}
	
	public void computeSleep() {
		if (pointsList.size() < 1)
			return;
		List <Integer> datai = getDistanceAsMoveOrNot();
		int timeBinSize = getTimeBinSize() ;
		int j = 0;
		for (XYTaValue pos: pointsList) {
			int isleep = 1;
			int k = 0;
			for (int i= 0; i < sleepThreshold; i+= timeBinSize) {
				if ((k+j) >= datai.size())
					break;
				isleep = datai.get(k+j) * isleep;
				if (isleep == 0)
					break;
				k++;
			}
			pos.sleep = (isleep == 1);
			j++;
		}
	}
	
	public List<Double> getSleepAsDoubleArray() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size());
		for (XYTaValue pos: pointsList) {
			dataArray.add(pos.sleep ? 1.0: 0.0);
		}
		return dataArray;
	}
	
	public int isAsleepAtTimeIndex(int timeIndex) {
		if (pointsList.size() < 2)
			return -1;
		int index = timeIndex / getTimeBinSize();
		if (index >= pointsList.size())
			return -1;
		return (pointsList.get(index).sleep ? 1: 0); 
	}

	public void computeNewPointsOrigin(Point2D newOrigin) {
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		for (XYTaValue pos : pointsList) {
			pos.point.setLocation(pos.point.getX()-deltaX, pos.point.getY()-deltaY);
		}
	}
	
	public void changePixelSize(double newpixelSize) {
		if (newpixelSize == pixelsize)
			return;
		double ratio = 1/pixelsize*newpixelSize;
		for (XYTaValue pos : pointsList) 
			pos.point.setLocation(pos.point.getX()*ratio, pos.point.getY()*ratio);
		pixelsize = newpixelSize;
		origin.setLocation(origin.getX()*ratio, origin.getY()*ratio);
	}

	
	public void clearValues(int fromIndex) {
		int toIndex = pointsList.size();
		if (fromIndex > 0 && fromIndex < toIndex) 
			pointsList.subList(fromIndex, toIndex).clear();
		
	}

}
