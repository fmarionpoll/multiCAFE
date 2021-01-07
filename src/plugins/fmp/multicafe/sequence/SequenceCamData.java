 package plugins.fmp.multicafe.sequence;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import icy.file.Loader;
import icy.gui.dialog.LoaderDialog;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.roi.ROI;
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
import plugins.kernel.roi.roi2d.ROI2DShape;



public class SequenceCamData {
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
	protected String 				csFileName 				= null;
	protected String				directory 				= null;
	
	private final static String[] 	acceptedTypes 			= {".jpg", ".jpeg", ".bmp", "tiff", "tif", "avi"};  
	// TODO: accept mpeg mv4?
	
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
		if (loadSequenceFromList(listFiles, true) != null) {
			Path path = Paths.get(listFiles.get(0));
			int iback = 2;
			String dir = path.getName(path.getNameCount()-iback).toString();
			if (dir != null) {
				if (dir.equals("grabs"))
					dir = path.getName(path.getNameCount()-3).toString();
				seq.setName(dir);
			}
		}
	}
	
	public SequenceCamData (List<String> listNames, boolean testLR) {
		listFiles.clear();
		for (String cs: listNames)
			listFiles.add(cs);
		if (loadSequenceFromList(listFiles, testLR) != null) {
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
	
	public static boolean isAcceptedFileType(String name) {
		if (name==null) 
			return false;
		for (int i=0; i< acceptedTypes.length; i++) {
			if (name.endsWith(acceptedTypes[i]))
				return true;
		}
		return false;
	}	
	
	public static int getCodeIfAcceptedFileType(String name) {
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
		if (directory == null)
			directory = seq.getFilename();
		return directory;
	}

	public IcyBufferedImage getImage(int t, int z) {
		return seq.getImage(t, z);
	}
	
	public IcyBufferedImage subtractReference(IcyBufferedImage image, int t, TransformOp transformop) {
		switch (transformop) {
			case REF_PREVIOUS: {
				int t0 = t-seqAnalysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = getImage(t0, 0);
				image = subtractImages (image, ibufImage0);
				}	
				break;
			case REF_T0:
			case REF:
				if (refImage == null)
					refImage = getImage((int) seqAnalysisStart, 0);
				image = subtractImages (image, refImage);
				break;

			case NONE:
			default:
				break;
		}
		return image;
	}
		
	public List <String> getListofFiles() {
		return listFiles;
	}

	public String getDecoratedImageName(int t) {
		currentFrame = t; 
		if (seq!= null)
			return csFileName + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
		else
			return csFileName + "[]";
	}
	
	public String getFileName(int t) {
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles.get(t);
//		else if (status == EnumStatus.AVIFILE)
//			csName = csFileName;
		return csName;
	}
	
	public IcyBufferedImage getImageDirect(int t) {
		currentFrame = t;
		String name = listFiles.get(t);
		BufferedImage image = null;
		try {
	    	image = ImageIO.read(new File(name));
		} catch (IOException e) {
		}
		return IcyBufferedImage.createFrom(image);
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
		
	public String[] keepOnlyAcceptedNamesFromArray(String[] rawlist) {
		int count = rawlist.length;
		for (int i=0; i< rawlist.length; i++) {
			if ( !isAcceptedFileType(rawlist[i]) ) {
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

	// TODO: use GPU
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
	
	public Sequence loadSequenceFromDialog(String path) {
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    directory = selectedFiles[0].isDirectory() ? selectedFiles[0].getAbsolutePath() : selectedFiles[0].getParentFile().getAbsolutePath();
		if (directory != null ) {
			if (selectedFiles.length == 1) {
				seq = loadSequence(selectedFiles[0].getAbsolutePath());
			}
			else {
				String [] list = new String [selectedFiles.length];
				for (int i = 0; i < selectedFiles.length; i++) {
					if (!selectedFiles[i].getName().toLowerCase().contains(".avi"))
						list[i] = selectedFiles[i].getAbsolutePath();
				}
				seq = loadSequenceFromListAndDirectory(list, directory);
			}
		}
		return seq;
	}
	
	public Sequence loadSequence(String textPath) {
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
				list = keepOnlyAcceptedNamesFromList(list, 0);	
				if (list.size() == 0)
					return null;
				if (!(filepath.isDirectory()) && filepath.getName().toLowerCase().contains(".avi"))
					seq = Loader.loadSequence(filepath.getAbsolutePath(), 0, true);
				else {
					if (list.get(0).contains("avi")) {
						seq = Loader.loadSequence(filepath.getAbsolutePath(), 0, true);
					} else {
						status = EnumStatus.FAILURE;
						seq = loadSequenceFromList(list, true);
					}
				}
			}
		}
		return seq;
	}

	private Sequence loadSequenceFromListAndDirectory(String [] list, String directory) {
		status = EnumStatus.FAILURE;
		list = keepOnlyAcceptedNamesFromArray(list);
		list = StringSorter.sortNumerically(list);
		listFiles = new ArrayList<String>(list.length);
		for (int i=0; i<list.length; i++) {
			if (list[i]!=null)
				listFiles.add(directory + File.separator + list[i]);
		}
		nTotalFrames = list.length;	
		return loadSequenceFromList(listFiles, true);
	}
	
	protected Sequence loadSequenceFromList(List<String> myListOfFilesNames, boolean testFilenameDecoratedwithLR) {
		seq = null;
		if (testFilenameDecoratedwithLR && isLinexLRFileNames(myListOfFilesNames)) {
			listFiles = convertLinexLRFileNames(myListOfFilesNames);
		} else {
			listFiles = myListOfFilesNames;
		}

		List<Sequence> lseq = Loader.loadSequences(null, listFiles, 0, false, false, false, true);
		if (lseq.size() > 0) {
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
		return seq;
	}
	
	private void initAnalysisParameters() {
		seqAnalysisStart = 0;
		seqAnalysisEnd = seq.getSizeT()-1;
		seqAnalysisStep = 1;
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
	
	public String getSequenceFileName() {
		if (seq != null)
			return seq.getFilename();
		return null;	
	}
	
	public void setCSFileName(String name) {
		csFileName = name;		
	}
	
	public String getCSFileName() {
		return csFileName;		
	}
	
	public void setParentDirectoryAsFileName() {
		if (seq == null)
			return;
		String directory = seq.getFilename();
		Path path = Paths.get(directory);
		String dirupup = path.getName(path.getNameCount()-2).toString();
		if (dirupup.equals("grabs"))
			dirupup = path.getName(path.getNameCount()-3).toString();
		setCSFileName(dirupup);
		seq.setName(dirupup);
	}
	
	// ---------------------------
	
	public void closeSequence() {
		if (seq == null)
			return;
		seq.removeAllROI();
		seq.close();
	}

	public boolean xmlReadROIs(String fileName) {
		if (fileName != null)  {
			final Document doc = XMLUtil.loadDocument(fileName);
			if (doc != null) {
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
	
	public FileTime getFileTimeFromStructuredName(int t) {
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
		try {
			date = new SimpleDateFormat(dateFormat).parse(text);
		} 
		catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		FileTime fileTime = FileTime.fromMillis(date.getTime());		
		return fileTime;
	}
	
	public FileTime getFileTimeFromFileAttributes(int t) {
		FileTime filetime=null;
		File file = new File(getFileName(t));
        Path filePath = file.toPath();

        BasicFileAttributes attributes = null;
        try {
            attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        }
        catch (IOException exception) {
            System.out.println("Exception handled when trying to get file " +
                    "attributes: " + exception.getMessage());
        }
        
        long milliseconds = attributes.creationTime().to(TimeUnit.MILLISECONDS);
        if((milliseconds > Long.MIN_VALUE) && (milliseconds < Long.MAX_VALUE)) {
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
		} catch (ImageProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filetime;
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
					&& roi.getName().contains(string)
					&&  (t < 0 || ((ROI2D) roi).getT() == t ))
				seq.removeROI(roi);
		}
	}
	
	public List<ROI2D> getROIs2DContainingString (String string) {
		List<ROI2D> roiList = seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2DNameComparator());
		List<ROI2D> capillaryRois = new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList ) {
			if ((roi instanceof ROI2DShape) && roi.getName().contains(string)) 
				capillaryRois.add(roi);
		}
		return capillaryRois;
	}
	
	public List<Cage> getCages () {
		List<ROI2D> roiList = seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2DNameComparator());
		List<Cage> cageList = new ArrayList<Cage>();
		for ( ROI2D roi : roiList ) {
			String csName = roi.getName();
			if (!(roi instanceof ROI2DPolygon))
				continue;
//			if (( csName.contains( "cage")
			if ((csName.length() > 4 && csName.substring( 0 , 4 ).contains("cage")
				|| csName.contains("Polygon2D")) ) {
				Cage cage = new Cage();
				cage.cageRoi = roi;
				cageList.add(cage);
			}
		}
		return cageList;
	}
	
	public void getCamDataROIS (Capillaries capillaries) {
		capillaries.capillariesArrayList.clear();
		List<ROI2D> listROISCap = getROIs2DContainingString ("line");
		for (ROI2D roi:listROISCap) {
			capillaries.capillariesArrayList.add(new Capillary((ROI2DShape)roi));
		}
	}
	
	public IcyBufferedImage getImageCopy(int t) {	
		return IcyBufferedImageUtil.getCopy(getImage(t, 0));
	}


	
}