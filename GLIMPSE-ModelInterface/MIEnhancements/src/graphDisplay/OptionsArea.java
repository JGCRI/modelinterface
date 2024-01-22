/*
* LEGAL NOTICE
* This computer software was prepared by US EPA.
* THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* EXPORT CONTROL
* User agrees that the Software will not be shipped, transferred or
* exported into any country or used in any manner prohibited by the
* United States Export Administration Act or any other applicable
* export laws, restrictions or regulations (collectively the "Export Laws").
* Export of the Software may require some form of license or other
* authority from the U.S. Government, and failure to obtain such
* export control license may result in criminal liability under
* U.S. laws. In addition, if the Software is identified as export controlled
* items under the Export Laws, User represents and warrants that User
* is not a citizen, or otherwise located within, an embargoed nation
* (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
*     and that User is not otherwise prohibited
* under the Export Laws from receiving the Software.
*
* SUPPORT
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors * from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. Coding contributions have also been made by Aaron 
* Parks and Yadong Xu of ARA through the EPA�s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*/
package graphDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import chart.Chart;
import chart.LegendUtil;
import listener.ThumbnailBoxPopup;

/**
 * The class to handle options functions in display a chart panel.
 * 
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 */

public class OptionsArea {
	private JPanel jp;
	private JSplitPane sp;
	private Chart[] chart;
	private int w;
	private int gridWidth;
	private boolean sameScale;
	//private int typeLineChart = 2;
	//private int typeRelativeLineChart = 3;
	public static String LINE_CHART="LineChart";
	public static String STACKED_BAR_CHART="StackedBarChart";
	public static String STACKED_AREA_CHART="StackedAreaChart";
	public static String REL_RATIO_LINE ="RelativeRatio(Line)";
	public static String REL_DIFF_LINE="RelativeDiff(Line)";
	public static String REL_DIFF_BAR="RelativeDiff(bar)";

	public OptionsArea(JPanel jp, Chart[] chart, int gridWidth, boolean sameScale, JSplitPane sp) {
		this.jp = jp;
		this.chart = chart;
		this.gridWidth = gridWidth;
		this.sameScale = sameScale;
		this.sp = sp;
		setOptionsArea();
	}

