package plugins.fmp.multicafeTools;

import java.awt.Point;


import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class XLSUtils {

	public static void setValue (XSSFSheet sheet, Point pt, boolean transpose, int ivalue) {
		XSSFCell cell = getCell(sheet, pt, transpose);
		cell.setCellValue(ivalue);
	}
	
	public static void setValue (XSSFSheet sheet, Point pt, boolean transpose, String string) {
		XSSFCell cell = getCell(sheet, pt, transpose);
		cell.setCellValue(string);
	}
		
	public static void setValue (XSSFSheet sheet, Point pt, boolean transpose, double value) {
		XSSFCell cell = getCell(sheet, pt, transpose);
		cell.setCellValue(value);
	}
		
	/*
	// from ICYSpreadSheet.jjava and Workbooks.java in workbooks from adufour
	public static void setBackgroundColor (XSSFSheet sheet, Point pt, boolean transpose, Color color) {
		XSSFCell cell = getCell(sheet, pt, transpose);
		XSSFColor newColor = new XSSFColor(color);
	     
	    // look for an existing style
	    boolean styleExists = false;
	    try
	    {
	        int numStyles = book.getNumCellStyles();
	        for (int i = 0; i < numStyles; i++)
	        {
	            XSSFCellStyle cellStyle = (XSSFCellStyle) book.getCellStyleAt(i);
	            if (cellStyle.getFillForegroundXSSFColor() == newColor)
	            {
	                cell.setCellStyle(cellStyle);
	                styleExists = true;
	                break;
	            }
	        }
	    }
	    catch (IllegalStateException e)
	    {
	        styleExists = false;
	    }
	     
	    if (!styleExists)
	    {
	        XSSFCellStyle newStyle = (XSSFCellStyle) book.createCellStyle();
	        newStyle.setFillForegroundColor(newColor);
	        cell.setCellStyle(newStyle);
	    }
 
	 cell.getCellStyle().setFillPattern(CellStyle.SOLID_FOREGROUND);
	}
	*/
	
	public static double getValueDouble (XSSFSheet sheet, Point pt, boolean transpose) {
		return getCell(sheet, pt, transpose).getNumericCellValue();
	}
	
	public static XSSFCell getCell (XSSFSheet sheet, int rownum, int colnum) {
		XSSFRow row = getSheetRow(sheet, rownum);
		XSSFCell cell = getRowCell (row, colnum);
		return cell;
	}
	
	public static XSSFRow getSheetRow (XSSFSheet sheet, int rownum) {
		XSSFRow row = sheet.getRow(rownum);
		if (row == null)
			row = sheet.createRow(rownum);
		return row;
	}
	
	public static XSSFCell getRowCell (XSSFRow row, int cellnum) {
		XSSFCell cell = row.getCell(cellnum);
		if (cell == null)
			cell = row.createCell(cellnum);
		return cell;
	}
	
	public static XSSFCell getCell (XSSFSheet sheet, Point point, boolean transpose) {
		Point pt = new Point(point);
		if (transpose) {
			int dummy = pt.x;
			pt.x = pt.y;
			pt.y = dummy;
		}
		return getCell (sheet, pt.y, pt.x);
	}

}
