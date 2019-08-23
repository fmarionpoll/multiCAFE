 package plugins.fmp.multicafeSequence;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

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
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.ImageOperationsStruct;
import plugins.fmp.multicafeTools.StringSorter;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class SequenceCamData  {
	public Sequence					seq						= null;
	public IcyBufferedImage 		refImage 				= null;
	
	public long						analysisStart 			= 0;
	public long 					analysisEnd				= 99999999;
	public int 						analysisStep 			= 1;
	public int 						currentFrame 			= 0;
	public int						nTotalFrames 			= 0;

	public EnumStatus 				status					= EnumStatus.REGULAR;		
	public Cages					cages 					= new Cages();
		
	// image cache
	public PreFetchForwardThread 	bufferThread 			= null;
	public IcyBufferedImage 		cacheTransformedImage 	= null;
	public ImageOperationsStruct 	cacheTransformOp 		= new ImageOperationsStruct();
	public IcyBufferedImage 		cacheThresholdedImage 	= null;
	public ImageOperationsStruct 	cacheThresholdOp 		= new ImageOperationsStruct();
	
	public List <String>  			listFiles 				= new ArrayList<String>();
	protected String 				csFileName 				= null;
	protected String				directory 				= null;
	
	private final static String[] 	acceptedTypes 			= {".jpg", ".jpeg", ".bmp", "tiff", "tif"};
	protected ArrayList <TaggedImage> imgPrefetchArray 		= null;
	

	
	// ----------------------------------------
	
	public SequenceCamData () {
		seq = new Sequence();
	}
	
	public SequenceCamData(String name, IcyBufferedImage image) {
		seq = new Sequence (name, image);
	}

	public SequenceCamData (String csFile) {
		seq = Loader.loadSequence(csFile, 0, true);
		seq.setName(csFile);
	}

	public SequenceCamData (String [] list, String directory) {
		loadSequenceFromListAndDirectory(list, directory);
		seq.setName(listFiles.get(0));
	}
	
	public SequenceCamData (List<String> listNames) {
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		if (loadSequenceFromList(listFiles, true)) {
			Path path = Paths.get(listFiles.get(0));
			String dir = path.getName(path.getNameCount()-2).toString();
			if (dir != null)
				seq.setName(dir);
		}
	}
	
	public SequenceCamData (List<String> listNames, boolean testLR) {
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		if (loadSequenceFromList(listFiles, testLR)) {
			Path path = Paths.get(listFiles.get(0));
			String dir = path.getName(path.getNameCount()-2).toString();
			if (dir != null)
				seq.setName(dir);
		}
	}
	
	// -----------------------
	
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
	
	public static int acceptedFileType2(String name) {
		/* 
		 * Returns accepted type (0-n) or -1 if not found 
		 */
		int ifound = -1;
		if (name==null) 
			return ifound;
		for (int i=0; i< acceptedTypes.length; i++) {
			if (name.endsWith(acceptedTypes[i]))
				return i;
		}
		return ifound;
	}

	public String getDirectory () {
		return directory;
	}
	
	public IcyBufferedImage getImageTransf(int t, int z, int c, TransformOp transformop)  {
		IcyBufferedImage image =  getImageAndSubtractReference(t, transformop);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
	public IcyBufferedImage getImage(int t, int z) {
		return seq.getImage(t, z);
	}
	
	public IcyBufferedImage getImageAndSubtractReference(int t, TransformOp transformop) {
		IcyBufferedImage ibufImage = getImage(t, 0);
		switch (transformop) {
			case REF_PREVIOUS: // subtract image n-analysisStep 
				{
				int t0 = t-analysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = getImage(t0, 0);
				ibufImage = subtractImages (ibufImage, ibufImage0);
				}	
				break;
			case REF_T0: // subtract reference image
			case REF:
				if (refImage == null)
					refImage = getImage((int) analysisStart, 0);
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
		return csFileName + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
	}
	
	public String getFileName(int t) {
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles.get(t);
		else if (status == EnumStatus.AVIFILE)
			csName = csFileName;
		return csName;
	}
	
	public String getFileNameNoPath(int t) {
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
	
	public List<String> keepOnlyAcceptedNamesFromList(List<String> rawlist, int filetype) {
		int count = rawlist.size();
		List<String> outList = new ArrayList<String> (count);
		for (String name: rawlist) {
			int itype = acceptedFileType2(name);
			if ( itype >= 0) {
				if (filetype < 0)
					filetype = itype;
				if (itype == filetype)
					outList.add(name);
			}
		}
		return outList;
	}
		
	public String[] keepOnlyAcceptedNamesFromArray(String[] rawlist) {
		int count = rawlist.length;
		for (int i=0; i< rawlist.length; i++) {
			if ( !acceptedFileType(rawlist[i]) ) {
				rawlist[i] = null;
				count --;
			}
		}
		if (count==0) 
			return null;

		String[] list = rawlist;
		if (count < rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i] != null)
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
	
	// --------------------------
	
	public String loadSequenceFromDialog(String path) {
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    directory = selectedFiles[0].isDirectory() ? selectedFiles[0].getAbsolutePath() : selectedFiles[0].getParentFile().getAbsolutePath();
		if (directory != null ) {
			if (selectedFiles.length == 1) {
				loadSequence(selectedFiles[0].getAbsolutePath());
			}
			else {
				String [] list = new String [selectedFiles.length];
				for (int i = 0; i < selectedFiles.length; i++) {
					if (!selectedFiles[i].getName().toLowerCase().contains(".avi"))
						list[i] = selectedFiles[i].getAbsolutePath();
				}
				loadSequenceFromListAndDirectory(list, directory);
			}
		}
		return directory;
	}
	
	public String loadSequence(String textPath) {
		if (textPath == null) 
			return loadSequenceFromDialog(null); 
		File filepath = new File(textPath); 
		
	    directory = filepath.isDirectory()? filepath.getAbsolutePath(): filepath.getParentFile().getAbsolutePath();
		if (directory != null ) {
			List<String> list = new ArrayList<String> ();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directory))) {
				for (Path entry: stream) {
					list.add(entry.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (list.size() == 0)
				return null;
			else {
				if (!(filepath.isDirectory()) && filepath.getName().toLowerCase().contains(".avi"))
					seq = Loader.loadSequence(filepath.getAbsolutePath(), 0, true);
				else {
					loadSequenceFromList2(list);
				}
			}
		}
		return directory;
	}
	
	private void loadSequenceFromList2(List<String> list) {
		status = EnumStatus.FAILURE;
		list = keepOnlyAcceptedNamesFromList(list, 0);	
		loadSequenceFromList(list, true);
	}
	
	private void loadSequenceFromListAndDirectory(String [] list, String directory) {
		status = EnumStatus.FAILURE;
		list = keepOnlyAcceptedNamesFromArray(list);
		list = StringSorter.sortNumerically(list);
		listFiles = new ArrayList<String>(list.length);
		for (int i=0; i<list.length; i++) {
			if (list[i]!=null)
				listFiles.add(directory + File.separator + list[i]);
		}
		nTotalFrames = list.length;	
		loadSequenceFromList(listFiles, true);
	}
	
	protected boolean loadSequenceFromList(List<String> myListOfFilesNames, boolean testFilenameDecoratedwithLR) {
		if (testFilenameDecoratedwithLR && isLinexLRFileNames(myListOfFilesNames)) {
			listFiles = convertLinexLRFileNames(myListOfFilesNames);
		} else {
			listFiles = myListOfFilesNames;
		}
		boolean flag = false;
		List<Sequence> lseq = Loader.loadSequences(null, listFiles, 0, false, false, false, true);
		if ((flag = (lseq.size() > 0))) {
			seq = lseq.get(0);
			if (lseq.size() > 1) {
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
			nTotalFrames = listFiles.size();	
			status = EnumStatus.FILESTACK;	
			initAnalysisParameters();
		}
		return flag;
	}
	
	private void initAnalysisParameters() {
		analysisStart = 0;
		analysisEnd = seq.getSizeT()-1;
		analysisStep = 1;
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
	
	protected List<String> convertLinexLRFileNames(List<String> myListOfFilesNames) {
		List<String> newList = new ArrayList<String>();
		for (String oldName: myListOfFilesNames) {
			String newName = null;
			if (oldName.contains("R.")) {
				newName = oldName.replace("R.", "2.");
				newList.add(newName);
			}
			else if (oldName.contains("L")) {
				newName = oldName.replace("L.", "1.");
				newList.add(newName);
			}
			else
				newList.add(oldName);

			File oldfile = new File(oldName);
			if (newName != null && oldfile.exists()) {
				try {
					renameFile(oldName, newName);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return newList; 
	}
	
	protected String convertLinexLRFileName(String oldName) {
			String newName = null;
			if (oldName.contains("R.")) 
				newName = oldName.replace("R.", "2.");
			else if (oldName.contains("L")) 
				newName = oldName.replace("L.", "1.");
			else
				newName = oldName;
		return newName; 
	}
	
	// --------------------------

	public void renameFile(String pathSource, String pathDestination) throws IOException {
	    FileUtils.moveFile(
	      FileUtils.getFile(pathSource), 
	      FileUtils.getFile(pathDestination));
	}
	
	public String getFileName() {
		if (seq != null)
			return seq.getFilename();
		return null;	
	}
	
	public void setFileName(String name) {
		csFileName = name;		
	}
	
	public void setParentDirectoryAsFileName() {
		String directory = seq.getFilename();
		Path path = Paths.get(directory);
		String dirupup = path.getName(path.getNameCount()-2).toString();
		setFileName(dirupup);
		seq.setName(dirupup);
	}
	
	// --------------------------
	
	public void storeAnalysisParametersToCages() {
		cages.detect.analysisEnd = analysisEnd;
		cages.detect.analysisStart = analysisStart;
		cages.detect.analysisStep = analysisStep;
	}
	
	public boolean xmlReadDrosoTrackDefault() {
		return cages.xmlReadCagesFromFileNoQuestion(getDirectory() + "\\drosotrack.xml", this);
	}
	
	public boolean xmlReadDrosoTrack(String filename) {
		boolean flag = cages.xmlReadCagesFromFileNoQuestion(filename, this);
		return flag;
	}
	
	public boolean xmlWriteDrosoTrackDefault() {
		return cages.xmlWriteCagesToFile("drosotrack.xml", getDirectory());
	}
	
	// ---------------------------
	
	public boolean xmlReadROIs(String csFileName) {
		
		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				//xmlReadCapillaryParameters(doc);
				List<ROI> listOfROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
				seq.addROIs(listOfROIs, false);
				return true;
			}
		}
		return false;
	}
	
	// --------------------------

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

	public void removeROIsAtT(int t) {
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
	
	public void prefetchForwardThread_START (int numberOfImageForBuffer) {
		prefetchForwardThread_STOP();
		if (numberOfImageForBuffer <= 0) 
			return;
		bufferThread = new PreFetchForwardThread(numberOfImageForBuffer);
		bufferThread.setName("+++Prefetch_Data");
		bufferThread.setPriority(Thread.NORM_PRIORITY);
		bufferThread.start();
	}
	
	public void prefetchForwardThread_RESTART () {
		int depth = bufferThread.getFenetre();
		prefetchForwardThread_STOP();
		
		bufferThread = new PreFetchForwardThread(depth);
		bufferThread.setName("+++Prefetch_Data");
		bufferThread.setPriority(Thread.NORM_PRIORITY);
		bufferThread.start();
	}
	
	public void prefetchForwardThread_STOP() {
		if (bufferThread != null && bufferThread.isAlive()) {
			bufferThread.interrupt();
			try {
				bufferThread.join();
			}
			catch (final InterruptedException e1) { e1.printStackTrace(); }
		}
		bufferThread = null;
	}
	
	public IcyBufferedImage getImageFromForwardBuffer(int t) {
		IcyBufferedImage img = null;
		for (int j=0; j< 5; j++) {
			for (int i = 0; i < imgPrefetchArray.size(); i++) {
				if (imgPrefetchArray.get(i).t == t) {
					if (imgPrefetchArray.get(i).img == null)
						break;
					img = IcyBufferedImage.createFrom(imgPrefetchArray.get(i).img);
					bufferThread.tcurrent = t;
					break;
				}
			}
			if (img != null)
				break;
		}
		
		if (img == null) {
			File f = new File(listFiles.get(t));
			try {
				img = IcyBufferedImage.createFrom(ImageIO.read(f));
			} catch (IOException e) {
				e.printStackTrace();
			}
			bufferThread.tcurrent = t;
			currentFrame = t;
		}
		return img;
	}
	
	public class PreFetchForwardThread extends Thread {
		/**
		 * pre-fetch files / companion to SequenceCamData
		 */
		private int fenetre = 10; //200; // 100;
		int tnext = (int) (analysisStart - analysisStep);
		volatile int tcurrent = -1;
		

		public PreFetchForwardThread() {
		}

		public PreFetchForwardThread(int depth) {
			fenetre = depth;			
			imgPrefetchArray = new ArrayList <TaggedImage> (depth);
			for (int i=0; i < depth; i++)
				imgPrefetchArray.add(new TaggedImage());
			tnext = (int) (analysisStart);
		}
		
		public int getFenetre () {
			return fenetre;
		}
		
		@Override
		public void run() {
			try {
				while (!isInterrupted()) {
					ThreadUtil.sleep(100);
					for (int i = 0; i < imgPrefetchArray.size(); i++) {
						int t = imgPrefetchArray.get(i).t;
						if (t < 0) {
							t = tnext;
							if (t < listFiles.size()) {
								TaggedImage timg = imgPrefetchArray.get(i);
								File f = new File(listFiles.get(t));
								try {
									timg.img = ImageIO.read(f);
								} catch (IOException e) {
									e.printStackTrace();
								}
								timg.t = t;
								tnext = t + analysisStep;
							}
						}
						else if (t < (tcurrent-analysisStep)) {
							imgPrefetchArray.get(i).t = -1;
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