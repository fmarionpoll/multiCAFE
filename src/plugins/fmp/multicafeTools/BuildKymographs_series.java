package plugins.fmp.multicafeTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;



public class BuildKymographs_series extends SwingWorker<Integer, Integer>  {
		public BuildKymographs_Options 	options 			= new BuildKymographs_Options();
		public boolean 					stopFlag 			= false;
		public boolean 					threadRunning 		= false;
		public boolean					buildBackground		= true;
		
		
		private IcyBufferedImage 		workImage 			= null; 
		private Sequence 				seqForRegistration	= null;
		private DataType 				dataType 			= DataType.INT;
		private int 					imagewidth =1;
		private ArrayList<double []> 	sourceValuesList 	= null;
		private List<ROI> 				roiList 			= null;
		
		@Override
		protected Integer doInBackground() throws Exception {
	        threadRunning = true;
	        int nbiterations = 0;
			ExperimentList expList = options.expList;
			int nbexp = expList.index1 - expList.index0 +1;
			ProgressChrono progress = new ProgressChrono("Build kymographs");
			progress.initChrono(nbexp);
			progress.setMessageFirstPart("Analyze series ");
			
			for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
				if (stopFlag)
					break;
				Experiment exp = expList.experimentList.get(index);
				System.out.println(exp.experimentFileName);
				progress.updatePosition(index-expList.index0+1);
				
				exp.loadExperimentDataForBuildKymos();
				Build_series.initViewerCamData(exp);

				exp.step = options.analyzeStep;
				exp.analysisStart = options.startFrame;
				exp.analysisEnd = options.endFrame;
				if (computeKymo(exp) && !stopFlag) {
					saveComputation(exp);
				}
				Build_series.closeViewer(exp);
			}
			progress.close();
			threadRunning = false;
			return nbiterations;
		}
		
		@Override
		protected void done() {
			int statusMsg = 0;
			try {
				statusMsg = get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} 
			System.out.println("iterations done: "+statusMsg);
			if (!threadRunning || stopFlag) {
				firePropertyChange("thread_ended", null, statusMsg);
			}
			else {
				firePropertyChange("thread_done", null, statusMsg);
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
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
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
			
			exp.xmlSaveExperiment();
			progress.close();
		}
		
		private boolean computeKymo (Experiment exp) {
			SequenceCamData seqCamData = exp.seqCamData;
			SequenceKymos seqKymos = exp.seqKymos;
			if (seqCamData == null || seqKymos == null)
				return false;
			System.out.println("start buildkymographsThread");
			
			if (options.startFrame < 0) 
				options.startFrame = 0;
			if ((options.endFrame >= (int) seqCamData.nTotalFrames) || (options.endFrame < 0)) 
				options.endFrame = (int) seqCamData.nTotalFrames-1;
			
			int nbframes = options.endFrame - options.startFrame +1;
			ProgressChrono progressBar = new ProgressChrono("Processing started");
			progressBar.initChrono(nbframes);
			threadRunning = true;
			stopFlag = false;

			initArraysToBuildKymographImages(exp);
			if (exp.capillaries.capillariesArrayList.size() < 1) {
				System.out.println("Abort (1): nbcapillaries = 0");
				progressBar.close();
				return false;
			}
			
			int vinputSizeX = seqCamData.seq.getSizeX();		
			int ipixelcolumn = 0;
			workImage = seqCamData.seq.getImage(options.startFrame, 0); 
			Thread thread = null;
			ViewerUpdater visuUpdater = null;
			if (options.updateViewerDuringComputation) {
				roiList = seqCamData.seq.getROIs();
				seqCamData.seq.removeAllROI();
				visuUpdater = new ViewerUpdater(seqCamData, 200);
				thread = new Thread(null, visuUpdater, "+++visuUpdater");
				thread.start();
			}
			
			seqForRegistration	= new Sequence();
			seqForRegistration.addImage(0, workImage);
			seqForRegistration.addImage(1, workImage);
			int nbcapillaries = exp.capillaries.capillariesArrayList.size();
			if (nbcapillaries == 0) {
				System.out.println("Abort(2): nbcapillaries = 0");
				progressBar.close();
				return false;
			}
			
			seqCamData.seq.beginUpdate();
			for (int t = options.startFrame ; t <= options.endFrame; t += options.analyzeStep, ipixelcolumn++ ) {
				if (stopFlag)
					break;
				progressBar.updatePosition(t);
				workImage = seqCamData.getImageCopy(t);
				if (workImage == null)
					continue;
				if (options.doRegistration ) 
					adjustImage();
				transferWorkImageToDoubleArrayList ();
				
				for (int iroi=0; iroi < nbcapillaries; iroi++) {
					Capillary cap = exp.capillaries.capillariesArrayList.get(iroi);
					final int t_out = ipixelcolumn;
					for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) { 
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
			seqCamData.seq.endUpdate();
			seqKymos.seq.removeAllImages();
			seqKymos.seq.setVirtual(false); 
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
				seqCamData.seq.addROIs(roiList, false);
			}

			for (int t=0; t < nbcapillaries; t++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) {
					double [] tabValues = cap.tabValuesList.get(chan); 
					Object destArray = cap.bufImage.getDataXY(chan);
					Array1DUtil.doubleArrayToSafeArray(tabValues, destArray, cap.bufImage.isSignedDataType());
					cap.bufImage.setDataXY(chan, destArray);
				}
				seqKymos.seq.setImage(t, 0, cap.bufImage);
				
				cap.masksList.clear();
				cap.tabValuesList.clear();
				cap.bufImage = null;
			}
			seqKymos.seq.setName(exp.getDecoratedImageNameFromCapillary(0));
			
			System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
			progressBar.close();
			
			return true;
		}
		
		// -------------------------------------------
		
		private boolean transferWorkImageToDoubleArrayList() {	
			sourceValuesList = new ArrayList<double []>();
			for (int chan = 0; chan < workImage.getSizeC(); chan++)  {
				double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
				sourceValuesList.add(sourceValues);
			}
			return true;
		}
		
		private void initArraysToBuildKymographImages(Experiment exp) {
			SequenceCamData seqCamData = exp.seqCamData;
			int sizex = seqCamData.seq.getSizeX();
			int sizey = seqCamData.seq.getSizeY();	
			int numC = seqCamData.seq.getSizeC();
			if (numC <= 0)
				numC = 3;
			double fimagewidth =  1 + (options.endFrame - options.startFrame )/options.analyzeStep;
			imagewidth = (int) fimagewidth;
			dataType = seqCamData.seq.getDataType_();
			if (dataType.toString().equals("undefined"))
				dataType = DataType.UBYTE;

			int nbcapillaries = exp.capillaries.capillariesArrayList.size();
			int masksizeMax = 0;
			for (int t=0; t < nbcapillaries; t++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				cap.masksList = new ArrayList<ArrayList<int[]>>();
				initExtractionParametersfromROI(cap.capillaryRoi, cap.masksList, options.diskRadius, sizex, sizey);
				if (cap.masksList.size() > masksizeMax)
					masksizeMax = cap.masksList.size();
			}
			
			for (int t=0; t < nbcapillaries; t++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
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