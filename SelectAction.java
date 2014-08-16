
import java.awt.Point;
import java.util.ArrayList;

/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

public abstract class SelectAction
{
	public abstract void doAction(MapScreen here, Piece forPiece, Point target);
	public abstract Point[] validSpots(MapScreen here, Piece forPiece);

	public static boolean normalChecks(Piece piece, GameInstance myGame)
	{
			if(myGame.playerTurn!=piece.player())
				return false;
			if(piece.actions()<1)
				return false;
			return true;
	}
	public static final SelectAction Move=new SelectAction(){
		public Point[] validSpots(MapScreen here, Piece piece)
		{
			GameInstance game = here.myGame;
			ArrayList<Point> options=new ArrayList();
			options.add(new Point(piece.x(), piece.y()));
			for(int i=piece.moveRange();i>0;i--)
			{
				for(int j=options.size()-1;j>=0;j--)
				{
					Point point=options.get(j);
					Point next=new Point(point.x+1, point.y);
					if(!options.contains(next) && MechanicsLibrary.mayPositionAt(game, piece, next.x, next.y))
						options.add(next);
					next=new Point(point.x, point.y+1);
					if(!options.contains(next) && MechanicsLibrary.mayPositionAt(game, piece, next.x, next.y))
						options.add(next);
					next=new Point(point.x-1, point.y);
					if(!options.contains(next) && MechanicsLibrary.mayPositionAt(game, piece, next.x, next.y))
						options.add(next);
					next=new Point(point.x, point.y-1);
					if(!options.contains(next) && MechanicsLibrary.mayPositionAt(game, piece, next.x, next.y))
						options.add(next);
				}
			}
			options.remove(0);
			return options.toArray(new Point[options.size()]);
		}
		public void doAction(MapScreen here, Piece piece, Point target)
		{
			if(!normalChecks(piece, here.myGame))
				return;
			if(here.myGame.myMap.getPieceAt(target.x, target.y)!=null)
				return;
			
			piece.useAction(here.myGame, 1);
			here.myGame.myMap.movePiece(piece, piece.x(), piece.y(), target.x, target.y);
		}
	};
	public static final SelectAction Attack=new SelectAction(){
		public Point[] validSpots(MapScreen here, Piece piece)
		{
			GameMap map=here.myGame.myMap;
			ArrayList<Point> options=new ArrayList();
			if(piece.attackMinRange()<=piece.attackMaxRange())
			{
				DiamondIterator iter=new DiamondIterator(piece.x(),piece.y(),piece.attackMinRange(),piece.attackMaxRange());
				while(iter.next())
				{
					if(!map.okSpot(iter.cX(), iter.cY())) continue;
					Piece target=map.getPieceAt(iter.cX(), iter.cY());
					if(target==null || target.player()==piece.player())
						continue;
					options.add(new Point(iter.cX(), iter.cY()));
				}
			}
			return options.toArray(new Point[options.size()]);
		}
		public void doAction(MapScreen here, Piece piece, Point targetPoint)
		{
			if(!normalChecks(piece, here.myGame))
				return;
			Piece target=here.myGame.myMap.getPieceAt(targetPoint.x, targetPoint.y);
			if(target==null)
				return;
			
			piece.useAction(here.myGame, 1);
			MechanicsLibrary.doAttack(here.myGame, piece, target);
		}
	};
	public static final SelectAction Special=new SelectAction(){
		public Point[] validSpots(MapScreen here, Piece piece)
		{
			return piece.pieceType().special.validSpaces(here.myGame, piece, piece.x(), piece.y());
		}
		public void doAction(MapScreen here, Piece piece, Point target)
		{
			if(!normalChecks(piece, here.myGame))
				return;
			if(piece.energy()<1) return;
			
			piece.useAction(here.myGame, 1);
			piece.useEnergy(here.myGame, 1);
			piece.pieceType().special.doEffect(here.myGame, piece, piece.x(), piece.y(), target);
		}
	};
}
