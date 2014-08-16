/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

package JExtensions;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class MyButton extends JLabel implements MouseListener
{
	public MyButton(ImageIcon I)
	{
		super(I);
	}
	{
		addMouseListener(this);
	}
	
	public void mouseClicked(MouseEvent e) {
		if(e.getButton()==MouseEvent.BUTTON1)
			doAction(e);
	}
	public abstract void doAction(MouseEvent e);

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	
}
