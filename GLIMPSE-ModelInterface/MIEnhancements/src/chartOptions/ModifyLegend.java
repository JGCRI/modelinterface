package chartOptions;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jfree.chart.JFreeChart;

import chart.Chart;
import chart.LegendUtil;
import graphDisplay.CreateComponent;
import listener.LineAndShapePopup;

public class ModifyLegend extends JDialog {
	private static final long serialVersionUID = 1L;
	private Chart chart;
	private Chart[] charts;
	private String[] legend;
	private int id;
	private JFreeChart jfchart;
	private JTextField jtf;
	// legend 0, Color 1, Pattern 2, Line Stroke
	public int eventApply;
	public JDialog cancelDialog;
	private JButton jbColor;
	private JTextField jtfChanged;
	private String changeColLegend;
	private TexturePaint changeColPaint;
	private int changeColColor;

	public ModifyLegend(Chart[] charts, int id) {
		if (charts == null)
			return;
		
		this.charts = charts;// .clone();
		this.id = id;
		this.chart = charts[id];
		cancelDialog = this;
		setLegendUI();
	}

	private void setLegendUI() {
		GridBagLayout gridbag = new GridBagLayout();
		JPanel jp = new JPanel();
		jp.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(1, 10, 5, 50);
		c.gridheight = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		setColumnLabel(gridbag, jp);
		legend = chart.getLegend().split(",");

		for (int j = 0; j < legend.length; j++) {
			c = new GridBagConstraints();
			c.fill = 1;
			String name = String.valueOf(j);
			JButton jb = CreateComponent.crtJButton(name, (ImageIcon) null);
			jtf = CreateComponent.crtJTextField(name, legend[j], j);
			jtf.setToolTipText("0");
			jtf.setScrollOffset(10);
			jtf.getDocument().addDocumentListener(new MyDocumentListener(jtf, jb));
			jtf.setEditable(false);
			Font font1 = new Font("SansSerif", Font.PLAIN, 14);
			jtf.setFont(font1);
			gridbag.setConstraints(jtf, c);
			jp.add(jtf);

			ImageIcon icon = new ImageIcon();
			icon.setImage(chart.getPaint()[j].getImage());
			Image image = icon.getImage();
			image = image.getScaledInstance(80, 20, 4);
			icon.setImage(image);
			jb.setIcon(icon);
			jb.setToolTipText("1");
			jb.setActionCommand("colorChanges");
			ColorModifyActionListener mbl = new ColorModifyActionListener();
			jb.setFocusable(true);
			jb.addActionListener(mbl);
			gridbag.setConstraints(jb, c);
			jp.add(jb);

			int[] pattern = chart.getPattern();
			jtf = CreateComponent.crtJTextField(name, String.valueOf(pattern[j]), j);
			jtf.setToolTipText("2");
			jtf.getDocument().addDocumentListener(new MyDocumentListener(jtf, jb));
			jtf.setFont(font1);
			gridbag.setConstraints(jtf, c);
			jp.add(jtf);

			int[] ls = chart.getLineStrokes();
			jtf = CreateComponent.crtJTextField(name, String.valueOf(ls[j]), j);
			jtf.setToolTipText("3");
			jtf.getDocument().addDocumentListener(new MyDocumentListener(jtf, jb));
			jtf.setFont(font1);
			new LineAndShapePopup(jtf, chart);
			gridbag.setConstraints(jtf, c);
			jp.add(jtf);

			c.gridwidth = GridBagConstraints.REMAINDER;
			JLabel jl = new JLabel("");
			gridbag.setConstraints(jl, c);
			jp.add(jl);
			c.gridwidth = 0;
			c.weightx = 0.0;
		}
		String[] options = { "Apply", "Save", "Done" };
		JButton jb1;
		Box box = Box.createHorizontalBox();
		box.add(Box.createVerticalStrut(30));
		for (int i = 0; i < options.length; i++) {
			jb1 = crtJButton(options[i], i);
			gridbag.setConstraints(jb1, c);
			box.add(jb1);
		}
		box.add(Box.createVerticalStrut(30));
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;
		c.ipadx = 60;
		gridbag.setConstraints(box, c);
		jp.add(box);

		JScrollPane jsp = new JScrollPane(jp);
		jsp.setBorder(BorderFactory.createEmptyBorder());

		// Make this dialog display it.
		setContentPane(jsp);
		pack();
		// Handle window closing correctly.
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private JButton crtJButton(String name, int i) {
		JButton jb = new JButton(name);
		jb.setName(name);
		jb.setToolTipText(String.valueOf(i));
		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				JButton jb1 = (JButton) e.getSource();
				if (e.getClickCount() > 0)
					if (jb1.getName().equals("Apply"))
						doApply();
					else if (jb1.getName().equals("Save"))
						chart.storelegendInfo(chart.getLegend().split(","), getLegendInfoStr());
					else if (jb1.getName().equals("Done")) {
						if (jbColor != null) {
							if (JOptionPane.showConfirmDialog(null,
									"Click OK, Action has not been applied will be lost", "Confirm",
									JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
								cancelDialog.dispose();
							else
								JOptionPane.showMessageDialog(null, "Click Apply Button to apply changes.",
										"Information", JOptionPane.INFORMATION_MESSAGE);
						} else
							cancelDialog.dispose();
					}
			}
		};
		jb.addMouseListener(ml);
		return jb;
	}

	private void doApply() {
		if (jbColor != null) {
			switch (eventApply) {
			case 0:
				SetModifyChanges.setLegendChanges(charts, id, jtfChanged);
				break;
			case 1:
				SetModifyChanges.setColorChanges(charts, id, changeColLegend, changeColColor, changeColPaint);
				break;
			case 2:
				SetModifyChanges.setPatternChanges(charts, id, changeColLegend, jtfChanged, jbColor);
				break;
			case 3:
				SetModifyChanges.setStrokeChanges(charts, id, changeColLegend, jtfChanged);
				break;
			}
			jbColor = null;
			jtfChanged = null;
		}
	}

	private String[] getLegendInfoStr() {
		String[] s = new String[chart.getColor().length];
		for (int i = 0; i < s.length; i++) {
			s[i] = chart.getColor()[i] + "," + chart.getpColor()[i] + "," + chart.getPattern()[i] + ","
					+ chart.getLineStrokes()[i];
		}
		return s;
	}

	private void setColumnLabel(GridBagLayout gridbag, JPanel jp) {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		String[] name = { "Legend", "Color", "Pattern", "Line Stroke" };
		JLabel jl = null;

		for (int j = 0; j < name.length; j++) {
			Box box = Box.createHorizontalBox();
			if (j == 0) {
				jl = CreateComponent.crtJLabel(name[j], name[j], 10, 2, new Dimension(200, 20));
			} else {
				if (j == 2)
					box.add(new LegendHelpButton("patternList", LegendUtil.patternList));
				else if (j == 3)
					box.add(new LegendHelpButton("strokeList", LegendUtil.strokeList));

				jl = CreateComponent.crtJLabel(name[j], name[j], 10, 2, new Dimension(80, 20));
			}
			jl.setFont(new Font("Verdana", 1, 12));
			box.add(jl);
			gridbag.setConstraints(box, c);
			jp.add(box);
		}
		c.gridwidth = GridBagConstraints.REMAINDER;
		jl = new JLabel("");
		gridbag.setConstraints(jl, c);
		jp.add(jl);
	}

	public class ColorModifyActionListener implements ActionListener {

		public ColorModifyActionListener() {
		}

		public void actionPerformed(ActionEvent e) {
			eventApply = 1;
			jbColor = (JButton) e.getSource();
			changeColLegend = legend[Integer.valueOf(jbColor.getName().trim())];
			ColorChooser4DynamicModifyColor cc =  new ColorChooser4DynamicModifyColor(chart, jbColor);
			changeColPaint = cc.getPaint();
			changeColColor = cc.getColor();
		}
	}

	class MyDocumentListener implements DocumentListener {
		JTextField jtf;
		JButton jb;

		public MyDocumentListener(JTextField jtf, JButton jb) {
			this.jtf = jtf;
			this.jb = jb;
		}

		public void changedUpdate(DocumentEvent e) {
			setFldValue(e);
		}

		public void insertUpdate(DocumentEvent e) {
			setFldValue(e);
			eventApply = Integer.valueOf(jtf.getToolTipText().trim());
			jbColor = jb;
			jtfChanged = jtf;			
		}

		public void removeUpdate(DocumentEvent e) {
			setFldValue(e);
		}

		private void setFldValue(DocumentEvent e) {
			try {
				Document doc = e.getDocument();
				int vStrLen = doc.getLength();
				doc.getText(0, vStrLen);
				changeColLegend = legend[Integer.valueOf(jb.getName().trim())];
			} catch (BadLocationException e1) {
				e1.printStackTrace();
			}
		}
	}

	public JFreeChart getJfchart() {
		return jfchart;
	}

}
