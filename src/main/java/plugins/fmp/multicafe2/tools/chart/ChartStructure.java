package plugins.fmp.multicafe2.tools.chart;

import org.jfree.data.xy.XYSeriesCollection;

import plugins.fmp.multicafe2.tools.MinMaxDouble;

public class ChartStructure 
{
	public MinMaxDouble minmax = null;
	public XYSeriesCollection xyDataset = null;

	public ChartStructure () 
	{
	}
	
	public ChartStructure (MinMaxDouble minmax, XYSeriesCollection xyDataset) 
	{
		this.minmax = minmax;
		this.xyDataset = xyDataset;
	}
}
