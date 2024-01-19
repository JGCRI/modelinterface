package chart;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

/**
 * The class handle to create a interface to create a JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class MyChartFactory {
	private static boolean debug = false;
	
	public static Chart createChart(String className, String path, String graphName, String meta, 
			String[] titles, String[] axis_name_unit, String legend, int[] color, int[] pColor,
			int[] pattern, int[] lineStrokes, String[][] annotationText, 
			DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape, String graphType)
			throws ClassNotFoundException {

		Object[] o = {path, graphName, meta, titles, axis_name_unit, legend,  color, pColor,
				pattern, lineStrokes, annotationText, dataset, relativeColIndex, ShowLineAndShape,graphType };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart1:className: " + t.getName());
		Chart chart = (Chart) ChartUtil.creatNewInstance(t, o);
		System.runFinalization();

		return chart;
	}

	public static Chart createChart(String className, String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, int[] color, int[] pColor, int[] pattern, 
			int[] lineStrokes, String[][] annotationText, DefaultXYDataset dataset, 
			int relativeColIndex, boolean ShowLineAndShape)
			throws ClassNotFoundException {

		Object[] o = {path, graphName, meta, titles, axis_name_unit, legend,  color, pColor,
				pattern, lineStrokes, annotationText, dataset, relativeColIndex, ShowLineAndShape };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart2:className: " + t.getName());

		Chart chart = (Chart) ChartUtil.creatNewInstance(t, o);

		return chart;
	}

	// Single data set called from graphDisplayUtil
	// Dan: Step 3 - Creates a new instance of a chart
	public static Chart createChart(String className, String path, String graphName, String id, String[] titles,
			String[] axisName_unit, String legend, String column, String[][] annotationText, String[][] data,
			int relativeColIndex) throws ClassNotFoundException {

		Object[] o = { path, graphName, id.trim(), titles, axisName_unit, legend, column, annotationText, data,
				Integer.valueOf(relativeColIndex) };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart3:className: " + t.getName());
		return (Chart) ChartUtil.creatNewInstance(t, o);
	}

	// Single data set for Box and Whisker
	public static Chart createChart(String className, String path, String graphName, String id, String[] titles,
			String[] axisName_unit, String column, String[][] annotation, ArrayList<List<String[]>> data)
			throws ClassNotFoundException {

		Object[] o = { path, graphName, id, titles, axisName_unit, column, annotation, data };

		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart4:className: " + t.getName());
		return (Chart) ChartUtil.creatNewInstance(t, o);
	}
}