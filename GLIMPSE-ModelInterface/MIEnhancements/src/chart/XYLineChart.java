package chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

/**
 * The class handle to create a XYLine JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class XYLineChart extends XYChart {
	public XYLineChart(String path, String graphName, String meta, String[] titles, String[] axis_name_unit, String legend,
			int[] color, int[] pColor, int[] pattern, int[] lineStrokes, String[][] annotationText, DefaultXYDataset dataset,
			int relativeColIndex, boolean ShowLineAndShape) {
		super(path, graphName, meta, titles, axis_name_unit, legend, color, pColor, pattern, lineStrokes, annotationText, dataset,
				relativeColIndex, ShowLineAndShape);

		chartClassName = "chart.XYLineChart";
		crtChart();
	}

	public XYLineChart(String path, String graphName, String meta, String[] titles, String[] axis_name_unit, String legend,
			String column, String[][] annotationText, String[][] data, int relativeColIndex) {
		super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText, data, relativeColIndex);

		chartClassName = "chart.XYLineChart";
		crtChart();
	}

	private void crtChart() {

		DrawingSupplier supplier = new DefaultDrawingSupplier();
		if (this.color != null)
			supplier = ChartUtil.setDrawingSupplier(this.chartClassName, this.color, this.lineStrokes);
		else
			supplier = (new DefaultDrawingSupplier(
					DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
					DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
					DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
					DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
					DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
		
		NumberAxis domainAxis = new NumberAxis(verifyAxisName_unit(0));
		ValueAxis rangeAxis = new NumberAxis(verifyAxisName_unit(1));
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);
		plot.setDrawingSupplier(supplier);

		setPlotProperty();
		setAxisProperty();
		RendererUtil.setRendererProperty(renderer);
		chart = new JFreeChart("", plot);
		setLegendProperty();
		for (int i = 0; i < getLineStrokes().length; i++)
			renderer.setSeriesShapesVisible(i, isShowLineAndShape());
		
		for (int i = 0; i < getLineStrokes().length ; i++)
			renderer.setSeriesStroke(i, LegendUtil.getLineStroke(getLineStrokes()[i]));
		ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
		setChartProperty();
	}
}
