/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.Point;
import java.util.ArrayList;

public abstract class Special
{
	public abstract Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY);
	public abstract void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere);
	public final boolean dynamic;
	public final String name;
	public final String description;
	public String name(Piece unit){return name;}
	public String description(Piece unit){return description;}
	
	protected final void checkAddMoveSpace(GameInstance game, Piece myPiece, int pieceX, int pieceY, ArrayList<Point> options)
	{
		if(MechanicsLibrary.mayPositionAt(game, myPiece, pieceX, pieceY))
			options.add(new Point(pieceX, pieceY));
	}
	protected final void checkAddEnemySpace(GameMap map, Piece myPiece, int pieceX, int pieceY, ArrayList<Point> options)
	{
		if(!map.okSpot(pieceX, pieceY)) return;
		Piece otherPiece=map.getPieceAt(pieceX, pieceY);
		if(otherPiece!=null && MechanicsLibrary.mayFight(myPiece, otherPiece))
			options.add(new Point(pieceX, pieceY));
	}
	public Special(String n, String d) { name=n; description=d; dynamic=false; }
	public Special(String n, String des, boolean dyn) { name=n; description=des; dynamic=dyn; }
	
	public static final Special Jump=new Special(
			"Jump",
			"Jump 2 squares (ignores other units and obstacles)")
	{
		public Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY)
		{
			ArrayList<Point> options=new ArrayList();
			DiamondIterator iter=new DiamondIterator(pieceX, pieceY, 2, 2);
			while(iter.next())
			{
				checkAddMoveSpace(game, myPiece, iter.cX(), iter.cY(), options);
			}
			return options.toArray(new Point[options.size()]);
		}
		public void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere)
		{
			game.myMap.movePiece(myPiece, pieceX, pieceY, toHere.x, toHere.y);
		}
	};
	public static final Special Block=new Special(
			"Block",
			"Reduces all damage by 2, until next turn")
	{
		class BlockingPiece extends Piece.AffectedPiece
		{
			public BlockingPiece(Piece p){super(p);}
			public void takeDamage(GameInstance game, int i){heldPiece.takeDamage(game, i-2);}
			public void rejuv(GameInstance game)
			{
				endEffect(game.myMap);
				heldPiece.rejuv(game);
			}
			public String affectName(){return name;}
		}
		{Piece.affectTypes.put("BlockingPiece",(Class)BlockingPiece.class);}
		public Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY)
		{
			while(myPiece instanceof Piece.AffectedPiece)
			{
				if(myPiece instanceof BlockingPiece)
					return new Point[0];
				myPiece=((Piece.AffectedPiece)myPiece).heldPiece;
			}
			return new Point[]{new Point(pieceX, pieceY)};
		}
		public void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere)
		{
			game.myMap.movePiece(new BlockingPiece(myPiece), 0, 0, myPiece.x(), myPiece.y());
		}
	};
	public static final Special Aim=new Special(
			"Aim",
			"+2 range to next attack")
	{
		class AimingPiece extends Piece.AffectedPiece
		{
			public AimingPiece(Piece p){super(p);}
			//public int attackDamage(){return heldPiece.attackDamage()+4;}
			public int attackMaxRange(){return heldPiece.attackMaxRange()+2;}
			public void didDamage(GameInstance game)
			{
				endEffect(game.myMap);
				heldPiece.didDamage(game);
			}
			public String affectName(){return name;}
		}
		{Piece.affectTypes.put("AimingPiece",(Class)AimingPiece.class);}
		public Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY)
		{
			while(myPiece instanceof Piece.AffectedPiece)
			{
				if(myPiece instanceof AimingPiece)
					return new Point[0];
				myPiece=((Piece.AffectedPiece)myPiece).heldPiece;
			}
			return new Point[]{new Point(pieceX, pieceY)};
		}
		public void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere)
		{
			game.myMap.movePiece(new AimingPiece(myPiece), 0, 0, myPiece.x(), myPiece.y());
		}
	};
	public static final Special Blast=new Special(
			"Blast",
			"1 damage to 3x3 square<br>Range 1-3")
	{
		public Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY)
		{
			GameMap map=game.myMap;
			ArrayList<Point> options=new ArrayList();
			DiamondIterator iter=new DiamondIterator(pieceX, pieceY, 1, 3);
			while(iter.next())
			{
				if(!map.okSpot(iter.cX(), iter.cY())) continue;
				//Check for actual enemies in range
				SquareIterator iter2=new SquareIterator(iter.cX(), iter.cY(), 0, 1);
				while(iter2.next())
				{
					if(!map.okSpot(iter2.cX(), iter2.cY())) continue;
					Piece targetPiece=map.getPieceAt(iter2.cX(), iter2.cY());
					if(targetPiece!=null && MechanicsLibrary.mayFight(myPiece, targetPiece))
					{
						options.add(new Point(iter.cX(), iter.cY()));
						break;
					}
				}
			}
			return options.toArray(new Point[options.size()]);
		}
		public void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere)
		{
			GameMap map=game.myMap;
			for(int i=toHere.x-1;i<=toHere.x+1;i++)
			{
				for(int j=toHere.y-1;j<=toHere.y+1;j++)
				{
					Piece target=map.getPieceAt(i, j);
					if(target!=null)
						MechanicsLibrary.doDamage(game, myPiece, target, 1+MechanicsLibrary.subTypeBonus(game, myPiece, target));
				}
			}
		}
	};
	public static final Special Heal=new Special(
			"Heal",
			"Target recieves +4 health<br>Range 0-1")
	{
		public Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY)
		{
			GameMap map=game.myMap;
			ArrayList<Point> options=new ArrayList();
			DiamondIterator iter=new DiamondIterator(pieceX, pieceY, 0, 1);
			while(iter.next())
			{
				if(!map.okSpot(iter.cX(), iter.cY())) continue;
				Piece target=map.getPieceAt(iter.cX(), iter.cY());
				if(target!=null && target.health()<target.maxHealth() && target.player()==myPiece.player())
					options.add(new Point(iter.cX(), iter.cY()));
			}
			return options.toArray(new Point[options.size()]);
		}
		public void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere)
		{
			Piece target=game.myMap.getPieceAt(toHere.x, toHere.y);
			if(target!=null)
				target.takeHeal(game, 4);
		}
	};
	public static final Special Shot=new Special(
			"Shot",
			null,
			true)
	{
		public String description(Piece unit)
		{
			if(unit instanceof Piece.LeaderPiece)
			{
				Piece.LeaderPiece leader=(Piece.LeaderPiece)unit;
				if(leader.trait1 == Piece.LeaderPiece.Trait_SpecialDamage || leader.trait2 == Piece.LeaderPiece.Trait_SpecialDamage)
					return "3 damage attack<br>Range 2-3";
			}
			return "2 damage attack<br>Range 2-3";
		}
		public Point[] validSpaces(GameInstance game, Piece myPiece, int pieceX, int pieceY)
		{
			GameMap map=game.myMap;
			ArrayList<Point> options=new ArrayList();
			DiamondIterator iter=new DiamondIterator(pieceX, pieceY, 2, 3);
			while(iter.next())
			{
				checkAddEnemySpace(map, myPiece, iter.cX(), iter.cY(), options);
			}
			return options.toArray(new Point[options.size()]);
		}
		public void doEffect(GameInstance game, Piece myPiece, int pieceX, int pieceY, Point toHere)
		{
			Piece target=game.myMap.getPieceAt(toHere.x, toHere.y);
			int damage=2;
			if(myPiece instanceof Piece.LeaderPiece)
			{
				Piece.LeaderPiece leader=(Piece.LeaderPiece)myPiece;
				if(leader.trait1==Piece.LeaderPiece.Trait_SpecialDamage || leader.trait2==Piece.LeaderPiece.Trait_SpecialDamage)
					damage+=1;
			}
			if(target!=null)
				MechanicsLibrary.doDamage(game, myPiece, target, damage);
		}
	};
}
