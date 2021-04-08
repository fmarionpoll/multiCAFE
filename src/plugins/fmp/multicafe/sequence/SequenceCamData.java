 package plugins.fmp.multicafe.sequence;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.gui.dialog.LoaderDialog;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ImageOperationsStruct;
import plugins.fmp.multicafe.tools.ROI2DUtilities;
import plugins.fmp.multicafe.tools.StringSorter;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DPolygon;




public class SequenceCamData 
{
	public Sequence					seq						= null;
	public IcyBufferedImage 		refImage 				= null;
	
	public long						seqAnalysisStart 		= 0;
	public long 					seqAnalysisEnd			= 99999999;
	public int 						seqAnalysisStep 		= 1;
	
	public int 						currentFrame 			= 0;
	public int						nTotalFrames 			= 0;

	public EnumStatus 				status					= EnumStatus.REGULAR;		
	
	public IcyBufferedImage 		cacheTransformedImage 	= null;
	public ImageOperationsStruct 	cacheTransformOp 		= new ImageOperationsStruct();
	public IcyBufferedImage 		cacheThresholdedImage 	= null;
	public ImageOperationsStruct 	cacheThresholdOp 		= new ImageOperationsStruct();
	volatile public List <String>	listFiles 				= new ArrayList<String>();
	protected String 				csCamFileName 			= null;
	protected String				seqCamDataDirectory 	= null;
	
	private final static String[] 	acceptedTypes 			= {".jpg", ".jpeg", ".bmp", "tiff", "tif", "avi"};  
	// TODO: accept mpeg mv4?
	
	// ----------------------------------------
	
	public SequenceCamData () 
	{
		seq = new Sequence();
	}
	
	public SequenceCamData(String name, IcyBufferedImage image) 
	{
		seq = new Sequence (name, image);
	}

	public SequenceCamData (String csFile) 
	{
		seq = Loader.loadSequence(csFile, 0, true);
		seq.setName(csFile);
	}

	public SequenceCamData (String [] list, String directory) 
	{
		loadSequenceOfImagesFromListAndDirectory(list, directory);
		seq.setName(listFiles.get(0));
	}
	
	public SequenceCamData (List<String> listNames) {
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		seq = loadSequenceOfImagesFromList(listFiles, true) ; 
		if (seq != null) 
		{
			Path path = Paths.get(listFiles.get(0));
			int iback = 2;
			String dir = path.getName(path.getNameCount()-iback).toString();
			if (dir != null) 
			{
				if (dir.equals("grabs"))
					dir = path.getName(path.getNameCount()-3).toString();
				seq.setName(dir);
			}
		}
	}
	
	public SequenceCamData (List<String> listNames, boolean testLR) 
	{
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		seq = loadSequenceOfImagesFromList(listFiles, testLR);
		if (seq != null) 
		{
			Path path = Paths.get(listFiles.get(0));
			String dir = path.getName(path.getNameCount()-2).toString();
			if (dir != null) {
				if (dir.equals("grabs"))
					dir = path.getName(path.getNameCount()-3).toString();
				seq.setName(dir);
			}
		}
	}
	
	// -----------------------
	
	public static boolean isAcceptedFileType(String name) 
	{
		if (name==null) 
			return false;
		for (int i=0; i< acceptedTypes.length; i++) 
		{
			if (name.endsWith(acceptedTypes[i]))
				return true;
		}
		return false;
	}	
	
	public static int getCodeIfAcceptedFileType(String name) 
	{
		/* 
		 * Returns accepted type (0-n) or -1 if not found 
		 */
		int ifound = -1;
		if (name==null) 
			return ifound;
		for (int i=0; i< acceptedTypes.length; i++) 
		{
			if (name.endsWith(acceptedTypes[i]))
				return i;
		}
		return ifound;
	}

	public String getSeqDataDirectory () 
	{
		return seqCamDataDirectory;
	}

	public IcyBufferedImage getImage(int t, int z) 
	{
		return seq.getImage(t, z);
	}
	
	public IcyBufferedImage subtractReference(IcyBufferedImage image, int t, TransformOp transformop) 
	{
		switch (transformop) 
		{
			case REF_PREVIOUS: 
				{
				int t0 = t-seqAnalysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = getImage(t0, 0);
				image = subtractImagesAsInteger (image, ibufImage0);
				}	
				break;
			case REF_T0:
			case REF:
				if (refImage == null)
					refImage = getImage((int) seqAnalysisStart, 0);
				image = subtractImagesAsInteger (image, refImage);
				break;
			case NONE:
			default:
				break;
		}
		return image;
	}
		
	public List <String> getListofFiles() 
	{
		return listFiles;
	}

