package chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * The class handle to create a Area JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class CategoryAreaChart extends CategoryChart {
	
	public CategoryAreaChart(String path, String graphName, String meta, String[] titles,
			String[] axisName_unit, String legend, int[] color, int[] pColor,
			int[] pattern, int[] lineStrokes, String[][] annotationText,
			DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape,String graphType) {
		super(path, graphName, meta, titles, axisName_unit, legend,  color, pColor,
				pattern, lineStrokes, annotationText, dataset,
				relativeColIndex, ShowLineAndShape,graphType);

		chartClassName = "chart.CategoryAreaChart";
		crtChart();
	}

	public CategoryAreaChart(String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, String column, String[][] annotationText, String[][] data,
			int relativeColIndex) {
		super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText,
				data, relativeColIndex);

		chartClassName = "chart.CategoryAreaChar";
		crtChart();
	}
	
	private void crtChart() {
		chart = ChartFactory.createAreaChart("", verifyAxisName_unit(0),
				verifyAxisName_unit(1), dataset, PlotOrientation.VERTICAL,
				true, true, false);
		plot = (CategoryPlot) chart.getPlot();
		plot.setDataset(0, dataset);
		AreaRenderer renderer = (AreaRenderer) plot.getRenderer(0);
		plot.setRenderer(0, renderer);
		setPlotProperty();
		setLegendProperty();
		setAxisProperty();
		RendererUtil.setRendererProperty(renderer);
		setChartProperty();
	}
}
