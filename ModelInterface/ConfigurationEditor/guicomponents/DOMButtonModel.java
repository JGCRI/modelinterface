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
package ModelInterface.ConfigurationEditor.guicomponents;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ModelInterface.ConfigurationEditor.utils.DOMUtils;
import ModelInterface.ConfigurationEditor.utils.Messages;

/**
 * A class which wraps a default button model and replaces call to set and get
 * whether the button is selected with calls that query and set a value into the
 * DOM document.
 * 
 * @author Josh Lurz
 * 
 */
public class DOMButtonModel extends DefaultButtonModel implements ButtonModel, ItemListener, PropertyChangeListener {
    /**
     * Automatically generated unique class identifier.
     */
    private static final long serialVersionUID = -4448518797574448977L;

    /**
     * The XPath of the parent of this combo box.
     */
    private transient final String mParentXPath;

    /**
     * The name of the element for this button.
     */
    private transient final String mElementName;

    /**
     * The name of this item.
     */
    private transient final String mItemName;

    /**
     * The name of the parent of this item.
     */
    private transient String mParentName = null;

    /**
     * Whether to put the name attribute on the lowest level node or the parent.
     * TODO: Add a way to pass in an object which determines the XPath.
     */
    private transient final boolean mParentHasName;

    /**
     * The Document containing the information for this list. This
     * document may be changed by property change events.
     */
    private transient Document mDocument = null;

    /**
     * Constructor.
     * 
     * @param aDocument The document containing the information for this list.
     * @param aParentXPath
     *            The XPath of the parent element of this node.
     * @param aElementName
     *            The element node name.
     * @param aItemName
     *            The name of this specific button as stored on the name
     *            attribute.
     * @param aParentHasName
     *            Whether to put the name attribute on the lowest level node or
     *            the parent.
     */
    public DOMButtonModel(Document aDocument, String aParentXPath,
            String aElementName, String aItemName, boolean aParentHasName) {
        super();
        mDocument = aDocument;
        mParentXPath = aParentXPath;
        mElementName = aElementName;
        mItemName = aItemName;
        mParentHasName = aParentHasName;
    }

    /**
     * Internal method to get the button model selected state from the DOM tree.
     * 
     * @return Whether the button is selected.
     */
    @Override
    public boolean isSelected() {
        // Check if the checkbox is in a queryable state.
        if(!isValidState()){
        	return false;
        }
        
        // Perform the query.
        final Node resultNode = DOMUtils.getResultNodeFromQuery(mDocument,
                getXPath());

        // If the node is null it means that there were no results. If a value
        // does not exist this should return false as it is unset.
        if (resultNode == null) {
            Logger.global.log(Level.INFO, Messages
                    .getString("DOMButtonModel.1") + mItemName); //$NON-NLS-1$
            return false;
        }
        // Check the text content value of the node.
        return DOMUtils.isTextContentTrue(resultNode);
    }

    /**
     * Internal method to set whether the button is selected. Need to make sure
     * this is called when the underlying document changes.
     * 
     * @param aArmed
     *            The new state of the button.
     */
    @Override
    public void setArmed(final boolean aArmed) {
        // Ignore the the setArmed when the value is false as this is the user
        // releasing the mouse.
        if (aArmed) {
            // Check if the checkbox is in a queryable state.
            if(!isValidState()){
            	return;
            }
            
            // Perform the query.
            Node resultNode = DOMUtils.getResultNodeFromQuery(mDocument,
                    getXPath());

            boolean previousValue = false;
            // If the node is null it means that there were no results.
            if (resultNode == null) {
                // Create a position in the DOM tree to store the value.
                resultNode = DOMUtils.addNodesForXPath(mDocument, getXPath());
            }
            // Get the text content of the result node.
            else {
                previousValue = DOMUtils.isTextContentTrue(resultNode);
            }
            // Flip the state of the button and store the value in the tree.
            final String nodeValue = previousValue ? "0" : "1"; //$NON-NLS-1$ //$NON-NLS-2$
            
            // Failed to create a node for the model.
            if (resultNode == null) {
                Logger.global.log(Level.SEVERE, Messages
                        .getString("DOMButtonModel.4")); //$NON-NLS-1$
            } 
            
            else {
            	resultNode.setTextContent(nodeValue);
            }
        }
        // Set the underlying button state last as this will notify any
        // listeners.
        super.setArmed(aArmed);
    }

	/**
	 * Returns whether the model is in a queryable state. A model is 
	 * queryable if it has a valid document and if it requires a parent
	 * XPath has one set.
	 * @return Whether the model is in a queryable state.
	 */
	private boolean isValidState() {
		// If there isn't a document return right away. We can't get
        // the state from a DOM that doesn't exist.
        if (mDocument == null) {
            return false;
        }
        
        // If the parent is supposed to have a name and does not yet,
        // immediately return.
        if(mParentHasName && (mParentName == null || mParentName.equals(""))){
        	return false;
        }
        return true;
	}

    /**
     * Function to return the XPath for the node containing the value for this
     * button model.
     * TODO: This is hackfest.
     * 
     * @return The XPath string which will locate the node which contains the
     *         information for this button model.
     */
    private String getXPath() {
        final StringBuilder XPath = new StringBuilder();
        if (mParentXPath != null) {
            XPath.append(mParentXPath);
        }

        if (mParentHasName && mParentName != null && !mParentName.equals("")) {
            XPath.append("[@name='").append(mParentName).append("']"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        XPath.append("/").append(mElementName);
        if (!mParentHasName && mItemName != null && !mItemName.equals("")) {
            XPath.append("[@name='").append(mItemName).append("']"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return XPath.toString();
    }

	/**
	 * Method called when the parent item's state is changed. This
     * should not be called if there is not a parent item.
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 * @param aEvent The event received.
	 */
	public void itemStateChanged(final ItemEvent aEvent) {
		// Ignore unselected events, there should be a selected event
		// that follows.
		if(aEvent.getStateChange() == ItemEvent.SELECTED){
			mParentName = aEvent.getItem().toString();
			// Enable or disable based on whether the parent name is null.
			setEnabled(mParentName != null && !mParentName.equals(""));
		}
	}

    /**
     * Method called when a property change from the parent editor 
     * is received. If this is a document replaced event it will reset
     * the stored document to the new one from the event. It also 
     * enables and disables itself based on whether the document is valid.
     * @param aEvent The property change event.
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(final PropertyChangeEvent aEvent) {
        if(aEvent.getPropertyName().equals("document-replaced")) {
            mDocument = (Document)aEvent.getNewValue();
            // Enable or disable the button model.
            this.setEnabled(mDocument != null);
        }
        else {
            Logger.global.log(Level.WARNING, "Property change listener added for the wrong property change.");
        }
        
    }
}
