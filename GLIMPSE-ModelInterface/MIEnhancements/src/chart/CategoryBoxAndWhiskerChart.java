package chart;

import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;


/**
 * The class handle to create a statistics JFreeChart with all properties stored in Chart. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class CategoryBoxAndWhiskerChart extends CategoryChart {
	private DefaultBoxAndWhiskerCategoryDataset dataset;

	public CategoryBoxAndWhiskerChart(String path, String graphName, String id,
			String[] titles, String[] axisName_unit, String column,	String legend,		
			String[][] annotationText, ArrayList<List<String[]>> data) {
		super(path, graphName, id, titles, axisName_unit, column, null, data, -1);
		chartClassName = "chart.CategoryBoxAndWhiskerChart";
		this.legend = legend;
		dataset = (DefaultBoxAndWhiskerCategoryDataset) new MyDataset()
				.createBoxAndWhiskerCategoryDataset(data, legend.split(","),
						column.split(","));
		crtChart();
	}

	private void crtChart() {
		chart = ChartFactory.createBoxAndWhiskerChart(titles[0], "", "value",
				dataset, true);
		plot = (CategoryPlot) chart.getPlot();
		plot.setDataset(0, dataset);
		setPlotProperty();
		setLegendProperty();
		setAxisProperty();
		RendererUtil.setRendererProperty(plot.getRenderer());
		chart.getLegend().setPosition(RectangleEdge.RIGHT);
	}
}
