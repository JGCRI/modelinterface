package graphDisplay;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;

import ModelInterface.ModelGUI2.DbViewer;
import chart.Chart;
import conversionUtil.ArrayConversion;

/**
 * The class to handle displaying data with functions in display a chart panel.
 * You can subset the chart by selecting data
 * 
 * Author Action Date Flag
 * ======================================================================= 
 * TWU   created 1/2/2016
 */

public class DataPanel extends JPanel implements ListSelectionListener {
	private static final long serialVersionUID = 1L;
	protected DefaultTableModel tableModel;
	protected TableColumnModel cmodel;
	protected DefaultTableCellRenderer renderer;
	protected JTable table;
	protected String tableCol[];
	protected int id;
	protected JFreeChart[] chart;
	protected JFreeChart[] copyChart;
	protected LegendItemCollection copyLgd;
	// Inherit values
	protected DefaultCategoryDataset[] cds;
	protected XYDataset ds;
	protected String dataValue[][];
	protected boolean addRow = true;

	public DataPanel(JFreeChart ch) {
		init(ch);
	}

	public DataPanel(Chart[] charts, int id) {
		// this.charts = charts;
		this.id = id;
		init(charts);
	}

	private void init(Chart[] charts) {
		setLayout(new BorderLayout());

		chart = new JFreeChart[charts.length];
		copyChart = new JFreeChart[charts.length];
		for (int i = 0; i < charts.length; i++) {
			try {
				chart[i] = charts[i].getChart();
				if (chart[i] != null)
					copyChart[i] = (JFreeChart) chart[i].clone();
				else
					copyChart = chart;
			} catch (CloneNotSupportedException e) {
				copyChart = chart;
			}
		}
		if (copyChart[id].getPlot().getPlotType().contains("XY")) {
			copyLgd = copyChart[id].getXYPlot().getFixedLegendItems();
		}else {
			copyLgd = copyChart[id].getCategoryPlot().getFixedLegendItems();
		}
		
		crtTable(chart[id]);
	}

	private void init(JFreeChart ch) {
		setLayout(new BorderLayout());

		chart = new JFreeChart[1];
		copyChart = new JFreeChart[1];
		try {
			chart[0] = ch;
			copyChart[0] = (JFreeChart) ch.clone();
		} catch (CloneNotSupportedException e) {
			copyChart = chart;
		}
		crtTable(chart[0]);
	}

	private void crtTable(final JFreeChart chart) {
		table = new JTable();
		tableModel = (DefaultTableModel) table.getModel();
		cmodel = table.getColumnModel();
		renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(4);
		table.getTableHeader().setFont(new Font("Verdana", 0, 15));
		table.setAutoCreateRowSorter(false);
		//table.setDragEnabled(true);
		table.setFont(new Font("Verdana", 0, 14));
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.setDefaultEditor(Object.class, null);
		//table.setToolTipText("Select first columns to return full chart");
		//table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//java.awt.event.MouseListener ml = new MouseAdapter() {
		//	public void mouseClicked(MouseEvent e) {
		//		if (e.getButton() == 3) {
		//			new DatasetSimpleStatistics(dataValue, table);
		//		}
		//	}
		//};
		//table.addMouseListener(ml);
		
		
		
		JScrollPane jsp = new JScrollPane(table);
		jsp.setPreferredSize(new Dimension(700, 100));
		add(jsp, "Center");
	}

	protected void SetColumnModel() {
		for (int j = 1; j < table.getColumnCount(); j++) {
			cmodel.getColumn(j).setCellRenderer(renderer);
			cmodel.getColumn(j).setCellEditor(table.getDefaultEditor(getClass()));
		}
	}

