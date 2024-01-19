package listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import chart.Chart;
import chartOptions.SetModifyChanges;

/**
 * The class handles line and shape options. Referenced classes of package listener:
 *            JPopupMenuShower
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class LineAndShapePopup implements ActionListener {

	private MouseListener mouseListener;
	private boolean lineAndShape;
	private Chart chart;
	public JPopupMenu popup;

	public LineAndShapePopup(JTextField jtf, Chart chart) {
		this.chart = chart;
		popup = new JPopupMenu();
		popup.add(createMenuItem("Line and Shape"));
		popup.add(createMenuItem("Line without Shape"));
		mouseListener = new JPopupMenuShower(popup);
		jtf.addMouseListener(mouseListener);
	}

	public boolean isLineAndShape() {
		return lineAndShape;
	}

	JMenuItem createMenuItem(String name) {
		JMenuItem menuItem = new JMenuItem(name);
		menuItem.addActionListener(this);
		return menuItem;
	}

	public void actionPerformed(ActionEvent e) {
		if (chart.getChart().getPlot().getPlotType().contains("Category"))
			if (!(chart.getChart().getCategoryPlot().getRenderer() instanceof LineAndShapeRenderer)) {
				JOptionPane.showMessageDialog(null, "Support for Line Chart", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

		if (chart.getChart().getPlot().getPlotType().contains("XY"))
			if (!(chart.getChart().getXYPlot().getRenderer() instanceof XYLineAndShapeRenderer)) {
				JOptionPane.showMessageDialog(null, "Support for Line Chart", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

		JMenuItem source = (JMenuItem) e.getSource();
		if (source.getText().equalsIgnoreCase("Line and Shape"))
			lineAndShape = true;
		else
			lineAndShape = false;
		chart.setShowLineAndShape(lineAndShape);
		SetModifyChanges.setLineAndShapeChanges(chart.getChart(), lineAndShape);
	}

}
