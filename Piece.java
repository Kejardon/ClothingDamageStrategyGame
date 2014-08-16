/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.Image;
import java.util.HashMap;

public abstract class Piece
{
	public abstract Image baseGraphic();
	public abstract Image graphicFor(Leader leader);
	public abstract String name();
	public abstract PieceType pieceType(); //While this exists, piece data should be called through piece.
	public abstract SubType subType();
	public abstract int player();
	
	public abstract int health();
	public abstract int energy();
	public abstract int actions();
	public abstract int x();
	public abstract int y();
	
	public abstract int maxHealth();
	public abstract int maxEnergy();
	public abstract int maxActions();
	public abstract int moveRange();
	public abstract int attackMinRange();
	public abstract int attackMaxRange();
	public abstract int attackDamage();

	public abstract void rejuv(GameInstance game);
	public abstract void takeDamage(GameInstance game, int i);
	public abstract void setPosition(GameInstance game, int x, int y);
	public abstract void didDamage(GameInstance game);
	public abstract void takeHeal(GameInstance game, int i);
	public abstract void useAction(GameInstance game, int i);
	public abstract void useEnergy(GameInstance game, int i);
	public abstract Piece copy();
	public abstract DefaultPiece rootPiece();
	
	public static HashMap<String, Class<AffectedPiece>> affectTypes=new HashMap();
	public static boolean hasEffect(Piece P, Class<AffectedPiece> effect)
	{
		while(P instanceof AffectedPiece)
		{
			if(effect.isInstance(P)) return true;
			P=((AffectedPiece)P).heldPiece;
		}
		return false;
	}
	public static Piece unaffectedPiece(Piece P)
	{
		while(P instanceof AffectedPiece)
			P=((AffectedPiece)P).heldPiece;
		return P;
	}
	
	public static abstract class AffectedPiece extends Piece implements Cloneable
	{
		public Piece heldPiece;
		public abstract String affectName();
		public AffectedPiece(Piece p)
		{
			heldPiece=p;
		}
		public void endEffect(GameMap map)
		{
			AffectedPiece parent=(AffectedPiece)map.getPieceAt(heldPiece.x(), heldPiece.y());
			if(parent==this)
				map.movePiece(heldPiece, 0, 0, heldPiece.x(), heldPiece.y());
			else
			{
				while(parent.heldPiece!=this)
					parent=(AffectedPiece)parent.heldPiece;
				parent.heldPiece=heldPiece;
			}
		}
		public Image baseGraphic(){return heldPiece.baseGraphic();}
		public Image graphicFor(Leader leader){return heldPiece.graphicFor(leader);}
		public String name(){return heldPiece.name();}
		public PieceType pieceType(){return heldPiece.pieceType();}
		public SubType subType(){return heldPiece.subType();}
		public int player(){return heldPiece.player();}

		public int health(){return heldPiece.health();}
		public int energy(){return heldPiece.energy();}
		public int actions(){return heldPiece.actions();}
		public int x(){return heldPiece.x();}
		public int y(){return heldPiece.y();}

		public int maxHealth(){return heldPiece.maxHealth();}
		public int maxEnergy(){return heldPiece.maxEnergy();}
		public int maxActions(){return heldPiece.maxActions();}
		public int moveRange(){return heldPiece.moveRange();}
		public int attackMinRange(){return heldPiece.attackMinRange();}
		public int attackMaxRange(){return heldPiece.attackMaxRange();}
		public int attackDamage(){return heldPiece.attackDamage();}

		public void rejuv(GameInstance game){heldPiece.rejuv(game);}
		public void takeDamage(GameInstance game, int i){heldPiece.takeDamage(game, i);}
		public void setPosition(GameInstance game, int x, int y){heldPiece.setPosition(game, x, y);}
		public void didDamage(GameInstance game){heldPiece.didDamage(game);}
		public void takeHeal(GameInstance game, int i){heldPiece.takeHeal(game, i);}
		public void useAction(GameInstance game, int i){heldPiece.useAction(game, i);}
		public void useEnergy(GameInstance game, int i){heldPiece.useEnergy(game, i);}
		
		public int hashCode()
		{
			return heldPiece.hashCode()^affectName().hashCode();
		}
		public boolean equals(Object O)
		{
			if(!(O instanceof AffectedPiece)) return false;
			AffectedPiece other=(AffectedPiece)O;
			return affectName()==other.affectName() && heldPiece.equals(other.heldPiece);
			//Strings will == if equals because of how affectName is used.
		}
		public Piece copy()
		{
			try{
				AffectedPiece copy=(AffectedPiece)this.clone();
				copy.heldPiece=heldPiece.copy();
				return copy;
			}catch(CloneNotSupportedException e){}
			return null;
		}
		public DefaultPiece rootPiece()
		{
			return heldPiece.rootPiece();
		}
	}
	
	public static class DefaultPiece extends Piece implements Cloneable
	{
		public final Image baseGraphic;
		public final String name;
		public final PieceType pieceType;
		public final SubType subType;
		public final int player;
		protected int health;
		protected int energy;
		protected int actions;
		protected int x;
		protected int y;

