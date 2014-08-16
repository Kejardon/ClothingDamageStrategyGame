/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import java.awt.event.*;
import javax.swing.JButton;

public class TitleScreen extends GameScreen
{
	public static final TitleScreen instance=new TitleScreen();

	public void initiate()
	{
		if(!initiated)
		{
			initiated=true;
			
			setLayout(null);
			setOpaque(true);
			
			JButton newGameButton = new JButton("New Game"); //new ImageIcon("NewGame.png")
			newGameButton.setFont(Main.buttonFont);
			//newGameButton.setMultiClickThreshhold(50);
			newGameButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {Main.changeScreen(NewGameScreen.instance);}
			});
			add(newGameButton);
			newGameButton.setBounds(8, 8, 200, 75);

			JButton quitButton = new JButton("Quit"); //new ImageIcon("NewGame.png")
			quitButton.setFont(Main.buttonFont);
			//newGameButton.setMultiClickThreshhold(50);
			quitButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) { Main.myWindow.dispose();}
			});
			add(quitButton);
			quitButton.setBounds(Main.displayWidth-200-8, Main.displayHeight-75-8, 200, 75);
		}
	}
}
