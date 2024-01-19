package listener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JList;

/**
 * The class handles finding match sequence selection. 
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class ListKeyListener implements KeyListener {

    private JList<String> jl;
    
    public ListKeyListener(JList<String> Jl)
    {
        this.jl = Jl;
    }

    public void keyTyped(KeyEvent e)
    {
        String s = "";
        char c = e.getKeyChar();
        s = (new StringBuilder(String.valueOf(s))).append(c).toString();
        int i = jl.getNextMatch(s, 0, javax.swing.text.Position.Bias.Forward);
        jl.setSelectedIndex(i);
        jl.setFocusCycleRoot(true);
        jl.setOpaque(true);
    }

    public void keyPressed(KeyEvent keyevent)
    {
    }

    public void keyReleased(KeyEvent keyevent)
    {
    }

}
