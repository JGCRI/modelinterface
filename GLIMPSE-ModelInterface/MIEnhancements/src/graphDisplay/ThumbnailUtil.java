package graphDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import chart.CategoryChart;
import chart.Chart;
import chart.DatasetUtil;
import chart.MyChartFactory;
import chart.XYChart;
import conversionUtil.ArrayConversion;
import listener.IconMouseListener;

/**
 * The class to handle utility functions for thumbnail panel.
 * 
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 */

public class ThumbnailUtil {
	private static boolean debug = false;

	/**
	 * Create thumb nail charts from table data.
	 *
	 * @param chartName the name of a JFreeChart ({@code null} not permitted).
	 * @param unit      the unit label of the chart ({@code null} not permitted).
	 * @param column    the column label of the chart ({@code null} not permitted).
	 * @param tableData the data set of the chart ({@code null} not permitted).
	 * @param metaMap   range of indexes of a selected meta chart ({@code null} not
	 *                  permitted).
	 * @param legendG   the legend of the chart ({@code null} not permitted).
	 * @param path      the legend property file ({@code null} permitted).
	 * @param metaCol   the meta column label of the chart stored in Chart object
	 *                  use for export data ({@code null} not permitted).
	 * @return array of charts with different meta selections
	 */

	public static Chart[] createChart(String chartName, String[] unit, String column, String[][] tableData,
			Map<String, Integer[]> metaMap, String[] legendG, String path, String metaCol) {

		String[] keys = metaMap.keySet().toArray(new String[0]);
		ArrayList<Chart> chartL = new ArrayList<Chart>();

		for (int i = 0; i < keys.length; i++) {// each meta selected
			try {
				Integer[] range = metaMap.get(keys[i]);
				if (debug)
					System.out.println("ThumbnailUtil::createChart:key: " + i + " kl: " + keys.length + " " + keys[i]
							+ " dl: " + tableData[0].length + " lc: " + legendG.length + " legend: "
							+ Arrays.toString(legendG) + " range: " + Arrays.toString(range) + " mcol: " + metaCol);
				if (range != null) {
					//this may be problematic if data are not together on the table. Modify to copy specific values?
					String[][] temp = Arrays.copyOfRange(tableData, range[0], range[1] + 1);
					String[][] data = new String[temp.length][temp[0].length - 1];
					String[] l = new String[temp.length];

					for (int k = 0; k < l.length; k++) {
						l[k] = temp[k][0].trim().trim().replace(",", "-");
						data[k] = Arrays.copyOfRange(temp[k], 1, temp[k].length);
						if (debug)
							System.out.println("ThumbnailUtil::createChart:data: " + l.length + " " + l[k] + " data: "
									+ Arrays.toString(data[k]));
					}
					String stitle = getSubTitle(keys, keys[i], metaCol);

					Chart tempC = MyChartFactory.createChart("chart.CategoryLineChart", path,
							chartName.replace(" ", ""), keys[i] + "|" + metaCol, new String[] { chartName, stitle },
							unit, ArrayConversion.array2String(l), column, null, data, -1);
					
					if (debug)
						System.out.println("ThumbnailUtil::createChart:chart:class " + tempC.getChartClassName()
								+ " plot: " + tempC.getChart().getPlot().getPlotType());
					chartL.add(tempC);
				} else
					chartL.add(new Chart(new String[] { chartName, keys[i] }));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.out.println("ThumbnailUtil::createChart:Null Map ");
				e.printStackTrace();
			}
		}
		Chart[] chart = new Chart[chartL.size()];
		if (chartL.size() > 0)
			for (int i = 0; i < chart.length; i++) {
				chart[i] = chartL.get(i);
			}

		if (debug) {
			System.out.println("ThumbnailUtil::createChart:chart:length " + chart.length);
			System.out.println("ThumbnailUtil::createChart:max memory " + Runtime.getRuntime().maxMemory() + " total: "
					+ Runtime.getRuntime().totalMemory() + " free: " + Runtime.getRuntime().freeMemory());
		}
		
		return chart;
	}

