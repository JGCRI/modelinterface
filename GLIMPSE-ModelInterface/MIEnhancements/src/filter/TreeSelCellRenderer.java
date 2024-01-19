package filter;

/**
 * The class to handle renderering a node's checkbox on the filter tree pane. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

// Referenced classes of package ui.output:
//            TrNode

class TreeSelCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;
	private JLabel label;
	private TristateCheckBox checkBox;
	private JTextField textField;
	private JPanel panel;
	
	public TreeSelCellRenderer() {
		label = new JLabel();

		checkBox = new TristateCheckBox();
		checkBox.setBackground(UIManager.getColor("Tree.background"));
		checkBox.setBorder(null);

		textField = new JTextField();
		textField.setEditable(false);
		textField.setBackground(UIManager.getColor("Tree.background"));
		textField.setBorder(null);

		panel = new JPanel();
		panel.setOpaque(false);
		panel.add(checkBox, 0);
		panel.add(textField, 1);
		panel.setMaximumSize(new Dimension(300, 18));
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		checkBox.setHalfSelected(false);
		if (node.getUserObject() instanceof TrNode) {
			TrNode trNode = (TrNode) node.getUserObject();
			if (trNode.type.equals("root")){
				label.setBackground(UIManager.getColor("Tree.background"));
				label.setText(node.toString());
				return label;
			}else if (trNode.isPartialSelectedForParent() && !node.isLeaf()){
				checkBox.setSelected(false);
				checkBox.setHalfSelected(true);
			} else if (selected) {
				checkBox.setHalfSelected(false);
				if (trNode.isSelected()) {
					checkBox.setSelected(true);
				} else
					checkBox.setSelected(false);
			} else if (!selected) {
				if (!trNode.isSelected())
					checkBox.setSelected(false);
				else
					checkBox.setSelected(true);
			} else {
				if (selected)
					checkBox.setSelected(true);
				else
					checkBox.setSelected(false);
			}
			
			textField.setText(trNode.nodeName);
			return panel;
		} else {
			label.setBackground(UIManager.getColor("Tree.background"));
			label.setText(node.toString());
			return label;
		}
	}

}
