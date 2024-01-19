package filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import ModelInterface.ModelGUI2.DbViewer;
import chart.LegendUtil;
import chartOptions.SelectDecimalFormat;
import graphDisplay.ModelInterfaceUtil;
import graphDisplay.Thumbnail;

/**
 * The class to handle a JTable filtered by meta data of other JTable, then
 * displaying on the splitpane.
 * 
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 */

public class FilteredTable_orig {

	private TableModel tableModel;
	private TableRowSorter<TableModel> sorter;
	private int doubleIndex;
	private String[][] newData;
	private JTable jtable;
	private JSplitPane sp;
	private String[] tableColumnData;
	private Thumbnail tn;
	private boolean debug = false;
	private int sigfigs=3;

	public FilteredTable_orig(Map<String, String> sel, String chartName, String[] unit, String path, final JTable jTable,
			JSplitPane sp) {

		this.sp = sp;
		JPanel jp = new JPanel((new BorderLayout()));
		Component c = sp.getRightComponent();
		if (c != null)
			sp.remove(c);

		if (sel == null)
			Var.origYRange = ModelInterfaceUtil.getColumnFromTable(jTable, 0);

		tableColumnData = ModelInterfaceUtil.getColumnFromTable(jTable, 4);

		String[] cls = new String[tableColumnData.length];
		for (int j = 0; j < tableColumnData.length; j++) {
			//cls[j] = jTable.getColumnClass(j).getName();
		    cls[j] = jTable.getColumnName(j);
		}

		doubleIndex = ModelInterfaceUtil.getDoubleTypeColIndex(cls);
		String[] qualifier = ModelInterfaceUtil.getColumnFromTable(jTable, 3);
		ArrayList<String> al = new ArrayList<String>();
		ArrayList<Integer> alI = new ArrayList<Integer>();

		Integer[] tableColumnIndex = getTableColumnIndex(sel);

		if (debug)
			System.out.println("FilteredTable: colidx: " + Arrays.toString(tableColumnIndex));

		for (int i = 0; i < doubleIndex; i++) {
			al.add(tableColumnData[i]);
			alI.add(new Integer(i));
		}

		for (int i = 0; i < tableColumnIndex.length; i++) {
			al.add(tableColumnData[tableColumnIndex[i]]);
			alI.add(new Integer(tableColumnIndex[i]));
		}

		al.add(tableColumnData[tableColumnData.length - 1]);
		alI.add(new Integer(tableColumnData.length - 1));

		if (debug) {
			System.out.println("FilteredTable: col: " + Arrays.toString(tableColumnData));
			System.out.println("FilteredTable: colidx: " + Arrays.toString(alI.toArray(new Integer[0])));
		}

		String[][] tData = getTableData(jTable, alI.toArray(new Integer[0]));
		Comparator<String> columnDoubleComparator =
			    (String v1, String v2) -> {

			    //cast v1 to double
			    Double val1=Double.parseDouble(v1);
			    //cast v2 to double
			    Double val2=Double.parseDouble(v2);
			    //return result
			   
			    	
			  return Double.compare(val1, val2);

			};
		
		if (sel == null || sel.isEmpty())
			newData = tData.clone();
		else
			newData = getfilterTableData(tData, getFilterData(qualifier, sel));
		try {
			DefaultTableModel dtm=new DefaultTableModel(newData,  al.toArray(new String[0])){

				@Override
				public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
				}
				};
			//jtable = new JTable(newData, al.toArray(new String[0]));// tableColumnData);
			jtable=new JTable(dtm);
			jtable.setDragEnabled(true);
			tableModel = jtable.getModel();
			sorter = new TableRowSorter<TableModel>(tableModel);
			jtable.setRowSorter(sorter);
			//add custom sorters to columns that are numbers
			for(int colC=0;colC<jtable.getColumnCount();colC++) {
				String clsName = jtable.getColumnName(colC);
				boolean isDouble=false;
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
			//TableColumnModelListener tableColumnModelListener = new TableSorterColumnModelListener();
			//jtable.getTableHeader().getColumnModel().addColumnModelListener(tableColumnModelListener);
		} catch (Exception e) {
			System.out.println("FilteredTable Caught: ");
			e.printStackTrace();
		}

		Box box = Box.createHorizontalBox();
		JButton jb = new JButton("Filter");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		java.awt.event.MouseListener ml = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				new FilterTreePane(chartName, unit, path, jTable, sel, sp);
			}
		};
		jb.addMouseListener(ml);
		box.add(jb);

		jb = new JButton("Graph");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		java.awt.event.MouseListener ml1 = new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				if (debug)
					System.out.println("FilteredTable: graph press: " + chartName + " " + Arrays.toString(unit) + " "
							+ path + " " + doubleIndex + " " + jtable.getColumnCount() + "  " + jtable.getRowCount());
				if (tn == null) {
					Map<String, Integer[]> metaMap = ModelInterfaceUtil.getMetaIndex2(jtable, doubleIndex);
					HashMap<String,String> unitsMap=ModelInterfaceUtil.getUnitDataFromTableByLastNamedCol(jTable);
					
					tn = new Thumbnail(chartName, unit, path, doubleIndex, jtable, metaMap, sp,unitsMap);
				}
				JPanel jp = tn.getJp();
				if (jp != null)
					setRightComponent(jp);
				else {
					tn = null;
					System.gc();
				}
			}
		};
		jb.addMouseListener(ml1);
		box.add(jb);
		
		jb = new JButton("Format");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		java.awt.event.MouseListener ml2 = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				String[][] data = getTableData(jTable, alI.toArray(new Integer[0]));

				if (sel == null || sel.isEmpty())
					newData = data.clone();
				else
					newData = getfilterTableData(data, getFilterData(qualifier, sel));

				Object dataValue[][] = new SelectDecimalFormat(newData, doubleIndex).getDataValue();
				for (int i = 0; i < jtable.getRowCount(); i++)
					for (int j = doubleIndex; j < jtable.getColumnCount() - 1; j++)
						jtable.setValueAt(dataValue[i][j], i, j);
			}
		};
		jb.addMouseListener(ml2);
		box.add(jb);
				
		box.setSize(new Dimension(80, 20));

		jp.add(box, BorderLayout.NORTH);
		jp.add(new JScrollPane(jtable), BorderLayout.CENTER);
		jp.updateUI();

		c = sp.getLeftComponent();
		if (c != null)
			sp.remove(c);

		sp.setLeftComponent(jp);
		if (debug)
			System.out.println("FilteredTable::FilteredTable:max memory " + Runtime.getRuntime().maxMemory()
					+ " total: " + Runtime.getRuntime().totalMemory() + " free: " + Runtime.getRuntime().freeMemory());
		
		
		
	}
	
