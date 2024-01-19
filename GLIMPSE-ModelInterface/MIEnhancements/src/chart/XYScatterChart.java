package chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;

/**
 * The class handle to create a XYAcatter JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class XYScatterChart extends XYChart {
	public XYScatterChart(String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, int[] color, int[] pColor,
			int[] pattern, int[] lineStrokes, String[][] annotationText,
			DefaultXYDataset dataset, int relativeColIndex, boolean ShowLineAndShape) {
		super(path, graphName, meta, titles, axis_name_unit, legend,  color, pColor,
				pattern, lineStrokes, annotationText, dataset,
				relativeColIndex, ShowLineAndShape);

		chartClassName = "chart.XYScatterChart";
		crtChart();
	}

	public XYScatterChart(String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, String column, String[][] annotationText, String[][] data,
			int relativeColIndex) {
		super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText,
				data, relativeColIndex);

		chartClassName = "chart.XYScatterChart";
		crtChart();
	}

	
	private void crtChart() {
		chart = ChartFactory.createScatterPlot("", verifyAxisName_unit(0),
				verifyAxisName_unit(1), dataset, PlotOrientation.VERTICAL,
				true, true, false);
		plot = (XYPlot) chart.getPlot();
		plot.setDataset(0, dataset);
		XYItemRenderer renderer = plot.getRenderer();
		plot.setRenderer(0, renderer);
		setPlotProperty();
		setLegendProperty();
		setAxisProperty();
		RendererUtil.setRendererProperty(renderer);
		setChartProperty();
	}
}
