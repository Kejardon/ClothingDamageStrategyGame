/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

public class GameMap extends JComponent
{
	protected TileType[][] tileGrid;
	protected Piece[][] pieceGrid;
	//Measured in tiles
	public final int width;
	public final int height;
	//Measured in pixels
	public final static int extraHeight=39;
	public final int pWidth;
	public final int pHeight;
	public final GameInstance myGame;
	protected Point[] hilightTargets;
	protected Color hilightColor;
	
	static final AlphaComposite transparency=AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	
	public GameMap(GameInstance owner, int x, int y)
	{
		super();
		myGame=owner;
		width=x;
		height=y;
		pWidth=x*(TileType.tileSize.width+1)+1;
		pHeight=y*(TileType.tileSize.height+1)+1; //Should there be extra for pieces extra height?
		setSize(pWidth, pHeight+extraHeight);
		pieceGrid=new Piece[x][y];
		tileGrid=new TileType[x][y];
		
		xOffsets=new int[x];
		for(int i=0;i<x;i++)
			xOffsets[i]=i*(TileType.tileSize.width+1)+1+(TileType.tileSize.width/2);
		xOffsetsNoAdjust=new int[x];
		for(int i=0;i<x;i++)
			xOffsetsNoAdjust[i]=i*(TileType.tileSize.width+1)+1;
		yOffsets=new int[y];
		for(int i=0;i<y;i++)
			yOffsets[i]=(i+1)*(TileType.tileSize.height+1)+extraHeight;
		yOffsetsNoAdjust=new int[y];
		for(int i=0;i<y;i++)
			yOffsetsNoAdjust[i]=i*(TileType.tileSize.height+1)+1+extraHeight;
		
		//Somehow, tileGrid needs to be filled in!
		makeRandomGrid();
		//super.setOpaque(false);
		
	}
	//public void setOpaque(boolean b){}
	protected void makeRandomGrid()
	{
		//Default to grass for now
		for(int i=0;i<width;i++)
			for(int j=0;j<height;j++)
			{
				int rand=(int)(Math.random()*16);
				switch(rand)
				{
					case 0:
						tileGrid[i][j]=TileType.grass2;
						break;
					case 1:
						tileGrid[i][j]=TileType.grass3;
						break;
					default:
						if(rand<9)
							tileGrid[i][j]=TileType.grass0;
						else
							tileGrid[i][j]=TileType.grass1;
						break;

				}
			}
		
		int randomRoadPoints=1+(int)(Math.random()*Math.sqrt(width*height)/3);
		if(randomRoadPoints > 1)
		{
			boolean[][] roadGrid=new boolean[width][height];
			int[][] roadPoints=new int[2][randomRoadPoints];
			for(int i=0;i<randomRoadPoints;i++)
			{
				int x=(int)(Math.random()*width);
				int y=(int)(Math.random()*height);
				while(roadGrid[x][y])
				{
					x=(int)(Math.random()*width);
					y=(int)(Math.random()*height);
				}
				roadPoints[0][i]=x;
				roadPoints[1][i]=y;
				roadGrid[x][y]=true;
			}
			for(int i=0;i<randomRoadPoints;i++)
			{
				int j=(int)(Math.random()*(randomRoadPoints-1));
				if(j>=i) j++;
				int startX=roadPoints[0][i];
				int endX=roadPoints[0][j];
				int startY=roadPoints[1][i];
				int endY=roadPoints[1][j];
				if(endY==startY)
				{
					if(startX>endX)
					{
						startX=endX;
						endX=roadPoints[0][i];
					}
					startX++;
					while(startX<endX)
					{
						roadGrid[startX][startY]=true;
						startX++;
					}	
				}
				else if(endX==startX)
				{
					if(startY>endY)
					{
						startY=endY;
						endY=roadPoints[1][i];
					}
					startY++;
					while(startY<endY)
					{
						roadGrid[startX][startY]=true;
						startY++;
					}
				}
				else
				{
					int x=startX;
					int y=startY;
					float xSub=0.5f;
					float ySub=0.5f;
					float slope=(((float)(endY-startY))/(endX-startX));
					if(slope<0) slope=-slope;
					/* 3|0
					 * -+-
					 * 2|1
					int quadrant=(endY>startY?
										(endX>startX?1:2):
										(endX>startX?0:3));
					 */
					boolean right=endX>startX;
					boolean down=endY>startY;
					while(x!=endX || y!=endY)
					{
						float dX=(right?(1-xSub):(xSub));
						float dY=(down?(1-ySub):(ySub));
						if(dX*slope > dY)
						{
							xSub += right?(dY/slope):(-dY/slope);
							ySub=down?0.0f:1.0f;
							y+=down?1:-1;
						}
						else
						{
							ySub += down?(dX*slope):(-dX*slope);
							xSub=right?0.0f:1.0f;
							x+=right?1:-1;
						}
						roadGrid[x][y]=true;
					}
				}
			}
			for(int i=0;i<width;i++) for(int j=0;j<height;j++) if(roadGrid[i][j])
			{
				int index=-1;
				if(j==0)
				{
					if(Math.random()<0.5)
						index+=1;
				}
				else if(roadGrid[i][j-1])
					index+=1;
				if(i==width-1)
				{
					if(Math.random()<0.5)
						index+=2;
				}
				else if(roadGrid[i+1][j])
					index+=2;
				if(j==height-1)
				{
					if(Math.random()<0.5)
						index+=4;
				}
				else if(roadGrid[i][j+1])
					index+=4;
				if(i==0)
				{
					if(Math.random()<0.5)
						index+=8;
				}
				else if(roadGrid[i-1][j])
					index+=8;
				tileGrid[i][j]=TileType.roadGrid[index];
			}
		}
	}

