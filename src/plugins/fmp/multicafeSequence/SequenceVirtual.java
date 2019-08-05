package plugins.fmp.multicafeSequence;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import icy.canvas.Canvas2D;
import icy.file.Loader;
import icy.gui.dialog.LoaderDialog;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.math.ArrayMath;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;

import plugins.fmp.multicafeTools.ImageOperationsStruct;
import plugins.fmp.multicafeTools.StringSorter;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class SequenceVirtual  
{
	public Sequence					seq						= null;
	private List <String>  			listFiles 				= new ArrayList<String>();
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
	
	public String [] 				seriesname 				= null;
	public int [][] 				data_raw 				= null;
	public double [][] 				data_filtered 			= null;
	
	// image cache
	public IcyBufferedImage 		cacheTransformedImage 	= null;
	public ImageOperationsStruct 	cacheTransformOp 		= new ImageOperationsStruct();
	public IcyBufferedImage 		cacheThresholdedImage 	= null;
	public ImageOperationsStruct 	cacheThresholdOp 		= new ImageOperationsStruct();
	
	// ----------------------------------------
	public SequenceVirtual () 
	{
		seq = new Sequence();
	}
	
	public SequenceVirtual(String name, IcyBufferedImage image) {
		seq = new Sequence (name, image);
	}

	public SequenceVirtual (String csFile)
	{
		seq = Loader.loadSequence(csFile, 0, true);
		seq.setName(csFile);
	}

	public SequenceVirtual (String [] list, String directory)
	{
		loadSequenceFromListAndDirectory(list, directory);
		seq.setName(listFiles.get(0));
	}
	
	public SequenceVirtual (List<String> listNames)
	{
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		loadSequenceFromList(listFiles);
		seq.setName(listFiles.get(0));
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

	public void displayRelativeFrame( int nbFrame )
	{
		int currentTime = getT()+ nbFrame ;
		if (currentTime < 0)
			currentTime = 0;
		if (currentTime > nTotalFrames-1)
			currentTime = (int) (nTotalFrames -1);

		final Viewer v = Icy.getMainInterface().getFirstViewer(seq);
		if (v != null) 
			v.setPositionT(currentTime);
		displayImageAt(currentTime);
	}

	public void displayImageAt(int t)
	{
		currentFrame = t;
		if (seq.getImage(t, 0) == null)
		{
			final boolean wasEmpty = (seq.getNumImage() == 0);
			setCurrentVImage (t);
			if (wasEmpty)
			{
				for (Viewer viewer : seq.getViewers())
				{
					if (viewer.getCanvas() instanceof Canvas2D)
						((Canvas2D) viewer.getCanvas()).fitCanvasToImage();
				}
			}
		}
	}

	public String getDirectory () {
		return directory;
	}
	
	public IcyBufferedImage getImageTransf(int t, int z, int c, TransformOp transformop) 
	{
		IcyBufferedImage image =  loadVImageAndSubtractReference(t, transformop);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
	public IcyBufferedImage loadVImageAndSubtractReference(int t, TransformOp transformop)
	{
		IcyBufferedImage ibufImage = loadVImage(t);
		switch (transformop) {
			case REF_PREVIOUS: // subtract image n-analysisStep
			{
				int t0 = t-analysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = loadVImage(t0);
				ibufImage = subtractImages (ibufImage, ibufImage0);
			}	
				break;
			case REF_T0: // subtract reference image
			case REF:
				if (refImage == null)
					refImage = loadVImage((int) analysisStart);
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

	public int getT() {
		return currentFrame;
	}

	public double getVData(int t, int z, int c, int y, int x)
	{
		final IcyBufferedImage img = loadVImage(t);
		if (img != null)
			return img.getData(x, y, c);
		return 0d;
	}

	public String getDecoratedImageName(int t)
	{
		return  "["+t+ "/" + nTotalFrames + " V] : " + getFileName(t);
	}

	public String getFileName(int t) {
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles.get(t);
		else if (status == EnumStatus.AVIFILE)
			csName = csFileName;
		return csName;
	}
	
	public boolean isFileStack() {
		return (status == EnumStatus.FILESTACK);
	}
		
	public IcyBufferedImage loadVImage(int t, int z)
	{
		return seq.getImage(t, z);
	}
	
	public IcyBufferedImage loadVImage(int t)
	{
		return seq.getImage(t, 0);
	}
	
	public boolean setCurrentVImage(int t)
	{
		BufferedImage bimage = loadVImage(t);
		if (bimage == null)
			return false;

		seq.setImage(t, 0, bimage);
		setVImageName(t);		
		currentFrame = t;
		return true;
	}

	public String[] keepOnlyAcceptedNames(String[] rawlist) {
		// -----------------------------------------------
		// subroutines borrowed from FolderOpener
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
			Array1DUtil.doubleArrayToArray(img1DoubleArray, result.getDataXY(c));
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
		else
		{
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
	
	private void loadSequenceFromList(List<String> listFiles) {
		List<Sequence> lseq = Loader.loadSequences(null, listFiles, 0, false, false, false, true);
		status = EnumStatus.FILESTACK;
		seq = lseq.get(0);
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
			return directory;
		}
		
		loadSequenceFromListAndDirectory(list, directory);
		return directory;
	}

	public String loadInputVirtualFromNameSavedInRoiXML()
	{
		if (csFileName != null)
			loadSequenceVirtualFromName(csFileName);
		return csFileName;
	}
	
	public void loadInputVirtualFromName(String name)
	{
		if (name.toLowerCase().contains(".avi"))
			seq = Loader.loadSequence(name, 0, true);
		else
			loadSequenceVirtualFromName(name);
	}
	
	private void loadSequenceVirtualFromName(String name) 
	{
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
	
	private void setVImageName(int t) {
		if (status == EnumStatus.FILESTACK)
			seq.setName(getDecoratedImageName(t));
	}

	public String getFileName() {
		if (seq != null)
			return seq.getFilename();
		return null;
//		String fileName;
//		if (status == EnumStatus.FILESTACK) 
//			fileName = listFiles[0];
//		else //  if ((status == EnumStatus.AVIFILE))
//			fileName = csFileName;
//		return fileName;		
	}
	
	public void setFileName(String name) {
		csFileName = name;		
	}
	
	public void storeAnalysisParametersToCages() {
		cages.detect.analysisEnd = analysisEnd;
		cages.detect.analysisStart = analysisStart;
		cages.detect.analysisStep = analysisStep;
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
//		LocalDateTime loc = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
		return fileTime;
	}

	public void removeAllROISatT(int t) {
		final List<ROI> allROIs = seq.getROIs();
        for (ROI roi : allROIs) {
        	if (roi instanceof ROI2D && ((ROI2D) roi).getT() == t)
        		seq.removeROI(roi, false);
        }    
	}
}