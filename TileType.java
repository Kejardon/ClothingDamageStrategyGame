/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class TileType
{
	public static final Dimension tileSize=new Dimension(48, 32);
	
	public static final TileType grass0;
	public static final TileType grass1;
	public static final TileType grass2;
	public static final TileType grass3;
	
	public static final TileType roadN;
	public static final TileType roadNE;
	public static final TileType roadNS;
	public static final TileType roadNW;
	public static final TileType roadNES;
	public static final TileType roadNEW;
	public static final TileType roadNSW;
	public static final TileType roadNESW;
	public static final TileType roadE;
	public static final TileType roadES;
	public static final TileType roadEW;
	public static final TileType roadESW;
	public static final TileType roadS;
	public static final TileType roadSW;
	public static final TileType roadW;
	public static final TileType[] roadGrid;
	static {
		BufferedImage grassOptions=Main.loadImageFile("grass02.png");
		grass0=new TileType("grass", grassOptions.getSubimage(0,tileSize.height*3,tileSize.width,tileSize.height));
		grass1=new TileType("grass", grassOptions.getSubimage(0,tileSize.height,tileSize.width,tileSize.height));
		grass2=new TileType("grass", grassOptions.getSubimage(0,0,tileSize.width,tileSize.height));
		grass3=new TileType("grass", grassOptions.getSubimage(0,tileSize.height*2,tileSize.width,tileSize.height));
		
		roadN=new TileType("road", grassOptions.getSubimage(tileSize.width*2,tileSize.height*4,tileSize.width,tileSize.height));
		roadNE=new TileType("road", grassOptions.getSubimage(tileSize.width*2,tileSize.height*3,tileSize.width,tileSize.height));
		roadNS=new TileType("road", grassOptions.getSubimage(tileSize.width,tileSize.height*4,tileSize.width,tileSize.height));
		roadNW=new TileType("road", grassOptions.getSubimage(tileSize.width*3,tileSize.height*3,tileSize.width,tileSize.height));
		roadNES=new TileType("road", grassOptions.getSubimage(tileSize.width*2,tileSize.height,tileSize.width,tileSize.height));
		roadNEW=new TileType("road", grassOptions.getSubimage(tileSize.width,0,tileSize.width,tileSize.height));
		roadNSW=new TileType("road", grassOptions.getSubimage(tileSize.width*2,0,tileSize.width,tileSize.height));
		roadNESW=new TileType("road", grassOptions.getSubimage(tileSize.width,tileSize.height,tileSize.width,tileSize.height));
		roadE=new TileType("road", grassOptions.getSubimage(tileSize.width*3,0,tileSize.width,tileSize.height));
		roadES=new TileType("road", grassOptions.getSubimage(tileSize.width*2,tileSize.height*2,tileSize.width,tileSize.height));
		roadEW=new TileType("road", grassOptions.getSubimage(tileSize.width,tileSize.height*3,tileSize.width,tileSize.height));
		roadESW=new TileType("road", grassOptions.getSubimage(tileSize.width,tileSize.height*2,tileSize.width,tileSize.height));
		roadS=new TileType("road", grassOptions.getSubimage(tileSize.width*3,tileSize.height*4,tileSize.width,tileSize.height));
		roadSW=new TileType("road", grassOptions.getSubimage(tileSize.width*3,tileSize.height*2,tileSize.width,tileSize.height));
		roadW=new TileType("road", grassOptions.getSubimage(tileSize.width*3,tileSize.height,tileSize.width,tileSize.height));
		roadGrid=new TileType[15];
		roadGrid[0]=roadN;
		roadGrid[1]=roadE;
		roadGrid[2]=roadNE;
		roadGrid[3]=roadS;
		roadGrid[4]=roadNS;
		roadGrid[5]=roadES;
		roadGrid[6]=roadNES;
		roadGrid[7]=roadW;
		roadGrid[8]=roadNW;
		roadGrid[9]=roadEW;
		roadGrid[10]=roadNEW;
		roadGrid[11]=roadSW;
		roadGrid[12]=roadNSW;
		roadGrid[13]=roadESW;
		roadGrid[14]=roadNESW;
	}
	//public static final TileType dummy=new TileType("dummy", Main.loadImageFile("icon2.png"));
	
	public final Image baseGraphic;
	public final String baseName;
	
	public TileType(String n, Image base)
	{
		baseName=n;
		baseGraphic = base;
	}
	
}