	public Dimension getPreferredSize()
	{
		return new Dimension(pWidth, pHeight+extraHeight);
	}
	public Dimension getMaximumSize()
	{
		return new Dimension(pWidth, pHeight+extraHeight);
	}
	public boolean okSpot(int x, int y)
	{
		return !(x<0 || y<0 || x>=width || y>=height);
	}
	public Piece getPieceAt(int x, int y)
	{
		return pieceGrid[x][y];
	}
	protected ArrayList<Piece>[] Pieces=new ArrayList[2];
	{
		Pieces[0]=new ArrayList();
		Pieces[1]=new ArrayList();
	}
	public Piece getP1Piece(int i){return Pieces[0].get(i);}
	public int numP1Piece(){return Pieces[0].size();}
	public Piece getP2Piece(int i){return Pieces[1].get(i);}
	public int numP2Piece(){return Pieces[1].size();}
	public Piece getPiece(int player, int i){return Pieces[player-1].get(i);}
	public int numPiece(int player){return Pieces[player-1].size();}
	
	public void movePiece(Piece piece, int xS, int yS, int xD, int yD)
	{
		boolean swapped=false;
		if(pieceGrid[xS][yS]==piece)
		{
			swapped=true;
			pieceGrid[xS][yS]=null;
		}
		Piece oldPiece=pieceGrid[xD][yD];
		if(oldPiece!=null)
		{
			int i=oldPiece.player();
			if(i==1 || i==2)
				Pieces[i-1].remove(oldPiece);
		}
		pieceGrid[xD][yD]=piece;
		if(!swapped)
		{
			int i=piece.player();
			if(i==1 || i==2)
				Pieces[i-1].add(piece);
		}
		piece.setPosition(myGame, xD, yD);
		repaint();
	}
	public void removePiece(Piece piece, int x, int y)
	{
		if(pieceGrid[x][y]==piece)
		{
			pieceGrid[x][y]=null;
			int i=piece.player();
			if(i==1 || i==2)
				Pieces[i-1].remove(piece);
		}
	}

	protected final int[] xOffsets;
	protected final int[] xOffsetsNoAdjust;
	protected final int[] yOffsets;
	protected final int[] yOffsetsNoAdjust;
	public void paintComponent(Graphics g)
	{
		//super.paintComponent(g); 
		/*
		* Draw order: 
		*   Grid
		*   for(top to bottom)
		*    Row of Tiles
		*    Row of Pieces
		*/
		g.setColor(Color.BLACK);
		for(int i=0;i<=height;i++)
			g.drawLine(0, i*(TileType.tileSize.height+1)+extraHeight, pWidth, i*(TileType.tileSize.height+1)+extraHeight);
		for(int i=0;i<=width;i++)
			g.drawLine(i*(TileType.tileSize.width+1), extraHeight, i*(TileType.tileSize.width+1), pHeight+extraHeight);
		
		for(int j=0;j<height;j++)
		{
			for(int i=0;i<width;i++)
				g.drawImage(tileGrid[i][j].baseGraphic, xOffsetsNoAdjust[i], yOffsetsNoAdjust[j], null);
			for(int i=0;i<width;i++)
			{
				Piece piece=pieceGrid[i][j];
				if(piece==null) continue;
				Image img=piece.baseGraphic();
				SubType type=piece.subType();
				if(piece.player()==2)
				{
					g.drawImage(img, xOffsets[i]+(img.getWidth(null)/2), yOffsets[j]-img.getHeight(null), -img.getWidth(null), img.getHeight(null), null);
					if(type!=null)
						g.drawImage(type.image, xOffsets[i]+(img.getWidth(null)/2)-type.image.getWidth(null)+3, yOffsets[j]-img.getHeight(null)+4, null);
				}
				else
				{
					g.drawImage(img, xOffsets[i]-(img.getWidth(null)/2), yOffsets[j]-img.getHeight(null), null);
					if(type!=null)
						g.drawImage(type.image, xOffsets[i]-(img.getWidth(null)/2)-3, yOffsets[j]-img.getHeight(null)+4, null);
				}
			}
		}
		g.setColor(Color.RED);
		for(int j=0;j<height;j++)
		{
			for(int i=0;i<width;i++)
			{
				Piece piece=pieceGrid[i][j];
				if(piece==null) continue;
				int health=piece.health();
				int maxHealth=piece.maxHealth();
				int healthSize=health*TileType.tileSize.width/2/maxHealth;
				g.drawLine(xOffsets[i]-(TileType.tileSize.width/4), yOffsets[j]-4,
						xOffsets[i]-(TileType.tileSize.width/4)+healthSize, yOffsets[j]-4);
			}
		}
		if(hilightTargets!=null && hilightTargets.length>0)
		{
			Graphics2D g2=(Graphics2D)g;
			g2.setColor(hilightColor);
			Composite oldC=g2.getComposite();
			g2.setComposite(transparency);
			
			for(Point target : hilightTargets)
				g2.fillRect(xOffsetsNoAdjust[target.x], yOffsetsNoAdjust[target.y], TileType.tileSize.width, TileType.tileSize.height);
			
			g2.setComposite(oldC);
		}
	}
	public void hilightTargets(Point[] targets, Color color)
	{
		hilightTargets=targets;
		hilightColor=color;
	}
	
}