	// transpose
	public static Chart[] createChart(String chartName, String[] unit, String legend, String column,
			ArrayList<String[][]> data, String[] keys) {

		// columns selected
		if (debug)
			System.out.println(
					"createChart:legend: " + legend + " key: " + Arrays.toString(keys) + " dl: " + data.size());

		ArrayList<Chart> chartL = new ArrayList<Chart>();
		for (int i = 0; i < data.size(); i++) {// each meta selected
			try {
				chartL.add(MyChartFactory.createChart("chart.CategoryLineChart", null, chartName.replace(" ", ""),
						keys[i].trim() + "| ", new String[] { chartName, keys[i].trim() }, unit, legend, column, null,
						data.get(i), -1));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.out.println("map Null: ");
				e.printStackTrace();
				//break;
			} catch (Exception e) {
				System.out.println("other exception: "+e);
			}
		}
		Chart[] chart = new Chart[chartL.size()];
		if (chartL.size() > 0)
			for (int i = 0; i < chart.length; i++)
				chart[i] = chartL.get(i);
		return chart;

	}

	protected static int legendResourceExist(String[] data, String key) {
		int idx = Arrays.asList(data).indexOf(key);
		return idx;
	}

	protected static boolean keyExist(String[] qualifierData, String key) {
		boolean b = true;
		String[] keyPart = key.split(" ");
		for (int i = 0; i < keyPart.length; i++) {
			if (debug)
				System.out.println("createChart:key: " + keyPart.length + " " + keyPart[i] + " qualifier: "
						+ Arrays.toString(qualifierData));
			if (!Arrays.asList(qualifierData).contains(keyPart[i].trim())) {
				b = false;
				break;
			}
		}
		return b;
	}

	/**
	 * Create region chart.
	 *
	 * @param chartClassName Chart class name ({@code null} not permitted).
	 * @param path           the legend property file ({@code null} permitted).
	 * @param chartName      the name of chart ({@code null} not permitted).
	 * @param meta           the meta data of the chart ({@code null} not
	 *                       permitted).
	 * @param title          the title of the chart ({@code null} not permitted).
	 * @param unit           the unit label of the chart ({@code null} not
	 *                       permitted).
	 * @param legend         the legend of the chart ({@code null} not permitted).
	 * @param column         the column label of the chart ({@code null} not
	 *                       permitted).
	 * @param data           the data set of the chart ({@code null} not permitted).
	 * @return array of charts in the same region
	 */

