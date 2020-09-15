package plugins.fmp.multicafe.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	PATH("path", 0),
	DATE( "date", 1), 
	BOXID("box_ID", 2), 
	EXPMT("expmt", 3), COMMENT1("comment1", 4),
	CAPSTIM("stim", 5), CAPCONC("conc", 6), 
	CAM ("cam", 7), CAP ("cap", 8),
	CAGEINDEX("cage", 9), NFLIES("nflies", 10), CAGEID ("cage_ID", 11),
	CAGECOMMENT("cage_comment", 12), CAPVOLUME("cap_ul", 13), CAPPIXELS ("cap_npixels", 14), DUM4("dum4", 15), 
	COMMENT2("comment2", 16);
	
	private final String 	name;
	private final int 		value;
	
	
	EnumXLSColumnHeader (String label, int value) { 
		this.name = label;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}
	
	static final Map<String, EnumXLSColumnHeader> names = Arrays.stream(EnumXLSColumnHeader.values())
		      .collect(Collectors.toMap(EnumXLSColumnHeader::getName, Function.identity()));
	static final Map<Integer, EnumXLSColumnHeader> values = Arrays.stream(EnumXLSColumnHeader.values())
		      .collect(Collectors.toMap(EnumXLSColumnHeader::getValue, Function.identity()));
	
	public static EnumXLSColumnHeader fromName(final String name) {
	    return names.get(name);
	}

	public static EnumXLSColumnHeader fromValue(final int value) {
	    return values.get(value);
	}
}


