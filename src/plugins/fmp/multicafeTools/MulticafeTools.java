package plugins.fmp.multicafeTools;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.file.FileUtil;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.plugin.abstract_.Plugin;
import icy.type.geom.Polygon2D;



public class MulticafeTools extends Plugin {

	public static String saveFileAs(String defaultName, String directory, String csExt) {		
		// load last preferences for loader
		String csFile = null;
		final JFileChooser fileChooser = new JFileChooser();
		if (directory != null) {
			fileChooser.setCurrentDirectory(new File(directory));
		}
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY );
		FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter(csExt+" files",  csExt, csExt);
		fileChooser.addChoosableFileFilter(xlsFilter);
		fileChooser.setFileFilter(xlsFilter);
		if (defaultName != null)
			fileChooser.setSelectedFile(new File(defaultName));

		final int returnValue = fileChooser.showSaveDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			csFile = f.getAbsolutePath();
			int dotOK = csExt.indexOf(".");
			if (dotOK < 0)
				csExt = "." + csExt;
			int extensionOK = csFile.indexOf(csExt);
			if (extensionOK < 0)
			{
				csFile += csExt;
				f = new File(csFile);
			}

			if(f.exists())
				if (ConfirmDialog.confirm("Overwrite existing file ?"))
					FileUtil.delete(f, true);
				else 
					csFile = null;
		}
		return csFile;
	}

	public static String[] selectFiles(String directory, String csExt) {
		final JFileChooser fileChooser = new JFileChooser();

		final String path = directory;
		fileChooser.setCurrentDirectory(new File(path));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled(true);
		FileNameExtensionFilter csFilter = new FileNameExtensionFilter(csExt+" files",  csExt, csExt);
		fileChooser.addChoosableFileFilter(csFilter);
		fileChooser.setFileFilter(csFilter);

		final int returnValue = fileChooser.showDialog(null, "Load");
		String[] liststrings = null;
		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			File[] files = fileChooser.getSelectedFiles();
			liststrings = new String[files.length];
			
			for (int i=0; i< files.length; i++) {
				liststrings[i] = files[i].getAbsolutePath();
			}
		}
		return liststrings;
	}

	public static int[] rank(double[] values) {
		/** Returns a sorted list of indices of the specified double array.
		Modified from: http://stackoverflow.com/questions/951848 by N.Vischer.
		 */
		int n = values.length;
		final Integer[] indexes = new Integer[n];
		final Double[] data = new Double[n];
		for (int i=0; i<n; i++) {
			indexes[i] = new Integer(i);
			data[i] = new Double(values[i]);
		}
		Arrays.sort(indexes, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				return data[o1].compareTo(data[o2]);
			}
		});
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++)
			indexes2[i] = indexes[i].intValue();
		return indexes2;
	}

	public static int[] rank(final String[] data) {
		/** Returns a sorted list of indices of the specified String array. */
		int n = data.length;
		final Integer[] indexes = new Integer[n];
		for (int i=0; i<n; i++)
			indexes[i] = new Integer(i);
		Arrays.sort(indexes, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				return data[o1].compareToIgnoreCase(data[o2]);
			}
		});
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++)
			indexes2[i] = indexes[i].intValue();
		return indexes2;
	}
	
	public static Polygon2D orderVerticesofPolygon(Polygon roiPolygon) {
		if (roiPolygon.npoints > 4)
			new AnnounceFrame("Only the first 4 points of the polygon will be used...");
		Polygon2D extFrame = new Polygon2D();
		Rectangle rect = roiPolygon.getBounds();
		Rectangle rect1 = new Rectangle(rect);
		// find upper left
		rect1.setSize(rect.width/2, rect.height/2);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find lower left
		rect1.translate(0, rect.height/2 +2);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find lower right
		rect1.translate(rect.width/2+2, 0);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find upper right
		rect1.translate(0, -rect.height/2 - 2);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		return extFrame;
	}
	
	public static Polygon2D inflate(Polygon2D roiPolygon, int ncolumns, int nrows, int width_cage, int width_interval ) {
		double width_x_current = ncolumns*(width_cage + 2 * width_interval) - 2 * width_interval;
		double deltax_top = (roiPolygon.xpoints[3]- roiPolygon.xpoints[0]) * width_interval / width_x_current ;
		double deltax_bottom = (roiPolygon.xpoints[2]- roiPolygon.xpoints[1])  * width_interval / width_x_current ;
		
		double width_y_current = nrows*(width_cage + 2 * width_interval) - 2 * width_interval;
		double deltay_left = (roiPolygon.ypoints[1]- roiPolygon.ypoints[0]) * width_interval / width_y_current ;
		double deltay_right = (roiPolygon.ypoints[2]- roiPolygon.ypoints[3]) * width_interval / width_y_current ;

		double[] xpoints = new double[4];
		double[] ypoints = new double [4];
		int npoints = 4;
		
		xpoints[0] = roiPolygon.xpoints[0] - deltax_top;
		xpoints[1] = roiPolygon.xpoints[1] - deltax_bottom;
		xpoints[3] = roiPolygon.xpoints[3] + deltax_top;
		xpoints[2] = roiPolygon.xpoints[2] + deltax_bottom;
		
		ypoints[0] = roiPolygon.ypoints[0] - deltay_left;
		ypoints[3] = roiPolygon.ypoints[3] - deltay_right;
		ypoints[1] = roiPolygon.ypoints[1] + deltay_left;
		ypoints[2] = roiPolygon.ypoints[2] + deltay_right;
		
		Polygon2D result = new Polygon2D(xpoints, ypoints, npoints);
		return result;
	}
	
	public static Point2D lineIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
		if (denom == 0.0) { // Lines are parallel.
		     return null;
		}
		double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;
		double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;
		if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
		    // Get the intersection point.
		    return new Point2D.Double ( (x1 + ua*(x2 - x1)), (y1 + ua*(y2 - y1)));
			}
		return null; 
	}
	
}

