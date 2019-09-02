package plugins.fmp.multicafeTools;


import java.awt.geom.Point2D;
import java.util.List;

import icy.type.geom.Polyline2D;

public class Polyline2DUtil {
	
	public static boolean insertSeriesofYPoints(List<Point2D> points, Polyline2D destinationArray, int start, int end) {
		if (start < 0 || end > (destinationArray.npoints -1))
			return false;
		int i_list = 0;
		for (int i_array= start; i_array < end; i_array++, i_list++) {
			destinationArray.ypoints[i_array] = points.get(i_list).getY(); 
		}
		return true;
	}
	
}
