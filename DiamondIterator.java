/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

/*
 * Iterator class/utility for diamond-shaped grids
 */
public final class DiamondIterator
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
	public DiamondIterator(int x, int y, int min, int max)
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
			currentY=y;
			return true;
		}
		currentY++;
		int xComponent=Math.abs(currentX-x);
		int yComponent=Math.abs(currentY-y);
		if(yComponent + xComponent < min)
		{
			currentY++;
			//There is a formula to directly calculate this but meh.
			while(Math.abs(currentY-y) + xComponent < min)
				currentY++;
			return true;
		}
		if(yComponent + xComponent > max)
		{
			currentX++;
			if(currentX>x+max)
			{
				started=false;
				return false;
			}
			currentY=y-(max-Math.abs(x-currentX));
		}
		return true;
	}
	public int cX(){return currentX;}
	public int cY(){return currentY;}
}
