/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class NewGameScreen extends GameScreen
{
	public static final NewGameScreen instance=new NewGameScreen();

	protected ArrayList<Leader> rawLeaderList=new ArrayList();
	protected JList<Leader> p1LeaderList=new JList<Leader>();
	protected JList<Leader> p2LeaderList=new JList<Leader>();
	protected JScrollPane p1ScrollPane=new JScrollPane(p1LeaderList);
	protected JScrollPane p2ScrollPane=new JScrollPane(p2LeaderList);
	protected JButton startGameButton = new JButton("Start Game");
	public void initiate() throws java.io.IOException
	{
		if(!initiated)
		{
			initiated=true;
			
			setLayout(null);
			setOpaque(true);
			
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setFont(Main.buttonFont);
				cancelButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {Main.changeScreen(TitleScreen.instance);}
				});
				add(cancelButton);
				cancelButton.setBounds(Main.displayWidth-200-8, Main.displayHeight-75-8, 200, 75);
			}
			
			{
				JLabel title = new JLabel("Leader Selection", null, JLabel.CENTER);
				title.setFont(Main.titleFont);
				add(title);
				title.setBounds(8, 8, Main.displayWidth-8-8, 75);
			}
			
			{
				startGameButton.setFont(Main.buttonFont);
				startGameButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						if(Main.myGame==null) Main.myGame=new GameInstance();
						GameMap map=new GameMap(Main.myGame, 12,11);
						map.myGame.setLeaders(p1LeaderList.getSelectedValue(), p2LeaderList.getSelectedValue());
						map.myGame.setMap(map);
						for(int i=0;i<6;i++)
						{
							PieceType pT=PieceType.randomType();
							while(true)
							{
								int x=(int)(Math.random()*6);
								int y=(int)(Math.random()*6);
								if(map.getPieceAt(x, y)!=null) continue;
								map.movePiece(Piece.newPiece(null, null, pT, SubType.randomType(), 1), 0, 0, x, y);
								break;
							}
							while(true)
							{
								int x=11-(int)(Math.random()*6);
								int y=10-(int)(Math.random()*6);
								if(map.getPieceAt(x, y)!=null) continue;
								map.movePiece(Piece.newPiece(null, null, pT, SubType.randomType(), 2), 0, 0, x, y);
								break;
							}
						}
						while(true)
						{
							int x=(int)(Math.random()*6);
							int y=(int)(Math.random()*6);
							if(map.getPieceAt(x, y)!=null) continue;
							map.movePiece(map.myGame.p1Leader.getNewPiece(1), 0, 0, x, y);
							break;
						}
						while(true)
						{
							int x=11-(int)(Math.random()*6);
							int y=10-(int)(Math.random()*6);
							if(map.getPieceAt(x, y)!=null) continue;
							map.movePiece(map.myGame.p2Leader.getNewPiece(2), 0, 0, x, y);
							break;
						}
						Main.changeScreen(MapScreen.instance);
					}
				});
				add(startGameButton);
				startGameButton.setBounds(8, Main.displayHeight-75-8, 200, 75);
			}
			
			{
				Image tempStuff=Main.loadImageFile("Leader.png");
				rawLeaderList.add(new Leader("A Leader With an Unusually Long Name", tempStuff, Piece.LeaderPiece.Trait_Actions, Piece.LeaderPiece.Trait_Damage));
				rawLeaderList.add(new Leader("Leader 1", tempStuff, Piece.LeaderPiece.Trait_Energy, Piece.LeaderPiece.Trait_SpecialDamage));
				p1LeaderList.setListData(rawLeaderList.toArray(new Leader[0]));
				p2LeaderList.setListData(rawLeaderList.toArray(new Leader[0]));
				p1LeaderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				p2LeaderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				ListSelectionListener selectLeaderAction=new ListSelectionListener()
				{
					public void valueChanged(ListSelectionEvent e)
					{
						if(e.getValueIsAdjusting()) return;
						JList<Leader> source=(JList)e.getSource();
						boolean isFirstPlayer=(source==p1LeaderList);
						Leader leader=source.getSelectedValue();
						
						if(p1LeaderList.getSelectedValue()!=null && p2LeaderList.getSelectedValue()!=null)
							startGameButton.setEnabled(true);
					}
				};
				p1LeaderList.addListSelectionListener(selectLeaderAction);
				p2LeaderList.addListSelectionListener(selectLeaderAction);
				p1LeaderList.setFont(Main.textFont);
				p2LeaderList.setFont(Main.textFont);
				
				add(p1ScrollPane);
				add(p2ScrollPane);
				p1ScrollPane.setBounds(8, 90, (Main.displayWidth/2)-50, 120);
				p2ScrollPane.setBounds(Main.displayWidth/2+50-8, 90, (Main.displayWidth/2)-50, 120);
			}
			
			{
				JLabel p1Header=new JLabel("Player 1 Leader:", null, JLabel.CENTER);
				JLabel p2Header=new JLabel("Player 2 Leader:", null, JLabel.CENTER);
				p1Header.setFont(Main.buttonFont);
				p2Header.setFont(Main.buttonFont);
				add(p1Header);
				add(p2Header);
				p1Header.setBounds(8, 220, (Main.displayWidth/2)-50, 40);
				p2Header.setBounds((Main.displayWidth/2)+50-8, 220, (Main.displayWidth/2)-50, 40);
			}
		}
		
		//Reset newGameScreenPanel as appropriate
		//if(Main.myGame==null) Main.myGame=new GameInstance();
		//regenerate the list of leaders if there are new leader options
		p1LeaderList.clearSelection();
		p2LeaderList.clearSelection();
		p1ScrollPane.getVerticalScrollBar().setValue(0);
		p2ScrollPane.getVerticalScrollBar().setValue(0);
		//Horizontal scroll bars too?
		//Clear graphics if any were loaded
		startGameButton.setEnabled(false);
		
	}
	
}
