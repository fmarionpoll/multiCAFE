 package plugins.fmp.multicafe2.experiment;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.ImageOperationsStruct;
import plugins.fmp.multicafe2.tools.ImageTransformTools.TransformOp;
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
	protected String 				csCamFileName 			= null;
	
	volatile public List <String>	imagesList 				= new ArrayList<String>();
	volatile public boolean 		stopFlag 				= false;
	volatile public boolean 		threadRunning 			= false;
	
	// ----------------------------------------
	
	public SequenceCamData () 
	{
		seq = new Sequence();
		status = EnumStatus.FILESTACK;
	}
	
	public SequenceCamData(String name, IcyBufferedImage image) 
	{
		seq = new Sequence (name, image);
		status = EnumStatus.FILESTACK;
	}

	public SequenceCamData (List<String> listNames) 
	{
		setV2ImagesList(listNames);
		status = EnumStatus.FILESTACK;
	}
	
	// -----------------------
	
	public String getImagesDirectory () 
	{
		Path strPath = Paths.get(imagesList.get(0));
		return strPath.getParent().toString();
	}
	
	public List <String> getImagesList() 
	{
		return imagesList;
	}

	public String getDecoratedImageName(int t) 
	{
		currentFrame = t; 
		if (seq!= null)
			return getCSCamFileName() + " ["+(t)+ "/" + (seq.getSizeT()-1) + "]";
		else
			return getCSCamFileName() + "[]";
	}
	
	public String getCSCamFileName() 
	{
		if (csCamFileName == null) 
		{
			Path path = Paths.get(imagesList.get(0));
			csCamFileName = path.subpath(path.getNameCount()-4, path.getNameCount()-1).toString();
		}
		return csCamFileName;		
	}
	
	public String getFileName(int t) 
	{
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = imagesList.get(t);
//		else if (status == EnumStatus.AVIFILE)
//			csName = csFileName;
		return csName;
	}
	
	public String getFileNameNoPath(int t) 
	{
		String csName = null;
		csName = imagesList.get(t);
		if (csName != null) 
		{
			Path path = Paths.get(csName);
			return path.getName(path.getNameCount()-1).toString();
		}
		return csName;
	}
	
	// ------------------------------------------------------
	
	public IcyBufferedImage getSeqImage(int t, int z) 
	{
		currentFrame = t;
		return seq.getImage(t, z);
	}
	
	public IcyBufferedImage getImageCopy(int t) 
	{	
		return IcyBufferedImageUtil.getCopy(getSeqImage(t, 0));
	}

	// TODO: use GPU
	public IcyBufferedImage subtractReference(IcyBufferedImage image, int t, TransformOp transformop) 
	{
		switch (transformop) 
		{
			case REF_PREVIOUS: 
				{
				int t0 = t-seqAnalysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = getSeqImage(t0, 0);
				image = subtractImagesAsInteger (image, ibufImage0);
				}	
				break;
			case REF_T0:
			case REF:
				if (refImage == null)
					refImage = getSeqImage((int) seqAnalysisStart, 0);
				image = subtractImagesAsInteger (image, refImage);
				break;
			case NONE:
			default:
				break;
		}
		return image;
	}
		
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
	
	private void max(int[] a1, int[] a2, int[] output) 
	{
		for (int i = 0; i < a1.length; i++)
			if (a1[i] >= a2[i])
				output[i] = a1[i];
			else
				output[i] = a2[i];
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
	
	public List<Cage> getCagesFromROIs () 
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
	
	// ------------------------

	public void closeSequence() 
	{
		if (seq == null)
			return;
		if (threadRunning)
			stopFlag = true;
		seq.removeAllROI();
		seq.close();
	}

	public void setV2ImagesList(List <String> extImagesList) 
	{
		imagesList.clear();
		imagesList.addAll(extImagesList);
		nTotalFrames = imagesList.size();
		status = EnumStatus.FILESTACK;
	}
	
	public void attachV2Sequence(Sequence seq)
	{
		this.seq = seq;
		
		status = EnumStatus.FILESTACK;	
		seqAnalysisStart = 0;
		seqAnalysisEnd = seq.getSizeT()-1;
		seqAnalysisStep = 1;
	}
	
	public boolean loadImages() 
	{
		if (imagesList.size() == 0)
			return false;
		attachV2Sequence(loadV2SequenceFromImagesList(imagesList));
		return (seq != null);
	}
	
	public Sequence loadV2SequenceFromImagesList(List <String> imagesList) 
	{
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequence(seqFileImporter, imagesList, false);
		return seq;
	}
		
	public Sequence initV2SequenceFromFirstImage(List <String> imagesList) 
	{
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequence(seqFileImporter, imagesList.get(0), 0, false);
		return seq;
	}
	
	public IcyBufferedImage imageIORead(int t) 
	{
		String name = imagesList.get(t);
		BufferedImage image = null;
		try 
		{
	    	image = ImageIO.read(new File(name));
		} catch (IOException e) {
			 e.printStackTrace();
		}
		return IcyBufferedImage.createFrom(image);
	}
	
}