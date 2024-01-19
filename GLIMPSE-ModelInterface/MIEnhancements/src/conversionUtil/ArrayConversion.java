package conversionUtil;

/**
 * The class to handle a conversion between two arrays.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayConversion {
	public static String[][] array1to2Conversion(String[] in) {
		String[][] out = new String[in.length][in[0].split(",").length];
		for (int i = 0; i < in.length; i++) 
				out[i] = string2Array(in[i],",");
		return out;
	}

	public static String[] array2to1Conversion(String[][] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in[0].length; i++) 
				out[i] = array2String(in[i]);
		return out;
	}
	
	public static String[][] arrayDimReverse(String[][] in) {
		String[][] out = new String[in[0].length][in.length];
		for (int i = 0; i < in.length; i++)
			for (int j = 0; j < in[i].length; j++) 
				out[j][i] = in[i][j];
		return out;
	}

	public static String[] string2Array(String in, String sep) {
		return in.split(sep==null?",":sep);
	}
	
	public static String[] arrayList2Array(ArrayList<?> in) {
		return in.toArray(new String[0]);
	}
	
	public static Object[] arrayList2Array(ArrayList<?> in, boolean isChart) {
		return in.toArray(new Object[0]);
	}
	
	public static String[] list2Array(List<?> in) {
		return in.toArray(new String[0]);
	}
	
	public static String array2String(String[] in) {
		return Arrays.toString(in).replace("[", "").replace("]", "");
	}
	
	public static String list2String(List<?> in) {
		return in.toString().replace("[", "").replace("]", "");
	}
	
}
