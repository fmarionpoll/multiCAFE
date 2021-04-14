package plugins.fmp.multicafe.dlg.experiment;


import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import icy.gui.dialog.LoaderDialog;
import plugins.fmp.multicafe.tools.Directories;


public class ExperimentDirectories 
{
	public String cameraImagesDirectory = null;
	public List<String> cameraImagesList = null;
	
	public String resultsDirectory = null;
	public String binSubDirectory = null;
	public List<String> kymosImagesList = null;
	
	  
	
	public static List<String> getV2ImagesListFromPath(String strDirectory) 
	{
		List<String> list = new ArrayList<String> ();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(strDirectory))) 
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
	
		return list;
	}
	
	public static List<String> getV2ImagesListFromDialog(String strPath) 
	{
		List<String> list = new ArrayList<String> ();
		LoaderDialog dialog = new LoaderDialog(false);
		if (strPath != null) 
			dialog.setCurrentDirectory(new File(strPath));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    // TODO check strPath and provide a way to skip the dialog part (or different routine)
	    String strDirectory = Directories.clipNameToDirectory(selectedFiles[0].toString());
		if (strDirectory != null ) 
		{
			if (selectedFiles.length == 1) 
				list = getV2ImagesListFromPath(strDirectory);
		}
		return list;
	}
	
	public static List<String> keepOnlyAcceptedNames_List(List<String> namesList, String strExtension) 
	{
		int count = namesList.size();
		List<String> outList = new ArrayList<String> (count);
		String ext = strExtension.toLowerCase();
		for (String name: namesList) 
		{
			if (name.toLowerCase().endsWith(ext))
				outList.add(name);
		}
		return outList;
	}
	
}
