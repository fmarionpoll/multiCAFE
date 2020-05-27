package plugins.fmp.multicafeTools;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.XYTaSeries;


public class YPosMultiChart extends IcyFrame {

	public JPanel 	mainChartPanel = null;
	private ArrayList<ChartPanel> chartsInMainChartPanel = null;
	List<XYSeriesCollection> xyDataSetList = null;
	
	public IcyFrame mainChartFrame = null;
	private String 	title;
	private Point 	pt = new Point (0,0);
	
	public void createPanel(String cstitle) {
		title = cstitle; 
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		mainChartFrame.add(mainChartPanel);
	}
	
	public void setLocationRelativeToRectangle(Rectangle rectv, Point deltapt) {
		pt = new Point(rectv.x + deltapt.x, rectv.y + deltapt.y);
	}
	
	public void displayData(List<Cage> cageList, EnumListType option) {
		if (xyDataSetList == null)
			displayNewData(cageList, option);
		else
			fetchNewData(cageList, option);
	}
	
	private void displayNewData(List<Cage> cageList, EnumListType option) {
		xyDataSetList = new ArrayList <XYSeriesCollection>();
		MinMaxDouble valMinMax = new MinMaxDouble();
		int count = 0;
		for (Cage cage: cageList) {
			if (cage.flyPositions != null && cage.flyPositions.pointsList.size() > 0) {	
				YPosMultiChartStructure struct = getDataSet(cage, option);
				XYSeriesCollection xyDataset = struct.xyDataset;
				valMinMax = struct.minmax;
				if (count != 0)
					valMinMax.getMaxMin(struct.minmax);
				xyDataSetList.add(xyDataset);
				count++;
			}
		}
		cleanChartsPanel(chartsInMainChartPanel);
		int width = 100;
		boolean displayLabels = false; 
		
		for (XYSeriesCollection xyDataset: xyDataSetList) {
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			ValueAxis yAxis = xyChart.getXYPlot().getRangeAxis(0);
			yAxis.setRange(valMinMax.min, valMinMax.max);
			yAxis.setTickLabelsVisible(displayLabels);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, width, 200, 50, 100, 100, 200, false, false, true, true, true, true);
			mainChartPanel.add(xyChartPanel);
			width = 100;
			displayLabels = false; 
		}

		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}
	
	private void fetchNewData (List<Cage> cageList, EnumListType option) {
		for (Cage cage: cageList) {
			String name = cage.roi.getName();
			for (XYSeriesCollection xySeriesList: xyDataSetList) {
				int countseries = xySeriesList.getSeriesCount();
				for (int i = 0; i< countseries; i++) {
					XYSeries xySeries = xySeriesList.getSeries(i);
					if (name .equals(xySeries.getDescription())) {
						xySeries.clear();
						addPointsToXYSeries(cage, option, xySeries);
						break;
					}
				}
			}
		}
	}
	
	private MinMaxDouble addPointsToXYSeries(Cage cage, EnumListType option, XYSeries seriesXY) {
		XYTaSeries positionxyt = cage.flyPositions;
		int itmax = positionxyt.pointsList.size();
		MinMaxDouble minmax =null;
		if (itmax > 0) {
			switch (option) {
			case distance:
				double previousY = positionxyt.pointsList.get(0).point.getY();
				for ( int it = 0; it < itmax;  it++) {
					double currentY = positionxyt.pointsList.get(it).point.getY();
					double ypos = currentY - previousY;
					double t = positionxyt.pointsList.get(it).time;
					seriesXY.add( t, ypos );
					previousY = currentY;
				}
				Rectangle rect = cage.roi.getBounds();
				double length_diagonal = Math.sqrt((rect.height*rect.height) + (rect.width*rect.width));
				minmax = new MinMaxDouble(0.0, length_diagonal);
				break;
				
			case isalive:
				for ( int it = 0; it < itmax;  it++) {
					boolean alive = positionxyt.pointsList.get(it).alive;
					double ypos = alive? 1.0: 0.0;
					double t = positionxyt.pointsList.get(it).time;
					seriesXY.add( t, ypos );
				}
				minmax = new MinMaxDouble(0., 1.2);
				break;
				
			case sleep:
				for ( int it = 0; it < itmax;  it++) {
					boolean sleep = positionxyt.pointsList.get(it).sleep;
					double ypos = sleep ? 1.0: 0.0;
					double t = positionxyt.pointsList.get(it).time;
					seriesXY.add( t, ypos );
				}
				minmax = new MinMaxDouble(0., 1.2);
				break;
				
			default:
			case xyPosition:
				Rectangle rect1 = cage.roi.getBounds();
				double yOrigin = rect1.getY()+rect1.getHeight();	
				for ( int it = 0; it < itmax;  it++) {
					Point2D point = positionxyt.pointsList.get(it).point;
					double ypos = yOrigin - point.getY();
					double t = positionxyt.pointsList.get(it).time;
					seriesXY.add( t, ypos );
				}
				minmax = new MinMaxDouble(0., rect1.height * 1.2);
				break;
			}
		}
		return minmax;
	}
	
	private YPosMultiChartStructure getDataSet(Cage cage, EnumListType option) {
		XYSeriesCollection xyDataset = new XYSeriesCollection();	
		String name = cage.roi.getName();
		XYSeries seriesXY = new XYSeries(name);
		seriesXY.setDescription(name);
		MinMaxDouble minmax = addPointsToXYSeries(cage, option, seriesXY);
		xyDataset.addSeries(seriesXY);
		return new YPosMultiChartStructure(minmax, xyDataset);
	}
	
	private void cleanChartsPanel (ArrayList<ChartPanel> chartsPanel) {
		if (chartsPanel != null && chartsPanel.size() > 0) {
			chartsPanel.clear();
		}
	}

}
