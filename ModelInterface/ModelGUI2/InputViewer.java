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
package ModelInterface.ModelGUI2;

import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import org.w3c.dom.bootstrap.*;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.jfree.chart.JFreeChart;

import javax.xml.xpath.*;

import javax.xml.transform.TransformerException;

import javax.swing.event.*;
import javax.swing.tree.TreeSelectionModel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import javax.swing.tree.TreePath;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import ModelInterface.InterfaceMain;
import ModelInterface.MenuAdder;
import ModelInterface.BatchRunner;

import ModelInterface.ModelGUI2.tables.*;
import ModelInterface.ModelGUI2.csvconv.CSVToXMLMain;

import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.common.RecentFilesList;
import ModelInterface.common.RecentFilesList.RecentFile;

public class InputViewer implements ActionListener, TableModelListener, MenuAdder, BatchRunner {

	private InputViewer thisViewer;

	private Document doc;

	private LSInput lsInput;

	private LSParser lsParser;

	private DOMImplementationLS implls;

	private int lastFlipX = 0;

	private int lastFlipY = 0;

	public static String controlStr = "InputViewer";

	JSplitPane splitPane;

	JLabel infoLabel;

	//JTextField nameField;

	//JTextField attribField;

	//JTextField valueField;

	//Document lastDoc;

	//JTextArea textArea;

	JTree jtree;

	//JTable jTable;

	private JPopupMenu treeMenu;

	private TreePath selectedPath;

	private JDialog addChildDialog;

	private JPopupMenu tableMenu; // for new 'flip' right click option
	
	private JFrame chartWindow = null;
	
	private JPanel chartPanel = null;
	//private JLabel labelChart = null;

	XMLFilter xmlFilter = new XMLFilter();

	CSVFilter csvFilter = new CSVFilter();

	private TableSelector tableSelector;

	private File file;


	private int leftWidth;

	private Documentation documentation;

