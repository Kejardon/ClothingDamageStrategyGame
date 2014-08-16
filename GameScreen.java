/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class GameScreen extends JPanel
{
	public boolean initiated=false;
	public abstract void initiate() throws java.io.IOException;
	//public abstract void callScreen();
	
}
