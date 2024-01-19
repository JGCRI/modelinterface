package chart;

import java.util.Iterator;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;


/**
 * The class handle utility functions for JFreeChart Marker. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class MarkerUtil {
	public static void createMarker(JFreeChart jfchart, Map<String, Marker> markerMap) {

		Marker m;
		Iterator<String> it = markerMap.keySet().iterator();
		for (;it.hasNext();){
			m = markerMap.get(it.next());
			if (jfchart.getPlot().getPlotType()
				.equalsIgnoreCase("Category Plot")) {				
				if (m instanceof org.jfree.chart.plot.CategoryMarker)
					jfchart.getCategoryPlot().addDomainMarker((CategoryMarker) m);
				else if (m instanceof org.jfree.chart.plot.IntervalMarker)
					jfchart.getCategoryPlot().addRangeMarker(m);
				else if (m instanceof org.jfree.chart.plot.ValueMarker)
					jfchart.getCategoryPlot().addRangeMarker(m);
			} else if (jfchart.getPlot().getPlotType()
				.equalsIgnoreCase("XY Plot")) {
				if (m instanceof org.jfree.chart.plot.CategoryMarker)
					jfchart.getXYPlot().addDomainMarker(m);
				else if (m instanceof org.jfree.chart.plot.IntervalMarker)
					jfchart.getXYPlot().addRangeMarker(m);
				else if (m instanceof org.jfree.chart.plot.ValueMarker)
					jfchart.getXYPlot().addRangeMarker(m);
			}
		}
	}
	
	public static RectangleAnchor getMarkerLabelPosition(String name){	
		if (name.equals("Bottom"))
			return RectangleAnchor.BOTTOM;
		else if (name.equals("Bottom-Left"))
			return RectangleAnchor.BOTTOM_LEFT;
		else if (name.equals("Bottom-Right"))
			return RectangleAnchor.BOTTOM_RIGHT;
		else if (name.equals("Center"))
			return RectangleAnchor.CENTER;
		else if (name.equals("Left"))
			return RectangleAnchor.LEFT;
		else if (name.equals("Right"))
			return RectangleAnchor.RIGHT;
		else if (name.equals("Top"))
			return RectangleAnchor.TOP;		
		else if (name.equals("Top-Left"))
			return RectangleAnchor.TOP_LEFT;
		else if (name.equals("Top-Right"))
			return RectangleAnchor.TOP_RIGHT;
		else 
			return null;
	}
	
	public static TextAnchor getMarkerTextLabelPosition(String name){	
		if (name.equals("Baseline-Center"))
			return TextAnchor.BASELINE_CENTER;
		else if (name.equals("Baseline-Left"))
			return TextAnchor.BASELINE_LEFT;
		else if (name.equals("Baseline-Right"))
			return TextAnchor.BASELINE_RIGHT;
		else if (name.equals("Bottom-Center"))
			return TextAnchor.BOTTOM_CENTER;
		else if (name.equals("Bottom-Left"))
			return TextAnchor.BOTTOM_LEFT;
		else if (name.equals("Bottom-Right"))
			return TextAnchor.BOTTOM_RIGHT;
		else if (name.equals("Center"))
			return TextAnchor.CENTER;
		else if (name.equals("Center-Left"))
			return TextAnchor.CENTER_LEFT;
		else if (name.equals("Center-Right"))
			return TextAnchor.CENTER_RIGHT;
		else if (name.equals("Top-Center"))
			return TextAnchor.TOP_CENTER;		
		else if (name.equals("Top-Left"))
			return TextAnchor.TOP_LEFT;
		else if (name.equals("Top-Right"))
			return TextAnchor.TOP_RIGHT;
		else if (name.equals("Half-Ascent-Center"))
			return TextAnchor.HALF_ASCENT_CENTER;		
		else if (name.equals("Half-Ascent-Left"))
			return TextAnchor.HALF_ASCENT_LEFT;
		else if (name.equals("Half-Ascent-Right"))
			return TextAnchor.HALF_ASCENT_RIGHT;
		else 
			return null;
	}

}
