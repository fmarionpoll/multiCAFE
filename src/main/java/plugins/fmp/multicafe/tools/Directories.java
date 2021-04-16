package plugins.fmp.multicafe.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import plugins.fmp.multicafe.experiment.Capillary;



public class Directories 
{
	static public List<String> reduceFullNameToLastDirectory(List<String> dirList) 
	{	
		List<String> shortList = new ArrayList<String> (dirList.size());
		for (String name: dirList) 
		{
			Path pathName = Paths.get(name);
			shortList.add(pathName.getName(pathName.getNameCount()-1).toString());
		}
		Collections.sort(shortList, String.CASE_INSENSITIVE_ORDER);
		return shortList;
	}
	
	static public HashSet<String> getDirectoriesWithFilesType (String rootDirectory, String filter) 
	{
		HashSet<String> hSet = new HashSet<String>();
		try 
		{
			Path rootPath = Paths.get(rootDirectory);
			if (Files.exists(rootPath))  
			{
				Files.walk(rootPath)
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().toLowerCase().endsWith(filter))
				.forEach(p->hSet.add( p.toFile().getParent().toString()));
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return hSet;
	}
	
	static public List<String> fetchSubDirectoriesMatchingFilter(String directory, String filter) 
	{	
		List<Path> subfolders = getAllSubPathsOfDirectory(directory, 1);
		if (subfolders == null)
			return null;
		List<String> dirList = getPathsContainingString(subfolders, filter);
		Collections.sort(dirList, String.CASE_INSENSITIVE_ORDER);
		return dirList;
	}
	
	public static List<String> getPathsContainingString (List<Path> subfolders, String filter) 
	{
		if (subfolders == null)
			return null;
		HashSet<String> dirList = new HashSet<String>();
		for (Path dirPath: subfolders) 
		{
			String subString = dirPath.toString();
			if (subString.contains(filter)) {
				dirList.add(subString);
			}
		}
		return new ArrayList<String>(dirList);
	}
	
	public static List<Path> getAllSubPathsOfDirectory(String directory, int depth) 
	{
		Path pathExperimentDir = Paths.get(directory);
		List<Path> subfolders = null;
		try 
		{
			if (Files.exists(pathExperimentDir) ) 
			{
				subfolders = Files.walk(pathExperimentDir, depth) 
				    .filter(Files::isDirectory)
				    .collect(Collectors.toList());
			}
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (subfolders != null)
			subfolders.remove(0);
		return subfolders;
	}
	
	public static String getDirectoryFromName (String fileName)
	{
		File filepath = new File(fileName); 	
		String strDirectory = filepath.isDirectory()? filepath.getAbsolutePath(): filepath.getParentFile().getAbsolutePath();
		return strDirectory;
	}
	
	public static List<String> getSortedListOfSubDirectoriesWithTIFF(String parentDirectory) 
	{
		HashSet <String> hSet = getDirectoriesWithFilesType (parentDirectory, ".tiff");
		List<String> list = reduceFullNameToLastDirectory(new ArrayList<String>(hSet));
		// TODO pb if no number
		list.sort(Comparator
				.comparing(s->Integer.parseUnsignedInt(s.substring(4, s.length())))
				);
		return list;
	}

	public static void deleteFilesWithExtension(String directory, String filter) 
	{
		File folder = new File(directory);
		for (File file : folder.listFiles()) {
			String name = file.getName();
			if (name.toLowerCase().endsWith(filter)) {
				file.delete();
		   }
		}
	}
	
	public static void moveTIFFAndLINEfilesToSubdirectory(String directory, String subname)
	{
		String subdirectory = directory + File.separator + subname;
		File folder = new File(directory);
		File subfolder = new File(subdirectory);
		if (!subfolder.exists()) 
			subfolder.mkdir();

		for (File file : folder.listFiles()) {
			String name = file.getName();
			if (name.toLowerCase().endsWith(".tiff") || name.toLowerCase().startsWith("line")) 
			{
				String destinationName = Capillary.replace_LR_with_12(name);
				file.renameTo (new File(subdirectory + File.separator + destinationName));
				file.delete();
			}
		}
	}
}