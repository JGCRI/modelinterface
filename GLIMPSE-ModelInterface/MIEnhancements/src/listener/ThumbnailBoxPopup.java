package listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import chart.Chart;
import graphDisplay.AChartDisplay;
import graphDisplay.BoxAndWhiskerChartPane;
import graphDisplay.DifferenceChartPane;
import graphDisplay.SumAcrossChartPane;
import graphDisplay.Transpose;

/**
 * The class handles Thumbnail Box Popup events. Referenced classes of package
 * listener: JPopupMenuShower
 * 
 * Author Action Date Flag
 * ======================================================================= 
 * TWU    created 1/2/2016
 */

public class ThumbnailBoxPopup extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final String data[] = { /*"Sum Across",*/ "Difference", // "Relative",
																// "Fit Rank",
			//"Statistics", 
			"Transpose" };
	private Chart chart[];
	private int w;
	private int gridWidth;
	private boolean sameScale;
	private JSplitPane sp;

	public ThumbnailBoxPopup(Chart chart[], int w, int gridWidth, boolean sameScale, JSplitPane sp) {
		this.chart = chart;// .clone();
		this.w = w;
		this.gridWidth = gridWidth;
		this.sameScale = sameScale;
		this.sp = sp;
		crtMenuItem();
	}

	private void crtMenuItem() {
		JMenuItem menuItem = null;
		for (int i = 0; i < data.length; i++) {
			menuItem = new JMenuItem(data[i]);
			menuItem.addActionListener(this);
			this.add(menuItem);
		}
	}

	public void actionPerformed(ActionEvent e) {
		JMenuItem source = (JMenuItem) e.getSource();
		try {
			if (chart != null) {
				if (source.getText().equalsIgnoreCase("Difference"))
					new AChartDisplay(new DifferenceChartPane(chart).getChart());
				else if (source.getText().equalsIgnoreCase("Statistics"))
					new AChartDisplay(new BoxAndWhiskerChartPane(chart).getChart());
				else if (source.getText().equalsIgnoreCase("Sum Across")) {
					Chart[] ch = new SumAcrossChartPane(chart).getChart();
					for (int i = 0; i < ch.length; i++)
						new AChartDisplay(ch[i]);
				} else if (source.getText().equalsIgnoreCase("Transpose")) {
					new Transpose(chart.clone(), w, gridWidth, sameScale, sp);
				}
			}
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (java.lang.NullPointerException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			System.out.println("other error!");
			e1.printStackTrace();
		}
		this.setVisible(false);
	}

}
