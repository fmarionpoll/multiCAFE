package plugins.fmp.multicafeSequence;



import java.util.List;
import icy.roi.ROI2D;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class SequenceKymosUtils {
	
	public static boolean 	isInterrupted 			= false;
	public static boolean 	isRunning 				= false;
	
	static String 			extension 				= ".tiff";
	
	// -------------------------------------------------------

	
	public static void transferCamDataROIStoKymo (SequenceCamData seqCams, SequenceKymos seqKymos) {
		List<ROI2D> listROISCap = ROI2DUtilities.getListofCapillariesFromSequence(seqCams);
		if (seqKymos == null) {
			System.out.println("seqkymos null - return");
			return;
		}
		if (seqKymos.capillaries == null)
			seqKymos.capillaries = new Capillaries();
		
		// rois not in cap?
		for (ROI2D roi:listROISCap) {
			boolean found = false;
			for (Capillary cap: seqKymos.capillaries.capillariesArrayList) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seqKymos.capillaries.capillariesArrayList.add(new Capillary((ROI2DShape)roi));
		}
		
		// cap with no corresponding roi?
		for (Capillary cap: seqKymos.capillaries.capillariesArrayList) {
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seqKymos.capillaries.capillariesArrayList.remove(cap);
		}
	}
	
	public static void transferKymoCapillariesToCamData (SequenceCamData seqCams, SequenceKymos seqKymos) {
		if (seqKymos == null || seqKymos.capillaries == null)
			return;
		
		List<ROI2D> listROISCap = ROI2DUtilities.getListofCapillariesFromSequence(seqCams);
		
		// cap with no corresponding roi?
		for (Capillary cap: seqKymos.capillaries.capillariesArrayList) {
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seqCams.seq.addROI(cap.capillaryRoi);
		}
	}
}