	public String getDecoratedImageName(int t) 
	{
		currentFrame = t; 
		if (seq!= null)
			return csCamFileName + " ["+(t)+ "/" + (seq.getSizeT()-1) + "]";
		else
			return csCamFileName + "[]";
	}
	
	public String getFileName(int t) 
	{
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles.get(t);
//		else if (status == EnumStatus.AVIFILE)
//			csName = csFileName;
		return csName;
	}
	
	public IcyBufferedImage imageIORead(int t) 
	{
		currentFrame = t;
		String name = listFiles.get(t);
		BufferedImage image = null;
		try 
		{
	    	image = ImageIO.read(new File(name));
		} catch (IOException e) {
			 e.printStackTrace();
		}
		return IcyBufferedImage.createFrom(image);
	}
	
	public String getFileNameNoPath(int t) 
	{
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles.get(t);
		else if (status == EnumStatus.AVIFILE)
			csName = csCamFileName;
		if (csName != null) 
		{
			Path path = Paths.get(csName);
			return path.getName(path.getNameCount()-1).toString();
		}
		return csName;
	}
	
	public boolean isFileStack() 
	{
		return (status == EnumStatus.FILESTACK);
	}
	
	private List<String> keepOnlyAcceptedNamesFromList(List<String> rawlist, int filetype) 
	{
		int count = rawlist.size();
		List<String> outList = new ArrayList<String> (count);
		for (String name: rawlist) 
		{
			int itype = getCodeIfAcceptedFileType(name);
			if ( itype >= 0) {
				if (filetype < 0)
					filetype = itype;
				if (itype == filetype)
					outList.add(name);
			}
		}
		return outList;
	}
		
