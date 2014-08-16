/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

/*
 * Iterator class/utility for square-shaped grids
 */
public final class SquareIterator
{
	public final int x;
	public final int y;
	public final int min;
	public final int max;
	protected int currentX;
	protected int currentY;
	protected boolean started=false;
	/*
	 * min MUST be <= max or behavior is undefined.
	 */
	public SquareIterator(int x, int y, int min, int max)
	{
		this.x=x;
		this.y=y;
		this.min=min;
		this.max=max;
	}
	public boolean next()
	{
		if(!started)
		{
			started=true;
			currentX=x-max;
			currentY=y-max;
			return true;
		}
		currentY++;
		if(y>=currentY && (y-currentY) < min)
		{
			if(Math.abs(currentX-x) < min)
				currentY = y+min;
			return true;
		}
		if(currentY - y > max)
		{
			currentX++;
			if(currentX>x+max)
			{
				started=false;
				return false;
			}
			currentY=y-max;
		}
		return true;
	}
	public int cX(){return currentX;}
	public int cY(){return currentY;}
}
