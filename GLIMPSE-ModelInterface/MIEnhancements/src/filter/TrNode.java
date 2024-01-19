package filter;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The class to handle a node on the filter tree pane. It carrys parent node value 
 * as key String, as well partial selection flag.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

class TrNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;
	protected String nodeName;
	protected String type;
	protected String keyStr;
	protected boolean isSelected;
	protected boolean isPartialSelectedForParent = false;
	protected DefaultMutableTreeNode topNode;

	protected TrNode(String nodename, String type, boolean isSelected, DefaultMutableTreeNode topNode) {
		keyStr = "";
		nodeName = nodename;
		this.type = type;
		this.isSelected = isSelected;
		this.topNode = topNode == null ? null : topNode;
		setKeyStr();
	}

	public boolean isPartialSelectedForParent() {
		return isPartialSelectedForParent;
	}

	public void setPartialSelectedForParent(boolean isPartialSelectedForParent) {
		this.isPartialSelectedForParent = isPartialSelectedForParent;
	}

	public String toString() {
		return nodeName;
	}

	protected void setKeyStr() {
		if (topNode != null && !topNode.toString().equals("Heading")) {
			if (!type.trim().equals("value")) {
				if (topNode.toString().trim().equals("Filter All"))
					keyStr = nodeName;
				else
					keyStr = ((TrNode) topNode.getUserObject()).keyStr.trim() + "|" + nodeName.trim();
			} else
				keyStr = ((TrNode) topNode.getUserObject()).keyStr.trim();
		} else
			keyStr = " ";
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public boolean isSelected() {
		return isSelected;
	}

}
