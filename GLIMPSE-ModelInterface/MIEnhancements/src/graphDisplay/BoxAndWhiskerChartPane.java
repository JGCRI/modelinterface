package graphDisplay;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import chart.CategoryBoxAndWhiskerChart;
import chart.Chart;
import chart.DatasetUtil;

/**
 * The class to handle statistics chart. It can be displayed in subset of series
 * or data range.
 * 
 * Author Action Date Flag
 * ======================================================================= 
 * TWU    created 1/2/2016
 */

public class BoxAndWhiskerChartPane {

	private Chart chart;

	public BoxAndWhiskerChartPane(Chart[] charts) throws ClassNotFoundException {

		ArrayList<List<String[]>> d = new ArrayList<List<String[]>>();
		try {
			d = DatasetUtil.getStatisticsData(charts);
		} catch (java.lang.IndexOutOfBoundsException e1) {
			d.clear();
		}
		if (d.isEmpty()) {
			JOptionPane.showMessageDialog(null, "No Support for diferent number of technologies for each chart",
					"Information", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String id = "";
		for (int i = 0; i < charts.length; i++)
			id = "," + id + charts[i].getMeta() + "|" + charts[i].getMetaCol();
		chart = new CategoryBoxAndWhiskerChart(charts[0].getPath(), "BoxAndWhisker_" + charts[0].getGraphName(), id,
				charts[0].getTitles(), charts[0].getAxis_name_unit(),
				// DatasetUtil.getChartColumn(charts[0].getChart()),
				charts[0].getChartColumn(), charts[0].getChartRow(),
				// DatasetUtil.getChartRow(charts[0].getChart()),
				null, d);

	}

	public Chart getChart() {
		return chart;
	}

}
