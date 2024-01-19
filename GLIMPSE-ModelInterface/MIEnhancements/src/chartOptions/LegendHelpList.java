package chartOptions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import chart.LegendUtil;

/**
 * The class to handle showing supported legend lists
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class LegendHelpList extends JDialog {
	private static final long serialVersionUID = 1L;

	public LegendHelpList(String type) {
		String[] legendList = null;
		if (type.trim().equals("patternList"))
			legendList = LegendUtil.patternList.clone();
		else if (type.trim().equals("strokeList"))
			legendList = LegendUtil.strokeList.clone();

		BufferedImage[] image = new BufferedImage[legendList.length];
		for (int i = 0; i < legendList.length; i++)
			if (type.trim().equals("patternList"))
				image[i] = (BufferedImage) LegendUtil
						.getTexturePaint(Color.green, Color.darkGray, Integer.valueOf(legendList[i]).intValue(), 0)
						.getImage();
			else if (type.trim().equals("strokeList"))
				image[i] = (BufferedImage) LegendUtil
						.getTexturePaint(Color.green, Color.darkGray, 11, Integer.valueOf(legendList[i]).intValue())
						.getImage();

		JPanel jp = new JPanel(new GridLayout(legendList.length, 1));
		jp.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 5));

		for (int i = 0; i < legendList.length; i++)
			jp.add(new JLabel(legendList[i], new ImageIcon(image[i].getScaledInstance(120, 20, Image.SCALE_SMOOTH)),
					SwingConstants.LEFT));

		add(jp);
		setTitle("Legend Help for " + type);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setSize(new Dimension(220, 220));
		setResizable(false);
		setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new LegendHelpList("strokeList");
			}
		});
	}
}