	protected void setOptionsArea() {
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(5));
		JButton jb = new JButton("More");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		java.awt.event.MouseListener ml1 = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				ThumbnailBoxPopup popup = new ThumbnailBoxPopup(chart, w, gridWidth, sameScale, sp);
				popup.show(jp, e.getX(), e.getY());
			}
		};
		jb.addMouseListener(ml1);
		box.add(jb);
		box.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel("Display", 2);
		box.add(jl);
		box.add(Box.createHorizontalStrut(10));
		JScrollPane dspCol = displayCol();
		dspCol.setMaximumSize(new Dimension(90, 30));
		dspCol.setMinimumSize(new Dimension(30, 30));
		box.add(dspCol);

		GraphOptionPane gPane = new GraphOptionPane();
		gPane.setMaximumSize(new Dimension(150, 30));
		gPane.setMinimumSize(new Dimension(90, 30));
		gPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		gPane.setBackground(Color.green);
		box.add(gPane);

		box.add(scaleCheckBox());
		box.add(Box.createHorizontalStrut(10));

		jb = new JButton("Refresh");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		java.awt.event.MouseListener ml = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				setChartPane();
			}
		};
		jb.addMouseListener(ml);
		box.add(jb);
		box.add(Box.createHorizontalStrut(10));

		jp.add(box, BorderLayout.NORTH);
	}

	private JScrollPane displayCol() {
		final String c[] = { "1", "2", "3", "4", "5", "6" };
		JList<String> list = new JList<String>(c);
		list.setName("dispCol");
		list.setFont(new Font("Verdana", 0, 10));
		list.setVisibleRowCount(3);
		list.setSelectionMode(0);
		ListSelectionListener lsl = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				JList<?> list = (JList<?>) e.getSource();
				boolean adjust = e.getValueIsAdjusting();
				if (!adjust && !list.isSelectionEmpty()) {
					gridWidth = Integer.valueOf(c[list.getSelectedIndex()]).intValue();
					setChartPane();
					jp.updateUI();
				}
			}
		};
		list.addListSelectionListener(lsl);
		return new JScrollPane(list);
	}

	private JCheckBox scaleCheckBox() {
		JCheckBox jc = new JCheckBox("Same Scale");
		jc.setSelected(sameScale);
		ItemListener il = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				JCheckBox jcb = (JCheckBox) e.getSource();
				if (e.getStateChange() == 1) {
					sameScale = true;
					jcb.setSelected(true);
				} else if (e.getStateChange() == 2) {
					sameScale = false;
					jcb.setSelected(false);
				}
				setChartPane();
				jp.updateUI();
			}
		};
		jc.addItemListener(il);
		return jc;
	}

	private void setChartPane() {
		// Dan: Using modified version (2)
		ThumbnailUtil2.validateChartPane(jp);
		w = ThumbnailUtil.computeFixGridLayoutViewSize(sp.getSize().width, gridWidth);
		// Dan: Using modified version (2)
		JPanel chartPane = ThumbnailUtil2.setChartPane(chart, w, gridWidth, sameScale, false);
		jp.add(chartPane, BorderLayout.CENTER);
		jp.updateUI();
	}

	/**
	 * The class to handle JFreeChart type conversion and relative value among
	 * columns of Thumbnail charts
	 *
	 */

	private class GraphOptionPane extends JComboBox<String> implements ActionListener {

		private static final long serialVersionUID = 1L;
//		private String graphType[] = { "BarChart", "StackedBarChart", "StackedAreaChart", "____________", "AreaChart",
//				"LineChart", "RelativeIndex", "XYLineChart", "ScatterChart" };
//		private String graphClassName[] = { "chart.CategoryBarChart", "chart.CategoryStackedBarChart",
//				"chart.CategoryStackedAreaChart", "____________", "chart.CategoryAreaChart", "chart.CategoryLineChart",
//				"", "chart.XYLineChart", "chart.XYScatterChart" };
		// private String graphType[] = { "StackedBarChart",
		// "StackedAreaChart","BarChart",
		// "LineChart", "RelativeIndex"};
		// private String graphClassName[] = { "chart.CategoryStackedBarChart",
		// "chart.CategoryStackedAreaChart","chart.CategoryBarChart","chart.CategoryLineChart"};

		//private String graphType[] = { "StackedBarChart", "StackedAreaChart", "LineChart", "RelativeIndex" };
		//private String graphClassName[] = { "chart.CategoryStackedBarChart", "chart.CategoryStackedAreaChart",
		//		"chart.CategoryLineChart" };

		private String graphType[] = {OptionsArea.LINE_CHART, OptionsArea.STACKED_BAR_CHART, OptionsArea.STACKED_AREA_CHART,OptionsArea.REL_RATIO_LINE,OptionsArea.REL_DIFF_LINE,OptionsArea.REL_DIFF_BAR };
		private String graphClassName[] = {"chart.CategoryLineChart","chart.CategoryStackedBarChart","chart.CategoryStackedAreaChart","chart.CategoryLineChart","chart.CategoryLineChart","chart.CategoryStackedBarChart" };

		
		private int idx;
		private int relativeIndex;
		private String cn;
		private Chart[] oChart = null;

		public GraphOptionPane() {
			// Dan: Using modified version (2)
			idx = ThumbnailUtil2.getFirstNonNullChart(chart);
			if (idx != -1) {
				relativeIndex = chart[idx].getRelativeColIndex();
				cn = chart[idx].getChartClassName();
				oChart = chart.clone();
				setPane();
			}
		}

		private void setPane() {
			int listC = graphType.length;
			for (int i = 0; i < listC; i++)
				addItem(graphType[i]);

			setName("GraphOptionPane");
			// Dan: Using modified version (2)
			setSelectedIndex(getIndex(chart[ThumbnailUtil2.getFirstNonNullChart(chart)]));
			setFont(new Font("Verdana", 0, 12));
			setPreferredSize(new Dimension(120, 30));
			addActionListener(this);
			setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 540));
		}

		private int getIndex(Chart ch) {
			int sIndex = 0;
			for (int i = 0; i < graphType.length; i++) {
				if (!ch.getChartClassName().equalsIgnoreCase(graphClassName[i].trim()))
					continue;
				sIndex = i;
				break;
			}
			return sIndex;
		}

		public void actionPerformed(ActionEvent e1) {

			JList<String> listYrsForRelChart = new JList<String>(
					("back to original chart," + chart[idx].getChartColumn()).split(","));
			listYrsForRelChart.setSelectionMode(0);
			listYrsForRelChart.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {

					JList<?> list = (JList<?>) e.getSource();
					boolean adjust = e.getValueIsAdjusting();
					if (!adjust) {
						relativeIndex = list.getSelectedValue().equals("back to original chart") ? -1
								: list.getSelectedIndex() - 1;
						if (relativeIndex == -1) {
							resetChart();
							//setSelectedIndex(typeLineChart);
						}
					}
				}
			});

			JComboBox<?> source = (JComboBox<?>) e1.getSource();

			int selectedChartIndex = source.getSelectedIndex();
			String selectedValue=source.getSelectedItem().toString();
			//if (selectedChartIndex == typeRelativeLineChart) {
			if(selectedValue.compareTo(OptionsArea.REL_DIFF_BAR)==0
				|| selectedValue.compareTo(OptionsArea.REL_DIFF_LINE)==0
				|| selectedValue.compareTo(OptionsArea.REL_RATIO_LINE)==0) {
				resetChart();

				
				//setSelectedIndex(sele);
				listYrsForRelChart.setSelectedIndex(1);

				String options[] = { "ok" };
				JOptionPane pane0 = new JOptionPane(new JScrollPane(listYrsForRelChart), -1, 0, null, options,
						options[0]);
				JDialog dialog = pane0.createDialog("Please select a relative data value");
				dialog.setLayout(null);
				dialog.setResizable(true);
				dialog.setVisible(true);

			} else if (relativeIndex > -1) {

				resetChart();
				setSelectedIndex(selectedChartIndex);
				listYrsForRelChart.setSelectedIndex(selectedChartIndex);

			}

			// trying this
			int index = selectedChartIndex;
			//if (selectedChartIndex == typeRelativeLineChart)
			//	index = typeLineChart;
			cn = graphClassName[index];
			chart = ThumbnailUtil2.createChart(cn, relativeIndex, chart,selectedValue);

			setChartPane();

		}

		public void resetChart() {

			cn = "chart.CategoryLineChart";
			relativeIndex = -1;

			chart = oChart;

			setChartPane();

			// isLine = true;
			// setSelectedIndex(typeLineChart);

		}

	}

}
