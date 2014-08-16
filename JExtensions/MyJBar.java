package JExtensions;

/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.*;
import javax.swing.Box;
import javax.swing.JComponent;

public class MyJBar extends EnlargingComponent
{
	public final Color FGColor;
	public final Color BGColor;
	protected int current=0;
	protected int max=1;
	protected int defaultHeight=14;
	public MyJBar(Color FG, Color BG)
	{
		FGColor=FG;
		BGColor=BG;
	}
	public MyJBar(Color FG, Color BG, int current, int max)
	{
		FGColor=FG;
		BGColor=BG;
		this.current=current;
		this.max=max;
	}
	public void setDefaultHeight(int i){defaultHeight=i;}
	public void setCurrent(int i){current=i; repaint();}
	public void setValues(int c, int m){current=c; max=m; repaint();}
	public void paintComponent(Graphics g)
	{
		int x=getWidth();
		int y=getHeight();
		int partX=x*current/max;
		
		if(partX<x)
		{
			g.setColor(BGColor);
			g.fillRect(partX, 0, x-partX, y);
		}
		if(partX>0)
		{
			g.setColor(FGColor);
			g.fillRect(0, 0, partX, y);
		}
	}
	protected Dimension getPreferredSize(Dimension parentDimension)
	{
		parentDimension.height = defaultHeight;
		parentDimension.width -= 16;
		return parentDimension;
	}
	public Dimension getMaximumSize()
	{
		return getPreferredSize(super.getMaximumSize());
	}
}
