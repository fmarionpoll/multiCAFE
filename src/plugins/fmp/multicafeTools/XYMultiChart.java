package plugins.fmp.multicafeTools;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;

public class XYMultiChart extends IcyFrame  {

	public JPanel 	mainChartPanel = null;
	public IcyFrame mainChartFrame = null;
	
	private Point pt = new Point (0,0);
	private boolean flagMaxMinSet = false;
	private int globalYMax = 0;
	private int globalYMin = 0;

	private int ymax = 0;
	private int ymin = 0;
	private List<XYSeriesCollection> 	xyDataSetList 	= new ArrayList <XYSeriesCollection>();
	private List<XYSeriesCollection> 	xyDataSetList2 	= new ArrayList <XYSeriesCollection>();
	private List<JFreeChart> 			xyChartList 	= new ArrayList <JFreeChart>();
	private String title;

	//----------------------------------------
	public void createPanel(String cstitle) {

		title = cstitle;
		
		// create window 
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		mainChartFrame.add(mainChartPanel);
	}

	public void setLocationRelativeToRectangle(Rectangle rectv, Point deltapt) {
		pt = new Point(rectv.x + deltapt.x, rectv.y + deltapt.y);
	}
	
	public void displayData(Experiment exp, EnumListType option) {
		xyChartList.clear();
		SequenceKymos kymoseq = exp.seqKymos;
		if (kymoseq == null || kymoseq.seq == null)
			return;
		ymax = 0;
		ymin = 0;
		xyDataSetList.clear();
		xyDataSetList2.clear();
		flagMaxMinSet = false;
		int nimages = kymoseq.seq.getSizeT();
		int kmax = exp.capillaries.desc.grouping;
		int startFrame = (int) exp.capillaries.desc.analysisStart;
		// get data arrays to display
		for (int t=0; t< nimages; t+= kmax) {
			XYSeriesCollection xyDataset = new XYSeriesCollection();
			XYSeriesCollection xyDataset2 = new XYSeriesCollection();
			for (int k=0; k <kmax; k++) {
				if ((t+k) >= nimages)
					continue;
				Capillary cap = exp.capillaries.capillariesArrayList.get(t+k);
				EnumListType ooption = option;
				if (option == EnumListType.topAndBottom)
					ooption = EnumListType.topLevel;
				List<Integer> results = cap.getMeasures(ooption);
				if (option == EnumListType.topLevelDelta) {
					results = kymoseq.subtractTi(results);
				}
				XYSeries seriesXY = getXYSeries(results, cap.capillaryRoi.getName(), startFrame);
				if (option == EnumListType.topAndBottom) 
					appendDataToXYSeries(seriesXY, cap.getMeasures(EnumListType.bottomLevel), startFrame );
				xyDataset.addSeries( seriesXY );
				getMaxMin();
			}
			xyDataSetList.add(xyDataset);
			if (option == EnumListType.topAndBottom)
				xyDataSetList2.add(xyDataset2);
		}
		// display data in charts
		for (int i=0; i< xyDataSetList.size(); i++) {	
			XYSeriesCollection xyDataset = xyDataSetList.get(i);
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			// set Y range from 0 to max 
			xyChart.getXYPlot().getRangeAxis(0).setRange(globalYMin, globalYMax);
			if (option == EnumListType.topAndBottom) {
				XYSeriesCollection xyDataset2 = xyDataSetList2.get(i);
				xyChart.getXYPlot().setDataset(1, xyDataset2);
			}
			
			if (option == EnumListType.topLevel || option == EnumListType.bottomLevel || option == EnumListType.topAndBottom) {
				xyChart.getXYPlot().getRangeAxis(0).setInverted(true);
			}
			xyChartList.add(xyChart);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, 100, 200, 50, 100, 100, 200, false, false, true, true, true, true);
			mainChartPanel.add(xyChartPanel);
		}
		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}

	public void fetchNewData(Experiment exp, EnumListType option) {
		SequenceKymos kymoseq = exp.seqKymos;
		int ixy = 0;
		if (xyDataSetList == null || xyDataSetList.size() < 1)
			return;
		flagMaxMinSet = false;
		XYSeriesCollection xyDataset = null;
		XYSeriesCollection xyDataset2 = null;
		int kmax = exp.capillaries.desc.grouping;
		int nimages = kymoseq.seq.getSizeT();
		int startFrame = (int) exp.capillaries.desc.analysisStart;
		for (int t=0; t< nimages; t+= kmax, ixy++) {
			if (ixy >= xyDataSetList.size())
				break;
			xyDataset = xyDataSetList.get(ixy);
			xyDataset.removeAllSeries();
			EnumListType ooption = option;
			if (option == EnumListType.topAndBottom) {
				ooption = EnumListType.topLevel;
				xyDataset2 = xyDataSetList2.get(ixy);
				xyDataset2.removeAllSeries();
			}
			// collect xy data
			for (int k=0; k <kmax; k++) {
				if ((t+k) >= nimages)
					continue;
				Capillary cap = exp.capillaries.capillariesArrayList.get(t+k);
				List<Integer> results = cap.getMeasures(ooption);
				if (option == EnumListType.topLevelDelta) {
					results = kymoseq.subtractTi(results);
				}
				XYSeries seriesXY = getXYSeries(results, cap.capillaryRoi.getName(), startFrame);
				if (option == EnumListType.topAndBottom) 
					appendDataToXYSeries(seriesXY, cap.getMeasures(EnumListType.bottomLevel), startFrame );
				
				xyDataset.addSeries( seriesXY );
				getMaxMin();
			}
			// save data into xyDataSetList
			xyDataSetList.set(ixy, xyDataset);
			if (option ==  EnumListType.topAndBottom)
				xyDataSetList2.set(ixy, xyDataset2);
		}
		// create charts
		for (int i=0; i< xyDataSetList.size(); i++) {	
			xyDataset = xyDataSetList.get(i);
			JFreeChart xyChart = xyChartList.get(i);
			xyChart.getXYPlot().setDataset(0, xyDataset);
			xyChart.getXYPlot().getRangeAxis(0).setRange(globalYMin, globalYMax);
			
			if (option ==  EnumListType.topAndBottom) {
				xyDataset2 = xyDataSetList2.get(i);
				xyChart.getXYPlot().setDataset(1, xyDataset2);
			}
			// invert Y scale if raw levels
			if (option == EnumListType.topLevel || option == EnumListType.bottomLevel || option == EnumListType.topAndBottom) {
					xyChart.getXYPlot().getRangeAxis(0).setInverted(true);

			}
		}
	}

	private void getMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			flagMaxMinSet = true;
		}
		else {
			if (globalYMax < ymax) globalYMax = ymax;
			if (globalYMin >= ymin) globalYMin = ymin;
		}
	}

	private XYSeries getXYSeries(List<Integer> data, String name, int startFrame) {
		XYSeries seriesXY = new XYSeries(name, false);
		if (data != null) {
			int npoints = data.size();
			if (npoints != 0) {
				int x = 0;
				ymax = data.get(0);
				ymin = ymax;
				for (int j=0; j < npoints; j++) {
					int y = data.get(j);
					seriesXY.add( x+startFrame , y );
					if (ymax < y) ymax = y;
					if (ymin > y) ymin = y;
					x++;
				}
			}
		}
		return seriesXY;
	}

	private void appendDataToXYSeries(XYSeries seriesXY, List<Integer> data, int startFrame ) {
		
		if (data == null)
			return;
		int npoints = data.size();
		if (npoints != 0) {
			seriesXY.add(Double.NaN, Double.NaN);
			int x = 0;
			for (int j=0; j < npoints; j++) {
				int y = data.get(j);
				seriesXY.add( x+startFrame , y );
				if (ymax < y) ymax = y;
				if (ymin > y) ymin = y;
				x++;
			}
		}
	}
		
}
