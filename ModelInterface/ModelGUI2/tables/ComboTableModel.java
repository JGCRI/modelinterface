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

import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.DOMmodel;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.Documentation;
import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.undo.FlipUndoableEdit;
import ModelInterface.ModelGUI2.undo.FilterUndoableEdit;
import ModelInterface.ModelGUI2.undo.TableUndoableEdit;
import ModelInterface.ModelGUI2.xmldb.QueryBinding;
import ModelInterface.ModelGUI2.xmldb.DbProcInterrupt;
import ModelInterface.common.DataPair;

import java.io.IOException;
import java.util.*;
import org.apache.poi.hssf.usermodel.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.w3c.dom.*;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.table.TableCellRenderer;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.CompoundEdit;

import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.node.ANode;
import org.basex.api.dom.BXNode;
import org.basex.api.dom.BXElem;

public class ComboTableModel extends BaseTableModel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// new stuff
	Vector TreeMapVector = new Vector();
	Vector leftSideVector = new Vector();
	Vector leftHeaderVector;

	Vector indCol;
	Vector indRow;
	String ind1Name;
	String ind2Name;
	boolean flipped;
	TableCellRenderer documentationRenderer;
	int chartLabelCol  = -1;

	//Vector tables;

	/**
	 * Constructor initializes data members, and calls buildTable to create the arrays of data maps,
	 * and the path vectors for each data map. Creates the headers for the table axis and path, , also
	 * creates the filterMaps based on the path information.
	 * @param tp the Tree Path which was selected from the tree, needed to build table
	 *        doc needed to run the XPath query against
	 *        tableTypeString to be able to display the type of table this is
	 */ 
	public ComboTableModel(TreePath tp, Document doc, String tableTypeString, Documentation documentationIn) {
		super(tp, doc, tableTypeString, documentationIn);
		leftHeaderVector = null;
		wild = chooseTableHeaders(tp);
		wild.set(0, ((DOMmodel.DOMNodeAdapter)wild.get(0)).getNode().getNodeName());
		wild.set(1, ((DOMmodel.DOMNodeAdapter)wild.get(1)).getNode().getNodeName());
		buildTable(treePathtoXPath(tp, doc.getDocumentElement(), 0));
		activeRows = new Vector( leftSideVector.size() * indRow.size() );
		for(int i = 0; i < (leftSideVector.size() * indRow.size() ); i++) {
			activeRows.add(new Integer(i));
		}
		indCol.add(0, ind1Name);
		documentationRenderer = getDocumentationRenderer();
	}

	/**
	 * Switches the row and column headers, and names. Also sets a boolean so we know 
	 * it has been flipped, since it makes a difference how we reference into the data maps
	 * @param row not used here
	 *        col not used here
	 */
	public void flip(int row, int col) {
		Vector tempArr = indCol;
		indCol = indRow;
		indRow = tempArr;
		indRow.remove(0);
		String tempStr = ind1Name;
		ind1Name = ind2Name;
		ind2Name= tempStr;
		indCol.add(0, ind1Name);
		flipped = !flipped;
		// to set active rows appropriatly
		doFilter( new Vector(tableFilterMaps.keySet()) );
		fireTableStructureChanged();
        final InterfaceMain main = InterfaceMain.getInstance();
		if(row >= 0 && col >= 0) {
			UndoManager undoManager = main.getUndoManager();
			undoManager.addEdit(new FlipUndoableEdit(this));
		}
		main.refreshUndoRedo();
	}

	public TableCellRenderer getCellRenderer(int row, int col) {
		if( col <= leftHeaderVector.size() ){
			return null;
		} else {
			return documentationRenderer;
		}
	}

	/**
	 * Builds table and sets data maps, in a similar fashions as the MultiTableModel
	 * @see MultiTableModel#buildTable(XPathExpression)
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
	  indCol = new Vector( regions );
	  indRow = new Vector( years );
	  ind1Name = (String)wild.get(0);
	  ind2Name = (String)wild.get(1);
	}

	/**
	 * Gets path information from a node in a similar fashions as the MultiTableModel
	 * @param n the node to get path info from
	 * @param filterMaps temporary filter map to update
	 * @see MultiTableModel#getRegionAndYearFromNode(Node, Map)
	 */
	private Object[] getRegionAndYearFromNode(Node n, Map filterMaps) {
	  Vector ret = new Vector(2,0);
	  do {
		  if(n.getNodeName().equals((String)wild.get(0)) || n.getNodeName().equals((String)wild.get(1))) {
			  //ret.add(n.getAttributes().getNamedItem("name").getNodeValue());
			  if(!n.hasAttributes()) {
				  ret.add(n.getNodeName());
			  } else {
				ret.add(getOneAttrVal(n));
			  }
				  /*
			  } else if(!getOneAttrVal(n).equals("fillout=1")) {
				ret.add(getOneAttrVal(n));
			  } else {
					ret.add(getOneAttrVal(n, 1));
			  }
			  */

		  } else if(n.hasAttributes()) {
			  HashMap tempFilter;
			  if (filterMaps.containsKey(n.getNodeName())) {
				  tempFilter = (HashMap)filterMaps.get(n.getNodeName());
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
   * Sorts nodes in a map of maps creating a tree of data, same as in MulitTableModel.
   * @param currNode current level in tree being sorted
   * @param dataTree the entire data maps tree
   * @return the current map being used
   * @see MultiTableModel#addToDataTree(Node, Map)
   */
  private Map addToDataTree(Node currNode, Map dataTree) {
	  if (currNode.getNodeType() == Node.DOCUMENT_NODE) {
		  return dataTree;
	  }
	  Map tempMap = addToDataTree(currNode.getParentNode(), dataTree);
	  if( ((((String)wild.get(0)).matches(".*[Ss]ector") || ((String)wild.get(1)).matches(".*[Ss]ector"))) && currNode.getNodeName().matches(".*[Ss]ector") ) {
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
   * Similar to MultiTable model, except instead of creating a table adds the data to a vector of data maps, 
   * and splits the path string up into a vector for path info.
   * @param dataTree the mappings of attrubutes which will get us to the data
   * @param parent so that we can get the data map which is a level up once we hit the bottom
   * @param regions column axis attrubutes
   * @param years row axis attributes
   * @param title a string describing the path in which the data in the table is coming from
   * @see MultiTableModel#recAddTables(Map, Map.Entry, TreeSet, TreeSet, String)
   */
  private void recAddTables(Map dataTree, Map.Entry parent, Set regions, Set years, String titleStr) {
	Iterator it = dataTree.entrySet().iterator();
	while(it.hasNext()) {	
		Map.Entry me = (Map.Entry)it.next();
		if(me.getValue() instanceof Node || me.getValue() instanceof Double || me.getValue() instanceof String) {
			TreeMapVector.add( (Map)parent.getValue() );
			
			// create a left side 2d vector, add it to LeftSideVector
			
			String lineToParse = titleStr+'/';
			
			// example:		/populationSGMRate@year=1985/gender:type=female/
	
			// get rid of begin and end '/'
			lineToParse = lineToParse.substring( 1, lineToParse.length()-1 );
			
			StringTokenizer st = new StringTokenizer( lineToParse, "~", false);
			int numberOfThem = st.countTokens();
			
			Vector onerow = new Vector( numberOfThem );
			Vector tempVector = new Vector();
			while( st.hasMoreTokens() ){
				//onerow = new Vector( numberOfThem );
				String allNodeInfo = st.nextToken(); // first one
				// 		populationSGMRate@year=1985
				StringTokenizer innerSt = new StringTokenizer( allNodeInfo, "@", false);
				if( innerSt.countTokens() != 2 ){
					System.out.println("BIG PROBLEM, COUNT TOKENS ISN'T 2!!!!!!!!!!: "+allNodeInfo);
					System.out.println("lineToParse: "+lineToParse);
					System.out.println("allNodeInfo: "+allNodeInfo);
					//return;
				} else {
				String firstHalf = innerSt.nextToken(); //	populationSGMRate
				if(leftHeaderVector == null){
					tempVector.add( firstHalf );
				}
				String secHalf = innerSt.nextToken(); //	year=1985
				onerow.add( secHalf );
				}
			}
			if(leftHeaderVector == null) {
				leftHeaderVector = tempVector;
			}
			if( ! onerow.isEmpty() ){
				leftSideVector.add( onerow );
			}
			return;
		}else{
			recAddTables((Map)me.getValue(), me, regions, years, titleStr+"~"+(String)me.getKey());
		}
	}
  }
  
  /**
   * Gets the Key needed to reference into the data map, given a row and col position in the table
   * @param row the row for which we should get the row key
   * @param col the col for which we should get the col key
   * @return a string in format key1;key2
   */
  private String getKey (int row, int col) {
	  // if it is flipped the row needs to go first
	  // need to mod by the number of data blocks we have to get the correct key for the row
	  // and have to take into account the additional column headers for path
	  if(flipped) {
		  return (String)indRow.get(row % (indRow.size()))+";"+(String)indCol.get(col - leftHeaderVector.size());
	  }
	  return (String)indCol.get(col- leftHeaderVector.size())+";"+(String)indRow.get(row % (indRow.size()));
  }

  
        /**
	 * Returns the total number of column headers, which include the path headers
	 * one space for row axis, and column headers
	 * @return total number of column headers
	 */
	public int getColumnCount() {
		return leftHeaderVector.size() + indCol.size();
	}

	/**
	 * Total number of rows, does not include rows that have been filtered out
	 * @return length of acitveRows
	 */
	public int getRowCount() {
		return activeRows.size();
	}

	/**
	 * Returns the value to be displayed in the table at a certain position.
	 * @param row the row position in the table
	 *        col the col position in the table
	 * @return the data at the position requested
	 */
	public Object getValueAt(int row, int col) {
		try{
			// this is part of the path get info from leftHeaderVector
			if( col < leftHeaderVector.size() ){
				return ((Vector)leftSideVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( col );
			// this is the col for row axis
			}else if( col == leftHeaderVector.size() ){
				return indRow.get( ((Integer)activeRows.get( row )).intValue() % (indRow.size()) );
			// these columns represent data
			}else{
				Object temp = ((Map)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( (Integer)activeRows.get(row), col ) );
				if(temp instanceof Node) {
					return new Double(((Node)temp).getNodeValue());
				} else if(temp instanceof Double) {
					return (Double)temp;
				} else if(temp == null && doc == null) {
					return new Double(0.0);
				} else {
					return temp;
				}
			}
		} catch(NullPointerException e) {
			return "";
		} catch(NumberFormatException nf) { // if the data is not numbers
			Object temp = ((Map)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) );
			if(temp instanceof Node) {
				return ((Node)temp).getNodeValue();
			} else {
				nf.printStackTrace();
			}
		} catch(IndexOutOfBoundsException indEx) {
			// can happen when the data is not at the same level in the XML in which case
			// the label would be not applicable
			return "N/A";
		}
		return "";
	}

	/**
	 * Determine if there is a real data value pointed to at
	 * at the passed in row and col. This equates to checking
	 * in the correct Map in TreeMapVector and see if the 
	 * key at this pos exists.
	 * @param row The row to check.
	 * @param col The column to check.
	 * @return True if there is a real value, false otherwise. 
	 */ 
	protected boolean hasValueAt(int row, int col) {
		return col > leftHeaderVector.size() && 
			((Map)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() 
					 / (indRow.size()))).containsKey( 
					 getKey( (Integer)activeRows.get(row), col ) );
	}

	/**
	 * returns the actual Node that is contained at the position row, col in the table
	 * @param row position in table
	 * 	  col position in table
	 * @return the Node is the position, or null if it was an invalid positon
	 */
	protected Node getNodeAt(int row, int col) {
		if( col <= leftHeaderVector.size() ){
			return null;
		}
		Object temp = ((Map)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) );
		if(temp instanceof Node) {
			return (Node)temp;
		} else {
			// annotate shouldn't be enabled
			return null;
		}
	}

	/**
	 * returns the attr value which defines the column/path by number passed in
	 * @param column an integer position to define which column
	 * @return the header value in the column index, or path index  at the position passed in
	 */
	public String getColumnName(int col) {
		if( col < leftHeaderVector.size() ){
			return (String)leftHeaderVector.get( col );
		}else{
			return (String)indCol.get( col - leftHeaderVector.size() );
		}
	}

	/**
	 * Get the class for the data that will be in the specified class.  This
	 * will return String.class for the left lable columnd and the last column
	 * which is the units column elseDouble.class for the rest.
	 * @param columnIndex The column being queried.
	 * @return Double.class for leftHeaderVector.size()  
	 * 	&lt; columnIndex &lt; getColumnCount()-1 else String.class
	 */
	public Class getColumnClass(int columnIndex) {
		return (columnIndex > leftHeaderVector.size()) 
			&& (columnIndex < getColumnCount()-1)? Double.class : String.class;
	}

	/**
	 * Used to tell which cells are editable, which are all but the path columns and 
	 * the column for row headers.
	 * @param row the row position being queryed
	 *        col the column position being queryed
	 * @return true or false depeneding on if the cell is editable
	 */
	public boolean isCellEditable(int row, int col) {
		if(doc == null) {
			return false;
		}
		if( col <= leftHeaderVector.size() ){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * Update the activeRows vector by going through all of the path information, and
	 * the filterMaps and determining if any of the rows correspond the the qualifying
	 * path info.
	 * @param possibleFilters the list of nodeNames in the filterMap that wil be fillered
	 */
	protected void doFilter(Vector possibleFilters) {
		        // reset the activeRows
			Vector oldActiveRows = activeRows;
			activeRows = new Vector();
			for (int i = 0; i < (leftSideVector.size() * indRow.size()); i++) {
				activeRows.addElement(new Integer(i));
			}
			Integer rowPos = new Integer(-1);

			// Should be able to make this more efficient, but just need it to work right now
			// goes through all of the nodeNames in the filterMaps
			// then goes through each of its different attrubutes that are filtered out 
			// and then goes through all of activeRows to see if they have that attrubutes 
			// in the pathVector, 
			for (int i = 0; i < possibleFilters.size(); i++) {
				if (((String)possibleFilters.get(i)).equals("")) {
					continue;
				}
				currKeys = (String[])((Map)tableFilterMaps.get((String)possibleFilters.get(i))).keySet().toArray(new String[0]);
				//for (Iterator it = activeRows.iterator(); it.hasNext(); rowPos = (Integer)it.next()) {
				Iterator it = activeRows.iterator();
				while (it.hasNext()) {
					rowPos = (Integer)it.next();
					for (int j = 0; j < currKeys.length; j++) {
						if (!((Boolean)((Map)tableFilterMaps.get((String)possibleFilters.get(i))).get(currKeys[j])).booleanValue() ){
							if (((String)((Vector)leftSideVector.get( rowPos.intValue() / (indRow.size()) )).get( possibleFilters.size()-i-1 )).equals(currKeys[j])){
								it.remove();
								break;
	
							}
						}
					}
				}
			}
        final InterfaceMain main = InterfaceMain.getInstance();
		UndoManager undoManager = main.getUndoManager();
		// what about changeing the filter map
		undoManager.addEdit(new FilterUndoableEdit(this, oldActiveRows, activeRows));
		main.refreshUndoRedo();
	}
	
	/**
	 * Update the value of a cell in the table, same as in NewDataTableModel 
	 * @param val new value the cell should be changed to
	 * @param row the row of the cell being edited
	 * @param col the col of the cell being edited
	 * @see NewDataTableModel#setValueAt(Object, int, int)
	 */
	public void setValueAt(Object val, int row, int col) {
		
		Map data = ((Map)TreeMapVector.get( row / (indRow.size())));
		CompoundEdit setEdit = new CompoundEdit();

		Object o = data.get(getKey(row,col));
		Node n = null;
		if(o != null && o instanceof Double) {
			data.put(getKey(row, col), val);
			return;
		} else {
			n = (Node)o;
		}
		if( n != null ){
			String oldVal = n.getNodeValue();
			n.setNodeValue(val.toString());
			// data and key won't be necessary here so just giving it null
			setEdit.addEdit(new TableUndoableEdit(this, row, col, n, oldVal, null, null));
		}else{
			n = doc.createTextNode( val.toString() );
			n.setUserData("isSetValue", setEdit, null);
			//Node updown = null;
			Node side = null;

			// Try to look in table for value in this column
			/*
			for(int i = 0; i < getRowCount() && ( updown = ((Node)((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( i )).intValue() / (indRow.size()))).get( getKey( i, col ) ))) /*(Node)data.get(getKey(i, col))/ == null; ++i) {
			}
			*/
			// Try to look in this row to see if there is a value
			for(int i = leftHeaderVector.size()+1; i < getColumnCount() && 
					( side = (Node)data.get(getKey(row, i))) == null; ++i) {
			}
			// If there weren't values in the same column and row won't be 
			// able to figure out the path down the tree to put the data
			if(/* updown == null ||*/ side == null ) {
				// throw some exception
				System.out.println("Couldn't gather enough info to create Node");
				InterfaceMain.getInstance().showMessageDialog(
						"Couldn't gather enough information to \ncreate the data",
						"Set Value Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			ArrayList nodePath = new ArrayList();
			Node parent = ((Node)side.getParentNode());
			
			
			String headerone = ind1Name; // ex. region
			String headertwo = ind2Name; // ex. populationSGM
			
			String attributesLine = getKey( row, col );
			String[] attributesLineArray = attributesLine.split(";", 2);
			if(flipped) {
				String temp; //= attributesLineArray[0];
				/*
				attributesLineArray[0] = attributesLineArray[1];
				attributesLineArray[1] = temp;
				*/
				temp = headerone;
				headerone = headertwo;
				headertwo = temp;
			}

			StringTokenizer st = new StringTokenizer( attributesLineArray[ 1 ], "=", false);
			
			String attrFrom1 = st.nextToken();
			String attrTo1 = st.nextToken();
			
			st = new StringTokenizer( attributesLineArray[0], "=", false);
			String attrFrom2 = st.nextToken();
			String attrTo2 = st.nextToken();

			// Work our way up the until we find the tag for corrent
			// column header which by the way the axis are chosen should 
			// always be higher in the path
			while( !parent.getNodeName().equals( headerone ) ) {
				nodePath.add(parent);
				parent = parent.getParentNode();
			}

			// Go down the path back to where the value should be
			// if there needs to be nodes created they will be using info 
			// from the row header, or the path info from the same row
			parent = parent.getParentNode();
			parent = checkPath(parent, headerone, attrFrom1, attrTo1, setEdit);
			for(int i = nodePath.size()-1; i >= 0; --i) {
				Element temp = (Element)nodePath.get(i);
				if(temp.getNodeName().equals(headertwo)) {
					parent = checkPath(parent, headertwo, attrFrom2, attrTo2, setEdit);
				} else {
					Node attrTemp = temp.getAttributes().item(0);
					if(attrTemp == null) {
						parent = checkPath(parent, temp.getNodeName(), null, null, setEdit);
					} else {
						parent = checkPath(parent, temp.getNodeName(), attrTemp.getNodeName(), 
								attrTemp.getNodeValue(), setEdit);
					}
				}
			}

			parent.appendChild( n );
			data.put( getKey(row,col), n );
			// need to add and edit that tells the table to make this pos null again
			setEdit.addEdit(new TableUndoableEdit(this, row, col, n, null, data, getKey(row, col)));
		}
		setEdit.end();
        final InterfaceMain main = InterfaceMain.getInstance();
		UndoManager undoManager = main.getUndoManager();
		undoManager.addEdit(setEdit);
		main.refreshUndoRedo();
		
		fireTableCellUpdated(row, col);

		// fireOffSomeListeners?

	}

	/** 
	 * Used to follow the path down a tree where parent is the current parent and want to 
	 * go under the node with the passed in node name and attributes will create the node
	 * if it does not exsit
	 * @param parent current node were are at in the path
	 * @param nodeName name of the node that we want to follow
	 * @param attrKey attribute name of the node that we want to follow
	 * @param attrVal attribute value of the node that we want to follow
	 * @return the pointer to the node we wanted to follow
	 */
	private Node checkPath(Node parent, String nodeName, String attrKey, String attrVal, UndoableEdit setEdit) {
		NodeList nl = parent.getChildNodes();
		for(int i = 0; i < nl.getLength(); ++i) {
			Element temp = (Element)nl.item(i);
			if(temp.getNodeName().equals(nodeName) && attrKey == null) {
				return temp;
			} else if(temp.getNodeName().equals(nodeName) && temp.getAttribute(attrKey).equals(attrVal)) {
				return temp;
			}
		}
		Element newElement = doc.createElement(nodeName);
		newElement.setUserData("isSetValue", setEdit, null);
		if(attrKey != null) {
			newElement.setAttribute(attrKey, attrVal);
		}
		parent.appendChild(newElement);
		return newElement;
	}


	public JFreeChart createChart(int rowAt, int colAt) {
		// Start by creating an XYSeriesSet to contain the series.
		XYSeriesCollection chartData = new XYSeriesCollection();
		// Loop through the rows and create a data series for each.
		for( int row = 0; row < getRowCount(); ++row ){
			// Row name is at element zero.
			//String rowNameFull = (String)getValueAt(row,0);
			String rowNameFull;
			if(qg != null && qg.isGroup()) {
				// If the qg is group and it is creating a combo table
				// that means it is a single query.  That means we 
				// want the labels to be a combo for scenario+region
				rowNameFull = (String)getValueAt(row, 0)+" "+(String)getValueAt(row, 1);
			} else if(chartLabelCol >= 0) {
				rowNameFull = (String)getValueAt(row, chartLabelCol);
			} else {
				rowNameFull = (String)indRow.get( ((Integer)activeRows.get( row )).intValue() % (indRow.size()) );
			}
			
			// Split out the name attribute if it contains it.
			String rowName;
            rowName = rowNameFull;
			XYSeries currSeries = new XYSeries(rowName);
			// Skip column 1 because it contained the label.
			for( int col = leftHeaderVector.size() + 1; col < getColumnCount(); ++col ){
				String fullColumn = getColumnName(col);
				if(fullColumn.equals("Units")) {
					continue;
				}
				double yValue = ( (Double)getValueAt(row, col) ).doubleValue();
				// Get the year part of it.
				if(fullColumn.indexOf("=") != -1) {
					fullColumn = fullColumn.split("=")[1];
				}
				double year = Double.parseDouble( fullColumn );
				// only add if yValue is a legit value
				if(yValue != 0 || hasValueAt(row, col)) {
					currSeries.add( year, yValue);
				}
			}
			// Add the series to the set.
			chartData.addSeries(currSeries);
		}
		// Done adding series, create the chart.
		// Create the domain axis label.
		// TODO: Improve naming.
		NumberAxis xAxis;
		if(qg != null) {
			xAxis = new NumberAxis(qg.getAxis2Name());
		} else {
			xAxis = new NumberAxis("Year");
		}
		//NumberAxis xAxis = new NumberAxis("Year");
		
		// Use the parent element name as the name of the axis.
		NumberAxis yAxis;
		String appendUnits;
		if(units != null) {
			appendUnits = " ("+units+")";
		} else {
			appendUnits = "";
		}
		if(qg != null) {
			yAxis = new NumberAxis(qg.getVariable()+appendUnits);
		} else {
			yAxis = new NumberAxis(ind1Name+appendUnits);
		}
		
		// This turns off always including zero in the domain.
		xAxis.setAutoRangeIncludesZero(false);
		
		// This turns on automatic resizing of the domain..
		xAxis.setAutoRange(true);
		
		// This turns on automatic resizing of the range.
		yAxis.setAutoRange(true);
		
		// Create the plot.
		XYPlot xyPlot = new XYPlot( chartData, xAxis, yAxis, new XYLineAndShapeRenderer());
		
		// Draw the zero line.
		//xyPlot.setZeroRangeBaselineVisible(true);
		
		// Create the chart.
		JFreeChart chart = new JFreeChart( xyPlot );
		
		// Create a title for the chart.
		TextTitle ttitle = new TextTitle(title);
		chart.setTitle(ttitle);
		return chart;
	}

	public void setColNameIndex(String name) {
		if(name != null) {
			for(int i = 0; i < getColumnCount(); ++i) {
				if(name.equals(getColumnName(i))) {
					chartLabelCol = i;
					return;
				}
			}
		}
		chartLabelCol = -1;
	}

	protected QueryGenerator qg;
	public ComboTableModel(QueryGenerator qgIn, Object[] scenarios, Object[] regions, 
            QueryBinding singleBinding, DbProcInterrupt interrupt) throws Exception 
    {
        qg = qgIn;
        //title = qgIn.getVariable();
        title = qgIn.toString();
        boolean isTotal = false;
        boolean isGlobal = regions.length == 1 && regions[0].equals("Global");
        System.out.println("Before Function: "+System.currentTimeMillis());
        if(singleBinding == null) {
            buildTable(XMLDB.getInstance().createQuery(qgIn, scenarios, regions, interrupt), qgIn.isSumAll(), 
                    isTotal, isGlobal);
        } else {
            // TODO: figure out a better way of telling if this is a Total
            isTotal = !(singleBinding instanceof ModelInterface.ModelGUI2.xmldb.SingleQueryQueryBinding);
            buildTable(XMLDB.getInstance().createQuery(singleBinding, scenarios, regions, interrupt), 
                    qgIn.isSumAll(), isTotal, isGlobal);
        }
        ind2Name = qgIn.getVariable();
        indCol.add(0, ind1Name);
        // adjust active rows to remove any rows that are all blank
        // we should make sure we don't remove any rows which were added
        // because of the shouldAppendRewriteValues flag unless this is
        // a single query
        activeRows = new Vector( leftSideVector.size() * indRow.size() );
        Collection<String> rewriteValues = singleBinding == null && qg.shouldAppendRewriteValues()
            ? qg.getNodeLevelRewriteMap().values() : null;
        for(int i = 0; i < (leftSideVector.size() * indRow.size() ); i++) {
            boolean allNulls = true;
            for( int col = leftHeaderVector.size() + 1; col < getColumnCount() && allNulls; ++col ) {
                if(((Map)TreeMapVector.get( i 
                                / (indRow.size()))).get( getKey( i, col ) ) != null) {
                    allNulls = false;
                                }
            }
            if(!allNulls || (rewriteValues != null && rewriteValues.contains(indRow.get(i % indRow.size())))) {
                activeRows.add(new Integer(i));
            }
        }
        setColNameIndex(qg.getChartLabelColumnName());
    }
    private void buildTable(QueryProcessor queryProc, boolean sumAll, boolean isTotal, boolean isGlobal) throws Exception {
        System.out.println("In Function: "+System.currentTimeMillis());
        Iter res = queryProc.iter();
        ANode tempNode;
        final Set<String> yearLevelAxis = new TreeSet<String>();
        final Set<String> nodeLevelAxis = new TreeSet/*LinkedHashSet*/<String>();
        yearLevelAxis.addAll(getDefaultYearList());
        final Map dataTree = new TreeMap();
        final Map<String, String> rewriteMap = qg.getNodeLevelRewriteMap();
        // axisValues will be passed to the query generator which will set the
        // year level value as the key and the node level value as the value
        final DataPair<String, String> axisValues = new DataPair<String, String>();
        try {
            while((tempNode = (ANode)res.next()) != null) {
                BXNode domNode = BXNode.get(tempNode);
                // catgorize this result
                axisValues.setKey(null);
                axisValues.setValue(null);
                Map retMap = qg.addToDataTree(tempNode.parent(), dataTree, axisValues, isGlobal);
                if(axisValues.getKey() == null || axisValues.getValue() == null) {
                    System.out.println("Key: "+axisValues.getKey());
                    System.out.println("Value: "+axisValues.getValue());
                    throw new Exception("<html><body>Could not determine how to categorize the results.<br> Please check your axis node values.</body></html>");
                }

                // special case for the total single query value
                if(isTotal) {
                    axisValues.setValue("Total");
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

                // add the units for the current row under the Units column
                // This will use the unit seen from the first value in the
                retMap.put("Units;"+axisValues.getValue(), units);
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

        if(remove1975) {
            yearLevelAxis.remove("1975");
        }
        yearLevelAxis.add("Units");
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
        System.out.println("After build Tree: "+System.currentTimeMillis());
        recAddTables(dataTree, null, yearLevelAxis, nodeLevelAxis, "");
        System.out.println("After Add table: "+System.currentTimeMillis());
        indRow = new Vector(nodeLevelAxis);
        indCol = new Vector(yearLevelAxis);
        ind1Name = qg.getAxis1Name();
    }
  public void exportToExcel(HSSFSheet sheet, HSSFWorkbook wb, HSSFPatriarch dp) {
	  HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
	  row.createCell((short)0).setCellValue("title");
	  int isGlobal = 0;
	  //row = sheet.createRow(sheet.getLastRowNum()+1);
	  for(int i = 0; i < getColumnCount(); ++i) {
		  // hack to get globals to show up in output
		  // assumes region will always be in 1 
		  String colName = getColumnName(i);
		  if(i == 1 && !colName.equals("region")) {
			  isGlobal = 1;
			  row.createCell((short)(i+1)).setCellValue("region");
		  }
		  row.createCell((short)(i+1+isGlobal)).setCellValue(colName);
	  }
	  for(int rowN = 0; rowN < getRowCount(); ++rowN) {
		  row = sheet.createRow(sheet.getLastRowNum()+1);
		  row.createCell((short)0).setCellValue(title);
		  if(isGlobal == 1) {
			  row.createCell((short)1).setCellValue(getValueAt(rowN,0).toString());
			  row.createCell((short)2).setCellValue("Global");
		  }
		  for(int col = isGlobal; col < getColumnCount(); ++col) {
			  Object obj = sortedTable.getValueAt(rowN, col);
			  if(obj instanceof Double) {
				  row.createCell((short)(col+1+isGlobal)).setCellValue(((Double)obj).doubleValue());
			  } else {
				  row.createCell((short)(col+1+isGlobal)).setCellValue(getValueAt(rowN,col).toString());
			  }
		  }
	  }
	  if(dp == null) {
		  // did not want images so just return now we are done
		  return;
	  }
	  try {
		  //for a good chart, the number of rows labeling the cart is added the the 350*350 square
		  //at 10 pixels per line and 3 labels per line
		  double add = getRowCount()/6*10;

		  //adjusts the standard size of 350*350+rows to be as large or small as desired
		  //TODO: make this controlled by the user
		  double sizeMult = 1.4;

		  int imgWidth = (int)(350*sizeMult);
		  int imgHeight =(int)(sizeMult*(350+add));
		  java.awt.image.BufferedImage chartImage = createChart(0,0).createBufferedImage(imgWidth,imgHeight);



		  //TODO: figure out how many pixels are in a char dependent on system
		  int pixelWidthPerChar = 8;
		  short firstRow = (short) (sheet.getLastRowNum()-getRowCount());
		  short firstCol = (short)(sheet.getRow(firstRow).getLastCellNum()+2);
		  short colSpan = (short) (imgWidth/(pixelWidthPerChar*(sheet.getColumnWidth((short)(getColumnCount()+1)))) + firstCol);
		  short rowSpan = (short)((imgHeight/(sheet.getDefaultRowHeightInPoints()*5/3))  +firstRow);


		  // WARNING: This is a hack because of java some how looking to load some class that did
		  // not exist.  Instead of using the utilities which uses the Factory which uses the 
		  // reflextion which causes that mess I will use this encoder directly.


		  int where = wb.addPicture(new org.jfree.chart.encoders.SunJPEGEncoderAdapter().encode(chartImage), HSSFWorkbook.PICTURE_TYPE_JPEG);
		  dp.createPicture(new HSSFClientAnchor(0,0,255,255,firstCol,firstRow,colSpan,rowSpan), where);


	  } catch(IOException ioe) {
		  ioe.printStackTrace();
	  }
  }

  public void annotate(int[] rows, int[] cols, Documentation documentation) {
	  Vector<Node> selectedNodes = new Vector<Node>(rows.length*cols.length, 0);
	  for(int i = 0; i < rows.length; ++i) {
		  for(int j = 0; j < cols.length; ++j) {
			  selectedNodes.add(getNodeAt(rows[i], cols[j]));
		  }
	  }
	  documentation.getDocumentation(selectedNodes, rows, cols);
  }
}
