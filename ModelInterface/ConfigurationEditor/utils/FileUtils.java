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
package ModelInterface.ConfigurationEditor.utils;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import ModelInterface.ConfigurationEditor.configurationeditor.ConfigurationEditor;
import ModelInterface.ConfigurationEditor.configurationeditor.PropertiesInfo;
import ModelInterface.InterfaceMain;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;

/**
 * Static utility class with helper functions.
 * 
 * @author Josh Lurz
 * 
 */
final public class FileUtils {
	/**
	 * Static properties object.
	 */
	static Properties sProperties = new Properties();

	/**
	 * Whether the properties have been initialized.
	 */
	static boolean sPropertiesInitialized = false;

	/**
	 * Private constructor to prevent the creation of the class.
	 */
	private FileUtils() {
		super();
	}

	/**
	 * Check if a document is dirty or needs to be saved by checking an
	 * attribute on the root element of the document.
	 * 
	 * @param aDocument
	 *            The document to check if it needs to be saved.
	 * @return Whether the document needs to be saved.
	 */
	static public boolean isDirty(final Document aDocument) {
		synchronized (aDocument) {
			// Don't have to check for null because synchronization already
			// checks
			return (aDocument.getDocumentElement()
					.getAttribute("needs-save").length() > 0); //$NON-NLS-1$
		}
	}

	/**
	 * Get the document name from the document as a File.
	 * 
	 * @param aDocument
	 *            The document of which to get the name.
	 * @return The file name of the document.
	 */
	static public File getDocumentFile(final Document aDocument) {
		synchronized (aDocument) {
			final String uriString = aDocument.getDocumentURI();
			if (uriString == null) {
				return null;
			}
			try {
				// Create a URI object from the string.
				final URI docURI = new URI(uriString);
                
				return new File(docURI.getPath());
			} catch (URISyntaxException aException) {
				Logger.global.log(Level.SEVERE,
						"Failed to convert the document path into a URI.");
				return null;
			}
		}
	}

	/**
	 * Set the path of a document onto it.
	 * 
	 * @param aDocument
	 *            The document to set the name onto.
	 * @param aFile
	 *            The file to set as the path to the document.
	 */
	static public void setDocumentFile(final Document aDocument,
			final File aFile) {
		// TODO: Crashes on a null document here.
		synchronized (aDocument) {
			if (aFile == null) {
				aDocument.setDocumentURI(null);
			} else {
				aDocument.setDocumentURI(aFile.toURI().toString());
			}
		}
	}

	/**
	 * Get the selected files from a file chooser. This method handles multiple
	 * or single selected items correctly.
	 * 
	 * @param aChooser
	 *            The file chooser from which to get the selected files.
	 * @return The list of selected files.
	 */
	static public File[] getSelectedFiles(final JFileChooser aChooser) {
		// First try to get multiple items. If there is only one
		// item selected this will return an empty array.
		File[] selectedItems = aChooser.getSelectedFiles();
		if (selectedItems.length == 0) {
			selectedItems = new File[1];
			selectedItems[0] = aChooser.getSelectedFile();
		}
		return selectedItems;
	}

	/**
	 * Get the file extension string.
	 * 
	 * @param aFile
	 *            File name
	 * @return The file extension.
	 */
	public static String getExtension(final File aFile) {
		final String fileName = aFile.getName();
		final int periodIndex = fileName.lastIndexOf('.');
		String extension = null;
		if (periodIndex > 0 && periodIndex < fileName.length() - 1) {
			extension = fileName.substring(periodIndex + 1).toLowerCase();
		}
		return extension;
	}

	/**
	 * Initialize a properties object with read in data.
	 * 
	 * @param aWindow
	 *            A window over which to display error messages.
	 * @return An initialized properties object.
	 */
	public static Properties getInitializedProperties(final Component aWindow) {
		synchronized (sProperties) {
			// Check if the properties haven't already been initialized.
			if (!sPropertiesInitialized) {
				// changed to let the InterfaceMain handle properties
				// which means that we no longer load the properties
				// and can't fail here and the sProperties will always be
				// initialized even if it really wasn't loaded from a file..
				sProperties = InterfaceMain.getInstance().getProperties();
				sPropertiesInitialized = true;
			}
			return sProperties;
		}
	}

	/**
	 * Ask the user if they would like to perform a save, save the file if they
	 * respond affirmatively. TODO: Make this more generic.
	 * 
	 * @param aEditor
	 *            The editor
	 * 
	 * @return Whether the user wants to continue with the action which
	 *         initiated this request.
	 */
	public static boolean askForSave(final ConfigurationEditor aEditor) {
		if (aEditor.getDocument() != null && isDirty(aEditor.getDocument())) {
			final String message = Messages.getString("FileUtils.25"); //$NON-NLS-1$
			final int returnValue = JOptionPane
					.showConfirmDialog(
							aEditor,
							message,
							Messages.getString("FileUtils.26"), JOptionPane.YES_NO_CANCEL_OPTION); //$NON-NLS-1$
			if (returnValue == JOptionPane.YES_OPTION) {
				(aEditor).dispatchSave();
			} else if (returnValue == JOptionPane.CANCEL_OPTION) {
				// They closed the dialog, they want to continue.
				return false;
			}
		}
		return true;
	}

