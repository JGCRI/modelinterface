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
package ModelInterface;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ModelInterface.ConfigurationEditor.configurationeditor.ConfigurationEditor;
import ModelInterface.ConfigurationEditor.utils.DOMUtils;
import ModelInterface.ConfigurationEditor.utils.FileUtils;
//Dan: commented this out
//import ModelInterface.DMsource.DMViewer;
import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.InputViewer;
import ModelInterface.ModelGUI2.XMLFilter;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.PPsource.PPViewer;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.common.RecentFilesList;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class InterfaceMain implements ActionListener {
	/**
	 * Unique identifier used for serializing.
	 */
	private static final long serialVersionUID = -9137748180688015902L;

	public static final int FILE_MENU_POS = 0;
	public static final int EDIT_MENU_POS = 1;
	public static final int TOOLS_MENU_POS = 80; // YD added
	public static final int ADVANCED_MENU_POS = 90; // YD added
	public static final int ADVANCED_SUBMENU1_POS = 0; // YD added
	public static final int ADVANCED_SUBMENU15_POS = 2; // YD added
	public static final int ADVANCED_SUBMENU2_POS = 5; // YD added
	public static final int QUERIES_UNDO_MENUITEM_POS = 25; // YD added
	public static final int QUERIES_REDO_MENUITEM_POS = 30; // YD added
	public static final int HELP_MENU_POS = 100;
	public static final int FILE_NEW_MENUITEM_POS = 0;
	public static final int FILE_OPEN_SUBMENU_POS = 5;
	public static final int QUERIES_SAVE_MENUITEM_POS = 35; // YD changed
	public static final int QUERIES_SAVEAS_MENUITEM_POS = 40; // YD changed
	public static final int FILE_QUIT_MENUITEM_POS = 50;
	public static final int EDIT_QUERY_SUBMENU_POS = 18; // YD added
	public static final int EDIT_COPY_MENUITEM_POS = 10;
	public static final int EDIT_PASTE_MENUITEM_POS = 11;
	public static final int TOOLS_CSV_MENUITEM_POS = 1; // YD added
	public static final int TOOLS_UNIT_MENUITEM_POS = 2; // YD added
	public static final int TOOLS_SANKEY_MENUITEM_POS = 3;// YD added
	public static final int SANKEY_LOAD_MENUITEM_POS = 60; // YD added
	public static final int SANKEY_DISPLAY_MENUITEM_POS = 70; // YD added
	public static final String REGION_LIST_NAME = "region list"; // YD added

	private static File propertiesFile = new File("model_interface.properties");
	private static String oldControl;
	private static InterfaceMain main;
	private JMenuItem newMenu;
	private JMenuItem saveMenu;
	private JMenuItem saveAsMenu;
	private JMenuItem quitMenu;
	private JMenuItem copyMenu;
	private JMenuItem pasteMenu;
	private JMenuItem undoMenu;
	private JMenuItem redoMenu;
	private JMenuItem batchMenu;
	private JMenuItem toolsCSVMenu; // YD added
	private JMenuItem toolsUnitMenu; // YD added
	private JMenuItem toolsSankeyMenu; // YD added
	private JMenuItem loadMenu; // YD added
	private JMenuItem displayMenu; // YD added
	private JMenuItem editQuerySubMenu; // YD added
	private JMenu advancedSubMenu1;// YD added
	private JMenu advancedSubMenu2;// YD added
	private Properties savedProperties;
	private UndoManager undoManager;
	
	private MenuAdder dbView=null;

	private List<MenuAdder> menuAdders;
	static String path = null;
	static String queryFilename = null;
	// GLIMPSEUtils utils = GLIMPSEUtils.getInstance(); //YD added
	// GLIMPSEFiles files = GLIMPSEFiles.getInstance(); //YD added
	ArrayList<String> energyNameList = null; // YD added

	public static String unitFileLocation = null;
	/**
	 * The main GUI the rest of the GUI components of the ModelInterface will rely
	 * on.
	 */
	private JFrame mainFrame;

	/**
	 * Main function, creates a new thread for the gui and runs it.
	 */
	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {
			System.out.println("arg " + i + ": " + args[i]);
		}

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				// Dan commented out the error message which seemed to get negatively impacted
				// by threads
				if (InterfaceMain.getInstance() != null) {
					InterfaceMain.getInstance().showMessageDialog(e, "Unexpected Error", JOptionPane.ERROR_MESSAGE);
				}
				// still print the stack trace to the console for debugging
				e.printStackTrace();
			}
		});

		// -b <batch file> -l <log file> -o <dbpath>
		OptionParser parser = new OptionParser();
		parser.accepts("help", "print usage information").forHelp();
		parser.accepts("b", "XML batch file to process").withRequiredArg();
		parser.accepts("l", "log file into which to redirect ModelInterface output").withRequiredArg();
		parser.accepts("o", "path to XML DB").withRequiredArg();
		parser.accepts("q", "path to query file").withRequiredArg();
		parser.accepts("u", "Path to CSV file for unit conversions").withOptionalArg();

		OptionSet opts = null;
		try {
			opts = parser.parse(args);
		} catch (OptionException e) {
			System.err.println(e);
			System.exit(1);
		}

		if (opts.has("help")) {
			try {
				System.out.println("Usage: java -jar ModelInterface.jar -b <batch file> -l <log file>");
				parser.printHelpOn(System.out);
			} catch (Exception e) {
				System.err.println("Failed to write usage message");
				System.exit(1);
			}
			System.exit(1);
		}

		// if the -l option is set then we will redirect standard output to the
		// specified log file
		PrintStream stdout = System.out;
		if (opts.has("l")) {
			String logFile = (String) opts.valueOf("l");
			stdout.println("InterfaceMain: Directing stdout to " + logFile);
			try {
				FileOutputStream log = new FileOutputStream(logFile);
				System.setOut(new PrintStream(log));
			} catch (Exception e) {
				// If there was an error opening the log file we will post a message indicating
				// as
				// much but continue on with out the redirect.
				System.err.println("Failed to open log file '" + logFile + "' for writing: " + e);
			}
		}

		if (opts.has("o")) {
			path = (String) opts.valueOf("o");
			System.out.println("InterfaceMain: DB Path: " + path);
		}

		// added by Dan to allow query file to be specified as runtime argument
		if (opts.has("q")) {
			queryFilename = (String) opts.valueOf("q");
			System.out.println("InterfaceMain: Query File Path: " + path);
		}

		if (opts.has("b")) {
			String filename = (String) opts.valueOf("b");
			System.out.println("InterfaceMain: batchFile: " + filename);

			System.setProperty("java.awt.headless", "true");
			System.out.println("Running headless? " + GraphicsEnvironment.isHeadless());
			Document batchDoc = filename.equals("-") ? DOMUtils.parseInputStream(System.in)
					: FileUtils.loadDocument(new File(filename), null);
			main = new InterfaceMain();

			// Construct the subset of menu adders that are also BatchRunner while
			// avoiding creating any GUI components
			// TODO: avoid code duplication
			final MenuAdder dbView = new DbViewer();
			final MenuAdder inputView = new InputViewer();
			main.menuAdders = new ArrayList<MenuAdder>(2);
			main.menuAdders.add(dbView);
			main.menuAdders.add(inputView);
			
			

			// Run the batch file
			if (batchDoc != null) {
				main.runBatch(batchDoc.getDocumentElement());
			} else {
				System.out.println("Skipping batch " + filename + " due to parsing errors.");
			}
			System.setOut(stdout);
			return;
		}
		if (opts.has("u")) {
			unitFileLocation = (String) opts.valueOf("u");
		}else {
			//also look in the current directory
			File f=new File("units_rules.csv");
			if(f.exists()) {
				unitFileLocation=f.getAbsolutePath();
			}
		}

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// warn the user... should be ok to keep going
			System.out.println("Error setting look and feel: " + e);
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
				if(path!=null) {
					DbViewer db=(DbViewer)main.dbView;
					db.doOpenDB(new File(path));
					File f=new File(path);
					File[] files=new File[1];
					files[0]=f;
					RecentFilesList.getInstance().addFile(files, "ModelInterface.ModelGUI2.DbViewer","Open DB");
					
				}
			}
		});

	}

	/**
	 * Create a new instance of this class and makes it visible
	 */
	private static void createAndShowGUI() {
		main = null;
		main = new InterfaceMain();
		main.mainFrame = new JFrame("Model Interface");

		String image_str = ".\\results.png";
		main.mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(image_str));

		if (Boolean.parseBoolean(main.savedProperties.getProperty("isMaximized", "false"))) {
			main.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		String lastHeight = main.savedProperties.getProperty("lastHeight", "600");
		String lastWidth = main.savedProperties.getProperty("lastWidth", "800");
		main.mainFrame.setSize(Integer.parseInt(lastWidth), Integer.parseInt(lastHeight));

		main.mainFrame.setLayout(new BorderLayout());

		main.initialize();
		// main.pack();
		main.mainFrame.setVisible(true);
		if (path != null) {
			main.fireControlChange("DbViewer");
			
		}
	}

	private InterfaceMain() {
		mainFrame = null;
		savedProperties = new Properties();
		if (propertiesFile.exists()) {
			try {
				savedProperties.loadFromXML(new FileInputStream(propertiesFile));
				String prettyPrintProperty = savedProperties.getProperty("pretty-print", null);
				if (System.getProperty("ModelInterface.pretty-print", null) == null && prettyPrintProperty != null) {
					System.getProperties().setProperty("ModelInterface.pretty-print", prettyPrintProperty);
				}
			} catch (FileNotFoundException notFound) {
				// well I checked if it existed before so..
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		oldControl = "ModelInterface";
		if (path != null)
			savedProperties.setProperty("paramPath", path);
		else
			savedProperties.remove("paramPath");
		// added by Dan to allow query file to be specified as runtime argument
		if (queryFilename != null)
			savedProperties.setProperty("queryFile", queryFilename);
		
		
	}

	private void initialize() {
		MenuManager menuMan = new MenuManager(null);
		addWindowAdapters();
		addMenuItems(menuMan);
		addMenuAdderMenuItems(menuMan);
		finalizeMenu(menuMan);
		// if path to DB was provided, dispatch to DBViewer to open database
//		  if (path != null) fireControlChange("DbViewer");		 
	}

	public JFrame getFrame() {
		return mainFrame;
	}

	private void addMenuItems(MenuManager menuMan) {
		JMenu m = new JMenu("File");
		menuMan.addMenuItem(m, FILE_MENU_POS);
		// YD edits, August-2023
		// JMenu submenu; //YD commented out
		// submenu = new JMenu("Open"); //YD commented out, changed "Open" to "Open DB"
		// in "DbViewer.java"
		// submenu.setMnemonic(KeyEvent.VK_S); //YD commented out
		// menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(submenu,
		// FILE_OPEN_SUBMENU_POS); //YD commented out
		// menuMan.getSubMenuManager(FILE_MENU_POS).addSeparator(FILE_OPEN_SUBMENU_POS +
		// 2); //YD commented out
		// m.add(submenu);
		// m.addSeparator();

		// m.add(makeMenuItem("Quit"));
		// menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(newMenu = new
		// JMenuItem("New"), FILE_NEW_MENUITEM_POS); //YD commented out
		// menuMan.getSubMenuManager(FILE_MENU_POS).addSeparator(FILE_NEW_MENUITEM_POS);
		// //YD commented out
		// newMenu.setEnabled(false); //YD commented out
		// menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(saveMenu = new
		// JMenuItem("Save")/* makeMenuItem("Save") */, FILE_SAVE_MENUITEM_POS); //YD
		// commented out
		// saveMenu.setEnabled(false); //YD commented out
		// menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(saveAsMenu = new
		// JMenuItem("Save As"), FILE_SAVEAS_MENUITEM_POS);//YD commented out
		// menuMan.getSubMenuManager(FILE_MENU_POS).addSeparator(FILE_SAVEAS_MENUITEM_POS);//YD
		// commented out
		// saveAsMenu.setEnabled(false);//YD commented out
		menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(quitMenu = makeMenuItem("Quit"), FILE_QUIT_MENUITEM_POS);

		menuMan.addMenuItem(new JMenu("Edit"), EDIT_MENU_POS);
		// YD edits, August-2023
		// copyMenu = new JMenuItem("Copy");//YD commented out
		// key stroke is system dependent
		// copyMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
		// ActionEvent.CTRL_MASK));
		// menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(copyMenu,
		// EDIT_COPY_MENUITEM_POS);//YD commented out
		// pasteMenu = new JMenuItem("Paste");//YD commented out
		// key stroke is system dependent
		// pasteMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
		// ActionEvent.CTRL_MASK));
		// menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(pasteMenu,
		// EDIT_PASTE_MENUITEM_POS);//YD commented out
		// menuMan.getSubMenuManager(EDIT_MENU_POS).addSeparator(EDIT_PASTE_MENUITEM_POS);//YD
		// commented out

		// copyMenu.setEnabled(false);//YD commented out
		// pasteMenu.setEnabled(false);//YD commented out
		// YD commented lines 392-396 out because this "Batch File" menuItem needs to be
		// re-arranged to be under "Advanced" >> "Open Files"
		// batchMenu = new JMenuItem("Batch File");
		// batchMenu.setEnabled(true);
		// batchMenu.addActionListener(this);
		// menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(batchMenu,
		// FILE_OPEN_SUBMENU_POS);

		// YD added lines to add "Tools" and "Advanced" to the main menu bar
		menuMan.addMenuItem(new JMenu("Tools"), TOOLS_MENU_POS);
		menuMan.addMenuItem(new JMenu("Advanced"), ADVANCED_MENU_POS);
		menuMan.addMenuItem(new JMenu("Help"), HELP_MENU_POS);
		// YD added the following lines to add "Query File" under "Edit" dropdown menu
		editQuerySubMenu = new JMenuItem("Query File");
		editQuerySubMenu.setEnabled(true);
		editQuerySubMenu.addActionListener(this);
		editQuerySubMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(editQuerySubMenu, EDIT_QUERY_SUBMENU_POS);

		// YD added the following lines to add items under "Tools" dropdown menu
		// second round YD edits, commented it out and moved this block to
		// "InputViewer.java"
		/*
		 * toolsCSVMenu = new JMenuItem("CSV to XML"); toolsCSVMenu.setEnabled(true);
		 * toolsCSVMenu.addActionListener(this);
		 * menuMan.getSubMenuManager(TOOLS_MENU_POS).addMenuItem(toolsCSVMenu,
		 * TOOLS_CSV_MENUITEM_POS);
		 */
		toolsUnitMenu = new JMenuItem("Unit Conversions");
		toolsUnitMenu.setEnabled(true);
		toolsUnitMenu.addActionListener(this);
		toolsUnitMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));

		menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(toolsUnitMenu, EDIT_QUERY_SUBMENU_POS);
		// YD commented these lines out, hide "Sankey Diagrams" for now
		// toolsSankeyMenu= new JMenu("Sankey Diagrams");
		// menuMan.getSubMenuManager(TOOLS_MENU_POS).addMenuItem(toolsSankeyMenu,
		// TOOLS_SANKEY_MENUITEM_POS);
		// YD added the following lines to create two sub-menus under "Advanced"
		// dropdown menu
		advancedSubMenu1 = new JMenu("Queries");
		advancedSubMenu1.setMnemonic(KeyEvent.VK_S);
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).addMenuItem(advancedSubMenu1, ADVANCED_SUBMENU1_POS);
		advancedSubMenu2 = new JMenu("Open Files");
		advancedSubMenu2.setMnemonic(KeyEvent.VK_O);
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).addMenuItem(advancedSubMenu2, ADVANCED_SUBMENU2_POS);
		// YD added the following lines to re-arrange "Batch File" from "File" dropdown
		// menu to be under "Advanced" >> "Open Files"

		batchMenu = new JMenuItem("Batch Query File");
		batchMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		batchMenu.addActionListener(this);
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).getSubMenuManager(ADVANCED_SUBMENU2_POS).addMenuItem(batchMenu, 5);
		// YD moved these lines because "Save" and "Save as" are moved to be "Advanced"
		// >> "Queries"
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).getSubMenuManager(ADVANCED_SUBMENU1_POS)
				.addMenuItem(saveMenu = new JMenuItem("Save"), QUERIES_SAVE_MENUITEM_POS);
		saveMenu.setEnabled(false);
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).getSubMenuManager(ADVANCED_SUBMENU1_POS)
				.addMenuItem(saveAsMenu = new JMenuItem("Save As"), QUERIES_SAVEAS_MENUITEM_POS);
		saveAsMenu.setEnabled(false);

		setupUndo(menuMan);
		// doSankey(menuMan); //YD commented it out, hide "Sankey Diagrams" for now
	}

	// second round YD edited the following lines to move "Undo" and "Redo" to be
	// under "Advanced" >> "Queries"

	private void setupUndo(MenuManager menuMan) {
		undoManager = new UndoManager();
		undoManager.setLimit(10);

		undoMenu = new JMenuItem("Undo");
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).getSubMenuManager(ADVANCED_SUBMENU1_POS).addMenuItem(undoMenu,
				QUERIES_UNDO_MENUITEM_POS);
		// menuMan.getSubMenuManager(InterfaceMain.ADVANCED_MENU_POS).getSubMenuManager(InterfaceMain.ADVANCED_SUBMENU1_POS).addSeparator(QUERIES_UNDO_MENUITEM_POS);
		redoMenu = new JMenuItem("Redo");
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).getSubMenuManager(ADVANCED_SUBMENU1_POS).addMenuItem(redoMenu,
				QUERIES_REDO_MENUITEM_POS);
		menuMan.getSubMenuManager(ADVANCED_MENU_POS).getSubMenuManager(ADVANCED_SUBMENU1_POS)
				.addSeparator(QUERIES_REDO_MENUITEM_POS);

		undoMenu.setEnabled(false);
		redoMenu.setEnabled(false);

		ActionListener undoListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (cmd.startsWith("Undo")) {
					try {
						undoManager.undo();
						refreshUndoRedo();
					} catch (CannotUndoException cue) {
						cue.printStackTrace();
					}
				} else if (cmd.startsWith("Redo")) {
					try {
						undoManager.redo();
						refreshUndoRedo();
					} catch (CannotRedoException cre) {
						cre.printStackTrace();
					}
				} else {
					System.out.println("Didn't recognize: " + cmd);
				}
			}
		};

		undoMenu.addActionListener(undoListener);
		redoMenu.addActionListener(undoListener);
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}

	public void refreshUndoRedo() {
		undoMenu.setText(undoManager.getUndoPresentationName());
		undoMenu.setEnabled(undoManager.canUndo());
		redoMenu.setText(undoManager.getRedoPresentationName());
		redoMenu.setEnabled(undoManager.canRedo());
	}

	/*
	 * 
	 * //YD added these lines for Sankey diagram private void doSankey(MenuManager
	 * menuMan) { loadMenu = new JMenuItem("Load");
	 * menuMan.getSubMenuManager(TOOLS_MENU_POS).getSubMenuManager(
	 * TOOLS_SANKEY_MENUITEM_POS).addMenuItem(loadMenu, SANKEY_LOAD_MENUITEM_POS);
	 * 
	 * displayMenu = new JMenuItem("Display");
	 * menuMan.getSubMenuManager(TOOLS_MENU_POS).getSubMenuManager(
	 * TOOLS_SANKEY_MENUITEM_POS).addMenuItem(displayMenu,
	 * SANKEY_DISPLAY_MENUITEM_POS);
	 * 
	 * loadMenu.setEnabled(true); displayMenu.setEnabled(true);
	 * 
	 * ActionListener sankeyListener = new ActionListener() { public void
	 * actionPerformed(ActionEvent e) { String cmd = e.getActionCommand(); if
	 * (cmd.equals("Load")) { FileChooser fc = FileChooserFactory.getFileChooser();
	 * final File[] result = fc.doFilePrompt(mainFrame, "Load a csv File",
	 * FileChooser.LOAD_DIALOG, new
	 * File(getProperties().getProperty("lastDirectory", ".")), new CSVFilter());
	 * System.out.println("Check load actionEvent now!!!! "); if (result != null) {
	 * File file = result[0]; System.out.println("the uploaded csv file name is:");
	 * System.out.println(file.getAbsolutePath()); if
	 * ((file.getAbsolutePath().endsWith(".csv"))) { energyNameList =
	 * files.getStringArrayFromFile(file.getAbsolutePath(), "#");
	 * utils.printArrayList(energyNameList); }else {
	 * utils.warningMessage("Only CSV file is acceptted!!!");
	 * System.out.println("Only CSV file is acceptted!!!"); }
	 * 
	 * } // TODO: read the loaded csv file and save it to memory,YD added }else
	 * if(cmd.equals("Display")) {
	 * System.out.println("Check display actionEvent now!!!! "); //TODO: write a
	 * method to construct a FlowDataset from the query results //this part is just
	 * a placeholder and hard-coded with a sample FlowDataset
	 * 
	 * DefaultFlowDataset dataset = new DefaultFlowDataset(); dataset.setFlow(1,
	 * "Work 1", "house", 30); dataset.setFlow(1, "Work 1", "food", 50);
	 * dataset.setFlow(1, "Work 2", "food",50); dataset.setFlow(1, "Work 3",
	 * "Clothing", 100); dataset.setFlow(1, "work 4", "house", 70);
	 * 
	 * System.out.println("Check stageCount now:");
	 * System.out.println(dataset.getStageCount());
	 * System.out.println("Check all nodes:");
	 * System.out.println(dataset.getAllNodes());
	 * 
	 * FlowPlot myPlot = new FlowPlot(dataset);
	 * System.out.println("Check plot type from flowplot:");
	 * System.out.println(myPlot.getPlotType());
	 * 
	 * /* regions = getRegions(); regionList = new JList(regions);
	 * regionList.setName(REGION_LIST_NAME); Object[] regionSel =
	 * regionList.getSelectedValuesList().toArray();
	 * 
	 * if (regionSel.length == 0) { InterfaceMain.getInstance().
	 * showMessageDialog("Please select Regions to run the query against",
	 * "Run Sankey Error", JOptionPane.ERROR_MESSAGE); }else {
	 * System.out.println("some regions are selected.");
	 * System.out.println(regionSel); }
	 * 
	 * } } }; //actionListener end loadMenu.addActionListener(sankeyListener);
	 * displayMenu.addActionListener(sankeyListener); }
	 */

	private void addMenuAdderMenuItems(MenuManager menuMan) {
		/*
		 * FileChooserDemo is being removed, but I will leave this here, This is how I
		 * envision the menuitems to be added and hopefully all the listeners would be
		 * set up correctly and we won't need to keep the pointer to the classes around
		 * FileChooserDemo fcd = new FileChooserDemo(this); fcd.addMenuItems(menuMan);
		 */
		dbView = new DbViewer();
		dbView.addMenuItems(menuMan);
		final MenuAdder inputView = new InputViewer();
		inputView.addMenuItems(menuMan);
		final MenuAdder PPView = new PPViewer();
		PPView.addMenuItems(menuMan);
		// Dan: Commented this out
		// final MenuAdder DMView = new DMViewer();
		// DMView.addMenuItems(menuMan);
		final MenuAdder recentFilesList = RecentFilesList.getInstance();
		recentFilesList.addMenuItems(menuMan);
		final MenuAdder aboutDialog = new AboutDialog();
		aboutDialog.addMenuItems(menuMan);

		// Create the Configuration editor and allow it to add its menu items to the
		// menu system.
		final MenuAdder confEditor = new ConfigurationEditor();
		confEditor.addMenuItems(menuMan);

		menuAdders = new ArrayList<MenuAdder>(6);
		menuAdders.add(dbView);
		menuAdders.add(inputView);
		menuAdders.add(PPView);
		// menuAdders.add(DMView);
		menuAdders.add(recentFilesList);
		menuAdders.add(aboutDialog);
		menuAdders.add(confEditor);
	}

	private void finalizeMenu(MenuManager menuMan) {
		JMenuBar mb = menuMan.createMenu(); // new JMenuBar();
		mainFrame.setJMenuBar(mb);
	}

	private void addWindowAdapters() {
		// Add adapter to catch window events.
		WindowAdapter myWindowAdapter = new WindowAdapter() {
			public void windowStateChanged(WindowEvent e) {
				savedProperties.setProperty("isMaximized",
						String.valueOf((e.getNewState() & JFrame.MAXIMIZED_BOTH) != 0));
			}

			public void windowClosing(WindowEvent e) {
				// System.out.println("Caught the window closing");
				// fireProperty("Control", oldControl, "ModelInterface");
				if (!Boolean.parseBoolean(savedProperties.getProperty("isMaximized"))) {
					savedProperties.setProperty("lastWidth", String.valueOf(mainFrame.getWidth()));
					savedProperties.setProperty("lastHeight", String.valueOf(mainFrame.getHeight()));
				}
				try {
					savedProperties.storeToXML(new FileOutputStream(propertiesFile), "TODO: add comments");
				} catch (FileNotFoundException notFound) {
					notFound.printStackTrace();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				System.exit(0);
			}

			public void windowClosed(WindowEvent e) {
				// System.out.println("Caught the window closed");
				// fireProperty("Control", oldControl, "ModelInterface");
				if (!Boolean.parseBoolean(savedProperties.getProperty("isMaximized"))) {
					savedProperties.setProperty("lastWidth", String.valueOf(mainFrame.getWidth()));
					savedProperties.setProperty("lastHeight", String.valueOf(mainFrame.getHeight()));
				}
				try {
					savedProperties.storeToXML(new FileOutputStream(propertiesFile), "TODO: add comments");
				} catch (FileNotFoundException notFound) {
					notFound.printStackTrace();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				System.exit(0);
			}
		};
		mainFrame.addWindowListener(myWindowAdapter);
		mainFrame.addWindowStateListener(myWindowAdapter);

		mainFrame.getGlassPane().addMouseListener(new MouseAdapter() {
		});
		mainFrame.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private JMenuItem makeMenuItem(String title) {
		JMenuItem m = new JMenuItem(title);
		m.addActionListener(this);
		return m;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Quit")) {
			// fireProperty("Control", oldControl, "ModelInterface");
			mainFrame.dispose();
			// YD edits second round, when user choose "Query File", check the query file
			// saved in the savedProperties file
			// and open the system editor, allowing user to edit it
		} else if (e.getActionCommand().equals("Query File")) {
			if (propertiesFile.exists()) {
				String theCurrentQueryFile = savedProperties.getProperty("queryFile", null);
				System.out.println(
						"check the current query file path in the savedProperties file: " + theCurrentQueryFile);
				try {
					Desktop.getDesktop().edit(new File(theCurrentQueryFile));
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		} else if (e.getActionCommand().equals("Unit Conversions")) {
			if (propertiesFile.exists()) {
				String unitsFileName = InterfaceMain.unitFileLocation;
				if (unitsFileName != null && unitsFileName.length() > 0) {
					try {
						Desktop.getDesktop().edit(new File(unitsFileName));
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
		} else if (e.getActionCommand().equals("Batch File")) {
			// TODO: make it so recent files could work with this
			FileChooser fc = FileChooserFactory.getFileChooser();
			final File[] result = fc.doFilePrompt(mainFrame, "Open Batch File", FileChooser.LOAD_DIALOG,
					new File(getProperties().getProperty("lastDirectory", ".")), new XMLFilter());
			// these should be run off the GUI thread
			new Thread(new Runnable() {
				public void run() {
					if (result != null) {
						for (File file : result) {
							Document doc = FileUtils.loadDocument(file, null);
							// Only run if the batch file was parsed correctly
							// note an error would have already been given if it wasn't
							// parsed correctly
							if (doc != null) {
								runBatch(doc.getDocumentElement());
							}
						}
					}
					// TODO: message that all were run
				}
			}).start();
		}
	}

	public static InterfaceMain getInstance() {
		return main;
	}

	// YD commented lines 526-536 out because we removed "New","Save","Save As" from
	// "File" dropdown menu
	// public JMenuItem getNewMenu() {
	// return newMenu;
	// }

	public JMenuItem getSaveMenu() {
		return saveMenu;
	}

	public JMenuItem getSaveAsMenu() {
		return saveAsMenu;
	}

	public JMenuItem getQuitMenu() {
		return quitMenu;
	}

	// YD commented lines 542-548 out because we removed "Copy","Paste" from "Edit"
	// dropdown menu
	// public JMenuItem getCopyMenu() {
	// return copyMenu;
	// }

	// public JMenuItem getPasteMenu() {
	// return pasteMenu;
	// }

	// YD edits, these two methods are called in "DbViewer.java"
	public JMenuItem getUndoMenu() {
		return undoMenu;
	}

	public JMenuItem getRedoMenu() {
		return redoMenu;
	}

	public JMenuItem getBatchMenu() {
		return batchMenu;
	}

	public void fireControlChange(String newValue) {
		//System.out.println("Going to change controls");
		if (newValue.equals(oldControl)) {
			oldControl += "Same";
		}
		fireProperty("Control", oldControl, newValue);
		oldControl = newValue;
	}

	public void fireProperty(String propertyName, Object oldValue, Object newValue) {
		final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
		for (PropertyChangeListener listener : mainFrame.getPropertyChangeListeners()) {
			listener.propertyChange(event);
		}
	}

	public class MenuManager {
		private JMenuItem menuValue;
		private Map<Integer, MenuManager> subItems;
		private SortedSet<Integer> sepList;

		MenuManager(JMenuItem menuValue) {
			this.menuValue = menuValue;
			sepList = null;
			if (menuValue == null || menuValue instanceof JMenu) {
				subItems = new TreeMap<Integer, MenuManager>();
			} else {
				subItems = null;
			}
		}

		/*
		 * public JMenuItem getMenuValue() { return menuValue; } public Map
		 * getSubItems() { return subItems; }
		 */
		public void addSeparator(int where) {
			if (sepList == null) {
				sepList = new TreeSet<Integer>();
			}
			sepList.add(where);
		}

		public int addMenuItem(JMenuItem menu, int where) {
			if (subItems.containsKey(where)) {
				return addMenuItem(menu, where + 1);
			} else {
				subItems.put(where, new MenuManager(menu));
				return where;
			}
		}

		public MenuManager getSubMenuManager(int where) {
			if (!subItems.containsKey(where)) {
				// throw exception or just return null?
				return null;
			}
			return ((MenuManager) subItems.get(where));
		}

		JMenuBar createMenu() {
			JMenuBar ret = new JMenuBar();
			Object[] keys = subItems.keySet().toArray();
			for (int i = 0; i < keys.length; ++i) {
				ret.add(((MenuManager) subItems.get(keys[i])).createSubMenu());
			}
			return ret;
		}

		private JMenuItem createSubMenu() {
			if (subItems == null) {
				return menuValue;
			} else {
				Object[] keys = subItems.keySet().toArray();
				for (int i = 0; i < keys.length; ++i) {
					if (sepList != null && !sepList.isEmpty()
							&& ((Integer) keys[i]).intValue() > ((Integer) sepList.first()).intValue()) {
						((JMenu) menuValue).addSeparator();
						sepList.remove(sepList.first());
					}
					menuValue.add(((MenuManager) subItems.get(keys[i])).createSubMenu());
				}
				return menuValue;
			}
		}
	}

	public Properties getProperties() {
		return savedProperties;
	}

	/*
	 * //YD added line 679-722 private void doSankey(MenuManager menuMan) { loadMenu
	 * = new JMenuItem("Load");
	 * menuMan.getSubMenuManager(TOOLS_MENU_POS).getSubMenuManager(
	 * TOOLS_SANKEY_MENUITEM_POS).addMenuItem(loadMenu, SANKEY_LOAD_MENUITEM_POS);
	 * displayMenu = new JMenuItem("Display");
	 * menuMan.getSubMenuManager(TOOLS_MENU_POS).getSubMenuManager(
	 * TOOLS_SANKEY_MENUITEM_POS).addMenuItem(displayMenu,
	 * SANKEY_DISPLAY_MENUITEM_POS); ActionListener sankeyListener = new
	 * ActionListener() { public void actionPerformed(ActionEvent e) { String cmd =
	 * e.getActionCommand(); if (cmd.equals("Load")) { FileChooser fc =
	 * FileChooserFactory.getFileChooser(); final File[] result =
	 * fc.doFilePrompt(mainFrame, "Load a csv File", FileChooser.LOAD_DIALOG, new
	 * File(getProperties().getProperty("lastDirectory", ".")), new CSVFilter());
	 * System.out.println("Check load actionEvent now!!!! "); if (result != null) {
	 * File file = result[0]; System.out.println("the uploaded csv file name is:");
	 * System.out.println(file.getAbsolutePath()); if
	 * ((file.getAbsolutePath().endsWith(".csv"))) { energyNameList =
	 * files.getStringArrayFromFile(file.getAbsolutePath(), "#");
	 * utils.printArrayList(energyNameList); }else {
	 * utils.warningMessage("Only CSV file is acceptted!!!");
	 * System.out.println("Only CSV file is acceptted!!!"); }
	 * 
	 * } // TODO: read the loaded csv file and save it to memory,YD added }else
	 * if(cmd.equals("Display")) { //run the queries String
	 * q1="End-use energy consumption in buildings"; //read through tree getting
	 * object
	 * 
	 * //get the years
	 * 
	 * //make a list Object[] possibilities =
	 * {"2015","2020","2025","2030","2035","2040","2045","2050"};
	 * 
	 * //show options window String s = (String)JOptionPane.showInputDialog(
	 * main.mainFrame, "Please select a year", "Year Selection",
	 * JOptionPane.PLAIN_MESSAGE, null, possibilities, "ham");
	 * 
	 * //If a string was returned, say so. if ((s != null) && (s.length() > 0)) {
	 * return; }
	 * 
	 * 
	 * 
	 * 
	 * System.out.println("Check display actionEvent now!!!! ");
	 * 
	 * DefaultFlowDataset dataset = new DefaultFlowDataset(); dataset.setFlow(1,
	 * "Work 1", "house", 30); dataset.setFlow(1, "Work 1", "food", 50);
	 * dataset.setFlow(1, "Work 2", "food",50); dataset.setFlow(1, "Work 3",
	 * "Clothing", 100); dataset.setFlow(1, "work 4", "house", 70);
	 * 
	 * System.out.println("Check stageCount now:");
	 * System.out.println(dataset.getStageCount());
	 * System.out.println("Check all nodes:");
	 * System.out.println(dataset.getAllNodes());
	 * 
	 * FlowPlot myPlot = new FlowPlot(dataset);
	 * 
	 * /* regions = getRegions(); regionList = new JList(regions);
	 * regionList.setName(REGION_LIST_NAME); Object[] regionSel =
	 * regionList.getSelectedValuesList().toArray();
	 * 
	 * if (regionSel.length == 0) { InterfaceMain.getInstance().
	 * showMessageDialog("Please select Regions to run the query against",
	 * "Run Sankey Error", JOptionPane.ERROR_MESSAGE); }else {
	 * System.out.println("some regions are selected.");
	 * System.out.println(regionSel); }
	 * 
	 * >>>>>>> 8d731d5a2756344729c46c80d82ba5c1a127c654 } } }; //actionListener end
	 * loadMenu.addActionListener(sankeyListener);
	 * displayMenu.addActionListener(sankeyListener); }
	 */

	// YD added, 07-05-2023,line 703-723 this method "getRegions()" was copied from
	// "DbViewer.java"
	protected Vector getRegions() {
		Vector funcTemp = new Vector<String>(1, 0);
		funcTemp.add("distinct-values");
		Vector ret = new Vector();
		QueryProcessor queryProc = XMLDB.getInstance().createQuery(
				"/scenario/world/" + ModelInterface.ModelGUI2.queries.QueryBuilder.regionQueryPortion + "/@name",
				funcTemp, null, null);
		try {
			Iter res = queryProc.iter();
			Item temp;
			while ((temp = res.next()) != null) {
				ret.add(temp.toJava());
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} finally {
			queryProc.close();
		}
		ret.add("Global");
		return ret;
	}

	/**
	 * Get the menu adder with the specified class name. Used to get the instance of
	 * the menu adder that could open a recent file.
	 * 
	 * @param classname The class that is requested.
	 * @return The instance of the class or null if not found.
	 */
	public MenuAdder getMenuAdder(String classname) {
		for (Iterator<MenuAdder> it = menuAdders.iterator(); it.hasNext();) {
			MenuAdder curr = it.next();
			if (curr.getClass().getName().equals(classname)) {
				return curr;
			}
		}
		return null;
	}

	/**
	 * Runs the given batch file. Relies on the menuAdders list and if any of the
	 * class implements BatchRunner it will pass it off the command to that class.
	 * 
	 * @param doc The batch file parsed into a DOM document which contains the
	 *            commands to run.
	 * @see BatchRunner
	 */
	private void runBatch(Node doc) {
		// TODO: remove this check once batch queries get merged
		if (doc.getNodeName().equals("queries")) {
			System.out.println("Batch queries are not yet merged with this functionality.");
			System.out.println("Please open a database then run the batch file.");
			// TODO: print this on the screen
			return;
		}

		NodeList commands = doc.getChildNodes();
		for (int i = 0; i < commands.getLength(); ++i) {
			if (commands.item(i).getNodeName().equals("class")) {
				Element currClass = (Element) commands.item(i);
				String className = currClass.getAttribute("name");
				MenuAdder runner = getMenuAdder(className);
				if (runner != null && runner instanceof BatchRunner) {
					((BatchRunner) runner).runBatch(currClass);
				} else {
					showMessageDialog("Could not find batch runner for class " + className, "Batch File Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		showMessageDialog("Finished running batch file", "Batch File Complete", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Convert JOptionPane message types to string so that they can be logged to the
	 * console.
	 * 
	 * @param messageType The JOptionPane message type.
	 * @return A string representing the meaning of messageType.
	 */
	private static String convertMessageTypeToString(int messageType) {
		switch (messageType) {
		case JOptionPane.ERROR_MESSAGE:
			return "ERROR";
		case JOptionPane.INFORMATION_MESSAGE:
			return "INFO";
		case JOptionPane.PLAIN_MESSAGE:
			return "PLAIN";
		case JOptionPane.QUESTION_MESSAGE:
			return "QUESTION";
		case JOptionPane.WARNING_MESSAGE:
			return "WARNING";
		default:
			return "UNKNOWN";
		}
	}

	/**
	 * Wrapper for JOptionPane.showMessageDialog which checks if we are running
	 * headless. If we are running headless a message is just written to stdout
	 * instead of popping up on screen.
	 * 
	 * @param message     The message to show.
	 * @param title       The title of the dialog.
	 * @param messageType The message type.
	 */
	public void showMessageDialog(Object message, String title, int messageType) {
		// Dan: Message dialog seemed to cause threading issues. Now just prints to
		// stdout
		// if (GraphicsEnvironment.isHeadless()) {
		// Convert the message dialog to a console log
		System.out.print(convertMessageTypeToString(messageType));
		System.out.print("; ");
		System.out.println(message);
		/*
		 * } else { // Just forward to JOptionPane
		 * JOptionPane.showMessageDialog(mainFrame, message, title, messageType); }
		 */
	}

	/**
	 * Convert JOptionPane option types to string so that they can be logged to the
	 * console.
	 * 
	 * @param optionType The JOptionPane option type.
	 * @return A string representing the meaning of optionType.
	 */
	private static String convertOptionTypeToString(int optionType) {
		switch (optionType) {
		case JOptionPane.CANCEL_OPTION:
			return "CANCEL";
		case JOptionPane.CLOSED_OPTION:
			return "CLOSED";
		case JOptionPane.NO_OPTION:
			return "NO";
		case JOptionPane.YES_OPTION:
			return "YES";
		default:
			return "UNKNOWN";
		}
	}

	/**
	 * Wrapper for JOptionPane.showConfirmDialog which checks if we are running
	 * headless. If we are running headless a message is just written to stdout
	 * instead of popping up on screen and the defaultOption will be selected.
	 * 
	 * @param message       The message to show.
	 * @param title         The title of the dialog.
	 * @param optionType    The option types to choose from.
	 * @param messageType   The message type.
	 * @param defaultOption The default option to choose when running headless.
	 * @return The option chosen.
	 */
	public int showConfirmDialog(Object message, String title, int optionType, int messageType, int defaultOption) {
		if (GraphicsEnvironment.isHeadless()) {
			// Convert the message dialog to a console log
			System.out.print("YES/NO/CANCEL");
			System.out.print("; ");
			System.out.print(message);
			System.out.print("; ");
			System.out.println(convertOptionTypeToString(defaultOption));
			return defaultOption;
		} else {
			// Just forward to JOptionPane
			return JOptionPane.showConfirmDialog(mainFrame, message, title, optionType, messageType);
		}
	}
}
