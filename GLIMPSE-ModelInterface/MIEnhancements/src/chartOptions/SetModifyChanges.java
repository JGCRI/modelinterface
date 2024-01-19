package chartOptions;

import java.awt.Color;
import java.awt.Image;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import chart.Chart;
import chart.LegendUtil;
import conversionUtil.ArrayConversion;

/**
 * The class to handle a legend has been modified.
 * 
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 */

public class SetModifyChanges {
	protected static boolean debug = false;

	public static void setLegendChanges(Chart[] chart, int id, JTextField tf) {
		for (int i = 0; i < chart.length; i++) {
			String[] legend = chart[i].getLegend().split(",");
			int idx = Arrays.asList(legend).indexOf(legend[Integer.valueOf(tf.getName().trim()).intValue()].trim());
			
			if (idx > -1) {
				legend[idx] = tf.getText().trim();
				chart[i].setLegend(ArrayConversion.array2String(legend));
				setLegenditemcollection(chart[i], legend, chart[i].getPaint());
				setModifyChanges(chart[i], chart[i].getPaint(), idx);
				if (debug)
					System.out.println("SetModifyChanges::setLegendChanges:legend " + chart[i].getLegend());
				ChartUtils.applyCurrentTheme(chart[id].getChart());
			}
		}
	}

	public static void setColorChanges(Chart[] chart, int id1, String changeColLegend, int theColor,
			TexturePaint paint) {
		for (int i = 0; i < chart.length; i++) {
			String[] legend = chart[i].getLegend().split(",");
			int idx = Arrays.asList(legend).indexOf(changeColLegend);

			if (idx > -1) {
				TexturePaint[] color = chart[i].getPaint();
				color[idx] = paint;
				chart[i].setPaint(color);
				chart[i].setColor(theColor, idx);
				setLegenditemcollection(chart[i], legend, color);
				setModifyChanges(chart[i], color, idx);
				if (debug)
					System.out.println(
							"SetModifyChanges::setLegendChanges:color " + Arrays.toString(chart[i].getColor()));
			}
		}
	}

	public static void setPatternChanges(Chart[] chart, int id, String changeColLegend, JTextField tf, JButton jb) {

		for (int i = 0; i < chart.length; i++) {
			String[] legend = chart[i].getLegend().split(",");
			int idx = Arrays.asList(legend).indexOf(changeColLegend);

			if (idx > -1) {
				int pattern = Integer.parseInt(tf.getText().trim());
				TexturePaint paint = LegendUtil.getTexturePaint(new Color(chart[id].getColor()[idx]), Color.black,
						pattern, 0);
				TexturePaint[] tp = chart[id].getPaint();
				tp[idx] = paint;
				chart[i].setPaint(tp);
				chart[i].setPattern(pattern, idx);
				if (i == id)
					SetModifyChanges.updateButton(jb, chart[i].getPaint()[idx]);
				setLegenditemcollection(chart[i], legend, tp);
				setModifyChanges(chart[i], tp, idx);
			}
		}
	}

	public static JFreeChart setModifyChanges(Chart chart, TexturePaint[] tp, int idx) {
		AbstractRenderer renderer = null;
		JFreeChart jfchart = chart.getChart();

		if (jfchart.getPlot().getPlotType().contains("Pie")) {
			PiePlot plot = (PiePlot) jfchart.getPlot();
			String label = plot.getLegendItems().get(idx).getLabel();
			plot.setSectionPaint(label, tp[idx]);
		} else {
			if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
				CategoryPlot plot = jfchart.getCategoryPlot();
				renderer = (AbstractRenderer) plot.getRenderer();
			} else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
				XYPlot plot = (XYPlot) jfchart.getPlot();
				renderer = (AbstractRenderer) plot.getRenderer();
			}

			if (idx < tp.length)
				renderer.setSeriesPaint(idx, tp[idx]);
		}
		jfchart.getLegend().visible = true;
		ChartUtils.applyCurrentTheme(jfchart);
		return jfchart;
	}

	public static void setLegenditemcollection(Chart chart, String[] legend, TexturePaint[] color) {

		if (chart.getChartClassName().contains("Category")) {
			chart.getChart().getCategoryPlot().setFixedLegendItems(LegendUtil.crtLegenditemcollection(legend, color));
		} else if (chart.getChartClassName().contains("XY")) {
			chart.getChart().getXYPlot().setFixedLegendItems(LegendUtil.crtLegenditemcollection(legend, color));
		} else  //pie
			for (int i = 0; i < color.length; i++)
				((PiePlot)chart.getChart().getPlot()).setSectionPaint(legend[i], color[i]);
	}

	public static void setStrokeChanges(Chart[] chart, int id, String changeColLegend, JTextField tf) {
		if (!chart[id].getChartClassName().contains("Line"))
			return;

		int idx = Integer.valueOf(tf.getName().trim());
		int[] ls = chart[id].getLineStrokes();
		ls[idx] = Integer.parseInt(tf.getText().trim());
		for (int i = 0; i < chart.length; i++) {
			chart[i].setLineStrokes(ls);
			JFreeChart jfchart = chart[i].getChart();
			if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
				CategoryPlot plot = jfchart.getCategoryPlot();
				LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
				renderer.setSeriesStroke(idx, LegendUtil.getLineStroke(ls[idx]));
			} else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
				XYPlot plot = jfchart.getXYPlot();
				XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
				renderer.setSeriesStroke(idx, LegendUtil.getLineStroke(ls[idx]));
			}
			ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
			ChartUtils.applyCurrentTheme(jfchart);
		}
	}

	public static void updateButton(JButton jb, Paint paint) {
		ImageIcon icon = new ImageIcon(((TexturePaint) paint).getImage());
		Image image = icon.getImage();
		image = image.getScaledInstance(80, 20, 4);
		icon.setImage(image);
		jb.setIcon(icon);
	}

	public static void setLineAndShapeChanges(JFreeChart jfchart, boolean lineAndShape) {
		if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
			CategoryPlot plot = jfchart.getCategoryPlot();
			LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
			for (int i = 0; i < plot.getLegendItems().getItemCount(); i++)
				((LineAndShapeRenderer) renderer).setSeriesShapesVisible(i, lineAndShape);
		} else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
			XYPlot plot = jfchart.getXYPlot();
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
			for (int i = 0; i < plot.getLegendItems().getItemCount(); i++)
				((XYLineAndShapeRenderer) renderer).setSeriesShapesVisible(i, lineAndShape);
		}
		ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
		ChartUtils.applyCurrentTheme(jfchart);
	}

}
