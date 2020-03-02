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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Properties;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.jfree.chart.JFreeChart;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.tables.BaseTableModel;
import ModelInterface.ModelGUI2.tables.ComboTableModel;
import ModelInterface.ModelGUI2.tables.CopyPaste;
import ModelInterface.ModelGUI2.tables.MultiTableModel;
import ModelInterface.ModelGUI2.xmldb.QueryBinding;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import filter.FilteredTable;
import graphDisplay.ModelInterfaceUtil;
import ModelInterface.ModelGUI2.xmldb.DbProcInterrupt;

/**
 * Adds capability of running many queries parallel and will display
 * the results after the queries are run as well as change the icon 
 * to indicate to the user that it is done running.
 * 
 * Adds capability of running many queries parallel and will display the results
 * after the queries are run as well as change the icon to indicate to the user
 * that it is done running.
 * 
 *   Author			Action					Date		Flag
 *  ============================================================ 			
 *	TWU				Replace chart label 		1/2/2017	@1
 *					to a chart application
 *					Add capability to invoke
 *					command line query
 */

public class QueryResultsPanel extends JPanel {




	/** Referring to the thread that is running. Used to track
	 * which thread is being used/closed
	 */
	Thread runThread;

	/** The context for running queries which can be used to cancel it */
	DbProcInterrupt context	= null;

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new query results panel.
	 * 
	 * @param qg the query generator
	 * @param singleBinding The single query binding which will filter the results from qg, or null if the user did not select a single query.
	 * @param scenarioListValues list of selected scenarios
	 * @param regionListValues Regions to be used. 
	 * @param icon The icon that will be changing.
	 */
	public QueryResultsPanel(final QueryGenerator qg, final QueryBinding singleBinding, final Object[] scenarioListValues, final Object[] regionListValues, final TabCloseIcon icon){  
		initializeWaiting();
		context = new DbProcInterrupt();
		final QueryResultsPanel thisThread= this;
		runThread = new Thread(){
			public void run(){
				JComponent ret = null;
				String errorMessage = null;
				//do computations, return a JComponent
				try{
					if (qg.isGroup() && singleBinding == null) {
						ret = createGroupTableContent(qg, scenarioListValues, regionListValues);
					} else {
						ret = createSingleTableContent(qg, singleBinding, scenarioListValues, regionListValues);
					}
				} catch(Exception e) {
					errorMessage = e.getMessage();
				}
				//Stop process if the user terminated the process
				if(isInterrupted())
					return;

				//clear the text box in preparation of adding the new component 
				removeAll();

				//icon is changed to the finished state
				icon.finishedLoading();
				//error message displayed
				if(ret == null){
					JPanel tempPanel = new JPanel();
					tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));
					tempPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
					tempPanel.add(new JLabel(errorMessage));
					add(tempPanel);
				}
				//the new JPanel is added where the text box was
				else{
					setLayout(new BoxLayout(thisThread, BoxLayout.X_AXIS));
					add(ret);
				}
				//the panel is refreshed to show the changes
				revalidate();
			}
		};
		runThread.start();
	}

	/**
	 * Interrupt the query that is inprogress. This is normally run
	 * when the tab is closed before the results are finished.  Note
	 * that this method is asycnchronous and does not ensure the query
	 * thread has finished running by the time it returns which is 
	 * normally not necessary.
	 * @see killThreadAndWait
	 */
	public void killThread(){
        context.interrupt();
		runThread.interrupt();
	}

	/**
	 * Kills the running query and waits for it to stop running before 
	 * returning.  This would be useful to call for instance before we 
	 * are about to close the database since it would not be acceptable
	 * for the query to take it's time interrupting and coming to a stop.
	 */
	public void killThreadAndWait() {
		// kill the thread same as usual
		killThread();

		// join with the runThread which ensures the query thread
		// is done interrupting
		try {
			runThread.join();
		} catch(InterruptedException ie) {
			// ignore
		}
	}

	/**
	 * Inserts a text box that will be displayed until the results
	 * from the quert are available.
	 */
	public void initializeWaiting (){
		//write the text-box
		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));
		tempPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		tempPanel.add(new JLabel("Waiting for results. Close to terminate"));
		add(tempPanel);


	}




	/**
	 * Creates the group table content.
	 * 
	 * @param qg the query generator
	 * @param scnListValues 
	 * @param regionListValues 
	 * 
	 * @return a JComponent with the results in it as a  JScrollingPane
	 * @throws Exception thrown if the multiTableModel returns an invalid result
	 * 
	 */
	private JComponent createGroupTableContent(QueryGenerator qg,final Object[] scnListValues, final Object[] regionListValues) throws Exception {
		BaseTableModel bt = new MultiTableModel(qg, scnListValues, regionListValues, context);
		JTable jTable = new JTable(bt);
		jTable.setCellSelectionEnabled(true);
		jTable.getColumnModel().getColumn(0).setCellRenderer(((MultiTableModel)bt).getCellRenderer(0,0));
		jTable.getColumnModel().getColumn(0).setCellEditor(((MultiTableModel)bt).getCellEditor(0,0));
		InterfaceMain.getInstance().fireProperty("Query", null, bt);
		JScrollPane tableScrollPane = new JScrollPane(jTable);
		tableScrollPane.getViewport().setBackground(getBackground());
		return tableScrollPane;
	}

	/**
	 * Creates the single table content.
	 * 
	 * @param qg the qg
	 * @param singleBinding 
	 * @param scenarioListValues 
	 * @param regionListValues 
	 * 
	 * 
	 * @return a JComponent with the results in it as a  JScrollingPane
	 * @throws Exception thrown if the multiTableModel returns an invalid result
	 * 
	 */
	private JComponent createSingleTableContent(QueryGenerator qg, QueryBinding singleBinding, final Object[] scenarioListValues, final Object[] regionListValues) throws Exception  {
		//aseTableModel bt = new ComboTableModel(qg, scenarioListValues, regionListValues, singleBinding, context);

		final InterfaceMain main = InterfaceMain.getInstance();	//@1
		JSplitPane sp = new JSplitPane();
		//BaseTableModel 
		ComboTableModel bt = new ComboTableModel(qg, scenarioListValues, regionListValues, singleBinding, context);
		JTable jTable = bt.getAsSortedTable();
		new CopyPaste(jTable);
		jTable.setCellSelectionEnabled(true);

		javax.swing.table.TableColumn col;
		int j = 0;
		while(j < jTable.getColumnCount()) {
			col = jTable.getColumnModel().getColumn(j);
			if(jTable.getColumnName(j).equals("")) {
				col.setPreferredWidth(75);
			} else {
				col.setPreferredWidth(jTable.getColumnName(j).length()*5+30);
			}
			j++;
		}

		Properties props = main.getProperties();//@1
		// for restore legend information
		String path = System.getProperty("user.dir") + "\\LegendBundle.properties" ;
				//props.getProperty("lastDirectory") + "\\LegendBundle.properties";	//@1	
		new FilteredTable(null,qg.toString(), //@1
				/*ModelInterfaceUtil.getUnit(qg, (String) jTable.getValueAt(0, jTable.getColumnCount() - 1))*/new String[]{"TODO", "TODO"}, path, //@1
				jTable, sp); //@1

		main.fireProperty("Query", null, bt); //@1
		return sp;
	}

}
