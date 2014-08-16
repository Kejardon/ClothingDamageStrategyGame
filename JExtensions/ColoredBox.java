/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

package JExtensions;

import java.awt.*;
import javax.swing.Box;

public class ColoredBox extends Box
{
	public ColoredBox(int i){super(i);}
	public void paintComponent(Graphics g)
	{
		Color color=getBackground();
		if(color!=null)
		{
			g.setColor(color);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
	}
}
