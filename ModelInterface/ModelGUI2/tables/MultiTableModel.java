/*
* LEGAL NOTICE
* This computer software was prepared by Battelle Memorial Institute,
* hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
* with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
* CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
* Distributed as open-source under the terms of the Educational Community 
* License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
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
*/
package ModelInterface.ModelGUI2.tables;

import ModelInterface.InterfaceMain;
import ModelInterface.common.DataPair;
import ModelInterface.ModelGUI2.DOMmodel;
import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.Documentation;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.ModelGUI2.xmldb.DbProcInterrupt;

import java.util.*;

import org.apache.poi.hssf.usermodel.*;

import org.jfree.chart.JFreeChart;
import java.awt.image.BufferedImage;
import org.w3c.dom.*;
import javax.swing.table.*;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.TreePath;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.node.ANode;
import org.basex.api.dom.BXNode;
import org.basex.api.dom.BXElem;


public class MultiTableModel extends BaseTableModel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// used to be able to edit tables in a cell
	// don't really know what to do here
	// it seems to work, but obviously this isn't correct
	private class TableEditor implements TableCellEditor {
		private Vector<CellEditorListener> editListeners;
		public TableEditor () {
			editListeners = new Vector<CellEditorListener>();
		}
		public void removeCellEditorListener(CellEditorListener cE ) {
			editListeners.remove(cE);
		}
		public Object getCellEditorValue() {
			System.out.println("Cell Editor Value");
			return "I DON'T KNOW";
		}
		public boolean stopCellEditing() {
			fireEditingStopped();
			return true;
		}
		public void cancelCellEditing() {
			fireEditingCanceled();
		}
		public boolean isCellEditable(EventObject eO) {
			return true;
		}
		public boolean shouldSelectCell(EventObject eO) {
			return true;
		}
		public void addCellEditorListener(CellEditorListener cE ) {
			editListeners.add(cE);
		}
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
			return (JComponent)value;
		}
		private void fireEditingStopped() {
			ChangeEvent ce = new ChangeEvent(this);
			for(int i = 0; i < editListeners.size(); ++i) {
				editListeners.get(i).editingStopped(ce);
			}
		}
		private void fireEditingCanceled() {
			ChangeEvent ce = new ChangeEvent(this);
			for(int i = 0; i < editListeners.size(); ++i) {
				editListeners.get(i).editingCanceled(ce);
			}
		}
	}
	// to be able to render a table inside a cell
	private class TableRenderer implements TableCellRenderer {
		public TableRenderer () {}
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col ) {
			if(row % 2 == 0) {
				Component comp = (new DefaultTableCellRenderer()).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if(table.getRowHeight(row) != 16) {
					table.setRowHeight(row, 16);
				}
				comp.setBackground(new Color(240,214,19));
				return comp;
			} else {
				JComponent compValue = (JComponent)value;
				if(table.getRowHeight(row) != (int)compValue.getPreferredSize().getHeight()+30) {
					table.setRowHeight(row, (int)compValue.getPreferredSize().getHeight() +30);
				}
				return compValue;
			}
		}
	}
	Vector tables;
	TableRenderer tableRenderer;
	TableEditor tableEditor;

	/**
	 * Constructor initializes data members, and calls buildTable to initialize data, and filterMaps
	 * and create the individual tables
	 * @param tp the Tree Path which was selected from the tree, needed to build table
	 *        doc needed to run the XPath query against
	 *        tableTypeString to be able to display the type of table this is
	 */
	public MultiTableModel(TreePath tp, Document doc, String tableTypeString, Documentation documentationIn) {
		super(tp, doc, tableTypeString, documentationIn);
		wild = chooseTableHeaders(tp);
	        wild.set(0, ((DOMmodel.DOMNodeAdapter)wild.get(0)).getNode().getNodeName());
	        wild.set(1, ((DOMmodel.DOMNodeAdapter)wild.get(1)).getNode().getNodeName());
		wild.add("");
		buildTable(treePathtoXPath(tp, doc.getDocumentElement(), 0));
		tableEditor = new TableEditor();
		tableRenderer = new TableRenderer();
		activeRows = new Vector(tables.size());
		for(int i = 0; i < tables.size(); i++) {
			activeRows.add(new Integer(i));
		}
	}
	/**
	 * flips the axis of the individual table
	 * @param row used to figure out which cell needs to be flipped
	 *        col not really important since we only have 1 col
	 */
	public void flip(int row, int col) {
		((NewDataTableModel)((JTable)((JScrollPane)getValueAt(row, col)).getViewport().getView()).getModel()).flip(row, col);
	}

	/**
	 * Runs an XPath expression to get a set of nodes, which then are sorted, based on its path in
	 * the tree.  Uses the sorted data to create a set of tables, also initalizes the filterMaps.
	 * @param xpe the XPath expression which will be used to get nodes.
	 */
  	protected void buildTable(XPathExpression xpe) {
	  NodeList res = null;
      try {
          res = (NodeList)xpe.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
      } catch(XPathExpressionException e) {
          e.printStackTrace();
      }
	  xpe = null;
	  Node tempNode;
	  Object[] regionAndYear;
	  TreeSet regions = new TreeSet();
	  TreeSet years = new TreeSet();
	  tableFilterMaps = new LinkedHashMap();
	  Map dataTree = new TreeMap();
      for(int i = 0; i < res.getLength(); ++i) {
        tempNode = res.item(i);
		regionAndYear = getRegionAndYearFromNode(tempNode.getParentNode(), tableFilterMaps);
		regions.add(regionAndYear[0]);
		years.add(regionAndYear[1]);
		addToDataTree(tempNode, dataTree).put((String)regionAndYear[0]+";"+(String)regionAndYear[1], tempNode);
		if(units == null) {
			units = ((Element)tempNode.getParentNode()).getAttribute("unit");
		}
	  }
	  recAddTables(dataTree, null, regions, years, "");
  	}

	/**
	 * Gets the 2 attributes of the 2 wilds from going up the parent path of a node, also update the
	 * filter maps
	 * @param n the node whos wild node's attrubutes need to be determined
	 *        filterMaps maps which has the filtering information, which will be updated with the attribute value from this nodes parent path
	 * @return an array of size 2 with the attrubute values of the wild which lead to this node
	 */
  	private Object[] getRegionAndYearFromNode(Node n, Map filterMaps) {
	  Vector ret = new Vector(2,0);
	  do {
		  if(n.getNodeName().equals((String)wild.get(0)) || n.getNodeName().equals((String)wild.get(1))) {
			  //ret.add(n.getAttributes().getNamedItem("name").getNodeValue());
			  ret.add(getOneAttrVal(n));
			  /*
			  if(!getOneAttrVal(n).equals("fillout=1")) {
			  	ret.add(getOneAttrVal(n));
			  } else {
			        ret.add(getOneAttrVal(n, 1));
			  }
			  */

		  } else if(n.hasAttributes()) {
			  Map tempFilter;
	           	  if (filterMaps.containsKey(n.getNodeName())) {
	                          tempFilter = (Map)filterMaps.get(n.getNodeName());
                          } else {
                                  tempFilter = new HashMap();
                          }
			  String attr = getOneAttrVal(n);
			  /*
			  if(attr.equals("fillout=1")) {
				  attr = getOneAttrVal(n, 1);
			  }
			  */
			  if (!tempFilter.containsKey(attr)) {
                          	tempFilter.put(attr, new Boolean(true));
                          	filterMaps.put(n.getNodeName(), tempFilter);
			  }
		  }
		  n = n.getParentNode();
	  } while(n.getNodeType() != Node.DOCUMENT_NODE /*&& (region == null || year == null)*/);
	  return ret.toArray();
  	}
  /**
   * Sort data so that we know where each set of data comes from. Recursivly moves to the top of the parentPath
   * and at each level crates/uses the appropriate mapping for this node
   * @param currNode current node we are analyzing in the tree
   *        dataTree the complete set of maps sorting the data
   * @return the current mapping that was just created/used
   */
  private Map addToDataTree(Node currNode, Map dataTree) {
	  if (currNode.getNodeType() == Node.DOCUMENT_NODE) {
		  return dataTree;
	  }
	  Map tempMap = addToDataTree(currNode.getParentNode(), dataTree);
	  // used to combine sectors and subsectors when possible to avoid large amounts of sparse tables
	  if( ((((String)wild.get(0)).matches(".*[Ss]ector") || ((String)wild.get(1)).matches(".*[Ss]ector"))) && currNode.getNodeName().equals(".*[Ss]ector") ) {
		  return tempMap;
	  }
	  if(currNode.hasAttributes() && !currNode.getNodeName().equals((String)wild.get(0)) && !currNode.getNodeName().equals((String)wild.get(1))) {
		String attr = getOneAttrVal(currNode);
		/*
		if(attr.equals("fillout=1")) {
			attr = getOneAttrVal(currNode, 1);
		}
		*/
		attr = currNode.getNodeName()+"@"+attr;
		if(!tempMap.containsKey(attr)) {
			tempMap.put(attr, new TreeMap());
		}
		return (Map)tempMap.get(attr);
	  }
	  return tempMap;
  }

  /**
   * Move down the dataTree map until we hit the level of node, as apposed to mappin, then the mapping 
   * one level up is the data map for a table, and it's path is described by title
   * @param dataTree the mappings of attrubutes which will get us to the data
   *        parent so that we can get the data map which is a level up once we hit the bottom
   *        regions column axis attrubutes
   *        years row axis attributes
   *        title a string describing the path in which the data in the table is coming from
   */
  private void recAddTables(Map dataTree, Map.Entry parent, Set regions, Set years, String titleStr) {
	Iterator it = dataTree.entrySet().iterator();
	while(it.hasNext()) {
		Map.Entry me = (Map.Entry)it.next();
		if(me.getValue() instanceof Node || me.getValue() instanceof Double || 
				me.getValue() instanceof String) {
			NewDataTableModel tM;
			if(me.getValue() instanceof Double || me.getValue() instanceof String) {
				tM = new NewDataTableModel(regions, qg.getAxis1Name(), years, 
						qg.getVariable(), title, (Map)parent.getValue(), doc,
						null, qg.shouldAppendRewriteValues() ? qg.getNodeLevelRewriteMap().values() : null);
				tM.setColNameIndex(qg.getChartLabelColumnName());
			} else {
				tM = new NewDataTableModel(regions, (String)wild.get(0), years, 
						(String)wild.get(1), title, (Map)parent.getValue(), doc,
						documentation, null); 
			}
			tM.units = units;
	  		if(tables == null) {
		  		tables = new Vector();
	  		}
			String labelStr = titleStr.replace("/", ",   ").replace("@", ": ");
			labelStr = labelStr.substring(2, labelStr.length());
			tables.add(labelStr);
            final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
            if(parentFrame == null) {
                tables.add(tM);
            } else {
                JTable jTable = tM.getAsSortedTable();

                jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

                jTable.setCellSelectionEnabled(true);

                javax.swing.table.TableColumn col;
                Iterator i = regions.iterator();
                int j = 1;
                while(i.hasNext()) {
                    col = jTable.getColumnModel().getColumn(j);
                    col.setPreferredWidth(((String)i.next()).length()*5+30);
                    if(qg == null) { // only want to do this when values might have documentation
                        col.setCellRenderer(tM.getCellRenderer(0, j));
                    }
                    j++;
                }
                CopyPaste copyPaste = new CopyPaste( jTable );
                JScrollPane tV = new JScrollPane(jTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                JComponent tableView = tV;
                if(me.getValue() instanceof Double || me.getValue() instanceof String) {
                    final JSplitPane sp = new JSplitPane();

                    final JLabel labelChart = new JLabel();
                    try {
                        JFreeChart chart = tM.createChart(0,0);
                        Dimension chartDim = tM.getChartDimensions(chart);
                        BufferedImage chartImage = chart.createBufferedImage( (int)chartDim.getWidth(), 
                                (int)chartDim.getHeight());
                        labelChart.setIcon(new ImageIcon(chartImage));
                    } catch(Exception e) {
                        labelChart.setText("Cannot Create Chart");
                    }

                    sp.setLeftComponent(tV);
                    JPanel lcPanel = new JPanel();
                    lcPanel.setLayout(new BoxLayout(lcPanel, BoxLayout.Y_AXIS));
                    lcPanel.add(labelChart);
                    lcPanel.add(Box.createVerticalGlue());
                    sp.setRightComponent(lcPanel);
                    tV.setPreferredSize(jTable.getPreferredSize());
                    tableView = sp;
                    Dimension tableViewSize = tableView.getPreferredSize();
                    tableViewSize.setSize(tableViewSize.getWidth(), Math.max(labelChart.getMinimumSize().getHeight(), jTable.getMinimumSize().getHeight()));
                    tableView.setPreferredSize(tableViewSize);

                    // This is not the corrent location however we may want to go ahead and do it
                    // since the split pane will be showing before we can set the corrent divider location
                    // and it is pretty evedent that the resize is going on.  So if we do the following
                    // maybe it won't be as evident.
                    sp.setDividerLocation(parentFrame.getWidth()-(int)labelChart.getMinimumSize().getWidth()-30);
                }
                tables.add(tableView);
            }

			return;
		} else {
			recAddTables((Map)me.getValue(), me, regions, years, titleStr+'/'+(String)me.getKey());
		}
	}
  }
        /**
	 * gets the instance of table editor used to be able to edit a table within a table cell
	 * @return tableEditor
	 */
	public TableCellEditor getCellEditor(int row, int col ) {
			return tableEditor;
	}

        /**
	 * gets the instance of table renderer used to be able to view a table within a table cell
	 * @return tableRenderer
	 */
	public TableCellRenderer getCellRenderer(int row, int col ) {
			return tableRenderer;
	}

	/**
	 * get the number of columns in the table
	 * @return always returns 1
	 */
	public int getColumnCount() {
		return 1;
	}

	/**
	 * Get the number of rows in the table. This is really tables * 2, since each table has a label.
	 * Also need to account for the tables which have been filtered out.
	 * @return The number of elements in activeRows
	 */
	public int getRowCount() {
		return activeRows.size();
	}

	/**
	 * returns the table at the requested cell
	 * @param row the row position of the cell
	 *        col the column position of the cell
	 * @return the table at the requested cell
	 */
	public Object getValueAt(int row, int col) {
		return tables.get(((Integer)activeRows.get(row)).intValue());
	}

	/**
	 * return the heading for the column
	 * @param col there is only really 1 column, so not used
	 * @return heading for the column
	 */
	public String getColumnName(int col) {
		return title; 
	}

	/**
	 * determines wheter a cell is editable, only tables are editable, which are every other row.
	 * @param row the row position being queryed
	 *        col the column position being queryed
	 * @return true or false depeneding on if the cell is editable
	 */
	public boolean isCellEditable(int row, int col) {
		if(row % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Updates activeRows to include only the tables which didn't come from any of the attributes filtered
	 * out. Does this by creating a regular expression in the form /nodeName[attrName=attrVal | any more
	 * attrubutes which are still valid]/next child. Then tests each table's label against the regular
	 * expression
	 * @param possibleFilters the vector nodeNames that had valid attributes for filtering
	 */
	protected void doFilter(Vector possibleFilters) {
		StringBuffer regex = new StringBuffer("^/");
		for(int i = possibleFilters.size()-1; i >= 0; i--) {
			Iterator it = ((Map)tableFilterMaps.get(possibleFilters.get(i))).entrySet().iterator();
			if(it.hasNext()) {
				regex.append((String)possibleFilters.get(i)).append("@(");
				while(it.hasNext()) {
					Map.Entry me = (Map.Entry)it.next();
					if(((Boolean)me.getValue()).booleanValue()) {
						regex.append(me.getKey()).append("|");
					}
				}
				/*
				if(regex.endsWith("|")) {
					regex = regex.substring(0,regex.length()-1)+")/";
					*/
				if(regex.charAt(regex.length()-1) == '|') {
					regex.deleteCharAt(regex.length()-1).append(")/");
				} else {
					regex.append(")/");
				}
			} else {
				regex.append((String)possibleFilters.get(i)).append("/");
			}
		}
		regex.append("$");
		String regexStr = regex.toString();
		System.out.println("Regex is: "+regexStr);
		Vector tempActive = new Vector();
		for(int i = 0; i < tables.size(); i+=2) {
			if(((String)tables.get(i)).matches(regexStr)) {
				tempActive.add(new Integer(i));
				tempActive.add(new Integer(i+1));
			}
		}
		activeRows = tempActive;
	}
	public JFreeChart createChart(int rowAt, int colAt) {
		return ((NewDataTableModel)((JTable)((JScrollPane)getValueAt(rowAt, colAt)).getViewport().getView()).getModel()).createChart(rowAt, colAt);
	}

	QueryGenerator qg;
	
	
	public MultiTableModel(QueryGenerator qgIn, Object[] scenarios, Object[] regions, DbProcInterrupt interrupt) throws Exception
    {
        qg = qgIn;
        title = qgIn.toString();
        wild = new ArrayList();

        wild.add(qgIn.getNodeLevel());
        wild.add(qgIn.getYearLevel());
        System.out.println("Before Function: "+System.currentTimeMillis());
        boolean isGlobal = regions.length == 1 && regions[0].equals("Global");
        buildTable(XMLDB.getInstance().createQuery(qgIn, scenarios, regions, interrupt), qgIn.isSumAll(), isGlobal);
        tableEditor = new TableEditor();
        tableRenderer = new TableRenderer();
        activeRows = new Vector(tables.size());
        for(int i = 0; i < tables.size(); i++) {
            activeRows.add(new Integer(i));
        }
        // add some html to make it look nice
        title = "<html><body><b>"+title+"</b> Comments: "+qg.getComments();
        int numTables = getRowCount() / 2;
        if(numTables > 1) {
            title += " ("+numTables+" Tables)";
        } else {
            title += " (1 Table)";
        }
        title += "</body></html>";
    }
    private void buildTable(QueryProcessor queryProc, boolean sumAll, boolean isGlobal) throws Exception {
        System.out.println("In Function: "+System.currentTimeMillis());
        Iter res = queryProc.iter();
        ANode tempNode;
        final Set<String> yearLevelAxis = new TreeSet<String>();
        final Set<String> nodeLevelAxis = new TreeSet<String>();
        yearLevelAxis.addAll(getDefaultYearList());
        final Map dataTree = new LinkedHashMap();
        final Map<String, String> rewriteMap = qg.getNodeLevelRewriteMap();
        // axisValues will be passed to the query generator which will set the
        // year level value as the key and the node level value as the value
        final DataPair<String, String> axisValues = new DataPair<String, String>();
        try {
            while((tempNode = (ANode)res.next()) != null) {
                // catgorize this result
                BXNode domNode = BXNode.get(tempNode);
                axisValues.setKey(null);
                axisValues.setValue(null);
                Map retMap = qg.addToDataTree(tempNode.parent(), dataTree, axisValues, isGlobal);
                if(axisValues.getKey() == null || axisValues.getValue() == null) {
                    throw new Exception("<html><body>Could not determine how to categorize the results.<br> Please check your axis node values.</body></html>");
                }

                // if we did sum all we will collapse them all by always setting the node level
                // to All + whatever the node level was
                if(sumAll) {
                    axisValues.setValue("All "+qg.getNodeLevel());
                }
                // check for rewrites
                if(rewriteMap != null && rewriteMap.containsKey(axisValues.getValue())) {
                    axisValues.setValue(rewriteMap.get(axisValues.getValue()));
                    if(axisValues.getValue().equals("")) {
                        continue;
                    }
                }

                // check if the row has already set it's units, we will only overwrite
                // it the very first time around for performance reasons, this means
                // there will be no checking for mismatched units
                if((units = (String)retMap.get("Units;"+axisValues.getValue())) == null) {
                    units = XMLDB.getAttrMap(BXNode.get(tempNode.parent())).get("unit");
                    if(units == null) {
                        units = "None Specified";
                    }
                }

                // add the node level and year level (into a set so we only have unique values)
                yearLevelAxis.add(axisValues.getKey());
                nodeLevelAxis.add(axisValues.getValue());

                // add number into the lowest level table
                // if there was already an entry in it's spot sum the values
                double currNumber = Double.parseDouble(domNode.getNodeValue());
                String currKey = axisValues.getKey()+";"+axisValues.getValue();
                Double ret = (Double)retMap.get(currKey);
                retMap.put(currKey, ret == null ? currNumber : ret + currNumber);

                // also add the value to the total sum
                currKey = axisValues.getKey()+";Total";
                ret = (Double)retMap.get(currKey);
                retMap.put(currKey, ret == null ? currNumber : ret + currNumber);

                // add the units for the current row under the Units column
                // This will use the unit seen from the first value in the
                // row, see comment above regarding units
                retMap.put("Units;"+axisValues.getValue(), units);
                retMap.put("Units;Total", units);
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            queryProc.close();
        }
        // check if we had no results
        if(dataTree.isEmpty()) {
            throw new Exception("The query returned no results.");
        }

        if(qg.shouldAppendRewriteValues()) {
            for(Iterator<String> rewriteValueIt = rewriteMap.values().iterator(); rewriteValueIt.hasNext(); ) {
                String currRewriteValue = rewriteValueIt.next();
                // the empty string is a special case which meant that we wanted to delete that row
                // and we definitely do not want the empty string as a row so skip it
                if(!currRewriteValue.equals("")) {
                    nodeLevelAxis.add(currRewriteValue);
                }
            }
        }
        // before we add Total make sure we stop sorting by turning the Set
        // into a LinkedHashSet
        final Set<String> nodeLevelAxisOrdered = new LinkedHashSet<String>(nodeLevelAxis);
        nodeLevelAxisOrdered.add("Total");
        yearLevelAxis.add("Units");

        if(remove1975) {
            yearLevelAxis.remove("1975");
        }
        System.out.println("After build Tree: "+System.currentTimeMillis());
        // now that results are sorted into maps we can create the actual tables
        recAddTables(dataTree, null, yearLevelAxis, nodeLevelAxisOrdered, "");
        System.out.println("After Add table: "+System.currentTimeMillis());
    }

  public void exportToExcel(HSSFSheet sheet, HSSFWorkbook wb, HSSFPatriarch dp) {
	  HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
	  row.createCell((short)0).setCellValue(getColumnName(0));
	  for(int rowN = 0; rowN < getRowCount(); rowN +=2) {
		  row = sheet.createRow(sheet.getLastRowNum()+1);
		  row.createCell((short)0).setCellValue(getValueAt(rowN,0).toString());
		  getModelAt(rowN+1).exportToExcel(sheet, wb, dp);
	  }
  }
	public boolean equals(Object other) {
		if(other == this) {
			return true;
		} else if(!(other instanceof BaseTableModel) || other == null) {
			return false;
		} else if(other instanceof NewDataTableModel) {
			for(int i = 1; i < getRowCount(); i += 2) {
				if(getModelAt(i) == other) {
					return true;
				}
			}
		}
		return false;
	}

	private BaseTableModel getModelAt(int row) {
		Object ret = getValueAt(row, 0);
        if(ret instanceof BaseTableModel) {
            return (BaseTableModel)ret;
        }
        else if(ret instanceof JSplitPane) {
			ret = ((JScrollPane)((JSplitPane)ret).getLeftComponent()).getViewport().getView();
		} else {
			ret = ((JScrollPane)ret).getViewport().getView();
		}
		return (BaseTableModel)((TableSorter)((JTable)ret).getModel()).getTableModel();
	}


	protected Node getNodeAt(int row, int col) {
		// this table model doesn't acctually hold any nodes
		return null;
	}

  public void annotate(int[] rows, int[] cols, Documentation documentation) {
	  JTable jTable = (JTable)((JScrollPane)getValueAt(rows[0], cols[0])).getViewport().getView();
	  ((BaseTableModel)jTable.getModel()).annotate(jTable.getSelectedRows(), jTable.getSelectedColumns(), documentation);
  }
  public String exportToText(char delimiter) {
	  String lineEnding = System.getProperty("line.separator");
	  StringBuilder ret = new StringBuilder();
	  // excel doesn't handle html properly so we will have 
	  // to build the title without html
	  String tempTitle = qg.toString() + " Comments: ";
	  String comments = qg.getRealComments();
	  if(comments == null) {
		  comments = "None";
	  }
	  tempTitle += comments;
	  int numTables = getRowCount() / 2;
	  if(numTables > 1) {
		  tempTitle += " ("+numTables+" Tables)";
	  } else {
		  tempTitle += " (1 Table)";
	  }
      if(tempTitle.indexOf(delimiter) != -1) {
          tempTitle = '"'+tempTitle+'"';
      }
	  ret.append(tempTitle).append(lineEnding);
	  for(int i = 0; i < getRowCount(); i += 2) {
          String value = getValueAt(i, 0).toString();
          if(value.indexOf(delimiter) != -1) {
              value = '"'+value+'"';
          }
		  ret.append(value).append(lineEnding)
					  .append(getModelAt(i+1).exportToText(delimiter));
	  }
	  return ret.toString();
  }
}
