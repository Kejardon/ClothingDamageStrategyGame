/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

public class GameInstance
{
	protected Leader p1Leader;
	protected Leader p2Leader;
	protected GameMap myMap;
	protected int playerTurn=1;
	protected AI AIThread;
	
	//for now assuming p1 is human and p2 is computer
	protected boolean[] isComputer = new boolean[2];
	{
		isComputer[0]=false;
		isComputer[1]=true;
	}
	public GameInstance()
	{
	}
	public void setLeaders(Leader l1, Leader l2)
	{
		p1Leader=l1;
		p2Leader=l2;
	}
	public void setMap(GameMap map)
	{
		myMap=map;
	}
	public void endTurn(MapScreen screen)
	{
		playerTurn=playerTurn^3;
		for(int i=myMap.width-1;i>=0;i--)
		{
			for(int j=myMap.height-1;j>=0;j--)
			{
				Piece unit=myMap.getPieceAt(i, j);
				if(unit!=null && unit.player()==playerTurn)
				{
					unit.rejuv(this);
				}
			}
		}
		if(isComputer[playerTurn-1])
		{
			AIThread=new AI(screen, 30*1000);
			AIThread.start();
		}
		else
			AIThread=null;
	}
}
