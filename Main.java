/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Main
{
	public static final String GameTitle="Armour Battle";
	public static final int displayWidth=960;
	public static final int displayHeight=600;
	public static final Dimension minimumSize=new Dimension(displayWidth, displayHeight+25);
	public static final Font buttonFont=new Font("Arial", Font.PLAIN, 20);
	public static final Font titleFont=new Font("Arial", Font.PLAIN, 35);
	public static final Font textFont=new Font("Arial", Font.PLAIN, 16);
	public static final Color boxTextColor=new Color((86+131)/2, (213+218)/2, (204+212)/2);//(42+131, 207+218, 195+212);

	protected static boolean programDone=false;
	protected static boolean windowChange=false;
	
	protected static final JFrame myWindow=new JFrame(GameTitle);
	
	protected static GameInstance myGame=null;

	public static void main(String a[])
	{
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
		
		//com.sun.java.swing.plaf.windows.WindowsLookAndFeel newFeel;
		myWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//myWindow.setIconImage(new ImageIcon(Main.class.getClassLoader().getResource("icon2.png")).getImage());
		myWindow.setUndecorated(true);
		myWindow.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
		
		myWindow.setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width - minimumSize.width) / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height - minimumSize.height) / 2);
		myWindow.setMinimumSize(minimumSize);
		myWindow.setPreferredSize(minimumSize);
		myWindow.setResizable(false);
		//myWindow.pack();
		
		changeScreen(TitleScreen.instance);
	}
	
	protected static class ScreenChange implements Runnable
	{
		protected GameScreen nextScreen;
		public ScreenChange(GameScreen nextScreen)
		{
			this.nextScreen = nextScreen;
		}
		public void run()
		{
			myWindow.setContentPane(nextScreen);
			myWindow.setVisible(true);
			windowChange=false;
		}
	}
	public static void changeScreen(GameScreen newScreen)
	{
		if(!windowChange) try
		{
			windowChange=true;
			newScreen.initiate();
			SwingUtilities.invokeLater(new ScreenChange(newScreen));
		}
		catch(Exception ex)
		{
			System.out.println(ex);
			windowChange=false;
		}
	}
	public static BufferedImage loadImageFile(String location)
	{
		try{
			BufferedImage image=ImageIO.read(Main.class.getResource(location));
			if(image==null) throw new NullPointerException("File "+location+" was not found!");
			return image;
		}catch(Exception e){throw new RuntimeException(e);}
	}
}