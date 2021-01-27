package plugins.fmp.multicafe.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Directories {
	static public List<String>  reduceFullNameToLastDirectory(List<String> dirList) {	
		List<String> shortList = new ArrayList<String> (dirList.size());
		for (String name: dirList) {
			Path pathName = Paths.get(name);
			shortList.add(pathName.getName(pathName.getNameCount()-1).toString());
		}
		Collections.sort(shortList, String.CASE_INSENSITIVE_ORDER);
		return shortList;
	}
	
	static public HashSet<String> getDirectoriesWithFilesType (String rootDirectory, String filter) {
		HashSet<String> hSet = new HashSet<String>();
		try {
			Files.walk(Paths.get(rootDirectory))
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().toLowerCase().endsWith(filter))
				.forEach(p->hSet.add( p.toFile().getParent().toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return hSet;
	}
	
	static public List<String> fetchListOfSubDirectoriesMatchingFilter(String directory, String filter) {	
		List<Path> subfolders = getAllSubPaths(directory, 1);
		if (subfolders == null)
			return null;
		List<String> dirList = getSubListContainingString(subfolders, filter);
		Collections.sort(dirList, String.CASE_INSENSITIVE_ORDER);
		return dirList;
	}
	
	public static List<String> getSubListContainingString (List<Path> subfolders, String filter) {
		List<String> dirList = new ArrayList<String>();
		for (Path dirPath: subfolders) {
			String subString = dirPath.toString();
			if (subString.contains(filter)) {
				boolean found = false;
				for (String item: dirList) {
					if (item.equals(subString)) {
						found = true;
						break;
					}
				}
				if (!found)
					dirList.add(subString);
			}
		}
		return dirList;
	}
	
	public static List<Path> getAllSubPaths(String directory, int depth) {
		Path pathExperimentDir = Paths.get(directory);
		List<Path> subfolders = null;
	
		try {
			subfolders = Files.walk(pathExperimentDir, depth) 
			    .filter(Files::isDirectory)
			    .collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		subfolders.remove(0);
		return subfolders;
	}
	
	
}
