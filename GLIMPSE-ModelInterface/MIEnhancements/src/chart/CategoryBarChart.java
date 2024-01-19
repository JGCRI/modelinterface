package chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

/**
 * The class handle to create a Bar JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class CategoryBarChart extends CategoryChart {
	protected BarRenderer renderer ;
	
	public CategoryBarChart(String path, String graphName, String meta, String[] titles,
			String[] axisName_unit, String legend, int[] color, int[] pColor,
			int[] pattern, int[] lineStrokes, String[][] annotationText,
			DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape,String graphType) {
		super(path, graphName, meta, titles, axisName_unit, legend,  color, pColor,
				pattern, lineStrokes, annotationText, dataset,
				relativeColIndex, ShowLineAndShape,graphType);

		chartClassName = "chart.CategoryBarChart";
		crtChart();
	}

	public CategoryBarChart(String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, String column, String[][] annotationText, String[][] data,
			int relativeColIndex) {
		super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText,
				data, relativeColIndex);

		chartClassName = "chart.CategoryBarChart";
		crtChart();
	}
	
	private void crtChart() {

		ChartFactory.setChartTheme(StandardChartTheme
				.createLegacyTheme());
		BarRenderer.setDefaultBarPainter(new StandardBarPainter(){

			private static final long serialVersionUID = 1886557876432457L;

			public void paintBarShadow(java.awt.Graphics2D g2, BarRenderer renderer, int row,
		        int column, java.awt.geom.RectangularShape bar, RectangleEdge base,
		        boolean pegShadow) {}
		});
		chart = ChartFactory.createBarChart("", verifyAxisName_unit(0),
				verifyAxisName_unit(1), dataset, PlotOrientation.VERTICAL,
				true, true, false);
		plot = (CategoryPlot) chart.getPlot();
		plot.setDataset(0, dataset);
		renderer = (BarRenderer) plot.getRenderer(0);
		renderer.setMaximumBarWidth(.30);
		setPlotProperty();
		setLegendProperty();
		setAxisProperty();
		RendererUtil.setRendererProperty(renderer);
	}
}
