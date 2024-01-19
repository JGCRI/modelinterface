package graphDisplay;

import java.awt.Cursor;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import chart.Chart;
import conversionUtil.ArrayConversion;

/**
 * The class to handle multiple charts displaying with added on functions
 * 
 * @author TWU
 *
 */
public class Thumbnail {
	private boolean debug = false;
	private JPanel jp;
	private Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
	private Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	/**
	 * Create thumb nail charts from table data.
	 *
	 * @param chartName
	 *            the name of a JFreeChart ({@code null} not permitted).
	 * @param unit
	 *            the unit label of the chart ({@code null} not permitted).
	 * @param path
	 *            the legend property file ({@code null} permitted).
	 * @param jtable
	 *            table data includes selected meta data, column and row names,
	 *            the values of column and row. ({@code null} not permitted).
	 */

	public Thumbnail(String chartName, String[] unit, String path, int cnt, JTable jtable,
			Map<String, Integer[]> metaMap, JSplitPane sp, HashMap<String,String> unitLookup) {

		sp.setCursor(waitCursor);
		if (metaMap == null)
			metaMap = ModelInterfaceUtil.getMetaIndex(jtable, cnt);
		//ModelInterfaceUtil.getDataFromTable(jtable, 4);
		String metaCol = ArrayConversion.array2String(ModelInterfaceUtil.getColumnFromTable(jtable, cnt, 2));
		// create charts for Thumbnail
		String col = ArrayConversion.array2String(ModelInterfaceUtil.getColumnFromTable(jtable, cnt, 0));
//		Chart[] chart = ThumbnailUtil.createChart(chartName, unit, col,
//				ModelInterfaceUtil.getDataFromTable(jtable, cnt, 0), metaMap,
//				ModelInterfaceUtil.getLegend(metaMap, ModelInterfaceUtil.getDataFromTable(jtable, cnt, 1)), path,
//				metaCol);
		//new version that gets 
		Chart[] chart = ThumbnailUtil2.createChart(chartName, unit, 
				ModelInterfaceUtil.getColDataFromTable(jtable, jtable.getColumnCount()-1),
				col,
				ModelInterfaceUtil.getDataFromTable(jtable, cnt, 0), metaMap,
				ModelInterfaceUtil.getLegend(metaMap, ModelInterfaceUtil.getDataFromTable(jtable, cnt, 1)), path,
				metaCol,unitLookup);
		//Dan: Using modified version (2)
		int idx = ThumbnailUtil2.getFirstNonNullChart(chart);
		if (chart[idx] != null)
			//Dan: Using modified version (2)
			jp = ThumbnailUtil2.setChartPane(chart, idx, false, true, sp);
		sp.setCursor(defaultCursor);
		if (debug)
			System.out.println("Thumbnail::Thumbnail:max memory " + Runtime.getRuntime().maxMemory() + " total: "
					+ Runtime.getRuntime().totalMemory() + " free: " + Runtime.getRuntime().freeMemory());
	}

	public JPanel getJp() {
		return jp;
	}

}
