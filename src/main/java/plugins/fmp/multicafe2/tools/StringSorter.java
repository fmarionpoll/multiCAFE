package plugins.fmp.multicafe2.tools;

import java.util.Arrays;
import java.util.Comparator;

/*
 * String sorter routines from imageJ
 * https://github.com/imagej/imagej1/blob/master/ij/util/StringSorter.java
 * rasband 2013.06.06 (1.47t10)
 * 
 */

/** A simple QuickSort for String arrays. */
public class StringSorter 
{	
	public static void sort(String[] a) 
	{
		/** Sort the array. */
		if (!alreadySorted(a))
			sort(a, 0, a.length - 1);
	}
	
	static void sort(String[] a, int from, int to) 
	{
		int i = from, j = to;
		String center = a[ (from + to) / 2 ];
		do 
		{
			while ( i < to && center.compareTo(a[i]) > 0 ) 
				i++;
			while ( j > from && center.compareTo(a[j]) < 0 ) 
				j--;
			if (i < j) 
			{
				String temp = a[i]; a[i] = a[j]; 
				a[j] = temp; 
			}
			if (i <= j) 
			{ 
				i++; 
				j--; 
			}
		} while(i <= j);
		if (from < j) 
			sort(a, from, j);
		if (i < to) 
			sort(a,  i, to);
	}
		
	static boolean alreadySorted(String[] a) 
	{
		for ( int i=1; i<a.length; i++ ) 
		{
			if (a[i].compareTo(a[i-1]) < 0 )
			return false;
		}
		return true;
	}
	
	public static int[] rank(final String[] data) 
	{
		/** Returns a sorted list of indices of the specified String array. */
		int n = data.length;
		final Integer[] indexes = new Integer[n];
		for (int i=0; i<n; i++)
			indexes[i] = i;
		Arrays.sort(indexes, new Comparator<Integer>() 
		{
			public int compare(final Integer o1, final Integer o2) 
			{
				return data[o1].compareToIgnoreCase(data[o2]);
			}});
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++)
			indexes2[i] = indexes[i].intValue();
		return indexes2;
	}
	
	public static int[] rank(double[] values) 
	{
		/** Returns a sorted list of indices of the specified double array.
		Modified from: http://stackoverflow.com/questions/951848 by N.Vischer.
		 */
		int n = values.length;
		final Integer[] indexes = new Integer[n];
		final Double[] data = new Double[n];
		for (int i=0; i<n; i++) 
		{
			indexes[i] = i;
			data[i] = values[i];
		}
		Arrays.sort(indexes, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				return data[o1].compareTo(data[o2]);
			}
		});
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++)
			indexes2[i] = indexes[i].intValue();
		return indexes2;
	}
	
	public static String[] sortNumerically(String[] list) {
		/** Sorts file names containing numerical components.
		* Author: Norbert Vischer
		*/
		int n = list.length;
		String[] paddedList = getPaddedNames(list);
		String[] sortedList = new String[n];
		int[] indexes = rank(paddedList);
		for (int i = 0; i < n; i++)
			sortedList[i] = list[indexes[i]];
		return sortedList;
	}

	private static String[] getPaddedNames(String[] names) {
		// Pads individual numeric string components with zeroes for correct sorting
		int nNames = names.length;
		String[] paddedNames = new String[nNames];
		int maxLen = 0;
		for (int jj = 0; jj < nNames; jj++) {
			if (names[jj].length() > maxLen) {
				maxLen = names[jj].length();
			}
		}
		int maxNums = maxLen / 2 + 1; // calc array sizes
		int[][] numberStarts = new int[names.length][maxNums];
		int[][] numberLengths = new int[names.length][maxNums];
		int[] maxDigits = new int[maxNums];

		//a) record position and digit count of 1st, 2nd, .. n-th number in string
		for (int jj = 0; jj < names.length; jj++) 
		{
			String name = names[jj];
			boolean inNumber = false;
			int nNumbers = 0;
			int nDigits = 0;
			for (int pos = 0; pos < name.length(); pos++) 
			{
				boolean isDigit = name.charAt(pos) >= '0' && name.charAt(pos) <= '9';
				if (isDigit) 
				{
					nDigits++;
					if (!inNumber) 
					{
						numberStarts[jj][nNumbers] = pos;
						inNumber = true;
					}
				}
				if (inNumber && (!isDigit || (pos == name.length() - 1))) 
				{
					inNumber = false;
					if (maxDigits[nNumbers] < nDigits) 
						maxDigits[nNumbers] = nDigits;
					numberLengths[jj][nNumbers] = nDigits;
					nNumbers++;
					nDigits = 0;
				}
			}
		}
		
		//b) perform padding
		for (int jj = 0; jj < names.length; jj++) 
		{
			String name = names[jj];
			int numIndex = 0;
			StringBuilder destName = new StringBuilder();
			for (int srcPtr = 0; srcPtr < name.length(); srcPtr++) 
			{
				if (srcPtr == numberStarts[jj][numIndex]) 
				{
					int numLen = numberLengths[jj][numIndex];
					if (numLen > 0) {
						for (int pad = 0; pad < (maxDigits[numIndex] - numLen); pad++) 
							destName.append('0');
					}
					numIndex++;
				}
				destName.append(name.charAt(srcPtr));
			}
			paddedNames[jj] = destName.toString();
		}
		return paddedNames;
	}

}
