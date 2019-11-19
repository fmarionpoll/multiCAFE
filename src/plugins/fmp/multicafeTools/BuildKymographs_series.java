package plugins.fmp.multicafeTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;



public class BuildKymographs_series implements Runnable {
		public BuildKymographs_Options 	options 			= new BuildKymographs_Options();
		public boolean 					stopFlag 			= false;
		public boolean 					threadRunning 		= false;
		
		private IcyBufferedImage 		workImage 			= null; 
		private Sequence 				seqForRegistration	= new Sequence();
		private DataType 				dataType 			= DataType.INT;
		private int 					imagewidth =1;
		private ArrayList<double []> 	sourceValuesList 	= null;
		private List<ROI> 				roiList 			= null;
		Viewer viewer1 = null;
		
		
		@Override
		public void run() {
			threadRunning = true;
			int nbexp = options.index1 - options.index0;
			ProgressChrono progressBar = new ProgressChrono("Compute kymographs");
			progressBar.initStuff(nbexp);
			progressBar.setMessageFirstPart("Analyze series ");
			for (int index = options.index0; index < options.index1; index++) {
				Experiment exp = options.expList.experimentList.get(index);
				System.out.println(exp.experimentFileName);
				progressBar.updatePosition(index-options.index0);
				exp.loadExperimentData();
				initViewer(exp);
				options.seqCamData = exp.seqCamData;
				options.seqKymos = exp.seqKymos;
				exp.step = options.analyzeStep;
				if (computeKymo()) {
					saveComputation(exp);
				}
				closeViewer(exp);
			}
			progressBar.close();
			threadRunning = false;
		}
		

		
		private void closeViewer (Experiment exp) {
			exp.seqCamData.seq.close();
			exp.seqKymos.seq.close();
		}
		