	/**
	 * Use a file chooser to choose a file. Sets the initial location of the
	 * file chooser to the last selected file.
	 * 
	 * @param aParentWindow
	 *            The window to use to display the dialog.
	 * @param aFilter
	 *            The file filter to use.
	 * @param aCurrentFile
	 *            A directory to start the chooser in. If it is null than the
	 *            chooser will use the most recently opened directory.
	 * @param aIsSave
	 *            Whether this is a save instead of a load operation.
	 * @return The selected file or null if the user canceled the action.
	 */
	public static File selectFile(final Component aParentWindow,
			final FileFilter aFilter, final String aCurrentFile,
			final boolean aIsSave) {

		// Get the properties.
		final Properties props = FileUtils
				.getInitializedProperties(aParentWindow);

		// Determine in what directory the chooser should start.
		String recentFile;
		// First choice for the starting location of the field is the
		// current value of the field if there is one.
		if (aCurrentFile == null || aCurrentFile.equals("")) {
			// Find the most recent file the user opened from the properties.
			recentFile = props.getProperty(PropertiesInfo.RECENT_FILE);
		} else {
			recentFile = aCurrentFile;
		}

		// Ask the user for a file to load.
		final FileChooser chooser = FileChooserFactory.getFileChooser();
		// TODO: use to want single selection, figure out what to do about this
		// right now it will get the selections but only use the first one 
		File[] selectedFiles = chooser.doFilePrompt(aParentWindow, null, aIsSave ? FileChooser.SAVE_DIALOG :
				FileChooser.LOAD_DIALOG, new File(recentFile), aFilter);

		if(selectedFiles != null) {
			props.setProperty(PropertiesInfo.RECENT_FILE, selectedFiles[0]
					.getAbsolutePath());
			return selectedFiles[0];
		} else {
			return null;
		}
	}

	/**
	 * Load a document and check if it has the appropriate root tag.
	 * 
	 * @param aParentWindow
	 *            Parent window to center error messages, which is allowed to be
	 *            null.
	 * @param aFile
	 *            The location of the file to load.
	 * @param aRootTag
	 *            Tag to verify the loaded document's root tag. If this is null
	 *            the check is skipped.
	 * @return A loaded document or null if it failed.
	 */
	public static Document loadDocument(final File aFile, final String aRootTag) {

		// Check if the file exists.
		if (!aFile.exists()) {
			// Tell the user the file does not exist.
			final String message = Messages.getString("BatchFileEditor.3"); //$NON-NLS-1$
			final String messageTitle = Messages.getString("BatchFileEditor.4"); //$NON-NLS-1$
			Logger.global.log(Level.SEVERE, message);
			InterfaceMain.getInstance().showMessageDialog(message, messageTitle,
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
        
        // Convert the file path to a URI before parsing to
        // ensure the document has a valid heirarchical URI.
        final URI docURI = aFile.toURI();
        
		// Attempt to parse the document.
		Document loadedDocument = null;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			loadedDocument = parser.parse(docURI.getPath());
		} catch (Exception e) {
			// Unexpected error parsing the document.
			Logger.global.log(Level.SEVERE, e.getStackTrace().toString());
			final String errorMessage = Messages.getString("LoadAction.1") //$NON-NLS-1$
					+ e.getMessage() + "."; //$NON-NLS-1$
			final String errorTitle = Messages.getString("LoadAction.3"); //$NON-NLS-1$
			InterfaceMain.getInstance().showMessageDialog(errorMessage,
					errorTitle, JOptionPane.ERROR_MESSAGE);
			return null;
		}

		// Check if the root element is the correct element.
		if (aRootTag != null
				&& !loadedDocument.getDocumentElement().getNodeName().equals(
						aRootTag)) {
			final String errorTitle = Messages.getString("LoadAction.5"); //$NON-NLS-1$
			final String errorMessage = Messages.getString("LoadAction.6"); //$NON-NLS-1$
			Logger.global.log(Level.SEVERE, errorMessage);
			InterfaceMain.getInstance().showMessageDialog(errorMessage,
					errorTitle, JOptionPane.ERROR_MESSAGE);
			return null;
		}
		return loadedDocument;
	}

	/**
	 * Create a new document and add a root element.
	 * 
	 * @param aFile
	 *            The file to save the document as.
	 * @param aParentWindow
	 *            The parent window to use to center error messages.
	 * @param aRootTag
	 *            The root element name.
	 * @return A newly constructed batch file document with the root node set.
	 */
	public static Document createDocument(final Component aParentWindow,
			final File aFile, final String aRootTag) {
		// Check if the new file already exists.
		if (aFile != null && aFile.exists()) {
			final String errorMessage = Messages
					.getString("ConfigurationEditor.147"); //$NON-NLS-1$
			final String errorTitle = Messages
					.getString("ConfigurationEditor.148"); //$NON-NLS-1$
			JOptionPane.showMessageDialog(aParentWindow, errorMessage,
					errorTitle, JOptionPane.ERROR_MESSAGE);
			return null;
		}

		// Create a new document.
		DOMImplementation domImpl = null;
		try {
			domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.getDOMImplementation();
		} catch (ParserConfigurationException e) {
			Logger.global
					.log(Level.SEVERE,
							"Failed to create DOM implementation necessary to create the document.");
			return null;
		}
		final Document newDocument = domImpl.createDocument(null, aRootTag,
				null);

		// Set the name of the file into the document. This must be done
		// manually because the document was built from scratch.
		FileUtils.setDocumentFile(newDocument, aFile);

		return newDocument;
	}
}
