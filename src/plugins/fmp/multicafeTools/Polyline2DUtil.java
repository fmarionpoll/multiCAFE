package plugins.fmp.multicafeTools;


import java.awt.geom.Point2D;
import java.util.List;

import icy.type.geom.Polyline2D;

public class Polyline2DUtil {
	
	public static boolean InsertYPointsIntoArray(List<Point2D> points, Polyline2D array, int start, int end) {
		if (start < 0 || end > (array.npoints -1))
			return false;
		int i_list = 0;
		for (int i_array= start; i_array < end; i_array++, i_list++) {
			array.ypoints[i_array] = points.get(i_list).getY(); 
		}
		return true;
	}
	
}
