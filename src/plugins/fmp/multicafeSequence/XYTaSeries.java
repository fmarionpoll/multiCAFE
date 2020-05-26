package plugins.fmp.multicafeSequence;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.EnumListType;



public class XYTaSeries implements XMLPersistent {
	
	public Double 			moveThreshold 		= 50.;
	public int				sleepThreshold		= 5;
	public int 				lastTimeAlive 		= 0;
	public int 				lastIntervalAlive 	= 0;
	public ArrayList<XYTaValue> pointsList  	= new ArrayList<XYTaValue>();

	
	public void ensureCapacity(int minCapacity) {
		pointsList.ensureCapacity(minCapacity);
	}
	
	public XYTaSeries() {
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
	
	public List<Double> getDoubleArrayList (EnumListType option) {
		if (pointsList.size() == 0)
			return null;
		List<Double> datai = null;
		
		switch (option) {
		case distance:
			datai = getDistanceBetweenPoints();
			break;
		case isalive:
			datai = getDistanceBetweenPoints();
			computeIsAlive(datai, moveThreshold);
			datai = getIsAliveAsDoubleArray();
			break;
		case sleep:
			computeSleep();
			datai = getSleepAsDoubleArray();
			break;
		case xyPosition:
		default:
			datai = getXYPositions();
			break;
		}
		return datai;
	}
	
	public int computeLastIntervalAlive() {
		computeIsAlive(getDistanceBetweenPoints(), moveThreshold);
		return lastIntervalAlive;
	}
	
	private List<Double> getDistanceBetweenPoints() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size());
		if (pointsList.size() > 0) {
			Point2D previous = new Point2D.Double();
			previous = pointsList.get(0).point;
			for (XYTaValue pos: pointsList) {
				double distance = pos.point.distance(previous);
				if (previous.getX() < 0 || pos.point.getX() < 0)
					distance = Double.NaN;
				dataArray.add(distance);
				previous = pos.point;
			}
		}
		return dataArray;
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
	
	public void computeIsAlive(List<Double> data, Double threshold) {
		this.moveThreshold = threshold;
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= data.size() - 1; i >= 0; i--) {
			if (data.get(i) > threshold && !isalive) {
				lastIntervalAlive = i;
				lastTimeAlive = pointsList.get(i).time;
				isalive = true;				
			}
			pointsList.get(i).alive = isalive;
		}
	}
	
	private List<Double> getXYPositions() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size()*2);
		for (XYTaValue pos: pointsList) {
			double x = pos.point.getX(); 
			double y = pos.point.getY();
			dataArray.add(x);
			dataArray.add(y);
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
		if (firstIndex < 0 || secondIndex < 0)
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

	public void computeSleep() {
		List<Double> datai = getDistanceBetweenPoints();
		int sleepintervals = getTimeBinSize() * sleepThreshold;
		int j = 0;
		for (XYTaValue pos: pointsList) {
			boolean sleep = false;
			if (j + sleepintervals >= datai.size())
				break;
			for (int i= 0; i< sleepintervals; i++) {
				sleep &= datai.get(i+j) <= moveThreshold;
			}
			pos.sleep = sleep;
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
}
