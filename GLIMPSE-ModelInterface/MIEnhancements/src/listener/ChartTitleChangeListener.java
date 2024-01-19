package listener;

import org.jfree.chart.event.TitleChangeEvent;
import org.jfree.chart.event.TitleChangeListener;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import chart.Chart;

/**
 * The class handles chart title change. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class ChartTitleChangeListener implements TitleChangeListener {

	Chart chart;

	public ChartTitleChangeListener(Chart chart) {
		this.chart = chart;
	}

	@Override
	public void titleChanged(TitleChangeEvent e) {
		Title t = e.getTitle();
		if (!t.getClass().getName().equals("org.jfree.chart.title.LegendTitle")) {
			t.setBorder(0, 0, 0, 0);
		}else {
			System.out.println("LegendTitle");
			String newTitle = ((TextTitle) t).getText();
			chart.setTitles(newTitle, 0);
		}

	}
}
