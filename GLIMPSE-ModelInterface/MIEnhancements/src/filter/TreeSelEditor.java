package filter;

/**
 * The class to handle editing a node on the filter tree.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

import java.awt.Color;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TreeSelEditor extends DefaultTreeCellEditor
{

    public TreeSelEditor(JTree tree, DefaultTreeCellRenderer selRenderer)
    {
        super(tree, selRenderer);
        setBorderSelectionColor(Color.blue);
    }
}
