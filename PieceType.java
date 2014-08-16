/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PieceType
{
	public final Image baseGraphic;
	public final String baseName;
	public final int maxHealth;
	public final int maxEnergy;
	public final int maxActions;
	public final int moveRange;
	public final int attackMinRange;
	public final int attackMaxRange;
	public final int attackDamage;
	public final Special special;
	
	protected static BufferedImage sprites=Main.loadImageFile("Sprites.png");
	public static final PieceType Charger=new PieceType(
				"Charger",
				sprites.getSubimage(0, 4, 46, 68),
				2, 2, 3,
				2,
				1, 1, 2,
				Special.Jump);
	public static final PieceType Bruiser=new PieceType(
				"Bruiser",
				sprites.getSubimage(46, 10, 46, 62),
				5, 3, 2,
				2,
				1, 1, 3,
				Special.Block);
	public static final PieceType Marksman=new PieceType(
				"Marksman",
				sprites.getSubimage(92, 10, 46, 62),
				3, 2, 2,
				1,
				2, 3, 3,
				Special.Aim);
	public static final PieceType Blaster=new PieceType(
				"Blaster",
				sprites.getSubimage(138, 10, 46, 62),
				2, 3, 1,
				2,
				1, 3, 3,
				Special.Blast);
	public static final PieceType Healer=new PieceType(
				"Healer",
				sprites.getSubimage(184, 10, 46, 62),
				4, 4, 2,
				2,
				1, 2, 1,
				Special.Heal);
	public static final PieceType Leader=new PieceType(
				"Leader",
				null,
				6, 4, 3,
				3,
				1, 1, 3,
				Special.Shot);
	
	public PieceType(
			String name,
			Image img,
			int mH, int mE, int mA,
			int move,
			int atkMin, int atkMax, int dmg,
			Special spc)
	{
		baseName=name;
		baseGraphic = img;
		maxHealth=mH;
		maxEnergy=mE;
		maxActions=mA;
		moveRange=move;
		attackMinRange=atkMin;
		attackMaxRange=atkMax;
		attackDamage=dmg;
		special=spc;
	}
	
	protected static final ArrayList<PieceType> types=new ArrayList();
	static {
		types.add(Charger);
		types.add(Bruiser);
		types.add(Marksman);
		types.add(Blaster);
		types.add(Healer);
	}
	public int hashCode()
	{
		return baseName.hashCode();
	}
	public static PieceType randomType()
	{
		return types.get((int)(Math.random()*types.size()));
	}
}
