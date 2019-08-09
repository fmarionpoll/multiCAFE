package plugins.fmp.multicafeSequence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import icy.file.Loader;
import icy.gui.dialog.LoaderDialog;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.collection.array.Array1DUtil;

import plugins.fmp.multicafeTools.ImageOperationsStruct;
import plugins.fmp.multicafeTools.StringSorter;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class SequenceVirtual  {
	public Sequence					seq						= null;
	List <String>  					listFiles 				= new ArrayList<String>();
	private String 					csFileName 				= null;
	private final static String[] 	acceptedTypes 			= {".jpg", ".jpeg", ".bmp", "tiff", "tif"};
	private String					directory 				= null;
	
	public IcyBufferedImage 		refImage 				= null;
	
	public long						analysisStart 			= 0;
	public long 					analysisEnd				= 99999999;
	public int 						analysisStep 			= 1;
	public int 						currentFrame 			= 0;
	public int						nTotalFrames 			= 0;
		
	public boolean					bBufferON 				= false;
	public EnumStatus 				status					= EnumStatus.REGULAR;
		
	public Capillaries 				capillaries 			= new Capillaries();
	public Cages					cages 					= new Cages();
		
	// image cache
	public VImageBufferThread 		bufferThread 			= null;
	public IcyBufferedImage 		cacheTransformedImage 	= null;
	public ImageOperationsStruct 	cacheTransformOp 		= new ImageOperationsStruct();
	public IcyBufferedImage 		cacheThresholdedImage 	= null;
	public ImageOperationsStruct 	cacheThresholdOp 		= new ImageOperationsStruct();
	
	// ----------------------------------------
	
	public SequenceVirtual () {
		seq = new Sequence();
	}
	
	public SequenceVirtual(String name, IcyBufferedImage image) {
		seq = new Sequence (name, image);
	}

	public SequenceVirtual (String csFile) {
		seq = Loader.loadSequence(csFile, 0, true);
		seq.setName(csFile);
	}

	public SequenceVirtual (String [] list, String directory) {
		loadSequenceFromListAndDirectory(list, directory);
		seq.setName(listFiles.get(0));
	}
	
	public SequenceVirtual (List<String> listNames) {
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		if (loadSequenceFromList(listFiles)) {
			Path path = Paths.get(listFiles.get(0));
			String dir = path.getName(path.getNameCount()-2).toString();
			if (dir != null)
				seq.setName(dir);
		}
	}
	
	public static boolean acceptedFileType(String name) {
		/* 
		 * Returns true if 'name' includes one of the accepted types stored in the "accepted" list 
		 */
		if (name==null) return false;
		for (int i=0; i< acceptedTypes.length; i++) {
			if (name.endsWith(acceptedTypes[i]))
				return true;
		}
		return false;
	}	

	public String getDirectory () {
		return directory;
	}
	
	public IcyBufferedImage getImageTransf(int t, int z, int c, TransformOp transformop)  {
		IcyBufferedImage image =  loadVImageAndSubtractReference(t, transformop);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
	public IcyBufferedImage loadVImageAndSubtractReference(int t, TransformOp transformop) {
		IcyBufferedImage ibufImage = loadVImage(t, 0);
		switch (transformop) {
			case REF_PREVIOUS: // subtract image n-analysisStep 
				{
				int t0 = t-analysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = loadVImage(t0, 0);
				ibufImage = subtractImages (ibufImage, ibufImage0);
				}	
				break;
			case REF_T0: // subtract reference image
			case REF:
				if (refImage == null)
					refImage = loadVImage((int) analysisStart, 0);
				ibufImage = subtractImages (ibufImage, refImage);
				break;

			case NONE:
			default:
				break;
		}
		return ibufImage;
	}
		
	public List <String> getListofFiles() {
		return listFiles;
	}

	public String getDecoratedImageName(int t) {
		currentFrame = t;  
		if (EnumStatus.KYMOGRAPH == status)
			return getDecoratedImageNameFromCapillary(t);
		return csFileName + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
	}
	
	public String getDecoratedImageNameFromCapillary(int isel) {
		return capillaries.capillariesArrayList.get(isel).roi.getName() + " ["+(isel+1)+ "/" + seq.getSizeT() + "]";
	}

	public String getFileName(int t) {
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles.get(t);
		else if (status == EnumStatus.AVIFILE)
			csName = csFileName;
		if (csName != null) {
			Path path = Paths.get(csName);
			return path.getName(path.getNameCount()-1).toString();
		}
		return csName;
	}
	
	public boolean isFileStack() {
		return (status == EnumStatus.FILESTACK);
	}
		
	public IcyBufferedImage loadVImage(int t, int z) {
		return seq.getImage(t, z);
	}
	
	public String[] keepOnlyAcceptedNames(String[] rawlist) {
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if ( !acceptedFileType(name) )
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;

		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}

	public IcyBufferedImage subtractImages (IcyBufferedImage image1, IcyBufferedImage image2) {	
		IcyBufferedImage result = new IcyBufferedImage(image1.getSizeX(), image1.getSizeY(), image1.getSizeC(), image1.getDataType_());
		for (int c = 0; c < image1.getSizeC(); c++) {
			double[] img1DoubleArray = Array1DUtil.arrayToDoubleArray(image1.getDataXY(c), image1.isSignedDataType());
			double[] img2DoubleArray = Array1DUtil.arrayToDoubleArray(image2.getDataXY(c), image2.isSignedDataType());
			ArrayMath.subtract(img1DoubleArray, img2DoubleArray, img1DoubleArray);

			double[] dummyzerosArray = Array1DUtil.arrayToDoubleArray(result.getDataXY(c), result.isSignedDataType());
			ArrayMath.max(img1DoubleArray, dummyzerosArray, img1DoubleArray);
			Object destArray = result.getDataXY(c);
			Array1DUtil.doubleArrayToSafeArray(img1DoubleArray, destArray, result.isSignedDataType());
			result.setDataXY(c, destArray);
		}
		result.dataChanged();
		return result;
	}
	
	public String loadInputVirtualStack(String path) {
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    if (selectedFiles[0].isDirectory())
	    	directory = selectedFiles[0].getAbsolutePath();
	    else
	    	directory = selectedFiles[0].getParentFile().getAbsolutePath();
		if (directory == null )
			return null;

		String [] list;
		if (selectedFiles.length == 1) {
			list = (new File(directory)).list();
			if (list ==null)
				return null;
			
			if (!(selectedFiles[0].isDirectory()) && selectedFiles[0].getName().toLowerCase().contains(".avi")) {
				seq = Loader.loadSequence(selectedFiles[0].getAbsolutePath(), 0, true);
				return directory;
			}
		}
		else {
			list = new String[selectedFiles.length];
			  for (int i = 0; i < selectedFiles.length; i++) {
				if (selectedFiles[i].getName().toLowerCase().contains(".avi"))
					continue;
			    list[i] = selectedFiles[i].getAbsolutePath();
			}
		}
		loadSequenceFromListAndDirectory(list, directory);
		return directory;
	}
	
	private void loadSequenceFromListAndDirectory(String [] list, String directory) {
		status = EnumStatus.FAILURE;
		list = keepOnlyAcceptedNames(list);
		list = StringSorter.sortNumerically(list);
		listFiles = new ArrayList<String>(list.length);
		for (int i=0; i<list.length; i++) {
			if (list[i]!=null)
				listFiles.add(directory + File.separator + list[i]);
		}
		nTotalFrames = list.length;
		status = EnumStatus.FILESTACK;	
		loadSequenceFromList(listFiles);
	}
	
	private boolean isLinexLRFileNames(List<String> myListOfFilesNames) {
		boolean flag = false;
		int nfound = 0;
		for (String filename: myListOfFilesNames) {	
			if (filename.contains("R.") || filename.contains("L."))
				nfound++;
			if (nfound >1) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	private List<String> convertLinexLRFileNames(List<String> myListOfFilesNames) {
		List<String> newList = new ArrayList<String>();
		for (String filename: myListOfFilesNames) {
			String newName = null;
			if (filename.contains("R.")) {
				newName = filename.replace("R.", "2.");
			}
			else if (filename.contains("L")) {
				newName = filename.replace("L.", "1.");
			}
			if (newName != null ) {
				newList.add(newName);
				try {
					renameFile(filename, newName);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return newList; 
	}
	
	public void renameFile(String pathSource, String pathDestination) throws IOException {
	    FileUtils.moveFile(
	      FileUtils.getFile(pathSource), 
	      FileUtils.getFile(pathDestination));
	}
	
	private boolean loadSequenceFromList(List<String> myListOfFilesNames) {
		if (isLinexLRFileNames(myListOfFilesNames)) {
			listFiles = convertLinexLRFileNames(myListOfFilesNames);
		}
		boolean flag = false;
		List<Sequence> lseq = Loader.loadSequences(null, listFiles, 0, false, false, false, true);
		if ((flag = (lseq.size() > 0))) {
			seq = new Sequence();
			if (lseq.size() == 1) {
				seq = lseq.get(0);
			}
			else {
				seq = lseq.get(0);	
				int tmax = lseq.get(0).getSizeT();
				int tseq = 0;
				for (int t = 0; t < tmax; t++) {
					for (int i=0; i < lseq.size(); i++) {
						IcyBufferedImage bufImg = lseq.get(i).getImage(t, 0);
						seq.setImage(tseq, 0, bufImg);
						tseq++;
					}
				}
			}
			status = EnumStatus.FILESTACK;	
		}
		return flag;
	}
		
	public String loadVirtualStackAt(String textPath) {
		if (textPath == null) 
			return loadInputVirtualStack(null); 
		
		File filepath = new File(textPath); 
	    if (filepath.isDirectory())
	    	directory = filepath.getAbsolutePath();
	    else
	    	directory = filepath.getParentFile().getAbsolutePath();
		if (directory == null )
			return null;

		String [] list;
		list = (new File(directory)).list();
		if (list ==null)
			return null;
		
		if (!(filepath.isDirectory()) && filepath.getName().toLowerCase().contains(".avi")) {
			seq = Loader.loadSequence(filepath.getAbsolutePath(), 0, true);
		}
		else
			loadSequenceFromListAndDirectory(list, directory);
		return directory;
	}

	public String loadInputVirtualFromNameSavedInRoiXML() {
		if (csFileName != null)
			loadSequenceVirtualFromName(csFileName);
		return csFileName;
	}
	
	public void loadInputVirtualFromName(String name) {
		if (name.toLowerCase().contains(".avi"))
			seq = Loader.loadSequence(name, 0, true);
		else
			loadSequenceVirtualFromName(name);
	}
	
	private void loadSequenceVirtualFromName(String name) {
		File filename = new File (name);
		if (filename.isDirectory())
	    	directory = filename.getAbsolutePath();
	    else {
	    	directory = filename.getParentFile().getAbsolutePath();
	    }
		if (directory == null) {
			status = EnumStatus.FAILURE;
			return;
		}
		String [] list;
		File fdir = new File(directory);
		boolean flag = fdir.isDirectory();
		if (!flag)
			return;
		list = fdir.list();
		if (list != null)
			loadSequenceFromListAndDirectory(list, directory);
	}
	
	public String getFileName() {
		if (seq != null)
			return seq.getFilename();
		return null;	
	}
	
	public void setFileNameAsPathUp1() {
		String directory = seq.getFilename();
		Path path = Paths.get(directory);
		String dirupup = path.getName(path.getNameCount()-2).toString();
		setFileName(dirupup);
		seq.setName(dirupup);
	}
	
	public void setFileName(String name) {
		csFileName = name;		
	}
	
	public void storeAnalysisParametersToCages() {
		cages.detect.analysisEnd = analysisEnd;
		cages.detect.analysisStart = analysisStart;
		cages.detect.analysisStep = analysisStep;
	}
	
	public void updateCapillaries(int size) {
		if (capillaries == null || capillaries.capillariesArrayList.size() != size) {
			if (capillaries == null)
				capillaries = new Capillaries();
			capillaries.createCapillariesFromROIS(this);
			storeAnalysisParametersToCapillaries();
		}
		return;
	}
	
	public void storeAnalysisParametersToCapillaries () {
		capillaries.analysisStart = analysisStart;
		capillaries.analysisEnd = analysisEnd;
		capillaries.analysisStep = analysisStep;
	}
	
	public boolean xmlReadCapillaryTrackDefault() {
		boolean found = xmlReadCapillaryTrack(getDirectory()+"\\capillarytrack.xml");
		if (!found)
			found = capillaries.xmlReadROIsAndData(this);
		return found;
	}
	
	public boolean xmlReadCapillaryTrack(String filename) {
		boolean flag = capillaries.xmlReadROIsAndData(filename, this);
		if (flag) {
			analysisStart = capillaries.analysisStart;
			analysisEnd = capillaries.analysisEnd;
			analysisStep = capillaries.analysisStep;
		}
		return flag;
	}
		
	public boolean xmlReadDrosoTrackDefault() {
		return cages.xmlReadCagesFromFileNoQuestion(getDirectory() + "\\drosotrack.xml", this);
	}
	
	public boolean xmlReadDrosoTrack(String filename) {
		boolean flag = cages.xmlReadCagesFromFileNoQuestion(filename, this);
		if (flag) {
			analysisStart = cages.detect.analysisStart;
			analysisEnd = cages.detect.analysisEnd;
			analysisStep = cages.detect.analysisStep;
		}
		return flag;
	}
	
	public boolean xmlWriteDrosoTrackDefault() {
		return cages.xmlWriteCagesToFile("drosotrack.xml", getDirectory());
	}
	
	public FileTime getImageModifiedTime (int t) {
		String name = getFileName(t);
		Path path = Paths.get(name);
		FileTime fileTime;
		try { fileTime = Files.getLastModifiedTime(path); }
		catch (IOException e) {
			System.err.println("Cannot get the last modified time - "+e+ "image "+ t+ " -- file "+ name);
			return null;
		}
		return fileTime;
	}

	public void removeAllROISatT(int t) {
		final List<ROI> allROIs = seq.getROIs();
        for (ROI roi : allROIs) {
        	if (roi instanceof ROI2D && ((ROI2D) roi).getT() == t)
        		seq.removeROI(roi, false);
        }    
	}
	
	public void removeRoisContainingString(int t, String string) {
		for (ROI roi: seq.getROIs()) {
			if (roi instanceof ROI2D 
			&& ((ROI2D) roi).getT() == t 
			&& roi.getName().contains(string))
				seq.removeROI(roi);
		}
	}
	
	// --------------------------
	
	public void vImageBufferThread_START (int numberOfImageForBuffer) {
		vImageBufferThread_STOP();
		bufferThread = new VImageBufferThread(this, numberOfImageForBuffer);
		bufferThread.setName("Buffer Thread");
		bufferThread.setPriority(Thread.NORM_PRIORITY);
		bufferThread.start();
	}
	
	public void vImageBufferThread_STOP() {
		if (bufferThread != null) {
			bufferThread.interrupt();
			try {
				bufferThread.join();
			}
			catch (final InterruptedException e1) { e1.printStackTrace(); }
		}
	}

	public void cleanUpBufferAndRestart() {
		if (bufferThread == null)
			return;
		int depth = bufferThread.getFenetre();
		vImageBufferThread_STOP();
		vImageBufferThread_START(depth);
	}
	
	public class VImageBufferThread extends Thread {
		/**
		 * pre-fetch files / companion to SequenceVirtual
		 */
		private int fenetre = 200; // 100;
		private int span = fenetre/2;

		public VImageBufferThread() {
			bBufferON = true;
		}

		public VImageBufferThread(SequenceVirtual vseq, int depth) {
			fenetre = depth;
			span = fenetre/2 * analysisStep;
			bBufferON = true;
		}
		
		public void setFenetre (int depth) {
			fenetre = depth;
			span = fenetre/2 * analysisStep;
		}

		public int getFenetre () {
			return fenetre;
		}
		
		public int getStep() {
			return analysisStep;
		}

		@Override
		public void run() {
			boolean [] done = new boolean[seq.getSizeT()];
			try {
				while (!isInterrupted()) {
					ThreadUtil.sleep(100);

					int frameStart 	= currentFrame - span;
					int frameEnd 	= currentFrame + span;
					if (frameStart < 0) 
						frameStart = 0;
					if (frameEnd > nTotalFrames) 
						frameEnd = nTotalFrames;
				
					for (int t = frameStart; t < frameEnd ; t+= analysisStep) {
						if (!done[t]) {
							seq.getImage(t, 0).loadData();
							done[t] = true;
						}
						if (isInterrupted())
							return;
					}
				}			
			}
			catch (final Exception e) 
			{ e.printStackTrace(); }
		}
	}
}