package listener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * The class handles Popup event. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class JPopupMenuShower extends MouseAdapter
{

    public JPopupMenuShower(JPopupMenu popup)
    {
        this.popup = popup;
        popup.setVisible(false);
       
    }

    private void showIfPopupTrigger(MouseEvent mouseEvent)
    {
        if(mouseEvent.isPopupTrigger())
            popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        else
            popup.setVisible(false);
    }

    public void mousePressed(MouseEvent mouseEvent)
    {
        showIfPopupTrigger(mouseEvent);
    }

    public void mouseReleased(MouseEvent mouseEvent)
    {
        showIfPopupTrigger(mouseEvent);
    }

    private JPopupMenu popup;
}
