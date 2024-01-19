package chartOptions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * The class to handle showing help on legend modification
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class LegendHelpButton extends JButton {

	private static final long serialVersionUID = 1L;

	public LegendHelpButton(String name, String[] s){
		super();;
		this.setName(name);
		this.setText("?");
		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() > 0)
					new LegendHelpList(name.trim());
			}
		};
		this.addMouseListener(ml);
		setContentAreaFilled(false);
		setFocusable(false);
		setBorder(BorderFactory.createEtchedBorder());
		addMouseListener(buttonMouseListener);
		setRolloverEnabled(true);
		this.setPreferredSize(new Dimension(10, 10));
	}

	protected static final MouseListener buttonMouseListener = new MouseAdapter() {

		public void mouseEntered(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(true);
			}
		}

		public void mouseExited(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}

	};
	
}
