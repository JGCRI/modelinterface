package filter;

import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * The class to handle utility functions for filter package.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class TreeUtil {
	public static TreePath find(JTree tree, Object nodes[]) {
		TreeNode root = (TreeNode) tree.getModel().getRoot();
		return find2(tree, new TreePath(root), nodes, 0, false);
	}

	public static TreePath findByName(JTree tree, String names[]) {
		TreeNode root = (TreeNode) tree.getModel().getRoot();
		return find2(tree, new TreePath(root), names, 0, true);
	}

	private static TreePath find2(JTree tree, TreePath parent, Object nodes[], int depth, boolean byName) {
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		Object o = node;
		if (byName)
			o = o.toString();
		if (o.equals(nodes[depth])) {
			if (depth == nodes.length - 1)
				return parent;
			if (node.getChildCount() >= 0) {
				for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
					TreeNode n = (TreeNode) e.nextElement();
					TreePath path = parent.pathByAddingChild(n);
					TreePath result = find2(tree, path, nodes, depth + 1, byName);
					if (result != null)
						return result;
				}

			}
		}
		return null;
	}

}