	private String[] keepOnlyAcceptedNamesFromArray(String[] rawlist) 
	{
		int count = rawlist.length;
		for (int i=0; i< rawlist.length; i++) 
		{
			if ( !isAcceptedFileType(rawlist[i]) ) 
			{
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
			for (int i=0; i< rawlist.length; i++) 
			{
				if (rawlist[i] != null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}

	// TODO: use GPU
	public IcyBufferedImage subtractImagesAsDouble (IcyBufferedImage image1, IcyBufferedImage image2) 
	{	
		IcyBufferedImage result = new IcyBufferedImage(image1.getSizeX(), image1.getSizeY(), image1.getSizeC(), image1.getDataType_());
		for (int c = 0; c < image1.getSizeC(); c++) 
		{
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
	
	public IcyBufferedImage subtractImagesAsInteger (IcyBufferedImage image1, IcyBufferedImage image2) 
	{	
		IcyBufferedImage result = new IcyBufferedImage(image1.getSizeX(), image1.getSizeY(), image1.getSizeC(), image1.getDataType_());
		for (int c = 0; c < image1.getSizeC(); c++) 
		{
			int[] img1Array = Array1DUtil.arrayToIntArray(image1.getDataXY(c), image1.isSignedDataType());
			int[] img2Array = Array1DUtil.arrayToIntArray(image2.getDataXY(c), image2.isSignedDataType());
			ArrayMath.subtract(img1Array, img2Array, img1Array);

			int[] dummyzerosArray = Array1DUtil.arrayToIntArray(result.getDataXY(c), result.isSignedDataType());
			max(img1Array, dummyzerosArray, img1Array);
			Object destArray = result.getDataXY(c);
			Array1DUtil.intArrayToSafeArray(img1Array, destArray, true, result.isSignedDataType());
			result.setDataXY(c, destArray);
		}
		result.dataChanged();
		return result;
	}
	
	private  void max(int[] a1, int[] a2, int[] output) 
	{
		for (int i = 0; i < a1.length; i++)
			if (a1[i] >= a2[i])
				output[i] = a1[i];
			else
				output[i] = a2[i];
	}
	
	// --------------------------
	
	public Sequence loadSequenceFromDialog(String path) 
	{
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    seqCamDataDirectory = selectedFiles[0].isDirectory() ? selectedFiles[0].getAbsolutePath() : selectedFiles[0].getParentFile().getAbsolutePath();
		if (seqCamDataDirectory != null ) 
		{
			if (selectedFiles.length == 1) 
			{
				seq = loadSequenceOfImages(selectedFiles[0].getAbsolutePath());
			}
			else 
			{
				String [] list = new String [selectedFiles.length];
				for (int i = 0; i < selectedFiles.length; i++) 
				{
					if (!selectedFiles[i].getName().toLowerCase().contains(".avi"))
						list[i] = selectedFiles[i].getAbsolutePath();
				}
				seq = loadSequenceOfImagesFromListAndDirectory(list, seqCamDataDirectory);
			}
		}
		return seq;
	}
	
	protected Sequence loadSequenceOfImages(String textPath) 
	{
		if (textPath == null) 
			return loadSequenceFromDialog(null); 
		File filepath = new File(textPath); 	
	    seqCamDataDirectory = filepath.isDirectory()? filepath.getAbsolutePath(): filepath.getParentFile().getAbsolutePath();
		if (seqCamDataDirectory != null ) 
		{
			List<String> list = new ArrayList<String> ();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(seqCamDataDirectory))) 
			{
				for (Path entry: stream) 
				{
					list.add(entry.toString());
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
			if (list.size() == 0)
				return null;
			else 
			{
				list = keepOnlyAcceptedNamesFromList(list, 0);	
				if (list.size() == 0)
					return null;
				if (!(filepath.isDirectory()) && filepath.getName().toLowerCase().contains(".avi"))
					seq = Loader.loadSequence(filepath.getAbsolutePath(), 0, true);
				else 
				{
					if (list.get(0).contains("avi")) 
					{
						seq = Loader.loadSequence(filepath.getAbsolutePath(), 0, true);
					} else 
					{
						status = EnumStatus.FAILURE;
						seq = loadSequenceOfImagesFromList(list, true);
					}
				}
			}
		}
		setParentDirectoryAsCSCamFileName();
		return seq;
	}
	
	// ---------------------------------------------------------
	public List<String> getV2ImagesListFromDialog(String path) 
	{
		List<String> list = new ArrayList<String> ();
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    File filepath = selectedFiles[0]; 	
	    seqCamDataDirectory = filepath.isDirectory() ? filepath.getAbsolutePath() : filepath.getParentFile().getAbsolutePath();
		if (seqCamDataDirectory != null ) 
		{
			if (selectedFiles.length == 1) 
				list = getV2ImagesListFromPath(filepath);
			list = keepOnlyAcceptedNamesFromList(list, 0);
		}
		return list;
	}
	
	public List<String> getV2ImagesListFromPath(File filepath) 
	{
		List<String> list = new ArrayList<String> ();
		seqCamDataDirectory = filepath.isDirectory()? filepath.getAbsolutePath(): filepath.getParentFile().getAbsolutePath();
		if (seqCamDataDirectory != null ) 
		{
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(seqCamDataDirectory))) 
			{
				for (Path entry: stream) 
				{
					list.add(entry.toString());
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			if (list.size() != 0)
				list = keepOnlyAcceptedNamesFromList(list, 0);	
		}
		return list;
	}

	// --------------------------------------------------------
	private Sequence loadSequenceOfImagesFromListAndDirectory(String [] list, String directory) 
	{
		status = EnumStatus.FAILURE;
		list = keepOnlyAcceptedNamesFromArray(list);
		list = StringSorter.sortNumerically(list);
		listFiles = new ArrayList<String>(list.length);
		for (int i=0; i<list.length; i++) 
		{
			if (list[i]!=null)
				listFiles.add(directory + File.separator + list[i]);
		}
		nTotalFrames = list.length;	
		return loadSequenceOfImagesFromList(listFiles, true);
	}
	
	protected Sequence loadSequenceOfImagesFromList(List<String> myListOfFilesNames, boolean testFilenameDecoratedwithLR) 
	{
		long startTime = System.nanoTime();
		if (testFilenameDecoratedwithLR && isLinexLRFileNames(myListOfFilesNames)) 
		{
			listFiles = convertLinexLRFileNames(myListOfFilesNames);
		} else 
		{
			listFiles = myListOfFilesNames;
		}
		long endTime = System.nanoTime();
		System.out.println("load list of "+ listFiles.size() +" files "+ listFiles.get(0)); // "+ " - duration: "+((endTime-startTime)/ 1000000000f) + " s");
		
		startTime = endTime;
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(listFiles.get(0), true);
		Sequence seq = Loader.loadSequence(seqFileImporter, listFiles, false);
		endTime = System.nanoTime();
		System.out.println("loading sequence done - duration: "+((endTime-startTime)/ 1000000000f) + " s");		

		nTotalFrames = listFiles.size();	
		status = EnumStatus.FILESTACK;	
		seqAnalysisStart = 0;
		seqAnalysisEnd = seq.getSizeT()-1;
		seqAnalysisStep = 1;
		return seq;
	}
	
	private boolean isLinexLRFileNames(List<String> myListOfFilesNames) 
	{
		boolean flag = false;
		int nfound = 0;
		for (String filename: myListOfFilesNames) 
		{	
			if (filename.contains("R.") || filename.contains("L."))
				nfound++;
			if (nfound >1) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	private List<String> convertLinexLRFileNames(List<String> myListOfFilesNames) 
	{
		List<String> newList = new ArrayList<String>();
		for (String oldName: myListOfFilesNames) 
		{
			newList.add(convertLinexLRFileName(oldName));
		}
		return newList; 
	}
	
	private String convertLinexLRFileName(String oldName) 
	{
		String newName = oldName;
		if (oldName.contains("R.")) 
		{
			newName = oldName.replace("R.", "2.");
			renameOldFile(oldName, newName);
		}
		else if (oldName.contains("L")) 
		{ 
			newName = oldName.replace("L.", "1.");
			renameOldFile(oldName, newName);
		}
		return newName; 
	}
	
	private void renameOldFile(String oldName, String newName) 
	{
		File oldfile = new File(oldName);
		if (newName != null && oldfile.exists()) 
		{
			try 
			{
				FileUtils.moveFile(	FileUtils.getFile(oldName),  FileUtils.getFile(newName));
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	// --------------------------
	
	public String getCSCamFileName() 
	{
		return csCamFileName;		
	}
	
	protected void setParentDirectoryAsCSCamFileName() 
	{
		if (seq == null)
			return;
		String directory = seq.getFilename();
		Path path = Paths.get(directory);
		csCamFileName = path.getName(path.getNameCount()-2).toString();
		seq.setName(csCamFileName);
		if (csCamFileName.equals("grabs"))
			csCamFileName = path.getName(path.getNameCount()-3).toString();
	}
	
	// ---------------------------
	
	public void closeSequence() 
	{
		if (seq == null)
			return;
		seq.removeAllROI();
		seq.close();
	}

	public boolean xmlReadROIs(String fileName) 
	{
		if (fileName != null)  
		{
			final Document doc = XMLUtil.loadDocument(fileName);
			if (doc != null) 
			{
				List<ROI2D> seqRoisList = seq.getROI2Ds(false);
				List<ROI2D> newRoisList = ROI2DUtilities.loadROIsFromXML(doc);
				ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
				seq.removeAllROI();
				seq.addROIs(seqRoisList, false);
				return true;
			}
		}
		return false;
	}
	
	// --------------------------
	
	public FileTime getFileTimeFromStructuredName(int t) 
	{
		String fileName = getFileName(t);
		if (fileName == null)
			return null;
		int len = fileName.length();
		if (len < 23)
			return null;
		String text = "20"+fileName.substring(len-21, len-4);
		String dateFormat = "yyyy"
							+text.charAt(4)+"MM"
							+text.charAt(7)+"dd"
							+text.charAt(10)+"HH"
							+text.charAt(13)+"mm"
							+text.charAt(16)+"ss";
		Date date = null;
		try 
		{
			date = new SimpleDateFormat(dateFormat).parse(text);
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
			return null;
		}
		FileTime fileTime = FileTime.fromMillis(date.getTime());		
		return fileTime;
	}
	
	public FileTime getFileTimeFromFileAttributes(int t) 
	{
		FileTime filetime=null;
		File file = new File(getFileName(t));
        Path filePath = file.toPath();

        BasicFileAttributes attributes = null;
        try 
        {
            attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        }
        catch (IOException exception) 
        {
            System.out.println("Exception handled when trying to get file " +
                    "attributes: " + exception.getMessage());
        }
        
        long milliseconds = attributes.creationTime().to(TimeUnit.MILLISECONDS);
        if((milliseconds > Long.MIN_VALUE) && (milliseconds < Long.MAX_VALUE)) 
        {
            Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
            filetime = FileTime.fromMillis(creationDate.getTime());
        }
		return filetime;
	}

	public FileTime getFileTimeFromJPEGMetaData(int t) {
		FileTime filetime = null;
		File file = new File(getFileName(t));
		Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(file);
			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL); 
			filetime = FileTime.fromMillis(date.getTime());
		} 
		catch (ImageProcessingException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return filetime;
	}
	
	public List<Cage> getCages () 
	{
		List<ROI2D> roiList = seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2D_Name_Comparator());
		List<Cage> cageList = new ArrayList<Cage>();
		for ( ROI2D roi : roiList ) 
		{
			String csName = roi.getName();
			if (!(roi instanceof ROI2DPolygon))
				continue;
			if ((csName.length() > 4 && csName.substring( 0 , 4 ).contains("cage")
				|| csName.contains("Polygon2D")) ) 
			{
				Cage cage = new Cage();
				cage.cageRoi = roi;
				cageList.add(cage);
			}
		}
		return cageList;
	}
	
	public IcyBufferedImage getImageCopy(int t) 
	{	
		return IcyBufferedImageUtil.getCopy(getImage(t, 0));
	}

	public void displayViewerAtRectangle(Rectangle parent0Rect) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() 
			{ 
				public void run() 
				{
					Viewer v = seq.getFirstViewer();
					if (v == null)
						v = new Viewer(seq, true);
					Rectangle rectv = v.getBoundsInternal();
					rectv.setLocation(parent0Rect.x+ parent0Rect.width, parent0Rect.y);
					v.setBounds(rectv);				
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		} 
	}
	
}