//	private void modifyDataToSigFigs(JTable jTable,ArrayList<Integer> alI,Map<String, String> sel,String[] qualifier) {
//		String[][] data = getTableData(jTable, alI.toArray(new Integer[0]));
//
//		if (sel == null || sel.isEmpty())
//			newData = data.clone();
//		else
//			newData = getfilterTableData(data, getFilterData(qualifier, sel));
//
//		String dataValue[][] = new setToSigFigs(newData, doubleIndex);
//		for (int i = 0; i < jtable.getRowCount(); i++) {
//			for (int j = doubleIndex; j < jtable.getColumnCount() - 1; j++) {
//				jtable.setValueAt(dataValue[i][j], i, j);		
//			}
//		}
//	}
//	
//	public static String setToSigFig(double value, int significantDigits) {
//	    if (significantDigits < 0) throw new IllegalArgumentException();
//
//	    BigDecimal bd = new BigDecimal(value, MathContext.DECIMAL64);
//	    bd = bd.round(new MathContext(significantDigits, RoundingMode.HALF_UP));
//	    final int precision = bd.precision();
//	    if (precision < significantDigits)
//	    bd = bd.setScale(bd.scale() + (significantDigits-precision));
//	    return bd.toPlainString();
//	}
//	

	private Integer[] getTableColumnIndex(Map<String, String> sel) {

		Integer[] tableColumnIndex = null;
		Map<String, Integer> tableColumnDataIndex = new LinkedHashMap<String, Integer>();

		if (sel != null && !sel.isEmpty()) {
			String[] keys = sel.keySet().toArray(new String[0]);

			for (int k = 0; k < keys.length; k++) {
				String[] temp = keys[k].split("\\|");
				if (temp[0].contains("Year")) {
					tableColumnDataIndex.put(temp[1],
							Integer.valueOf(Arrays.asList(tableColumnData).indexOf(temp[1].trim())));
					if (debug)
						System.out.println("FilteredTable::getTableColumnIndex:col " + temp[0] + "  " + temp[1] + "  "
								+ Arrays.toString(tableColumnData));

				}
			}
			String[] k = tableColumnDataIndex.keySet().toArray(new String[0]);
			Var.sectionYRange = k.clone();
		}

		if (!tableColumnDataIndex.isEmpty())
			tableColumnIndex = tableColumnDataIndex.values().toArray(new Integer[0]);
		else {
			if (Var.sectionYRange == null)
				Var.sectionYRange = Var.defaultYRange.clone();

			ArrayList<Integer> temp = new ArrayList<Integer>();
			for (int k = 0; k < Var.sectionYRange.length; k++) {
				int i = Arrays.asList(Var.origYRange).indexOf(Var.sectionYRange[k]);
				if (i > -1)
					temp.add(Arrays.asList(Var.origYRange).indexOf(Var.sectionYRange[k]));
			}

			tableColumnIndex = new Integer[temp.size()];
			for (int k = 0; k < tableColumnIndex.length; k++)
				tableColumnIndex[k] = doubleIndex + temp.get(k);
		}

		Arrays.sort(tableColumnIndex);

		if (debug)
			System.out.println("FilteredTable::getTableColumnIndex::col" + Arrays.toString(tableColumnIndex) + " sec: "
					+ Arrays.toString(Var.sectionYRange));
		return tableColumnIndex;
	}

	private String[][] getTableData(JTable jtable, Integer[] col) {
		if (debug)
			System.out.println("FilteredTable::getTableData: colIdx: " + Arrays.toString(col));
		String[][] tData = new String[jtable.getRowCount()][col.length];
		for (int i = 0; i < jtable.getRowCount(); i++) {
			for (int j = 0; j < col.length; j++) {
				//Dan modified to work with new Diff Results panel
				//String cls = jtable.getColumnClass(col[j].intValue()).getName();
				String cls = jtable.getColumnName(col[j].intValue());
				boolean isDouble=false;
				try {
					Double.parseDouble(cls);
					isDouble=true;
				} catch (Exception e) {
					;
				}
				
				if (debug)
					System.out.println(
							"FilteredTable:getTableData: colData: " + jtable.getValueAt(i, col[j]) + "  " + cls);
				if (isDouble) {
					//double d = ((Double) jtable.getValueAt(i, col[j].intValue())).doubleValue();
					String toTry=jtable.getValueAt(i, col[j].intValue()).toString();
					double d = Double.parseDouble(toTry);
					tData[i][j] = toSigFigs(d,sigfigs);
				} else
					tData[i][j] = (String) jtable.getValueAt(i, col[j].intValue());
			}
		}
		return tData;
	}
	
	public static String toSigFigs(double value, int significantDigits) {
	    if (significantDigits < 0) throw new IllegalArgumentException();
	    if(DbViewer.disable3Digits) {
	    	Double d=value;
	    	return d.toString();
	    }

	    // this is more precise than simply doing "new BigDecimal(value);"
	    BigDecimal bd = new BigDecimal(value, MathContext.DECIMAL64);
	    bd = bd.round(new MathContext(significantDigits, RoundingMode.HALF_UP));
	    final int precision = bd.precision();
	    if (precision < significantDigits)
	    bd = bd.setScale(bd.scale() + (significantDigits-precision));
	    return bd.toPlainString();
	}

	public void setRightComponent(JPanel jpc) {

		if (debug)
			System.out.println("FilteredTable::setRightComponent: jpc: " + jpc.getName());

		JScrollPane chartScrollPane = new JScrollPane(jpc);
		chartScrollPane.getViewport().setBackground(Color.cyan);// jpc.getBackground());
		if (sp.getRightComponent() != null)
			sp.remove(sp.getRightComponent());
		sp.setRightComponent(chartScrollPane);
		sp.setDividerLocation(0.678);
		sp.updateUI();
	}

	private String[][] getfilterTableData(String[][] source, ArrayList<String[]> filter) {
		ArrayList<String[]> al = new ArrayList<String[]>();

		for (int i = 0; i < source.length; i++) {
			boolean found = false;

			for (int j = 0; j < filter.size(); j++) {
				for (int k = 0; k < filter.get(j).length; k++) {
					if (source[i][j].trim().equals(filter.get(j)[k].trim())) {
						found = true;
						break;
					} else
						found = false;
				}
				if (!found)
					break;
			}
			if (found) {
				al.add(source[i]);
				if (debug)
					System.out.println("getfilterTableData: " + i + "  " + Arrays.toString(source[i]));
			}
		}
		return al.toArray(new String[0][0]);
	}

	private ArrayList<String[]> getFilterData(String[] qualifier, Map<String, String> sel) {
		ArrayList<String[]> filter = new ArrayList<String[]>();
		String[] s = sel.values().toArray(new String[0]);

		for (int j = 0; j < qualifier.length; j++) {
			String key = qualifier[j].trim();
			ArrayList<String> uni = new ArrayList<String>();
			for (int i = 0; i < s.length; i++) {
				String[] temp = s[i].split("\\|");
				String q = temp[0].trim();
				if (debug)
					System.out.println("FilteredTable::getfilterData:QualiferIndex: " + i + " key: " + key + " sel: "
							+ Arrays.toString(temp));
				if (q.equals(key))
					if (!uni.contains(temp[1].trim()))
						uni.add(temp[1].trim());
			}

			if (debug)
				System.out.println("FilteredTable::getfilterData:RowIndex: " + j + "  "
						+ Arrays.toString(uni.toArray(new String[0])));
			filter.add(j, uni.toArray(new String[0]));
		}
		return filter;
	}

	/*
	private class TableSorterColumnModelListener implements TableColumnModelListener {

		@Override
		public void columnAdded(TableColumnModelEvent arg0) {
		}

		@Override
		public void columnMarginChanged(ChangeEvent arg0) {
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			TableColumnModel columnModel = (TableColumnModel) e.getSource();
			int end = ModelInterfaceUtil.getDoubleTypeColIndex(tableModel);
			int[] columns = new int[end];
			for (int i = 0; i < end; i++)
				columns[i] = columnModel.getColumn(i).getModelIndex();
			Arrays.sort(columns);
			setSortingStatus(columnModel, columns, 1);
		}

		@Override
		public void columnRemoved(TableColumnModelEvent arg0) {
		}

		@Override
		public void columnSelectionChanged(ListSelectionEvent arg0) {
		}

		public void setSortingStatus(TableColumnModel columnModel, int[] columns, int status) {
			List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
			for (int i = 0; i < columns.length; i++) {
				int column = columnModel.getColumn(columns[i]).getModelIndex();
				sortKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
			}
			sorter.setSortKeys(sortKeys);
			sorter.sort();
		}
	}*/

}