	public void setDigit(DefaultCategoryDataset[] cds, int n) {
		int r = cds[0].getRowCount();
		int c = cds[0].getColumnCount();

		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				if (n > 0 && !DbViewer.disable3Digits) {
					dataValue[i][j + 1] = String
							.valueOf(conversionUtil.DataConversion.roundDouble(cds[0].getValue(i, j).doubleValue(), n));
				}else {
					dataValue[i][j + 1] = String.valueOf(cds[0].getValue(i, j));
				}
			}
		}

		String[][] tranDataValue = ArrayConversion.arrayDimReverse(dataValue);
		for (int k = 1; k < cds.length; k++) {
			for (int i = 0; i < cds[k].getRowCount(); i++) {
				String[] temp = new String[cds[k].getColumnCount()];
				for (int j = 0; j < cds[k].getColumnCount(); j++) {
					if (addRow) {
						if (n > 0 && !DbViewer.disable3Digits) {
							dataValue[r + i][j + 1] = String.valueOf(
									conversionUtil.DataConversion.roundDouble(cds[k].getValue(i, j).doubleValue(), n));
						}else {
							dataValue[r + i][j + 1] = String.valueOf(Math.round((double) cds[k].getValue(i, j)));
						}
					} else {
						if (n > 0 && !DbViewer.disable3Digits) {
							temp[j] = String.valueOf(
									conversionUtil.DataConversion.roundDouble(cds[k].getValue(i, j).doubleValue(), n));
						}else {
							temp[j] = String.valueOf(Math.round((double) cds[k].getValue(i, j)));
						}
					}
				}
				if (!addRow) {
					tranDataValue[i + 1 + c] = temp;
				}
			}
		
			if (addRow) {
				r += cds[k].getRowCount();
			}
			else {
				c += cds[k].getRowCount();
			}
		
		}
		
		if (!addRow) {
			dataValue = ArrayConversion.arrayDimReverse(tranDataValue);
		}
		tableModel.setDataVector(dataValue, tableCol);
		Comparator<String> columnDoubleComparator =
			    (String v1, String v2) -> {

			    //cast v1 to double
			    Double val1=Double.parseDouble(v1);
			    //cast v2 to double
			    Double val2=Double.parseDouble(v2);
			    //return result
			   
			    	
			  return Double.compare(val1, val2);

			};

		
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
		table.setRowSorter(sorter);
		//add custom sorters to columns that are numbers
		for(int colC=0;colC<table.getColumnCount();colC++) {
			String clsName = table.getColumnName(colC);
			try {
				Double.parseDouble(clsName);
				//if we get here it is a numeric col
				//jtable.getColumnModel().getColumn(colC).setCo
				sorter.setComparator(colC, columnDoubleComparator);
				
				//tc.setCom
			} catch (Exception e) {
				;
			}
		}
	}

	public void setDigit(XYDataset ds, int n) {
		int l = 0;
		for (int k = 0; k < copyChart[id].getXYPlot().getDatasetCount(); k++) {
			ds = copyChart[id].getXYPlot().getDataset(k);
			for (int i = 0; i < ds.getSeriesCount(); i++) {
				for (int j = 0; j < ds.getItemCount(i); j++)
					dataValue[l + i][j + 1] = String
							.valueOf(conversionUtil.DataConversion.roundDouble(ds.getYValue(i, j), n));

			}
			l += ds.getSeriesCount();
		}
		tableModel.setDataVector(dataValue, tableCol);
	}

	public void valueChanged(ListSelectionEvent e) {
		boolean adjust = e.getValueIsAdjusting();

		if (!adjust) {
			int selectedC[] = (int[]) null;
			int selectedR[] = (int[]) null;

			if (table.getRowSelectionAllowed()) {
				selectedR = table.getSelectedRows();
				for (int i = 0; i < selectedR.length; i++) {
					if (selectedR[i] < table.getRowCount() - 1) {
						selectedR[i] = table.convertRowIndexToModel(selectedR[i]);
					} else {
						selectedR = Arrays.copyOf(selectedR, selectedR.length - 1);
						break;
					}
				}
			}

			if (table.getColumnSelectionAllowed()) {
				selectedC = table.getSelectedColumns();
				for (int i = 0; i < selectedC.length; i++) {
					if (selectedC[i] < table.getColumnCount() - 1) {
						selectedC[i] = table.convertColumnIndexToModel(selectedC[i]) - 1;
					} else {
						selectedC = Arrays.copyOf(selectedC, selectedC.length - 1);
						break;
					}
				}
			}
/*
			if (selectedC[0] < 0) {
				if (selectedC.length == 1) {
					selectedC = new int[table.getColumnCount() - 2];
					for (int i = 0; i < selectedC.length; i++)
						selectedC[i] = i + 1;
					for (int i = 0; i < selectedC.length; i++)
						selectedC[i] = table.convertColumnIndexToModel(selectedC[i]) - 1;
				} else {
					((javax.swing.DefaultListSelectionModel) e.getSource()).setValueIsAdjusting(true);
					table.clearSelection();
					JOptionPane.showMessageDialog(null, "Select first column only or without the first column",
							"Information", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}*/
			//GraphDisplayUtil.showSelectRow(selectedC, selectedR, chart[id], copyChart[id], copyLgd);
		}

	}

	public String[] getTableCol() {
		return tableCol;
	}

	public String[][] getDataValue() {
		return dataValue;
	}

	public DefaultTableModel getTableModel() {
		return tableModel;
	}

	public TableColumnModel getCmodel() {
		return cmodel;
	}

	public JFreeChart getChart() {
		return chart[id];
	}

	public JFreeChart getCopyChart() {
		return copyChart[id];
	}

	public DefaultCategoryDataset[] getCds() {
		return cds;
	}

	public XYDataset getDs() {
		return ds;
	}

}
