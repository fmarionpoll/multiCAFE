package plugins.fmp.multicafeTools.ToExcel;

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
import org.jfree.chart.axis.ValueAxis;
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
	private int globalXMax = 0;

	private int ymax = 0;
	private int ymin = 0;
	private int xmax = 0;
	private List<XYSeriesCollection> 	xyDataSetList 	= new ArrayList <XYSeriesCollection>();
	private List<JFreeChart> 			xyChartList 	= new ArrayList <JFreeChart>();
	private String title;

	//----------------------------------------
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
	
	private void getDataArrays(Experiment exp, EnumXLSExportType option, List<XYSeriesCollection> xyList) {
		SequenceKymos kymoseq = exp.seqKymos;
		int nimages = kymoseq.seq.getSizeT();
		int startFrame = (int) exp.capillaries.desc.analysisStart;
		int kmax = exp.capillaries.desc.grouping;
		char collection_char = '-';
		XYSeriesCollection xyDataset = null;
		for (int t=0; t< nimages; t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			char test = cap.getCapillaryName().charAt(cap.getCapillaryName().length() - 2);
			if (kmax < 2 || test != collection_char) {
				if (xyDataset != null)
					xyList.add(xyDataset);
				xyDataset = new XYSeriesCollection();
				collection_char = test;
			}
			EnumXLSExportType ooption = option;
			if (option == EnumXLSExportType.TOPLEVEL)
				ooption = EnumXLSExportType.TOPLEVEL;
			List<Integer> results = cap.getMeasures(ooption);
			if (option == EnumXLSExportType.TOPLEVELDELTA) 
				results = kymoseq.subtractTi(results);
			String name = cap.roi.getName();
			if (t == 0)	// trick to change the size of the legend so that it takes the same vertical space as others 
				name = name + "    ";
			XYSeries seriesXY = getXYSeries(results, name, startFrame);
			
			if (option == EnumXLSExportType.TOPLEVEL) 
				appendDataToXYSeries(seriesXY, cap.getMeasures(EnumXLSExportType.BOTTOMLEVEL), startFrame );
			
			xyDataset.addSeries( seriesXY );
			getMaxMin();
		}
		if (xyDataset != null)
			xyList.add(xyDataset);
	}
	
	public void displayData(Experiment exp, EnumXLSExportType option) {
		xyChartList.clear();
		SequenceKymos kymoseq = exp.seqKymos;
		if (kymoseq == null || kymoseq.seq == null)
			return;
		ymax = 0;
		ymin = 0;
		xyDataSetList.clear();
		flagMaxMinSet = false;
		getDataArrays(exp, option, xyDataSetList);
		
		// display charts
		int width = 130;
		int minimumDrawWidth = 100;
		int maximumDrawWidth = width;
		int height = 200;
		int maximumDrawHeight = height;
		boolean displayLabels = true; 

		for (XYSeriesCollection xyDataset : xyDataSetList) {
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, false, false);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			
			ValueAxis yAxis = xyChart.getXYPlot().getRangeAxis(0);
			yAxis.setRange(globalYMin, globalYMax);
			yAxis.setTickLabelsVisible(displayLabels);
			ValueAxis xAxis = xyChart.getXYPlot().getDomainAxis(0);
			xAxis.setRange(0, globalXMax);

			if (option == EnumXLSExportType.TOPLEVEL || option == EnumXLSExportType.BOTTOMLEVEL) {
				xyChart.getXYPlot().getRangeAxis(0).setInverted(true);
			}
			xyChartList.add(xyChart);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, width, height, minimumDrawWidth, 100, 
					maximumDrawWidth, maximumDrawHeight, false, false, true, true, true, true);
			mainChartPanel.add(xyChartPanel);

			width = 100;
			height = 200;
			minimumDrawWidth = 50;
			maximumDrawWidth = width;
			maximumDrawHeight = 200;
			displayLabels = false; 
		}
		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}

	private void getMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
		}
		else {
			if (globalYMax < ymax) globalYMax = ymax;
			if (globalYMin >= ymin) globalYMin = ymin;
			if (globalXMax < xmax) globalXMax = xmax;
		}
	}

	private XYSeries getXYSeries(List<Integer> data, String name, int startFrame) {
		XYSeries seriesXY = new XYSeries(name, false);
		if (data != null) {
			int npoints = data.size();
			if (npoints != 0) {
				xmax = npoints;
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
