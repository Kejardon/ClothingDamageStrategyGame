
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

public abstract class SubType
{
	public final String name;
	public final Image image;
	public abstract int DamageBonus(SubType againstThis);
	
	public SubType(String n, Image i){name=n;image=i;}
	
	public static final SubType Techno;
	public static final SubType Arcane;
	public static final SubType Holy;
	public static final SubType Mutant;
	public static final SubType Warbound;
	static {
		BufferedImage typeImages=Main.loadImageFile("SubTypes.png");
		Techno=new SubType("Techno", typeImages.getSubimage(32, 0, 16, 16))
		{
			public int DamageBonus(SubType againstThis){
				if(againstThis==Warbound) return 1;
				if(againstThis==Holy) return -1;
				return 0;
			}
		};
		Arcane=new SubType("Arcane", typeImages.getSubimage(48, 0, 16, 16))
		{
			public int DamageBonus(SubType againstThis){
				if(againstThis==Techno) return 1;
				if(againstThis==Warbound) return -1;
				return 0;
			}
		};
		Holy=new SubType("Holy", typeImages.getSubimage(0, 0, 16, 16))
		{
			public int DamageBonus(SubType againstThis){
				if(againstThis==Mutant) return 1;
				if(againstThis==Arcane) return -1;
				return 0;
			}
		};
		Mutant=new SubType("Mutant", typeImages.getSubimage(64, 0, 16, 16))
		{
			public int DamageBonus(SubType againstThis){
				if(againstThis==Arcane) return 1;
				if(againstThis==Techno) return -1;
				return 0;
			}
		};
		Warbound=new SubType("Warbound", typeImages.getSubimage(16, 0, 16, 16))
		{
			public int DamageBonus(SubType againstThis){
				if(againstThis==Holy) return 1;
				if(againstThis==Mutant) return -1;
				return 0;
			}
		};
	}
	protected static final ArrayList<SubType> types=new ArrayList();
	static {
		types.add(Techno);
		types.add(Arcane);
		types.add(Holy);
		types.add(Mutant);
		types.add(Warbound);
	}
	public static SubType randomType()
	{
		return types.get((int)(Math.random()*types.size()));
	}
	public int hashCode()
	{
		return name.hashCode();
	}
}
