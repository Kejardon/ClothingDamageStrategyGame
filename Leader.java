/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.Image;
import java.util.HashMap;

public class Leader
{
	/*
	 * Stuff this needs:
	 *  Name
	 *  Images
	 *  NumImages?
	 *  Tile(s?)
	 *  Selection Screen graphic?
	 *  Map Screen Icon?
	 *  ?
	 */
	public static final int ChargerIndex=0;
	public static final int BruiserIndex=1;
	public static final int MarksmanIndex=2;
	public static final int BlasterIndex=3;
	public static final int HealerIndex=4;
	public static final int maxSpriteIndex=5;
	protected static final HashMap<PieceType, Integer> typeMap=new HashMap();
	static {
		typeMap.put(PieceType.Blaster, BlasterIndex);
		typeMap.put(PieceType.Charger, ChargerIndex);
		typeMap.put(PieceType.Marksman, MarksmanIndex);
		typeMap.put(PieceType.Healer, HealerIndex);
		typeMap.put(PieceType.Bruiser, BruiserIndex);
	}
	
	public final String name;
	public final Image mapScreenImage;
	//public final PieceType pieceType;
	public final int trait1;
	public final int trait2;
	public final Image[] spriteImages;
	public Leader(String n, Image msi, int t1, int t2)
	{
		name=n;
		mapScreenImage=msi;
		this.trait1=t1;
		this.trait2=t2;
		spriteImages=null;
	}
	public String toString()
	{
		return name;
	}
	public Image unitSpriteImage(int i)
	{
		if(spriteImages==null) return null;
		return spriteImages[i];
	}
	public Image unitSpriteImage(PieceType p)
	{
		if(spriteImages==null) return null;
		Integer index=typeMap.get(p);
		if(index==null) return null;
		return spriteImages[index.intValue()];
	}
	public Piece.LeaderPiece getNewPiece(int player)
	{
		return new Piece.LeaderPiece(name, mapScreenImage, player, trait1, trait2);
	}
}
