package chartOptions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.TexturePaint;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import chart.Chart;
import chart.LegendUtil;

/**
 * The class to handle legend color change. Referenced classes of package
 * graphDisplay: AChartDisplay, DataPanel
 * 
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 */

public class ColorChooser4DynamicModifyColor extends JPanel implements ChangeListener {
	private static final long serialVersionUID = 1L;
	private JColorChooser tcc;
	private Chart chart; // event chart
	private JButton jb;
	private int keySeries;
	private TexturePaint paint;
	private int color;
	private JDialog dialog;
	protected boolean debug = false;

	public ColorChooser4DynamicModifyColor(Chart chart, JButton jb) {
		super(new BorderLayout());
		this.chart = chart;
		keySeries = Integer.valueOf(jb.getName().trim()).intValue();
		this.jb = jb;
		tcc = new JColorChooser();
		tcc.getSelectionModel().addChangeListener(this);
		tcc.setBorder(BorderFactory.createTitledBorder("Choose Graph Color"));
		tcc.remove(1);
		add(tcc, "Last");
		Object[] options = { "Ok" };
		JOptionPane pane = new JOptionPane(tcc, -1, 0, null, options, options[0]);
		dialog = pane.createDialog("Select a legend Paint");
		dialog.setLayout(null);
		dialog.setResizable(true);
		dialog.setVisible(true);
	}

	public void stateChanged(ChangeEvent e) {
		if (chart == null)
			return;

		if (debug)
			System.out.println(
					"ColorChooser4DynamicModifyColor::stateChanged:chartColor: " + Arrays.toString(chart.getColor()));
		color = tcc.getColor().getRGB();
		paint = LegendUtil.getTexturePaint(tcc.getColor(), new Color(chart.getpColor()[keySeries]),
				chart.getPattern()[keySeries], chart.getLineStrokes()[keySeries]);

		SetModifyChanges.updateButton(jb, paint);
	}

	public TexturePaint getPaint() {
		return paint;
	}

	public int getColor() {
		return color;
	}

}
