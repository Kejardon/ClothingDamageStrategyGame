

/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

//Dedicated class for handling and standardizing game mechanics.
public class MechanicsLibrary
{
	public static final boolean mayPositionAt(GameInstance game, Piece piece, int x, int y)
	{
		//TODO: Terrain check
		if(!game.myMap.okSpot(x, y) || game.myMap.getPieceAt(x, y)!=null)
			return false;
		return true;
	}
	public static final boolean mayEverPositionAt(GameInstance game, Piece piece, int x, int y)
	{
		//TODO: Terrain check
		if(!game.myMap.okSpot(x, y))
			return false;
		return true;
	}
	public static final boolean mayFight(Piece A, Piece B)
	{
		return (A.player()!=0 && B.player()!=0 && A.player()!=B.player());
	}
	//A may be null to indicate a sourceless attack of some kind
	public static final void doDamage(GameInstance game, Piece A, Piece B, int damage)
	{
		B.takeDamage(game, damage);
		if(B.health()==0)
		{
			//kill B
			game.myMap.removePiece(B, B.x(), B.y());
		}
	}
	public static final void doAttack(GameInstance game, Piece A, Piece B)
	{
		int damage=A.attackDamage();
		damage+=subTypeBonus(game, A, B);
		doDamage(game, A, B, damage);
		A.didDamage(game);
	}
	public static final int subTypeBonus(GameInstance game, Piece A, Piece B)
	{
			SubType targetType=B.subType();
			SubType sourceType=A.subType();
			if(targetType!=null && sourceType!=null)
				return sourceType.DamageBonus(targetType);
			return 0;
	}
	public static final void endTurn(MapScreen screen)
	{
		screen.selectUnit(null);
		screen.setAction(null);
		screen.myGame.endTurn(screen);
		screen.repaint();
	}
}