		private void initViewer (Experiment exp) {
			ThreadUtil.invoke (new Runnable() {
				@Override
				public void run() {
					viewer1 = new Viewer(exp.seqCamData.seq, true);
				}
			}, true);
			if (viewer1 == null) {
				viewer1 = exp.seqCamData.seq.getFirstViewer(); 
				if (!viewer1.isInitialized()) {
					try {
						Thread.sleep(1000);
						if (!viewer1.isInitialized())
							System.out.println("Viewer still not initialized after 1 s waiting");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		private void saveComputation(Experiment exp) {			
			Path dir = Paths.get(exp.seqCamData.getDirectory());
			dir = dir.resolve("results");
			String directory = dir.toAbsolutePath().toString();
			if (Files.notExists(dir))  {
				try {
					Files.createDirectory(dir);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Creating directory failed: "+ directory);
					return;
				}
			}
			ProgressFrame progress = new ProgressFrame("Save kymographs");		
			for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) {
				Capillary cap = exp.seqKymos.capillaries.capillariesArrayList.get(t);
				progress.setMessage( "Save kymograph file : " + cap.getName());	
				String filename = directory + File.separator + cap.getName() + ".tiff";
				File file = new File (filename);
				IcyBufferedImage image = exp.seqKymos.seq.getImage(t, 0);
				try {
					Saver.saveImage(image, file, true);
				} catch (FormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			progress.close();
		}
		
		private boolean computeKymo () {
			if (options.seqCamData == null || options.seqKymos == null)
				return false;
			System.out.println("start buildkymographsThread");
			
			if (options.startFrame < 0) 
				options.startFrame = 0;
			if ((options.endFrame >= (int) options.seqCamData.nTotalFrames) || (options.endFrame < 0)) 
				options.endFrame = (int) options.seqCamData.nTotalFrames-1;
			
			int nbframes = options.endFrame - options.startFrame +1;
			ProgressChrono progressBar = new ProgressChrono("Processing started");
			progressBar.initStuff(nbframes);
			threadRunning = true;
			stopFlag = false;

			initArraysToBuildKymographImages();
			if (options.seqKymos.capillaries.capillariesArrayList.size() < 1) {
				System.out.println("Abort (1): nbcapillaries = 0");
				progressBar.close();
				return false;
			}
			
			int vinputSizeX = options.seqCamData.seq.getSizeX();		
			int ipixelcolumn = 0;
			workImage = options.seqCamData.seq.getImage(options.startFrame, 0); 
			Thread thread = null;
			ViewerUpdater visuUpdater = null;
			if (options.updateViewerDuringComputation) {
				roiList = options.seqCamData.seq.getROIs();
				options.seqCamData.seq.removeAllROI();
				visuUpdater = new ViewerUpdater(options.seqCamData, 200);
				thread = new Thread(null, visuUpdater, "+++visuUpdater");
				thread.start();
			}
			
			seqForRegistration.addImage(0, workImage);
			seqForRegistration.addImage(1, workImage);
			int nbcapillaries = options.seqKymos.capillaries.capillariesArrayList.size();
			if (nbcapillaries == 0) {
				System.out.println("Abort(2): nbcapillaries = 0");
				progressBar.close();
				return false;
			}
			
			options.seqCamData.seq.beginUpdate();
			for (int t = options.startFrame ; t <= options.endFrame && !stopFlag; t += options.analyzeStep, ipixelcolumn++ ) {
				progressBar.updatePosition(t);
				if (!getImageAndUpdateViewer (t))
					continue;
				if (options.doRegistration ) 
					adjustImage();
				transferWorkImageToDoubleArrayList ();
				
				for (int iroi=0; iroi < nbcapillaries; iroi++) {
					Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(iroi);
					final int t_out = ipixelcolumn;
					for (int chan = 0; chan < options.seqCamData.seq.getSizeC(); chan++) { 
						double [] tabValues = cap.tabValuesList.get(chan); 
						double [] sourceValues = sourceValuesList.get(chan);
						int cnt = 0;
						for (ArrayList<int[]> mask:cap.masksList) {
							double sum = 0;
							for (int[] m:mask)
								sum += sourceValues[m[0] + m[1]*vinputSizeX];
							if (mask.size() > 1)
								sum = sum/mask.size();
							tabValues[cnt*imagewidth + t_out] = sum; 
							cnt ++;
						}
					}
				}
			}
			options.seqCamData.seq.endUpdate();
			options.seqKymos.seq.removeAllImages();
			options.seqKymos.seq.setVirtual(false); 
			if (options.updateViewerDuringComputation) {
				if (thread != null)
					thread.interrupt();
				if (thread != null && thread.isAlive()) {
					visuUpdater.isInterrupted = true;
					thread.interrupt();
					try {
						thread.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				options.seqCamData.seq.addROIs(roiList, false);
			}

			for (int t=0; t < nbcapillaries; t++) {
				Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(t);
				for (int chan = 0; chan < options.seqCamData.seq.getSizeC(); chan++) {
					double [] tabValues = cap.tabValuesList.get(chan); 
					Object destArray = cap.bufImage.getDataXY(chan);
					Array1DUtil.doubleArrayToSafeArray(tabValues, destArray, cap.bufImage.isSignedDataType());
					cap.bufImage.setDataXY(chan, destArray);
				}
				options.seqKymos.seq.setImage(t, 0, cap.bufImage);
				
				cap.masksList.clear();
				cap.tabValuesList.clear();
				cap.bufImage = null;
			}
			options.seqKymos.seq.setName(options.seqKymos.getDecoratedImageNameFromCapillary(0));
			
			System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
			progressBar.close();
			
			return true;
		}
		
		// -------------------------------------------
		
		private boolean getImageAndUpdateViewer(int t) {	
			workImage = IcyBufferedImageUtil.getCopy(options.seqCamData.getImage(t, 0));
			if (workImage == null) {
				System.out.println("workImage null at "+t);
				return false;
			}
			return true;
		}
		
		private boolean transferWorkImageToDoubleArrayList() {	
			sourceValuesList = new ArrayList<double []>();
			for (int chan = 0; chan < options.seqCamData.seq.getSizeC(); chan++)  {
				double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
				sourceValuesList.add(sourceValues);
			}
			return true;
		}
		
		private void initArraysToBuildKymographImages() {

			int sizex = options.seqCamData.seq.getSizeX();
			int sizey = options.seqCamData.seq.getSizeY();	
			int numC = options.seqCamData.seq.getSizeC();
			if (numC <= 0)
				numC = 3;
			double fimagewidth =  1 + (options.endFrame - options.startFrame )/options.analyzeStep;
			imagewidth = (int) fimagewidth;
			dataType = options.seqCamData.seq.getDataType_();
			if (dataType.toString().equals("undefined"))
				dataType = DataType.UBYTE;

			int nbcapillaries = options.seqKymos.capillaries.capillariesArrayList.size();
			int masksizeMax = 0;
			for (int t=0; t < nbcapillaries; t++) {
				Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(t);
				cap.masksList = new ArrayList<ArrayList<int[]>>();
				initExtractionParametersfromROI(cap.capillaryRoi, cap.masksList, options.diskRadius, sizex, sizey);
				if (cap.masksList.size() > masksizeMax)
					masksizeMax = cap.masksList.size();
			}
			
			for (int t=0; t < nbcapillaries; t++) {
				Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(t);
				cap.bufImage = new IcyBufferedImage(imagewidth, masksizeMax, numC, dataType);
				cap.tabValuesList = new ArrayList <double []>();
				for (int chan = 0; chan < numC; chan++) {
					Object dataArray = cap.bufImage.getDataXY(chan);
					double[] tabValues =  Array1DUtil.arrayToDoubleArray(dataArray, false);
					cap.tabValuesList.add(tabValues);
				}
			} 
		}
		
		private double initExtractionParametersfromROI( ROI2DShape roi, List<ArrayList<int[]>> masks,  double diskRadius, int sizex, int sizey)
		{
			CubicSmoothingSpline xSpline 	= Util.getXsplineFromROI((ROI2DShape) roi);
			CubicSmoothingSpline ySpline 	= Util.getYsplineFromROI((ROI2DShape) roi);
			double length 					= Util.getSplineLength((ROI2DShape) roi);
			double len = 0;
			while (len < length) {
				ArrayList<int[]> mask = new ArrayList<int[]>();
				double x = xSpline.evaluate(len);
				double y = ySpline.evaluate(len);
				double dx = xSpline.derivative(len);
				double dy = ySpline.derivative(len);
				double ux = dy/Math.sqrt(dx*dx + dy*dy);
				double uy = -dx/Math.sqrt(dx*dx + dy*dy);
				double tt = -diskRadius;
				while (tt <= diskRadius) {
					int xx = (int) Math.round(x + tt*ux);
					int yy = (int) Math.round(y + tt*uy);
					if (xx >= 0 && xx < sizex && yy >= 0 && yy < sizey)
						mask.add(new int[]{xx, yy});
					tt += 1d;
				}
				masks.add(mask);			
				len ++;
			}
			return length;
		}
			
		private void adjustImage() {
			seqForRegistration.setImage(1, 0, workImage);
			int referenceChannel = 1;
			int referenceSlice = 0;
			DufourRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
	        boolean rotate = DufourRigidRegistration.correctTemporalRotation2D(seqForRegistration, referenceChannel, referenceSlice);
	        if (rotate) 
	        	DufourRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
	        workImage = seqForRegistration.getLastImage(1);
		}

	}