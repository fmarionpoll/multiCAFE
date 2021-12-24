package plugins.fmp.multicafe2.tools.chart;

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

import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.fmp.multicafe2.tools.toExcel.XLSExport;
import plugins.fmp.multicafe2.tools.toExcel.XLSExportOptions;
import plugins.fmp.multicafe2.tools.toExcel.XLSResults;

public class LevelsXYMultiChart extends IcyFrame  
{
	public JPanel 	mainChartPanel = null;
	public IcyFrame mainChartFrame = null;
	
	private Point pt = new Point (0,0);
	private boolean flagMaxMinSet = false;
	private double globalYMax = 0;
	private double globalYMin = 0;
	private double globalXMax = 0;

	private double ymax = 0;
	private double ymin = 0;
	private double xmax = 0;
	private List<JFreeChart> xyChartList  = new ArrayList <JFreeChart>();
	private String title;

	//----------------------------------------
	public void createPanel(String cstitle) 
	{
		title = cstitle;
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		mainChartFrame.add(mainChartPanel);
	}

	public void setLocationRelativeToRectangle(Rectangle rectv, Point deltapt) 
	{
		pt = new Point(rectv.x + deltapt.x, rectv.y + deltapt.y);
	}
	
	private void getDataArrays(Experiment exp, EnumXLSExportType option, boolean subtractEvaporation, List<XYSeriesCollection> xyList) 
	{
		XLSExport xlsExport = new XLSExport();
		XLSExportOptions options = new XLSExportOptions();
		options.buildExcelStepMs = 60000;
		options.t0 = true;
		options.subtractEvaporation = subtractEvaporation;
		List <XLSResults> resultsList = xlsExport.getCapDataFromOneExperimentSeriesForGraph(exp, option, options);
		List <XLSResults> resultsList2 = null;
		if (option == EnumXLSExportType.TOPLEVEL) 
			resultsList2 = xlsExport.getCapDataFromOneExperimentSeriesForGraph(exp, EnumXLSExportType.BOTTOMLEVEL, options);
		
		String previousName = null;
		XYSeriesCollection xyDataset = null;
		for (XLSResults results: resultsList) 
		{
			String currentName = results.name.substring(4, results.name.length()-1);
			if (xyDataset == null) 
			{
				xyDataset = new XYSeriesCollection();
				previousName = currentName; 
				//results.name +=  "    ";
			} 
			else if (!previousName.equals(currentName)) 
			{
				if (xyDataset != null)
					xyList.add(xyDataset);
				previousName = currentName;
				xyDataset = new XYSeriesCollection();
			}
			
			XYSeries seriesXY = getXYSeries(results, results.name.substring(4));
			if (resultsList2 != null)
			{
				for (XLSResults results2 : resultsList2) 
				{
					if (results2.name .equals(results.name)) 
					{
						appendDataToXYSeries(seriesXY, results2);
						break;
					}
				}
			}
			xyDataset.addSeries(seriesXY );
			updateGlobalMaxMin();
		}
		
		if (xyDataset != null)
			xyList.add(xyDataset);
	}
	
	public void displayData(Experiment exp, EnumXLSExportType option, boolean subtractEvaporation) 
	{
		xyChartList.clear();

		ymax = 0;
		ymin = 0;
		List<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		flagMaxMinSet = false;
		getDataArrays(exp, option, subtractEvaporation, xyDataSetList);
		
		// display charts
		int width = 130;
		int minimumDrawWidth = 100;
		int maximumDrawWidth = width;
		int height = 200;
		int maximumDrawHeight = height;
		boolean displayLabels = true;
		
		String yTitle = "volume (ul)";

		for (XYSeriesCollection xyDataset : xyDataSetList) 
		{
			JFreeChart xyChart = ChartFactory.createXYLineChart(
					null,				// chartname 
					null, 				// xDomain
					yTitle, 			// yDomain
					xyDataset, 			// collection
					PlotOrientation.VERTICAL, 
					true, 				// legend
					false, 				// tooltips
					false);				// url
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			
			ValueAxis yAxis = xyChart.getXYPlot().getRangeAxis(0);
			yAxis.setRange(globalYMin, globalYMax);
			yAxis.setTickLabelsVisible(displayLabels);
			ValueAxis xAxis = xyChart.getXYPlot().getDomainAxis(0);
			xAxis.setRange(0, globalXMax);
			yTitle = null;

			if (option == EnumXLSExportType.TOPLEVEL || option == EnumXLSExportType.BOTTOMLEVEL) 
				xyChart.getXYPlot().getRangeAxis(0).setInverted(true);
			
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

	private void updateGlobalMaxMin() 
	{
		if (!flagMaxMinSet) 
		{
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
		}
		else 
		{
			if (globalYMax < ymax) globalYMax = ymax;
			if (globalYMin >= ymin) globalYMin = ymin;
			if (globalXMax < xmax) globalXMax = xmax;
		}
	}
	
	private XYSeries getXYSeries(XLSResults results, String name) 
	{
		XYSeries seriesXY = new XYSeries(name, false);
		if (results.values_out != null && results.values_out.length > 0) 
		{
			xmax = results.values_out.length;
			ymax = results.values_out[0];
			ymin = ymax;
			addPointsAndUpdateExtrema(seriesXY, results, 0);	
		}
		return seriesXY;
	}

	private void appendDataToXYSeries(XYSeries seriesXY, XLSResults results ) 
	{	
		if (results.values_out != null && results.values_out.length > 0) 
		{
			seriesXY.add(Double.NaN, Double.NaN);
			addPointsAndUpdateExtrema(seriesXY, results, 0);	
		}
	}
	
	private void addPointsAndUpdateExtrema(XYSeries seriesXY, XLSResults results, int startFrame) 
	{
		int x = 0;
		int npoints = results.values_out.length;
		for (int j = 0; j < npoints; j++) 
		{
			double y = results.values_out[j];
			seriesXY.add( x+startFrame , y );
			if (ymax < y) ymax = y;
			if (ymin > y) ymin = y;
			x++;
		}
	}
}
