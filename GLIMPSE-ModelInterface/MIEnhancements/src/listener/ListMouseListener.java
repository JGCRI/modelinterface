package listener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;

/**
 * The class handles clear list selections. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class ListMouseListener extends MouseAdapter {

    JList<String> list;
    public ListMouseListener(JList<String> list)
    {
        this.list = list;
    }

    public void mousePressed(MouseEvent e)
    {
        if(e.getClickCount() == 2)
        {
            list.getSelectionModel().setValueIsAdjusting(true);
            list.clearSelection();
            list.updateUI();
        }
    }

}