		public DefaultPiece(String n, Image base, PieceType pT, SubType sT, int p)
		{
			if(n==null)
				name=sT.name+" "+pT.baseName;
			else
				name=n;
			if(base==null)
				baseGraphic=pT.baseGraphic;
			else
				baseGraphic=base;
			pieceType=pT;
			subType=sT;
			player=p;
			init();
		}
		protected void init()
		{
			health=maxHealth();
			energy=maxEnergy();
			actions=maxActions();
		}
		public Image baseGraphic(){return baseGraphic;}
		public Image graphicFor(Leader leader)
		{
			if(leader==null) return baseGraphic();
			Image image=leader.unitSpriteImage(pieceType);
			if(image==null) return baseGraphic();
			return image;
		}
		public String name(){return name;}
		public PieceType pieceType(){return pieceType;}
		public SubType subType(){return subType;}
		public int player(){return player;}
		public int health(){return health;}
		public int energy(){return energy;}
		public int actions(){return actions;}
		public int x(){return x;}
		public int y(){return y;}
		public int maxHealth(){return pieceType.maxHealth;}
		public int maxEnergy(){return pieceType.maxEnergy;}
		public int maxActions(){return pieceType.maxActions;}
		public int moveRange(){return pieceType.moveRange;}
		public int attackMinRange(){return pieceType.attackMinRange;}
		public int attackMaxRange(){return pieceType.attackMaxRange;}
		public int attackDamage(){return pieceType.attackDamage;}
		public void rejuv(GameInstance game)
		{
			actions=game.myMap.getPieceAt(x, y).maxActions(); //In case an effect changes max.
		}
		public void takeDamage(GameInstance game, int i)
		{
			if(i<1) return;
			health-=i;
			if(health<0)
				health=0;
		}
		public void setPosition(GameInstance game, int x, int y)
		{
			this.x=x;
			this.y=y;
		}
		public void didDamage(GameInstance game){}
		public void takeHeal(GameInstance game, int i)
		{
			health+=i;
			int max=game.myMap.getPieceAt(x, y).maxHealth();
			if(health > max)
				health=max;
		}
		public void useAction(GameInstance game, int i)
		{
			actions-=i;
			if(actions < 0)
				actions=0;
		}
		public void useEnergy(GameInstance game, int i)
		{
			energy-=i;
			if(energy < 0)
				energy=0;
		}

		public int hashCode()
		{
			int hashCode=pieceType.hashCode();
			if(subType!=null)
				hashCode^=subType.hashCode()>>1;
			hashCode^=player>>2;
			hashCode^=health>>4;
			hashCode^=energy>>8;
			hashCode^=actions>>12;
			hashCode^=x>>16;
			hashCode^=y>>20;
			return hashCode;
		}
		public boolean equals(Object O)
		{
			if(!(O instanceof DefaultPiece)) return false;
			DefaultPiece other=(DefaultPiece)O;
			return player==other.player && health==other.health
					&& energy==other.energy && actions==other.actions
					&& x==other.x && y==other.y;
		}
		public Piece copy()
		{
			try{ return (Piece)this.clone(); }
			catch(Exception e){}
			return null;
		}
		public DefaultPiece rootPiece(){return this;}
	}
	public static class LeaderPiece extends DefaultPiece
	{
		public final int trait1;
		public final int trait2;
		
		public static final int Trait_None=0;
		public static final int Trait_Health=1;
		public static final int Trait_Energy=2;
		public static final int Trait_Actions=3;
		public static final int Trait_Move=4;
		public static final int Trait_Damage=5;
		public static final int Trait_SpecialDamage=6;

		public LeaderPiece(String n, Image base, int p, int t1, int t2)
		{
			super(n, base, PieceType.Leader, null, p);
			trait1=t1;
			trait2=t2;
			init();
		}
		public int maxHealth()
		{
			int mH=super.maxHealth();
			if(trait1==Trait_Health || trait2==Trait_Health)
				mH+=2;
			return mH;
		}
		public int maxEnergy()
		{
			int mE=super.maxEnergy();
			if(trait1==Trait_Energy || trait2==Trait_Energy)
				mE+=4;
			return mE;
		}
		public int maxActions()
		{
			int mA=super.maxActions();
			if(trait1==Trait_Actions || trait2==Trait_Actions)
				mA+=1;
			return mA;
		}
		public int moveRange()
		{
			int mR=super.moveRange();
			if(trait1==Trait_Move || trait2==Trait_Move)
				mR+=1;
			return mR;
		}
		public int attackDamage()
		{
			int aD=super.attackDamage();
			if(trait1==Trait_Damage || trait2==Trait_Damage)
				aD+=1;
			return aD;
		}
		public int hashCode()
		{
			return super.hashCode()^(trait1>>24)^(trait2>>28);
		}
		public boolean equals(Object O)
		{
			if(!(O instanceof LeaderPiece)) return false;
			LeaderPiece other=(LeaderPiece)O;
			return super.equals(other) && trait1==other.trait1 && trait2==other.trait2;
		}
	}
	public static Piece newPiece(String n, Image base, PieceType pT, SubType sT, int p)
	{
		return new DefaultPiece(n, base, pT, sT, p);
	}
}
