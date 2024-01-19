package listener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import chart.Chart;
import graphDisplay.AChartDisplay;
 
/**
 * The class handles thumbnail panel event. Referenced classes of package listener:
 *           ThumbnailBoxPopup
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class IconMouseListener extends MouseAdapter {
	private Chart chart[];
	private int id;

	public IconMouseListener(Chart chart[], final int id) {
		this.chart = chart;
		this.id = id;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			new AChartDisplay(chart, id);
		}
	}

	@Override
	public void mouseExited(MouseEvent mouseevent) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

}
