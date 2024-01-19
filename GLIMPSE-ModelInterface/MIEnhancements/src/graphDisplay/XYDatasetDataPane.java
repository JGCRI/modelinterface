package graphDisplay;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;

import chart.Chart;

/**
 * The class to handle displaying XY chart's data set with functions in display a chart panel. 
 * You can subset the chart by selecting data
 * Referenced classes of package graphDisplay: DataPanel
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class XYDatasetDataPane extends DataPanel {
	private static final long serialVersionUID = 1L;
	private XYDataset ds;

	public XYDatasetDataPane(JFreeChart chart)
			throws CloneNotSupportedException {
		super(chart);
		setDataset();
	}

	public XYDatasetDataPane(Chart[] charts, int id) {
		super(charts, id);
		setDataset();
	}
	
	private void setDataset() {
		getDataFromDataset();
		tableModel.setDataVector(dataValue, tableCol);
		table.getSelectionModel().addListSelectionListener(this);
		SetColumnModel();
		table.updateUI();
	}

	private void getDataFromDataset() {
		int lc = 0;
		for (int i = 0; i < copyChart[id].getXYPlot().getDatasetCount(); i++)
			lc = lc + copyChart[id].getXYPlot().getDataset(i).getSeriesCount();
		ds = copyChart[id].getXYPlot().getDataset(0);
		dataValue = new String[lc][ds.getItemCount(0) + 1];
		tableCol = new String[ds.getItemCount(0) + 1];
		tableCol[0] = copyChart[id].getXYPlot().getDomainAxis().getLabel() != null ? copyChart[id]
				.getXYPlot().getDomainAxis().getLabel()
				: "Series";
		for (int i = 0; i < lc; i++)
			dataValue[i][0] = copyChart[id].getXYPlot().getLegendItems()//.getFixedLegendItems()
					.get(lc - 1 - i).getLabel();
		for (int i = 0; i < ds.getItemCount(0); i++)
			tableCol[i + 1] = String.valueOf(ds.getXValue(0, i));

		setDigit(ds, 3);
	}

}
