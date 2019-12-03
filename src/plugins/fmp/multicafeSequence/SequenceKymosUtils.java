package plugins.fmp.multicafeSequence;



import java.util.Iterator;
import java.util.List;
import icy.roi.ROI2D;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class SequenceKymosUtils {
	

	public static void transferCamDataROIStoKymo (SequenceCamData seqCam, SequenceKymos seqKymos) {
		if (seqKymos == null) {
			System.out.println("seqkymos null - return");
			return;
		}
		if (seqKymos.capillaries == null) {
			seqKymos.capillaries = new Capillaries();
			System.out.println("Error in SequenceKymosUtils:transferCamDataROIstoKymo = seqkymos.capillaries was null");
		}
		
		// rois not in cap? add
		List<ROI2D> listROISCap = seqCam.getCapillaries();
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
		
		// cap with no corresponding roi? remove
		Iterator<Capillary> iterator = seqKymos.capillaries.capillariesArrayList.iterator();
		while(iterator.hasNext()) {
			Capillary cap = iterator.next();
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				iterator.remove();
		}
	}
	
	public static void transferKymoCapillariesToCamData (SequenceCamData seqCams, SequenceKymos seqKymos) {
		if (seqKymos == null || seqKymos.capillaries == null)
			return;
		List<ROI2D> listROISCap = seqCams.getCapillaries();
		// roi with no corresponding cap? add ROI
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
