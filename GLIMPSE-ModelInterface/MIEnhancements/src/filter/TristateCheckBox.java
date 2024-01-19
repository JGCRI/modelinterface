package filter;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

import chart.LegendUtil;

/**
 * The class to handle the status change of a tree (select/unselect/partial select) ont a branch.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class TristateCheckBox extends JCheckBox {

	private static final long serialVersionUID = 1L;
	private boolean halfState;
	private static Icon halfselected = new ImageIcon(LegendUtil.getBufferedImage(Color.white, Color.black, 10));
	private static Icon unselected = new ImageIcon(LegendUtil.getBufferedImage(Color.white, Color.black));

	@Override
	public void paint(Graphics g) {
		if (isSelected()) {
			halfState = false;
		}
		setIcon(halfState ? halfselected : isSelected() ? super.getSelectedIcon() : unselected);
		super.paint(g);
	}

	public boolean isHalfSelected() {
		return halfState;
	}

	public void setHalfSelected(boolean halfState) {
		this.halfState = halfState;
		if (halfState) {
			setSelected(false);
			repaint();
		}
	}

}