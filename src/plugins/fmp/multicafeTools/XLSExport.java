package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;



public class XLSExport {

	protected XLSExportOptions 	options 			= null;
	protected Experiment 		expAll 				= null;

	XSSFCellStyle 				xssfCellStyle_red 	= null;
	XSSFCellStyle 				xssfCellStyle_blue 	= null;
    XSSFFont 					font_red 			= null;
    XSSFFont 					font_blue 			= null;

	// -------------------------------------------------
    
	public long getnearest(long value, int step) {
		long diff0 = (value /step)*step;
		long diff1 = diff0 + step;
		if ((value - diff0 ) < (diff1 - value))
			value = diff0;
		else
			value = diff1;
		return value;
	}
		
	protected Point writeExperimentDescriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt) {
		boolean transpose = options.transpose;
		int row = pt.y;
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		int colseries = pt.x;
		for (int i = 0; i < 18; i++) {
			XLSUtils.setValue(sheet, pt, transpose, "--");
			pt.x++;
		}
		pt.x = colseries;
		
		String filename = exp.experimentFileName;
		if (filename == null)
			filename = exp.seqCamData.getDirectory();
		Path path = Paths.get(filename);
		String boxID = exp.boxID;
		String experiment = exp.experiment;
		String comment1 = exp.comment1;
		String comment2 = exp.comment2;
		String stimulusL = exp.capillaries.desc.stimulusL;
		String stimulusR = exp.capillaries.desc.stimulusR;
		String concentrationL = exp.capillaries.desc.concentrationL;
		String concentrationR = exp.capillaries.desc.concentrationR;
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String date = df.format(exp.getFileTimeImageFirst(false).toMillis());
		int subpath_i = 2;
		String name0 = path.toString();
		if (name0 .contains("grabs"))
			subpath_i++;
		String name1 = exp.getSubName(path, subpath_i);
		String cam = "-"; 
		if (name1.length() >= 5) cam = name1.substring(0, 5); 
		String sheetName = sheet.getSheetName();
		
		int rowmax = -1;
		for (EnumXLSColumnHeader dumb: EnumXLSColumnHeader.values()) {
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();		
		}
		
		List<Capillary> capList = exp.capillaries.capillariesArrayList;
		for (int t=0; t< capList.size(); t++) { 
			Capillary cap = capList.get(t);
			String	name = cap.capillaryRoi.getName();
			int col = getColFromKymoFileName(name);
			if (col >= 0) 
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.PATH.getValue(), transpose, name0);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DATE.getValue(), transpose, date);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.BOXID.getValue(), transpose, boxID);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.EXPMT.getValue(), transpose, experiment);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.COMMENT1.getValue(), transpose, comment1);
			String letter = name.substring(name.length() - 1);
			if (letter .equals("L")) XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.STIM.getValue(), transpose, stimulusL);
			else					 XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.STIM.getValue(), transpose, stimulusR);
			if (letter .equals("L")) XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CONC.getValue(), transpose, concentrationL);
			else 					 XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CONC.getValue(), transpose, concentrationR);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAM.getValue(), transpose, cam);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAP.getValue(), transpose, letter);
			int i = getCageFromCapillaryName(name);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGE.getValue(), transpose, i);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGEID.getValue(), transpose, charSeries+i);
			int j = 1;
			if (i < 1 || i > 8)
				j = 0;
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.NFLIES.getValue(), transpose, j);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM1.getValue(), transpose, name1);
//			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM2.getValue(), transpose, name11);
//			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM3.getValue(), transpose, name111);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM2.getValue(), transpose, exp.capillaries.desc.volume);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM3.getValue(), transpose, exp.capillaries.desc.pixels);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM4.getValue(), transpose, sheetName);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.COMMENT2.getValue(), transpose, comment2);
		}
		pt.x = col0;
		pt.y = rowmax +1;
		return pt;
	}
	
	public int outputFieldDescriptors(XSSFSheet sheet) {		
		Point pt = new Point(0,0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLSColumnHeader dumb: EnumXLSColumnHeader.values()) {
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol+1;
		return pt.y;
	}
	
	protected int getCageFromCapillaryName(String name) {
		if (!name .contains("line"))
			return -1;
	
		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		return numFromName;
	}
	
	protected String getShortenedName(SequenceCamData seq, int t) {
		return seq.getFileNameNoPath(t);
	}
	
	protected int getColFromKymoFileName(String name) {
		if (!name .contains("line"))
			return -1;

		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		if( name.length() > 5) {
			String side = name.substring(5, 6);
			if (side != null) {
				if (side .equals("R")) {
					numFromName = numFromName* 2;
					numFromName += 1;
				}
				else if (side .equals("L"))
					numFromName = numFromName* 2;
			}
		}
		return numFromName;
	}
	
	protected int getCageFromKymoFileName(String name) {
		if (!name .contains("line"))
			return -1;
		return Integer.parseInt(name.substring(4, 5));
	}
	
	protected int getColFromCageName(Cage cage) {
		String name = cage.roi.getName();
		if (!name .contains("cage"))
			return -1;
		
		String num = name.substring(4, name.length());
		int numFromName = Integer.parseInt(num);
		return numFromName;
	}
	
	protected Point getCellXCoordinateFromDataName(XLSCapillaryResults xlsResults, Point pt_main, int colseries) {
		int col = getColFromKymoFileName(xlsResults.name);
		if (col >= 0)
			pt_main.x = colseries + col;
		return pt_main;
	}
	
}
