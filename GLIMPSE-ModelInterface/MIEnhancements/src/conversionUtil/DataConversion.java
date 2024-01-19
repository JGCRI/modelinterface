package conversionUtil;

import java.awt.Dimension;
import java.awt.Font;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The class to handle a conversion between data types.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class DataConversion {
	public static String[] object2String(Object[] o) {
		String[] s = new String[o.length];
		for (int i = 0; i < o.length; i++)
			s[i] = ((String) o[i]).trim();
		return s;
	}

	public static double[][] String2Double(String[][] data) {
		double[][] d = new double[data.length][data[0].length];
		for (int i = 0; i < d.length; i++)
			for (int j = 0; j < d[i].length; j++)
				d[i][j] = Double.valueOf(data[i][j]).doubleValue();
		return d;
	}

	public static String[][] Double2String(double[][] data) {
		String[][] d = new String[data.length][data[0].length];
		for (int i = 0; i < d.length; i++)
			for (int j = 0; j < d[i].length; j++)
				d[i][j] = String.valueOf(data[i][j]);
		return d;
	}

	public static String[] Double2String(double[] data) {
		String[] d = new String[data.length];
		for (int i = 0; i < d.length; i++)
			d[i] = String.valueOf(data[i]);
		return d;
	}

	public static int[][] String2Int(String[][] data) {
		int[][] d = new int[data.length][data[0].length];
		for (int i = 0; i < d.length; i++)
			for (int j = 0; j < d[i].length; j++)
				d[i][j] = Integer.valueOf(data[i][j]).intValue();
		return d;
	}

	public static int[] String2Int(String[] data) {
		int[] d = new int[data.length];
		for (int i = 0; i < d.length; i++)
			d[i] = Integer.valueOf(data[i]).intValue();
		return d;
	}

	public static String[] int2String(int[] data) {
		String[] s = new String[data.length];
		for (int i = 0; i < s.length; i++)
			s[i] = String.valueOf(data[i]);
		return s;
	}

	public static double roundDouble(double d, int digit) {
		return Math.rint(d * Math.pow(10D, digit)) / Math.pow(10D, digit);
	}

	public static String getTheDay() {
		Calendar rightNow = Calendar.getInstance();
		java.util.Date d = rightNow.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String ds = sdf.format(d);
		return ds;
	}

	public static String getFormatDay(String s) {
		String ds = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			if (s != null && !s.equals("")) {
				java.util.Date d = sdf.parse(s);
				ds = sdf.format(d);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return ds;
	}

	public static Dimension measureString(String s, Font font) {
		return new Dimension(font.getSize() * Character.SIZE * s.length(), font.getSize() * Character.SIZE);
	}

	public static Dimension measureString(String[] s, Font font) {
		int size = 5;
		for (String s1:s)
			size = Math.min(size, 5 * s1.length());
		return new Dimension(size<20?20:size, 20);
	}
}
