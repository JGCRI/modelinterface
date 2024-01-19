package chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

/**
 * The class handle to create a StackedBar JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class CategoryStackedBarChart extends CategoryChart {
	public CategoryStackedBarChart(String path, String graphName, String meta,
			String[] titles, String[] axis_name_unit, String legend, int[] color, int[] pColor,
			int[] pattern, int[] lineStrokes, String[][] annotationText,
			DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape,String graphType) {
		super(path, graphName, meta, titles, axis_name_unit, legend,  color, pColor,
				pattern, lineStrokes, annotationText, dataset,
				relativeColIndex, ShowLineAndShape,graphType);

		chartClassName = "chart.CategoryStackedBarChart";
		crtChart();
	}

	public CategoryStackedBarChart(String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, String column, String[][] annotationText, 
			String[][] data, int relativeColIndex) {
		super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText,
				data, relativeColIndex);

		chartClassName = "chart.CategoryStackedBar";
		crtChart();
	}

	
	private void crtChart() {
		ChartFactory.setChartTheme(StandardChartTheme
				.createLegacyTheme());
		BarRenderer.setDefaultBarPainter(new StandardBarPainter(){

			private static final long serialVersionUID = 1L;

			public void paintBarShadow(java.awt.Graphics2D g2, BarRenderer renderer, int row,
		        int column, java.awt.geom.RectangularShape bar, RectangleEdge base,
		        boolean pegShadow) {}
		});
		chart = ChartFactory.createStackedBarChart("", verifyAxisName_unit(0),
				verifyAxisName_unit(1), dataset, PlotOrientation.VERTICAL,
				true, true, false);
		plot = (CategoryPlot) chart.getPlot();
		plot.setDataset(0, dataset);
		StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
		plot.setRenderer(0, renderer);
		setPlotProperty();
		setLegendProperty();
		setAxisProperty();
		RendererUtil.setRendererProperty(renderer);
		setChartProperty();
	}
}