	public static Chart[] createChart(String chartClassName, String path, String chartName, String meta, String title,
			String[] unit, String legend, String column, String[][] data) {

		int d = legend.split(",").length;
		int n = data.length / d;
		Chart[] chart = new Chart[n];
		String[] metaStr = meta.split(";");
		try {
			for (int i = 0; i < chart.length; i++)
				chart[i] = MyChartFactory.createChart(chartClassName, path, chartName, metaStr[i], title.split("\\|"),
						unit, legend, column, null, Arrays.copyOfRange(data, i * d, (i + 1) * d), -1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return chart;
	}

	/**
	 * create charts for thumb nail from option pane.
	 *
	 * @param cn            class name ({@code null} not permitted).
	 * @param relativeIndex index to a relative column ({@code null} not permitted).
	 * @param chart         array of existing charts
	 * @return array of charts with a new chart class name
	 */

	public static Chart[] createChart(String cn, int relativeIndex, Chart[] chart,String chartType) {
		Chart[] chart1 = new Chart[chart.length];
		for (int i = 0; i < chart.length; i++) {
			try {
				if (cn.contains("Category")) {
					DefaultCategoryDataset dataset = null;
					if (chart[i].getChartClassName().contains("XY"))
						dataset = DatasetUtil.XYDataset2CategoryDataset(((XYChart) chart[i]).getDataset());
					else if (chart[i].getChartClassName().contains("Category"))
						dataset = ((CategoryChart) chart[i]).getDataset();

					chart1[i] = MyChartFactory.createChart(cn, chart[i].getPath(), chart[i].getGraphName(),
							chart[i].getMeta() + "|" + chart[i].getMetaCol(), chart[i].getTitles(),
							chart[i].getAxis_name_unit(), chart[i].getLegend(), chart[i].getColor(),
							chart[i].getpColor(), chart[i].getPattern(), chart[i].getLineStrokes(),
							chart[i].getAnnotationText(), dataset, relativeIndex, chart[i].isShowLineAndShape(),chartType);
				} else {
					DefaultXYDataset dataset = null;
					if (chart[i].getChartClassName().contains("Category"))
						dataset = DatasetUtil.CategoryDataset2XYDataset(((CategoryChart) chart[i]).getDataset());
					else if (chart[i].getChartClassName().contains("XY"))
						dataset = DatasetUtil.createXYDataset(chart[i].getChart().getXYPlot().getDataset(),
								chart[i].getRelativeColIndex());

					chart1[i] = MyChartFactory.createChart(cn, chart[i].getPath(), chart[i].getGraphName(),
							chart[i].getMeta() + "|" + chart[i].getMetaCol(), chart[i].getTitles(),
							chart[i].getAxis_name_unit(), chart[i].getLegend(), chart[i].getColor(),
							chart[i].getpColor(), chart[i].getPattern(), chart[i].getLineStrokes(),
							chart[i].getAnnotationText(), dataset, relativeIndex, chart[i].isShowLineAndShape());
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (java.lang.NullPointerException e1) {
				chart1[i] = chart[i];
			}
		}
		return chart1;
	}

	/**
	 * Remove the content if content exists in the JPanel.
	 * 
	 * @param jp A JPanel to hold contents ({@code null} not permitted).
	 */

	public static void validateChartPane(JPanel jp) {
		BorderLayout bl = (BorderLayout) jp.getLayout();
		if (debug)
			System.out.println("ThumbnailUtil::validateChartPane:bl " + bl.getLayoutComponent("Center"));
		if (bl.getLayoutComponent("Center") != null) {
			jp.remove(bl.getLayoutComponent("Center"));
		}
	}

	/**
	 * Compute the sub-title of a chart.
	 * 
	 * @param keys      Keys of charts ({@code null} not permitted).
	 * @param keyString An Key String of a chart ({@code null} not permitted).
	 * @return region+senerio+date index(from the most current to the oldest)
	 */

	private static String getSubTitle(String[] keys, String keyString, String mCol) {

		
		// Dan: added this as approach to get labels correct when depth in subsector
		keyString = keyString.replace(",depth=1", "");
		
		String temp = keyString.split(",")[0];
		int ks = temp.split(" ").length;
		String scen = temp.split(" ")[ks - 1]; 
		String region = "";
		if (ks>=2) region = temp.split(" ")[ks - 2];
		String sect ="";
		int num=temp.indexOf(region);
		
		if (num>0) {
			sect=temp.substring(0,num-1).trim();
			scen+="\n"+sect;
		}
		
		
		String dateStr = "";
		if (keyString.contains(",")) {
			dateStr = keyString.split(",")[1];
		}
		int dateIndex = 0;
		if (dateStr != "") {
			for (String s : Arrays.asList(keys)) {
				// Dan: added this next line to address problem generating labels with depth
				s = s.replace(",depth=1", "");
				if (s.contains(temp))
					if (!s.contains(dateStr))
						dateIndex++;
					else
						break;
			}
		}
		String rtn_str = scen;
		if (dateIndex > 0) rtn_str += dateIndex;
		if (region.length()>0) rtn_str += "\nregion: " + region;
		return rtn_str;

	}

	/**
	 * Compute the maximum range value of input charts.
	 * 
	 * @param chart An array of Chart object ({@code null} not permitted).
	 * @return maximum range value of all charts
	 */

	public static double setMax(Chart[] chart) {
		double max = 0;
		for (int i = 0; i < chart.length && chart[i] != null; i++) {
			String s = chart[i].getChartClassName();
			if (s != null)
				if (s.contains("XY"))
					max = Math.max(chart[i].getChart().getXYPlot().getRangeAxis().getUpperBound(), max);
				else
					max = Math.max(chart[i].getChart().getCategoryPlot().getRangeAxis().getUpperBound(), max);
		}
		return max;
	}

	/**
	 * Compute the minimum range value of input charts.
	 * 
	 * @param chart An array of Chart object ({@code null} not permitted).
	 * @return minimum range value of all charts
	 */

	public static double setMin(Chart[] chart) {
		double min = 0;
		for (int i = 0; i < chart.length && chart[i] != null; i++) {
			String s = chart[i].getChartClassName();
			if (s != null)
				if (s.contains("XY"))
					min = Math.min(chart[i].getChart().getXYPlot().getRangeAxis().getLowerBound(), min);
				else
					min = Math.min(chart[i].getChart().getCategoryPlot().getRangeAxis().getLowerBound(), min);
		}
		return min;
	}

	@SuppressWarnings("rawtypes")
	public static Dimension getChartDimensions(JFreeChart jfreechart) {
		int i = 0;
		for (Iterator iterator = jfreechart.getPlot().getLegendItems().iterator(); iterator.hasNext();)
			i += ((LegendItem) iterator.next()).getLabel().length();

		if (i <= 500)
			return new Dimension(350, 350);
		else
			return new Dimension(350, 350 + (i - 500) / 2);
	}

	/**
	 * Compute a fixed image size for a GridLayout View.
	 *
	 * @param x         width of parent container ({@code null} not permitted).
	 * @param gridWidth number of columns in a grid layout ({@code null} not
	 *                  permitted).
	 * 
	 * @return image width in a grid
	 * 
	 */

	public static int computeFixGridLayoutViewSize(int x, int gridWidth) {
		int w;
		if (x > 0 && gridWidth > 1) {
			w = (x / gridWidth / 2) - 20;
			w = w < 180 ? 180 : w;
			w = w > 320 ? 320 : w;
		} else
			w = 320;
		return w;
	}

	/**
	 * Compute GridLayout View Size.
	 *
	 * @param x         width of parent container ({@code null} not permitted).
	 * @param gridWidth number of columns in a grid layout ({@code null} not
	 *                  permitted).
	 * 
	 * @return image width in a grid
	 * 
	 */

	public static int computeGridLayoutViewSize(int x, int gridWidth) {
		int w;
		// one column grid, use 1/4 of parent width - scroll bar size as image
		// width
		if (gridWidth < 2)
			w = (int) (x * 0.25 - 20);
		// else, use 2/5 of parent width - scroll bar size divide by 2 as image
		// width
		else
			w = (int) ((x * 0.4 - 20) / 2);

		return w;
	}

	public static JPanel setChartPane(Chart[] chart, int w, int gridWidth, boolean sameScale, boolean transpose) {
		if (debug)
			System.out.println(
					"ThumbnailUtil::setChartPane:cl " + chart.length + " gridWidth: " + gridWidth + " sameScale: "
							+ sameScale + " meta: " + chart[ThumbnailUtil.getFirstNonNullChart(chart)].getMeta());

		GridLayout gl = new GridLayout(0, gridWidth);
		gl.setHgap(0);
		gl.setVgap(0);

		JPanel chartPane = new JPanel(gl);
		double max = setMax(chart);
		double min = setMin(chart);

		for (int i = 0; i < chart.length; i++) {
			if (debug)
				System.out.println("ThumbnailUtil::setChartPane:meta: " + chart[i].getMeta() + " i: " + i);
			IconMouseListener iconListener = new IconMouseListener(chart, i);
			JButton jb = null;
			try {
				jb = buttonIcon(chart[i], chart.length - 1 - i, w, max, min, sameScale, transpose, iconListener);
			} catch (java.lang.OutOfMemoryError e2) {
				System.out.println("buttonIcon:java.lang.OutOfMemoryError: ");
				JOptionPane.showMessageDialog(null, "Too many charts to be created. No enough momery ", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				chartPane.removeAll();
				Runtime.getRuntime().gc();
				return null;
			}
			jb.setMargin(new Insets(0, 0, 0, 0));
			jb.setBackground(Color.lightGray);
			jb.setName(String.valueOf(i));
			jb.setPreferredSize(new Dimension(w, w));
			chartPane.add(jb);
		}
		chartPane.setSize(w * gridWidth, w * gridWidth);
		return chartPane;

	}

	public static JButton buttonIcon(Chart chart, int idx, int w, double max, double min, boolean sameScale,
			boolean transpose, IconMouseListener iconListener) {

		JButton jb = new JButton();
		JFreeChart freeChart = null;
		boolean category = false;
		try {
			if (chart.getChartClassName().contains("Category"))
				category = true;
			freeChart = chart.getChart();
		} catch (java.lang.IllegalStateException e) {
			System.out.println("java.lang.IllegalStateException");
		} catch (java.lang.NullPointerException e1) {
			System.out.println("buttonIcon:chart:class " + freeChart.getTitle().getText() + " plot: "
					+ freeChart.getPlot().getPlotType());
		}

		if (freeChart != null) {
			if (category)
				freeChart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			else
				freeChart.getXYPlot().getDomainAxis().setLabelAngle(90);

			if (sameScale) {
				if (category) {
					freeChart.getCategoryPlot().getRangeAxis().setUpperBound(max);
					freeChart.getCategoryPlot().getRangeAxis().setLowerBound(min);
				} else {
					freeChart.getXYPlot().getRangeAxis().setUpperBound(max);
					freeChart.getXYPlot().getRangeAxis().setLowerBound(min);
				}
			} else {
				if (category)
					freeChart.getCategoryPlot().getRangeAxis().setAutoRange(true);
				else
					freeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			}

			freeChart.getLegend().visible = false;
			freeChart.getTitle().setFont(new Font("Arial", 1, 12));
			//freeChart.getTitle().setVisible(true);
			freeChart.getTitle().setVisible(false);//Dan: modified to reduce amount of text on Thumbnails
			for (int j = 0; j < freeChart.getSubtitleCount()
					&& !(freeChart.getSubtitle(j) instanceof org.jfree.chart.title.LegendTitle); j++) {
				((TextTitle) freeChart.getSubtitle(j)).setFont(new Font("Arial", 1, 11));

				// change for showing subtitles always
				// for (int j = 0; j < freeChart.getSubtitleCount(); j++) {
				// if (j == 0 && transpose)
				freeChart.getSubtitle(j).setVisible(true);
				// else
				// freeChart.getSubtitle(j).setVisible(false);
			}

			ChartUtils.applyCurrentTheme(freeChart);

			try {
				BufferedImage thumb1 = freeChart.createBufferedImage(w, w, BufferedImage.TYPE_INT_ARGB, null);
				ImageIcon image1 = new ImageIcon(thumb1);
				jb.setIcon(image1);
				jb.setName(String.valueOf(idx));
			} catch (java.lang.IllegalStateException e) {
				System.out.println("buttonIcon:java.lang.IllegalStateException");
			} catch (java.lang.NullPointerException e1) {
				System.out.println("buttonIcon:java.lang.NullPointerException");
			}
			jb.addMouseListener(iconListener);
			jb.setToolTipText(chart.getMeta());
		} else {
			if (!transpose)
				jb.setText(getEmptyChartDesc(chart.getTitles()));
		}

		return jb;
	}

	public static String getEmptyChartDesc(String[] titles) {
		String pstyle = "<style type='text/css'> p{font-family: Verdana;font-size:10;font-weight: plan;}</style>";
		String s = "<html>" + pstyle + "<p>No Data For: </p><p>";
		s = s + titles[0] + "</p><p>" + titles[1] + "</p></html>";
		return s;
	}

	public static JPanel setChartPane(Chart[] chart, int firstNonNullidx, boolean sameScale, boolean transpose,
			JSplitPane sp) {
		int gridWidth = 2;
		//Dan: Using modified version (2)
		int w = ThumbnailUtil2.computeFixGridLayoutViewSize(sp.getSize().width, gridWidth);
		JPanel chartPane = setChartPane(chart, w, gridWidth, sameScale, transpose);
		JPanel jp = null;
		if (chartPane != null) {
			jp = new JPanel(new BorderLayout());
			jp.setMinimumSize(new Dimension(320, sp.getHeight()));
			jp.setBackground(Color.GREEN);
			jp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jp.setName(chart.length + "_" + chart[firstNonNullidx].getGraphName());
			new OptionsArea(jp, chart, gridWidth, false, sp);
			jp.add(chartPane, BorderLayout.CENTER);
			jp.updateUI();
		}
		return jp;
	}

	public static int getFirstNonNullChart(Chart[] chart) {
		int idx = -1;
		for (int i = 0; i < chart.length; i++) {
			if (chart[i].getChart() != null) {
				idx = i;
				break;
			}
		}
		return idx;
	}
}
