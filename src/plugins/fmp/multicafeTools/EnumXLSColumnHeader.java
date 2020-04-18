package plugins.fmp.multicafeTools;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	PATH("path", 0),
	DATE( "date", 1), 
	BOXID("box_ID", 2), 
	EXPMT("expmt", 3), COMMENT1("comment1", 4),
	STIM("stim", 5), CONC("conc", 6), 
	CAM ("cam", 7), CAP ("cap", 8),
	CAGE("cage", 9), NFLIES("nflies", 10), CAGEID ("cageID", 11),
	DUM1("dum1", 12), DUM2("cap_ul", 13), DUM3 ("cap_npixels", 14), DUM4("dum4", 15), 
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