	public InputViewer() {
        final InterfaceMain main = InterfaceMain.getInstance();
        final JFrame parentFrame = main.getFrame();
		try {
            /*
			System.setProperty(DOMImplementationRegistry.PROPERTY,
					//"com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");
					"org.apache.xerces.dom.DOMImplementationSourceImpl");
                    */
			DOMImplementationRegistry reg = DOMImplementationRegistry
					.newInstance();
			implls = (DOMImplementationLS)reg.getDOMImplementation("XML 3.0");
			if (implls == null) {
				System.out
						.println("Could not find a DOM3 Load-Save compliant parser.");
				InterfaceMain.getInstance().showMessageDialog(
						"Could not find a DOM3 Load-Save compliant parser.",
						"Initialization Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			lsInput = implls.createLSInput();
			lsParser = implls.createLSParser(
					DOMImplementationLS.MODE_SYNCHRONOUS, null);
			lsParser.setFilter(new ParseFilter());
		} catch (Exception e) {
			System.err.println("Couldn't initialize DOMImplementation: " + e);
			InterfaceMain.getInstance().showMessageDialog(
					"Couldn't initialize DOMImplementation\n" + e,
					"Initialization Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		thisViewer = this;
        if(parentFrame == null) {
            // no gui components available such as in batch mode.
            return;
        }

		// Create a window to display the chart in.
		chartWindow = new JFrame( "Charts" );
		chartPanel = new JPanel();
		chartPanel.setLayout( new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
		
		// Allow the chart panel to scroll if the user selects it.
		chartPanel.setAutoscrolls(true);
		chartPanel.addMouseMotionListener(new MouseMotionListener(){
		    public void mouseDragged(MouseEvent aEvent) {
			    System.out.println("Dragging");
		        // Use the drag even to force a scroll to the event position.
		        Rectangle currRect = new Rectangle(aEvent.getX(), aEvent.getY(), 1, 1);
		        chartPanel.scrollRectToVisible(currRect);
		    }

			public void mouseMoved(MouseEvent aEvent) {
				// Ignore mouse movement if not dragging.
			}
		}
		);
		chartWindow.setContentPane(new JScrollPane(chartPanel));
		((JScrollPane)chartWindow.getContentPane()).setPreferredSize(new Dimension(520,530));
		chartWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				chartPanel.removeAll();
				//chartWindow.getContentPane().removeAll();
			}
		});
		final PropertyChangeListener savePropListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				// TODO: listen for undo/redo or just get it right!
				if(e.getPropertyName().equals("Document-Modified")) {
					main.getSaveMenu().setEnabled(true);
				} else if(e.getPropertyName().equals("Document-Save")) {
					main.getSaveMenu().setEnabled(false);
				}
			}
		};

		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("Control")) {
					if(evt.getOldValue().equals(controlStr) || 
						evt.getOldValue().equals(controlStr+"Same")) {
						main.getSaveMenu().removeActionListener(thisViewer);
						main.getSaveAsMenu().removeActionListener(thisViewer);
						//main.getUndoMenu().removeActionListener(thisViewer);
						//main.getRedoMenu().removeActionListener(thisViewer);
						main.getSaveAsMenu().setEnabled(false);
						//main.getUndoMenu().setEnabled(false);
						//min.getRedoMenu().setEnabled(false);
						parentFrame.removePropertyChangeListener(savePropListener);
						//main.getQuitMenu().removeActionListener(thisViewer);
						main.getSaveMenu().setEnabled(false);
						doc = null;
						documentation = null;
						main.getUndoManager().discardAllEdits();
						main.refreshUndoRedo();
						parentFrame.getContentPane().removeAll();
						parentFrame.setTitle("ModelInterface");
						if(splitPane != null) {
							main.getProperties().setProperty("dividerLocation", 
								 String.valueOf(splitPane.getDividerLocation()));
						}
					}
					if(evt.getNewValue().equals(controlStr)) {
						main.getSaveMenu().addActionListener(thisViewer);
						main.getSaveAsMenu().addActionListener(thisViewer);
						//main.getUndoMenu().addActionListener(thisViewer);
						//main.getRedoMenu().addActionListener(thisViewer);
						main.getSaveAsMenu().setEnabled(true);
						//main.getUndoMenu().setEnabled(true);
						//main.getRedoMenu().setEnabled(true);
						parentFrame.addPropertyChangeListener(savePropListener);
						//main.getQuitMenu().addActionListener(thisViewer);
						//main.oldControl = "FileChooserDemo.File";
						leftWidth = Integer.parseInt(main.
							getProperties().getProperty("dividerLocation", "200"));
					}
				}
			}
		});
		tableSelector = new TableSelector();
	}

	public void addMenuItems(InterfaceMain.MenuManager menuMan) {
        final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		JMenuItem menuItem = new JMenuItem("XML file");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2,
				ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).
			getSubMenuManager(InterfaceMain.FILE_OPEN_SUBMENU_POS).addMenuItem(menuItem, 10);

		menuItem = new JMenuItem("CSV file");
		menuItem.addActionListener(this);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).
			getSubMenuManager(InterfaceMain.FILE_OPEN_SUBMENU_POS).addMenuItem(menuItem, 20);
		int addedTo;
		//JMenu tableMenu = new JMenu("Table");
		addedTo = menuMan.addMenuItem(new JMenu("Table"), 10);
		final JMenuItem menuTableFilter = makeMenuItem("Filter");
		menuTableFilter.setEnabled(false);
		menuMan.getSubMenuManager(addedTo).addMenuItem(menuTableFilter, 0);
		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("Control")) {
					//if(evt.getOldValue().equals(controlStr)) {
						menuTableFilter.setEnabled(false);
					//}
				} else if(evt.getPropertyName().equals("Table")) {
					menuTableFilter.setEnabled(true);
				}
			}
		});
	}





	/**
	 * Creates a new JTree with the current doc, then sets up a splitPane to
	 * hold the JTree on the left, and and empty pane for future tables of the
	 * right. Also creates the listener for the JTree so right click options can
	 * be handled.
	 */
	public void displayJtree() {
        final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		Container contentPane = parentFrame.getContentPane();
		contentPane.removeAll();
		// Set up the tree
		jtree = new JTree(new DOMmodel(doc));
		jtree.setEditable(true);
		jtree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		jtree.setShowsRootHandles(true);
		jtree.getModel().addTreeModelListener(new MyTreeModelListener());
		jtree.setCellEditor( new DOMTreeCellEditor());

		//listen for right click on the tree
		jtree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					selectedPath = jtree.getClosestPathForLocation(e.getX(), e
							.getY());
					jtree.setSelectionPath(selectedPath);
					MenuElement[] me = treeMenu.getSubElements();
					// Display Table is only availabe on elements that contain
					// text data
					// Add Child is available on non text nodes
					for (int i = 0; i < me.length; i++) {
						if (((JMenuItem) me[i]).getText().equals(
								"Display Table")) {
							Node currentNode = ((DOMmodel.DOMNodeAdapter) jtree
									.getLastSelectedPathComponent()).getNode();
							if (currentNode.getFirstChild() != null
									&& currentNode.getFirstChild()
											.getNodeType() == Element.TEXT_NODE) {
								((JMenuItem) me[i]).setEnabled(true);
							} else {
								((JMenuItem) me[i]).setEnabled(false);
							}
							/*
							 * if
							 * (jtree.getModel().isLeaf(jtree.getLastSelectedPathComponent())) {
							 * ((JMenuItem)me[i]).setEnabled(false); } else {
							 * ((JMenuItem)me[i]).setEnabled(true); }
							 */
						}
						if (((JMenuItem) me[i]).getText().equals("Add Child")) {
							Node nodeClicked = ((DOMmodel.DOMNodeAdapter) jtree
									.getLastSelectedPathComponent()).getNode();

							if (nodeClicked.getNodeType() == Element.TEXT_NODE) {
								((JMenuItem) me[i]).setEnabled(false);
							} else {
								((JMenuItem) me[i]).setEnabled(true);
							}
						}
						if (((JMenuItem) me[i]).getText().equals("Annotate")) {
							Node nodeClicked = ((DOMmodel.DOMNodeAdapter) jtree
									.getLastSelectedPathComponent()).getNode();

							if (nodeClicked.getNodeType() == Element.TEXT_NODE &&
									documentation != null) {
								((JMenuItem) me[i]).setEnabled(true);
							} else {
								((JMenuItem) me[i]).setEnabled(false);
							}
						}
					}
					treeMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		//******

		// Build left-side view
		JScrollPane treeView = new JScrollPane(jtree);
		treeView.setPreferredSize(new Dimension(leftWidth, parentFrame.getHeight()));

		//jTable = new JTable();
		JScrollPane tableView = new JScrollPane(/* jTable */);
		//tableView.setPreferredScrollableViewportSize( new Dimension (
		// rightWidth, windowHeight ));
		tableView.setPreferredSize(new Dimension(parentFrame.getWidth()- leftWidth, parentFrame.getHeight()));

		// Build split-pane view
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeView,
				tableView);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(leftWidth);
		splitPane.setPreferredSize(new Dimension(parentFrame.getWidth()+ 10, parentFrame.getHeight()+ 10));
		// Add GUI components
		contentPane.add("Center", splitPane);

		treeMenu = makePopupTreeMenu();

		//create the dialog for adding new node children, but leave invisible
		makeAddChildDialog();

		//this.show();
		//this.pack();
		parentFrame.setVisible(true);
	}

	/**
	 * Process events from the menu items.
	 * 
	 * @param e
	 *            the event, only care about a click on a menu item
	 */
	public void actionPerformed(ActionEvent e) {
        final InterfaceMain main = InterfaceMain.getInstance();
        final JFrame parentFrame = main.getFrame();
		boolean status = false;
		String command = e.getActionCommand();
		if (command.equals("XML file")) {
			// Open a file
			status = openXMLFile(e);
			if (doc == null) {
				// probably the cancel, just return here to avoid exceptions
				return;
			}
			if(status) {
				displayJtree();
				jtree.setTransferHandler(new DOMTransferHandler(doc, implls));
				jtree.setDragEnabled(true);
				parentFrame.setTitle("["+file+"] - ModelInterface");
			}
		} else if (command.equals("CSV file")) {
			// Open a file
			status = openCSVFile(e);
			if(status) {
				displayJtree();
				parentFrame.setTitle("["+file+"] - ModelInterface");
			}
		} else if (command.equals("Save")) {
			if (!(file.getAbsolutePath().endsWith(".xml"))) {
				status = saveFile();
			} else {
				status = saveFile(file);
			}
			if (!status) {
				main.showMessageDialog( "IO error in saving file!!", "File Save Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			parentFrame.setTitle("["+file+"] - ModelInterface");
			main.fireProperty("Document-Save", null, doc);
		} else if (command.equals("Save As")) {
			// Save a file
			status = saveFile();
			if (!status) {
				main.showMessageDialog( "IO error in saving file!!", "File Save Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			parentFrame.setTitle("["+file+"] - ModelInterface");
			main.fireProperty("Document-Save", null, doc);
		} else if (command.equals("Filter")) {
				/*
			try {
				if (((JTable) ((JScrollPane) splitPane.getRightComponent())
						.getViewport().getView()).getModel() instanceof TableSorter) {

					((BaseTableModel) ((TableSorter) ((JTable) ((JScrollPane) splitPane
							.getRightComponent()).getViewport().getView())
							.getModel()).getTableModel()).filterData(parentFrame);
				} else {
					((BaseTableModel) ((JTable) ((JScrollPane) splitPane
							.getRightComponent()).getViewport().getView())
							.getModel()).filterData(parentFrame);
					if (((JTable) ((JScrollPane) splitPane.getRightComponent())
							.getViewport().getView()).getModel() instanceof MultiTableModel) {
						// NOT THE BEST WAY TO SET ROW HEIGHT
						int j = 1;
						JTable jTable = (JTable) ((JScrollPane) splitPane
								.getRightComponent()).getViewport().getView();
						while (j < jTable.getRowCount()) {
							jTable.setRowHeight(j - 1, 16);
							jTable.setRowHeight(j, 200);
							j += 2;
						}
					}
				}
			} catch (UnsupportedOperationException uoe) {
				JOptionPane.showMessageDialog(null,
						"This table does not support filtering",
						"Table Filter Error", JOptionPane.ERROR_MESSAGE);
			}
				*/
			// new and old values should be??
			main.fireProperty("Filter", null, 1);
		} else if(command.equals("Add Child")) {
			jtree.setSelectionPath(selectedPath);

			Node nodeClicked = ((DOMmodel.DOMNodeAdapter) jtree
					.getLastSelectedPathComponent()).getNode();
			if (nodeClicked.getNodeType() != Element.TEXT_NODE) { // can't
				// add child to text node
				showAddChildDialog();
			}

		} else if (command.equals("Delete Node")) {
			jtree.setSelectionPath(selectedPath);
			deleteNode();
		} else if (command.equals("Display Table")) {
			displayTable();
		} else if(command.equals("Annotate")) {
			jtree.setSelectionPath(selectedPath);

			Node nodeClicked = ((DOMmodel.DOMNodeAdapter) jtree
					.getLastSelectedPathComponent()).getNode();
			// should probably not be enabled if documentation is null
			if(documentation != null) {
				Vector<Node> tempVec = new Vector<Node>(1,0);
				tempVec.add(nodeClicked);
				documentation.getDocumentation(tempVec);
			}
			/*
			String nodeCXPath = nodeToXPath(nodeClicked).toString();
			System.out.println("Node clicked XPath: "+nodeCXPath);
			//if (nodeClicked.getNodeType() != Element.TEXT_NODE) {
				try {
					PrefixResolver pr = new PrefixResolverDefault(doc.getDocumentElement());
					XPath xp = new XPath(xpStr, null, pr, XPath.MATCH);
					XPath xpCurrNode = new XPath(nodeCXPath, null, pr, XPath.MATCH);
					if(xp.getExpression().deepEquals(xpCurrNode.getExpression())) {
					//if(xp.getExpression().bool(new XPathContext())) {
						System.out.println("Wow it bool'ed");
					} else {
						System.out.println("Yea it didn't bool");
					}
					System.out.println("Merged XPath: "+meregeXPaths(xpStr, nodeCXPath));
				} catch(TransformerException te) {
					te.printStackTrace();
				}
			//}
			*/
		}
	}


	String meregeXPaths(String path1, String path2) {
		String[] path1Arr = path1.split("/");
		String[] path2Arr = path2.split("/");
		StringBuffer strBuff = new StringBuffer();
		if(path1Arr.length != path2Arr.length) {
			System.out.println("Can't merge "+path1+" and "+path2);
			return null;
		}
		for(int i = 1; i < path1Arr.length; ++i) {
			if(path1Arr[i].equals(path2Arr[i])) {
				strBuff.append("/").append(path1Arr[i]);
			} else if(path1Arr[i].indexOf('[') == -1 && path2Arr[i].indexOf('[') == -1) {
				return null;
			} else if(path1Arr[i].indexOf('[') != -1 && path2Arr[i].indexOf('[') != -1 && !path1Arr[i].substring(0, path1Arr[i].indexOf('[')).equals(path2Arr[i].substring(0, path2Arr[i].indexOf('[')))) {
				return null;
			} else if(path1Arr[i].indexOf('[') == -1 && path2Arr[i].indexOf('[') != -1) {
				strBuff.append("/").append(path2Arr[i]);
			} else {
				String[] attrs = path1Arr[i].substring(path1Arr[i].indexOf('[')+1, path1Arr[i].indexOf(']')).split(" or ");
				String p2Attr = path2Arr[i].substring(path2Arr[i].indexOf('[')+1, path2Arr[i].indexOf(']'));
				boolean found = false;
				for(int j = 0; j < attrs.length && !found; ++j) {
					if(attrs[j].equals(p2Attr)) {
						strBuff.append("/").append(path1Arr[i]);
						found = true;
					}
				}
				if(!found) {
					strBuff.append("/").append(path1Arr[i].substring(0,path1Arr[i].length()-1)).append(" or ").append(p2Attr).append("]");
				}
			}
		}
		return strBuff.toString();
	}
				
	/**
	 * This "helper method" makes a menu item and then registers this object as
	 * a listener to it.
	 * 
	 * @param name
	 *            Name of menu item
	 * @return a new menu item with specified name
	 */
	private JMenuItem makeMenuItem(String name) {
		JMenuItem m = new JMenuItem(name);
		m.addActionListener(this);
		return m;
	}

	/**
	 * Create right click menu for the JTree and returns it, also defines the
	 * listeners for each button
	 * 
	 * @return the right click menu for the JTree
	 */
	private JPopupMenu makePopupTreeMenu() {
		treeMenu = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("Display Table");
		menuItem.addActionListener(this);
		treeMenu.add(menuItem);
		treeMenu.add(new JSeparator());
		menuItem = new JMenuItem("Add Child");
		menuItem.addActionListener(this);
		treeMenu.add(menuItem);

		treeMenu.add(new JSeparator());
		menuItem = new JMenuItem("Delete Node");
		menuItem.addActionListener(this);
		treeMenu.add(menuItem);

		menuItem = new JMenuItem("Annotate");
		menuItem.addActionListener(this);
		treeMenu.add(menuItem);

		return treeMenu;
	}

	/**
	 * Creates the right click menu for tables which currently consists of flip,
	 * also creates the listener
	 * 
	 * @return the right click menu created
	 */
	private JPopupMenu makePopupTableMenu() {
		tableMenu = new JPopupMenu();
		final JMenuItem flipItem = new JMenuItem("Flip");
		flipItem.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				// get the correct row and col which only matters for
				// mulitablemodel
				// then be sure to pass on the call to the correct table
				// model
				e.translatePoint(lastFlipX, lastFlipY);
				Point p = e.getPoint();
				JTable jTable = (JTable) ((JScrollPane) splitPane
					.getRightComponent()).getViewport().getView();
				int row = jTable.rowAtPoint(p);
				int col = jTable.columnAtPoint(p);

				if (jTable.getModel() instanceof TableSorter) {
					((BaseTableModel) ((TableSorter) jTable.getModel())
					 .getTableModel()).flip(row, col);
				} else {
					((BaseTableModel) jTable.getModel()).flip(row, col);
				}
			}
		});
		tableMenu.add(flipItem);

		final JMenuItem annotateItem = new JMenuItem("Annotate");
		annotateItem.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				//e.translatePoint(lastFlipX, lastFlipY);
				//Point p = e.getPoint();
				JTable jTable = (JTable) ((JScrollPane) splitPane
					.getRightComponent()).getViewport().getView();
				int[] rows = jTable.getSelectedRows();
				int[] cols = jTable.getSelectedColumns();

				if (jTable.getModel() instanceof TableSorter) {
					((BaseTableModel) ((TableSorter) jTable.getModel())
					 .getTableModel()).annotate(rows, cols, documentation);
				} else {
					((BaseTableModel) jTable.getModel()).annotate(rows, cols, documentation);
				}
			}
		});
		tableMenu.add(annotateItem);
		
		final JMenuItem chartItem = new JMenuItem("Chart");
		chartItem.addMouseListener( new MouseAdapter(){
			public void mouseReleased(MouseEvent e) {
				final MouseEvent me = e;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JTable jTable = (JTable) ((JScrollPane) splitPane
								.getRightComponent()).getViewport().getView();

						// get the correct row and col which only matters for
						// mulitablemodel
						// then be sure to pass on the call to the correct table
						// model
						me.translatePoint(lastFlipX, lastFlipY);
						Point p = me.getPoint();
						int row = jTable.rowAtPoint(p);
						int col = jTable.columnAtPoint(p);

						try {
							JFreeChart chart;
							if (jTable.getModel() instanceof TableSorter) {
								chart = ((BaseTableModel) ((TableSorter) jTable
										.getModel()).getTableModel()).createChart(row, col);
							} else {
								chart = ((BaseTableModel) jTable.getModel())
									.createChart(row, col);
							}
							// Turn the chart into an image.
							BufferedImage chartImage = chart.createBufferedImage(
									500, 500);

							JLabel labelChart = new JLabel();
							labelChart.setIcon(new ImageIcon(chartImage));
							chartPanel.add(labelChart);
							chartPanel.add(Box.createVerticalStrut(10));

							chartWindow.pack();
							chartWindow.setVisible(true);
						} catch(NumberFormatException nfe) {
							InterfaceMain.getInstance().showMessageDialog(
									"Could not create a chart: No year values to chart.", 
									"Could Not Create", JOptionPane.ERROR_MESSAGE);
						}
					}
				});
			}
		});
		tableMenu.add(chartItem);
		return tableMenu;
	}

	/**
	 * Creates dialogs to confirm deleting a node from the tree, if yes then
	 * tell the tree model to delete the node from the slected path
	 */
	private void deleteNode() {
		//Node currNode =
		// ((DOMmodel.DOMNodeAdapter)selectedPath.getLastPathComponent()).getNode();
		String message = "Are you sure you want to delete this node";

		if (jtree.getModel().isLeaf(selectedPath.getLastPathComponent())) {
			message += "?";
		} else {
			message += " and all of its children?";
		}

		int ans = InterfaceMain.getInstance().showConfirmDialog(message, "Delete Node",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_OPTION);

		if (ans == JOptionPane.NO_OPTION)
			return;

		//delete the node
		((DOMmodel) jtree.getModel()).removeNodeFrom(selectedPath);
	}

	/**
	 * Intializes and shows the add child dialog
	 */
	private void showAddChildDialog() {
		infoLabel.setText("Adding child to "
				+ selectedPath.getLastPathComponent());

		//display possible locations where to add node

		addChildDialog.pack();
		//center above the main window
		addChildDialog.setLocationRelativeTo(addChildDialog.getParent());
		addChildDialog.setVisible(true);
	}

	/**
	 * Creates the dialog box that will be made visible when the user choses to
	 * add a child to an esiting node in the tree.
	 */
	public void makeAddChildDialog() {
        final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		addChildDialog = new JDialog(parentFrame, "Add Child Node", true);
		Container content = addChildDialog.getContentPane();
		content.setLayout(new BoxLayout(addChildDialog.getContentPane(),
				BoxLayout.Y_AXIS));

		infoLabel = new JLabel(".");
		final JTextField nameField = new JTextField();
		final JTextField attribField = new JTextField();
		final JTextField valueField = new JTextField();

		JPanel childPanel = new JPanel();
		childPanel.setLayout(new BoxLayout(childPanel, BoxLayout.Y_AXIS));
		childPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

		childPanel.add(infoLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JLabel nameLabel = new JLabel("Node Name (required): ");
		childPanel.add(nameLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		childPanel.add(nameField);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JLabel attribLabel = new JLabel(
				"Node Attribute(s) (optional list in the form name=node name, year=1975)");
		childPanel.add(attribLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		childPanel.add(attribField);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JLabel valueLabel = new JLabel("Node Value ");
		childPanel.add(valueLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		childPanel.add(valueField);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		content.add(childPanel);
		content.add(new JSeparator(SwingConstants.HORIZONTAL));

		JPanel buttonPanel = new JPanel();
		//buttonPanel.setLayout(new GridLayout(0, 1, 5, 5));
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		JButton addNodeButton = new JButton("Add Node");
		addNodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(addChildNode(nameField, attribField, valueField)) {
					addChildDialog.setVisible(false);
				}
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addChildDialog.setVisible(false);
			}
		});

		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(addNodeButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		buttonPanel.add(cancelButton);

		content.add(buttonPanel);

	}

	/**
	 * Creates the layout for the add node input part of the dialog
	 * 
	 * @return the panel which was created for the dialog layout
	 */
	/*
	private JPanel makeAddNodePanel() {
		infoLabel = new JLabel(".");
		nameField = new JTextField();
		attribField = new JTextField();
		valueField = new JTextField();

		JPanel childPanel = new JPanel();
		childPanel.setLayout(new BoxLayout(childPanel, BoxLayout.Y_AXIS));
		childPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

		childPanel.add(infoLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JLabel nameLabel = new JLabel("Node Name (required): ");
		childPanel.add(nameLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		childPanel.add(nameField);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JLabel attribLabel = new JLabel(
				"Node Attribute(s) (optional list in the form name=node name, year=1975)");
		childPanel.add(attribLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		childPanel.add(attribField);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JLabel valueLabel = new JLabel("Node Value ");
		childPanel.add(valueLabel);
		childPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		childPanel.add(valueField);
		childPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		return childPanel;
	}
	*/

	/**
	 * Creates the layout for the add node button part of the dialog
	 * 
	 * @return the panel which was created for the dialog layout
	 */
	/*
	private JPanel makeAddChildButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(0, 1, 5, 5));
		//buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

		JButton addNodeButton = new JButton("Add Node");
		addNodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addChildNode();
				addChildDialog.hide();
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addChildDialog.hide();
			}
		});

		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(addNodeButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createVerticalGlue());

		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));
		tempPanel.add(new JSeparator(SwingConstants.VERTICAL));
		tempPanel.add(buttonPanel);

		return buttonPanel;
	}
	*/

	/**
	 * Takes the newly created node and tells the tree model to add it to the
	 * tree
	 */
	private boolean addChildNode(JTextField nameField, JTextField attrField, JTextField dataField) {
		String name = nameField.getText();
		String attr = attrField.getText();
		String data = dataField.getText();
		Element tempNode = null;
		if(name.equals("")) {
			InterfaceMain.getInstance().showMessageDialog("You must supply a name", 
					"Invalid Name", JOptionPane.ERROR_MESSAGE);
			return false;
		} else {
			try {
				tempNode = doc.createElement(name);
			} catch(DOMException e) {
				if(e.code == DOMException.INVALID_CHARACTER_ERR) {
					InterfaceMain.getInstance().showMessageDialog("Invalid XML name, please Change your Node Name", 
							"Invalid Name", JOptionPane.ERROR_MESSAGE);
				} else {
					InterfaceMain.getInstance().showMessageDialog(e, 
							"Invalid Name", JOptionPane.ERROR_MESSAGE);
				}
				return false;
			}
		}
		if(!attr.equals("")) {
			boolean gotSome = false;
			Pattern pat = Pattern.compile("\\s*([\\w\\-]+)=([^,]+)\\s*(,|\\z)");
			Matcher mt = pat.matcher(attr);
			while(mt.find()) {
				try {
					tempNode.setAttribute(mt.group(1), mt.group(2));
					gotSome = true;
				} catch(DOMException e) {
					if(e.code == DOMException.INVALID_CHARACTER_ERR) {
						InterfaceMain.getInstance().showMessageDialog("Invalid XML attribute name, please check your attribute names", 
								"Invalid Attribute", JOptionPane.ERROR_MESSAGE);
					} else {
						InterfaceMain.getInstance().showMessageDialog(e, 
								"Invalid Name", JOptionPane.ERROR_MESSAGE);
					}
					return false;
				}
			}
			if(!gotSome) {
				// show error
				InterfaceMain.getInstance().showMessageDialog("Please check the syntax of you Attributes", 
						"Invalid Attributes", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		if(!data.equals("")) {
			tempNode.appendChild(doc.createTextNode(data));
		}

		DOMmodel model = (DOMmodel) jtree.getModel();
		model.insertNodeInto(tempNode, selectedPath);
		nameField.setText("");
		attrField.setText("");
		dataField.setText("");
		return true;
	}

	/**
	 * Listener for table model events, we only care about when a user has
	 * updated the data, in which case we have to tell the tree to refresh to
	 * show the changed value int the tree.
	 * 
	 * @param e
	 *            the event that has occured
	 */
	public void tableChanged(TableModelEvent e) {
		if (jtree != null && e.getType() == TableModelEvent.UPDATE) {
			((DOMmodel) jtree.getModel())
					.fireTreeNodesChanged(new TreeModelEvent(e.getSource(),
							selectedPath));
		}
	}

	/**
	 * Listener of tree model events only care about when a user has updated the
	 * data, in which case we have to tell the table to refresh to show the
	 * changed value. Make sure that the event came from the tree model so we
	 * don't keep sending messages back and forth between the table and tree
	 */
	class MyTreeModelListener implements TreeModelListener {
		public void treeNodesChanged(TreeModelEvent e) {
			try {
				if (e.getSource() instanceof DOMmodel) {
					BaseTableModel bt = getTableModelFromScrollPane((JScrollPane)splitPane.getRightComponent());
					if(bt != null) {
						bt.fireTableRowsUpdated(0, bt.getRowCount());
					}
					InterfaceMain.getInstance().fireProperty("Document-Modified", null, doc);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void treeNodesInserted(TreeModelEvent e) {
			InterfaceMain.getInstance().fireProperty("Document-Modified", null, doc);
		}

		public void treeNodesRemoved(TreeModelEvent e) {
			InterfaceMain.getInstance().fireProperty("Document-Modified", null, doc);
		}

		public void treeStructureChanged(TreeModelEvent e) {
			InterfaceMain.getInstance().fireProperty("Document-Modified", null, doc);
		}
	}

	/**
	 * Creates a JFileChooser to figure out which file to parse, then parses the
	 * file and sets doc to it
	 *
	 * @param evt The event that spured the opening 
	 * @return true if we parsed a file, false otherwise
	 */
	boolean openXMLFile(ActionEvent evt) {

        final InterfaceMain main = InterfaceMain.getInstance();
        final JFrame parentFrame = main.getFrame();
		File[] result;
		if(evt.getSource() instanceof RecentFile) {
			result = ((RecentFile)evt.getSource()).getFiles();
		} else {
			FileChooser fc = FileChooserFactory.getFileChooser();
			result = fc.doFilePrompt(parentFrame, "Open XML File", FileChooser.LOAD_DIALOG, 
					new File(main.getProperties().getProperty("lastDirectory", ".")),
					xmlFilter, this, "XML file");
		}

		if (result == null) {
			return false;
		} else {
			main.fireControlChange(controlStr);
			file = result[0];
			main.getProperties().setProperty("lastDirectory", file.getParent());

			doc = readXMLFile( file );
			
			// Set the document URI for the file.
			final String currURI = doc.getDocumentURI();
			
			// Check if the URI is blank.
			// TODO: Should check if the URI is incorrect.
			if (currURI == null) {
				// Parse the URI from the file.
				final URI newURI = file.toURI();
				// Set the document URI to the location of the file.
				doc.setDocumentURI(newURI.toString());
			}
			

			String docLoc = doc.getDocumentElement().getAttribute(
					"documentation");
			if (docLoc.equals("")) {
				documentation = new Documentation(doc, lsParser, lsInput);
			} else {
				// Parse the documentation location into a URI.
				try {
					URI docLocationURI = new URI(docLoc);
					System.out.println("DOCURI1: " + docLocationURI.toString());
					// Relativize the location with respect to the main
					// document.
					final String docURIString = doc.getDocumentURI();
					if (docURIString == null) {
						Logger.global
								.log(Level.SEVERE,
										"Main document does not have a URI. Cannot relativize the documentation URI.");
					} else {
						try {
							final URI currentMainURI = new URI(docURIString);
							docLocationURI = currentMainURI.resolve(docLocationURI);
							System.out.println("DOCUMENT URI = " + docLocationURI.toString());
						} catch (URISyntaxException e) {
							Logger.global
									.log(
											Level.SEVERE,
											"URI parsing failed of the main document URI failed. Cannot relativize the documentation URI.");
						}
					}
					documentation = new Documentation(doc, docLocationURI,
							lsParser, lsInput);
				} catch (URISyntaxException e) {
					// TODO Give the user an error.
					documentation = null;
					Logger.global
							.log(Level.SEVERE,
									"URI parsing failed. Cannot open the documentation XML file.");
				}
			}
		}
		return true;
	}

	/**
	 * Creates file choosers to get a CSV file and a header file, then processes
	 * them with the CSV to XML converter
	 *
	 * @param evt The action that caused the openCSV to occur 
	 * @return true if processed and created a doc, false otherwise
	 */
	boolean openCSVFile(ActionEvent evt) {

        final InterfaceMain main = InterfaceMain.getInstance();
        final JFrame parentFrame = main.getFrame();
		File[] csvFiles;
		File[] headerFiles;
		if(evt.getSource() instanceof RecentFile) {
			File[] files = ((RecentFile)evt.getSource()).getFiles();
			csvFiles = new File[files.length-1];
			headerFiles = new File[1];
			System.arraycopy(files, 0, csvFiles, 0, csvFiles.length);
			headerFiles[0] = files[files.length-1];
		} else {
			FileChooser fc = FileChooserFactory.getFileChooser();
			// can't use the normal recent file so will have to addFile manually
			csvFiles = fc.doFilePrompt(parentFrame, "Open CSV Files", FileChooser.LOAD_DIALOG, 
					new File(main.getProperties().getProperty("lastDirectory", ".")),
					csvFilter, null, null);
			if(csvFiles == null) {
				return false;
			}
			main.getProperties().setProperty("lastDirectory", csvFiles[0].getPath());
			headerFiles = fc.doFilePrompt(parentFrame, "Open Headers File", FileChooser.LOAD_DIALOG, 
					new File(main.getProperties().getProperty("lastDirectory", ".")),
					null, null, null);
			// return false or should it be true?
			if(headerFiles == null) {
				return false;
			}
			main.getProperties().setProperty("lastDirectory", headerFiles[0].getPath());
			File[] files = new File[csvFiles.length+1];
			System.arraycopy(csvFiles, 0, files, 0, csvFiles.length);
			files[files.length-1] = headerFiles[0];
			RecentFilesList.getInstance().addFile(files, this, "CSV file");
		}

		if(csvFiles == null || headerFiles == null) {
			return false;
		} 
		main.fireControlChange(controlStr);

		// TODO: get rid of the dependency on file
		file = csvFiles[0];
		readCSVFile(csvFiles, headerFiles[0]);
		return true;
	}

	boolean saveFile(File where) {
		return writeFile(where, doc);
	}

	boolean saveFile() {
		// save as..

        final InterfaceMain main = InterfaceMain.getInstance();
        final JFrame parentFrame = main.getFrame();
		FileChooser fc = FileChooserFactory.getFileChooser();
        String saveAsDefault = file.getAbsolutePath();
        if(!saveAsDefault.endsWith(".xml")) {
            saveAsDefault = saveAsDefault.replaceAll("\\....$", ".xml");
        }
		File[] result = fc.doFilePrompt(parentFrame, null, FileChooser.SAVE_DIALOG, 
				new File(saveAsDefault),
				xmlFilter);
		if(result == null) {
			return true;
		} else {
			File file = result[0];
			if (!file.getName().matches("[.]")) {
				if (!(file.getAbsolutePath().endsWith(".xml"))) {
					file = new File(file.getAbsolutePath() + ".xml");
				}
			}
			if (file.exists()) {
				int response = InterfaceMain.getInstance().showConfirmDialog(
						"Overwrite existing file?", "Confirm Overwrite",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_OPTION);
				//if they hit cancel it gives and error message, so i
				//made it return true, that could be a problem in the future
				if (response == JOptionPane.CANCEL_OPTION)
					return true;
			}
			main.getProperties().setProperty("lastDirectory", file.getParent());
			return writeFile(file, doc);
		}
	}


	/**
	 * Does the parsing of an XML file, and returns it
	 * 
	 * @param file
	 *            the file that will be parsed
	 * @return the parsed document
	 */
	public Document readXMLFile(File file) {
		try {
			lsInput.setByteStream(new FileInputStream(file));
			/*
			lsParser = implls.createLSParser(
					DOMImplementationLS.MODE_SYNCHRONOUS, null);
			lsParser.setFilter(new ParseFilter());
			*/
			return lsParser.parse(lsInput);
		} catch (Exception e) {
			System.out.println("Got Exception while creating XML document: "
					+ e);
			InterfaceMain.getInstance().showMessageDialog(
					"Exception while creating XML document\n" + e.getMessage(), "Exception",
					JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	/**
	 * Takes a CSV file, and Headers file, then processes the files by building
	 * a new tree with the DOMTreeBuilder class. After the tree is build doc is
	 * set to that tree
	 * 
	 * @param file
	 *            the CSV file
	 * @param file2
	 *            the Headers file
	 */
	public void readCSVFile(File[] csvFiles, File file2) {
        doc = CSVToXMLMain.runCSVConversion(csvFiles, file2, InterfaceMain.getInstance().getFrame());
    }

	/**
	 * Writes the DOM document to the specified file
	 * 
	 * @param file
	 *            where the XML tree will be written to
	 * @param thDoc
	 *            the tree that should be written
	 * @return whether the file was actually written or not
	 */
	public boolean writeFile(File file, Document theDoc) {
		// specify output formating properties
		OutputFormat format = new OutputFormat(theDoc);
		format.setEncoding("UTF-8");
		format.setLineSeparator("\r\n");
		format.setIndenting(true);
		format.setIndent(3);
		format.setLineWidth(0);
		format.setPreserveSpace(false);
		format.setOmitDocumentType(true);

		// create the searlizer and have it print the document

		try {
			FileWriter fw = new FileWriter(file);
			XMLSerializer serializer = new XMLSerializer(fw, format);
			serializer.asDOMSerializer();
			serializer.serialize(theDoc);
			fw.close();
		} catch (java.io.IOException e) {
			System.err.println("Error outputing tree: " + e);
			return false;
		}
		return true;
	}

	private BaseTableModel getTableModelFromScrollPane(JScrollPane sp) {
		try {
			Object ret = ((JTable)sp.getViewport().getView()).getModel();
			if(ret instanceof TableSorter) {
				return (BaseTableModel)((TableSorter)ret).getTableModel();
			} else {
				return (BaseTableModel)ret;
			}
		} catch(NullPointerException ne) {
			return null;
		}
	}

	public void displayTable() {
        final InterfaceMain main = InterfaceMain.getInstance();
        final JFrame parentFrame = main.getFrame();
		if (!jtree.getModel().isLeaf(jtree.getLastSelectedPathComponent())) {

			// find out the type of table and create it
			JScrollPane tableView = tableSelector.createSelection(selectedPath, doc,
					parentFrame, thisViewer);

			if (tableView == null) {
				return;
			}
			Object oldVal = null;
			if(((JScrollPane)splitPane.getRightComponent()).getViewport().getView() != null) {
				oldVal = getTableModelFromScrollPane((JScrollPane)splitPane.getRightComponent());
			}
			main.fireProperty("Table", oldVal, getTableModelFromScrollPane(tableView));

			// maybe this will solve the resizing of the left component
			//tableView.setPreferredSize(new Dimension(windowWidth - leftWidth, windowHeight));

			// don't know why but it is moving the divider all of a sudden, so will
			// force it to be where it was
			int divLoc = splitPane.getDividerLocation();
			splitPane.setRightComponent(tableView);
			splitPane.setDividerLocation(divLoc);
			tableMenu = makePopupTableMenu();
			// add the listener for right click which currently only
			// handles flip
			((JTable) tableView.getViewport().getView()).addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					maybeShowPopup(e);
				}

				public void mouseReleased(MouseEvent e) {
					maybeShowPopup(e);
				}

				private void maybeShowPopup(MouseEvent e) {
					if (e.isPopupTrigger()) {
						lastFlipX = e.getX();
						lastFlipY = e.getY();
						JTable jTable = (JTable)((JScrollPane)splitPane.getRightComponent()).getViewport().getView();
						Point p = e.getPoint();
						int row = jTable.rowAtPoint(p);
						int col = jTable.columnAtPoint(p);
								System.out.println("row: "+row);
								System.out.println("col: "+col);
								System.out.println("Point: "+p);
						if(row != -1 && col != -1 && !jTable.isCellSelected(row, col)) {
							jTable.setRowSelectionInterval(row, row);
							jTable.setColumnSelectionInterval(col, col);
						} 
						if(row != -1 && col != -1 && (jTable.getValueAt(row, col) instanceof JScrollPane)) {
							javax.swing.table.TableCellEditor tce = jTable.getCellEditor(row, col);
							JTable jTableTemp = jTable;
							if(jTable.editCellAt(row, col)) {
								System.out.println("Yes edit");
							} else {
								System.out.println("Couldn't edit");
							}
							jTable = (JTable)((JScrollPane)jTable.getModel().getValueAt(row, col))
								.getViewport().getView();
							//System.out.println("LocationOS: "+jTable.getLocationOnScreen());
							System.out.println("Mouse Loc: "+jTable.getMousePosition());
							System.out.println("Cols: "+jTable.getColumnCount());
							p = jTable.getMousePosition();
							row = jTable.rowAtPoint(p);
							col = jTable.columnAtPoint(p);
							System.out.println("row: "+row);
							System.out.println("col: "+col);
							System.out.println("Point: "+p);
							if(row != -1 && col != -1 && !jTable.isCellSelected(row, col)) {
								jTable.setRowSelectionInterval(row, row);
								jTable.setColumnSelectionInterval(col, col);
							}
							System.out.println("TableCellEditor: "+tce);
							tce.stopCellEditing();
							if(jTableTemp.isEditing()) {
								System.out.println("It is Editing");
							} else {
								System.out.println("It isn't Editing");
							}
						}

						/*
						MenuElement[] me = tableMenu.getSubElements();
						for (int i = 0; i < me.length; i++) {
							if (((JMenuItem) me[i]).getText()
								.equals("Flip")) {
								lastFlipX = e.getX();
								lastFlipY = e.getY();
							}
						}
						*/
						tableMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			});
		}

	}

	public Documentation getDocumentation() {
		return documentation;
	}

	public void runBatch(Node command) {
		NodeList children = command.getChildNodes();
		for(int i = 0; i < children.getLength(); ++i ) {
			Node child = children.item(i);
			// TODO: put in a parse filter for this
			if(child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			String actionCommand = ((Element)child).getAttribute("name");
			if(actionCommand == null) {
				continue;
			}
			if(actionCommand.equals("CSV file")) {
				File headerFile = null;
				File outFile = null;
				ArrayList<File> csvFiles = new ArrayList<File>();
				// read file names for header file, csv files, and the output file
				NodeList fileNameChildren = child.getChildNodes();
				for(int j = 0; j < fileNameChildren.getLength(); ++j) {
					Node fileNode = fileNameChildren.item(j);
					if(fileNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					System.out.println("on "+fileNode.getNodeName());
					File tempFile = new File(fileNode.getTextContent());
					if(fileNode.getNodeName().equals("headerFile")) {
						headerFile = tempFile;
					} else if(fileNode.getNodeName().equals("outFile")) {
						outFile = tempFile;
					} else if(fileNode.getNodeName().equals("csvFile")) {
						csvFiles.add(tempFile);
					} else {
						System.out.println("Unknown tag: "+fileNode.getNodeName());
						// should I print this error to the screen?
					}
				}
				// make sure we have enough to run the csv conversion
				// which means we have a header file, output file, and
				// at least one csv file.
				if(headerFile != null && outFile != null &&
						csvFiles.size() != 0) {
					File[] csvFilesArr = new File[csvFiles.size()];
					csvFilesArr = csvFiles.toArray(csvFilesArr);
					readCSVFile(csvFilesArr, headerFile);
					if(doc != null) {
						// only write if there were no conversion errors
						writeFile(outFile, doc);
						// null out the results after they have been writen since
						// we don't need them around
						doc = null;
					}
					// if it was null there was an error during conversion and the
					// user should have already gotten that so no need to tell them
					// again
				} else {
					System.out.println("Not enough info to run conversion");
					InterfaceMain.getInstance().showMessageDialog(
							"Not enough info to run conversion",
							"Batch File Error", JOptionPane.ERROR_MESSAGE);
				}
			} else {
				System.out.println("Unknown command: "+actionCommand);
				InterfaceMain.getInstance().showMessageDialog(
						"Unknown command: "+actionCommand,
						"Batch File Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
