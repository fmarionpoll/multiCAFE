package plugins.fmp.multicafe2.tools.chart;

import org.jfree.data.xy.XYSeriesCollection;

import plugins.fmp.multicafe2.tools.MinMaxDouble;

public class YPosMultiChartStructure 
{
	public MinMaxDouble minmax = null;
	public XYSeriesCollection xyDataset = null;

	public YPosMultiChartStructure () 
	{
	}
	
	public YPosMultiChartStructure (MinMaxDouble minmax, XYSeriesCollection xyDataset) 
	{
		this.minmax = minmax;
		this.xyDataset = xyDataset;
	}
}
