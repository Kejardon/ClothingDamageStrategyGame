/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

import JExtensions.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.*;

public class MapScreen extends GameScreen implements MouseListener, MouseWheelListener
{
	public static final MapScreen instance=new MapScreen();

	protected static final BufferedImage background;
	static{background=Main.loadImageFile("Background.png");}
	
	protected GameInstance myGame;
	
	protected JLabel p1Icon=new JLabel();
	protected JLabel p2Icon=new JLabel();
	protected JLabel p1Name=new JLabel();
	protected JLabel p2Name=new JLabel();
	protected JLabel p1Dialog=new JLabel();
	protected JLabel p2Dialog=new JLabel();

	protected JPanel mapPanel=new JPanel();

	protected Piece selectedPiece;
	protected SelectAction selectedAction;
	protected Point[] validActionTargets;

	protected UnitPanel unitPanel=new UnitPanel();
	//protected JButton endTurnButton=new JButton("End Turn");

	public void fillInVariables(GameInstance game)
	{
		this.myGame=game;
	}

	protected static class UnitPanel extends Box
	{
		public UnitPanel(){super(BoxLayout.X_AXIS);}
		
		//Box buttonPanel=new Box(BoxLayout.Y_AXIS);
		 //Box nameTurnBox=new Box(BoxLayout.X_AXIS);
		  JLabel unitName=new JLabel();
		  MyButton endTurn;
		 Box moveButtonBox=new Box(BoxLayout.X_AXIS);
		  MyButton moveButton;
		  //JLabel moveName=new JLabel("Move: ");
		  JLabel moveDesc=new JLabel();
		 Box attackButtonBox=new Box(BoxLayout.X_AXIS);
		  MyButton attackButton;
		  //JLabel attackName=new JLabel("Attack: ");
		  JLabel attackDesc=new JLabel();
		 Box specialButtonBox=new Box(BoxLayout.X_AXIS);
		  MyButton specialButton;
		  //JLabel specialName=new JLabel("Special: ");
		  JLabel specialDesc=new JLabel();
		
		protected static class CustomBGBar extends MyJBar
		{
			public CustomBGBar(Color FG, Color BG){super(FG, BG);}
			public CustomBGBar(Color FG, Color BG, int current, int max){super(FG, BG, current, max);}
			protected Dimension getPreferredSize(Dimension parentDimension)
			{
				parentDimension.height = defaultHeight;
				parentDimension.width = 185;
				return parentDimension;
			}
		}
		//Box statLabels=new Box(BoxLayout.Y_AXIS);
		 JLabel healthLabel=new JLabel("Health ");
		 MyJBar healthBar=new CustomBGBar(Color.RED, Color.BLACK);
		 JLabel energyLabel=new JLabel("Energy ");
		 MyJBar energyBar=new CustomBGBar(Color.BLUE, Color.BLACK);
		 JLabel actionsLabel=new JLabel("Actions ");
		 MyJBar actionsBar=new CustomBGBar(Color.YELLOW, Color.BLACK);
		
		{
			BufferedImage buttons=Main.loadImageFile("Buttons.png");
			moveButton=new MyButton(new ImageIcon(buttons.getSubimage(41, 36, 38, 38))) {
				public void doAction(MouseEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null)
					{
						if(map.selectedPiece!=null)
							map.setAction(SelectAction.Move);
					}
				}
			};
			attackButton=new MyButton(new ImageIcon(buttons.getSubimage(41, 75, 38, 38))) {
				public void doAction(MouseEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null)
					{
						if(map.selectedPiece!=null)
							map.setAction(SelectAction.Attack);
					}
				}
			};
			specialButton=new MyButton(new ImageIcon(buttons.getSubimage(41, 113, 38, 38))) {
				public void doAction(MouseEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null)
					{
						if(map.selectedPiece!=null)
							map.setAction(SelectAction.Special);
					}
				}
			};
			endTurn=new MyButton(new ImageIcon(buttons.getSubimage(5, 4, 72, 32))) {
				public void doAction(MouseEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null && !map.myGame.isComputer[map.myGame.playerTurn-1])
					{
						MechanicsLibrary.endTurn(map);
					}
				}
			};
		}
		
		public void initLayout(){
			JLabel label;
			Box buttonPanel=new Box(BoxLayout.Y_AXIS);
			buttonPanel.setAlignmentY(TOP_ALIGNMENT);
			add(buttonPanel);
			buttonPanel.setPreferredSize(new Dimension(410, getHeight()));
			buttonPanel.setMaximumSize(new Dimension(410, getHeight()));
			Box nameTurnBox=new Box(BoxLayout.X_AXIS);
			buttonPanel.add(Box.createRigidArea(new Dimension(1,5)));
			buttonPanel.add(nameTurnBox);
			unitName.setFont(Main.textFont);
			unitName.setForeground(Main.boxTextColor);
			nameTurnBox.add(Box.createRigidArea(new Dimension(17,1)));
			nameTurnBox.add(unitName);
			nameTurnBox.add(Box.createHorizontalGlue());
			
			nameTurnBox.add(endTurn);
			//Box moveButtonBox=new Box(BoxLayout.X_AXIS);
			buttonPanel.add(Box.createRigidArea(new Dimension(1,10)));
			buttonPanel.add(moveButtonBox);
			moveButtonBox.add(moveButton);
			moveButtonBox.add(Box.createRigidArea(new Dimension(8,1)));
			label=new JLabel("Move: "); label.setFont(Main.textFont); moveButtonBox.add(label); label.setForeground(Main.boxTextColor);
			moveDesc.setFont(Main.textFont);
			moveDesc.setForeground(Main.boxTextColor);
			moveButtonBox.add(moveDesc);
			//Box attackButtonBox=new Box(BoxLayout.X_AXIS);
			buttonPanel.add(attackButtonBox);
			attackButtonBox.add(attackButton);
			attackButtonBox.add(Box.createRigidArea(new Dimension(8,1)));
			label=new JLabel("Attack: "); label.setFont(Main.textFont); attackButtonBox.add(label); label.setForeground(Main.boxTextColor);
			attackDesc.setFont(Main.textFont);
			attackDesc.setForeground(Main.boxTextColor);
			attackButtonBox.add(attackDesc);
			//Box specialButtonBox=new Box(BoxLayout.X_AXIS);
			buttonPanel.add(specialButtonBox);
			specialButtonBox.add(specialButton);
			specialButtonBox.add(Box.createRigidArea(new Dimension(8,1)));
			label=new JLabel("Special: "); label.setFont(Main.textFont); specialButtonBox.add(label); label.setForeground(Main.boxTextColor);
			specialDesc.setFont(Main.textFont);
			specialDesc.setForeground(Main.boxTextColor);
			specialButtonBox.add(specialDesc);
			for(Component c : buttonPanel.getComponents())
				((JComponent)c).setAlignmentX(LEFT_ALIGNMENT);
			
			/*
			moveButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null)
					{
						if(map.selectedPiece!=null)
							map.setAction(SelectAction.Move);
					}
				}
			});
			attackButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null)
					{
						if(map.selectedPiece!=null)
							map.setAction(SelectAction.Attack);
					}
				}
			});
			specialButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					MapScreen map=getMapParent(e.getSource());
					if(map!=null)
					{
						if(map.selectedPiece!=null)
							map.setAction(SelectAction.Special);
					}
				}
			});
			*/
			JComponent part=(JComponent)Box.createRigidArea(new Dimension(13,1));
			part.setAlignmentY(TOP_ALIGNMENT);
			add(part);
			
			Box statLabels=new Box(BoxLayout.Y_AXIS);
			statLabels.setAlignmentY(TOP_ALIGNMENT);
			add(statLabels);
			statLabels.setPreferredSize(new Dimension(198, getHeight()));
			statLabels.add(Box.createRigidArea(new Dimension(1,20)));
			healthLabel.setFont(Main.textFont);
			healthLabel.setForeground(Main.boxTextColor);
			statLabels.add(healthLabel);
			statLabels.add(healthBar);
			statLabels.add(Box.createRigidArea(new Dimension(100,10)));
			energyLabel.setFont(Main.textFont);
			energyLabel.setForeground(Main.boxTextColor);
			statLabels.add(energyLabel);
			statLabels.add(energyBar);
			statLabels.add(Box.createRigidArea(new Dimension(100,10)));
			actionsLabel.setFont(Main.textFont);
			actionsLabel.setForeground(Main.boxTextColor);
			statLabels.add(actionsLabel);
			statLabels.add(actionsBar);
			for(Component c : statLabels.getComponents())
				((JComponent)c).setAlignmentX(LEFT_ALIGNMENT);
		}
		
		public void setUnit(Piece unit)
		{
			if(unit==null)
			{
				unitName.setText("");
				moveDesc.setText("<html> <br> </html>");
				attackDesc.setText("<html> <br> </html>");
				specialDesc.setText("<html> <br> </html>");
				healthLabel.setText("Health ");
				energyLabel.setText("Energy ");
				actionsLabel.setText("Actions ");
				healthBar.setValues(0, 1);
				energyBar.setValues(0, 1);
				actionsBar.setValues(0, 1);
				return;
			}
			unitName.setText(unit.name());
			int i=unit.moveRange();
			moveDesc.setText("<html>"+i+(i==1?" square </html>":" squares </html>"));
			attackDesc.setText(cachedAttack(unit));
			Special special=unit.pieceType().special;
			specialDesc.setText(special==null?"No special":(cachedSpecial(special, unit)));
			healthLabel.setText("Health "+unit.health()+"/"+unit.maxHealth());
			energyLabel.setText("Energy "+unit.energy()+"/"+unit.maxEnergy());
			actionsLabel.setText("Actions "+unit.actions()+"/"+unit.maxActions());
			healthBar.setValues(unit.health(), unit.maxHealth());
			energyBar.setValues(unit.energy(), unit.maxEnergy());
			actionsBar.setValues(unit.actions(), unit.maxActions());
		}
		protected static class TriInt
		{
			int x, y, z;
			public TriInt(int x, int y, int z){this.x=x;this.y=y;this.z=z;}
			public boolean equals(Object o)
			{
				if(!(o instanceof TriInt)) return false;
				TriInt other=(TriInt)o;
				return (other.x==x)&&(other.y==y)&&(other.z==z);
			}
			public int hashCode()
			{
				return (x*121)+(y*11)+z;
			}
		}
		protected HashMap<TriInt, String> attackCache=new HashMap();
		protected HashMap<Special, String> specialCache=new HashMap();
		protected String cachedAttack(Piece unit)
		{
			TriInt atk=new TriInt(unit.attackDamage(), unit.attackMinRange(), unit.attackMaxRange());
			String S=attackCache.get(atk);
			if(S==null)
			{
				S="<html>damage "+atk.x+"<br>range "+(atk.y==atk.z?atk.y:atk.y+"-"+atk.z)+"</html>";
				attackCache.put(atk, S);
			}
			return S;
		}
		protected String cachedSpecial(Special special, Piece unit)
		{
			String S;
			if(special.dynamic)
				S="<html>"+special.name(unit)+": "+special.description(unit)+"</html>";
			else
				S=specialCache.get(special);
			if(S==null)
			{
				S="<html>"+special.name+": "+special.description+"</html>";
				specialCache.put(special, S);
			}
			return S;
		}
	}
	
	public void initiate()
	{
		if(!initiated)
		{
			initiated=true;
			
			setLayout(null);
			setOpaque(true);
			
			addMouseWheelListener(this);
			{
				JScrollPane p1IconPane=new JScrollPane(p1Icon);
				JScrollPane p1NamePane=new JScrollPane(p1Name);
				JScrollPane p2IconPane=new JScrollPane(p2Icon);
				JScrollPane p2NamePane=new JScrollPane(p2Name);
				p1Icon.setHorizontalAlignment(JLabel.CENTER);
				p2Icon.setHorizontalAlignment(JLabel.CENTER);
				p1Name.setHorizontalAlignment(JLabel.CENTER);
				p2Name.setHorizontalAlignment(JLabel.CENTER);
				p1Name.setFont(Main.buttonFont);
				p1Name.setForeground(Main.boxTextColor);
				p2Name.setFont(Main.buttonFont);
				p2Name.setForeground(Main.boxTextColor);
				add(p1IconPane);
				add(p1NamePane);
				add(p2IconPane);
				add(p2NamePane);
				p1IconPane.setBorder(BorderFactory.createEmptyBorder());
				p1NamePane.setBorder(BorderFactory.createEmptyBorder());
				p2IconPane.setBorder(BorderFactory.createEmptyBorder());
				p2NamePane.setBorder(BorderFactory.createEmptyBorder());
				p1IconPane.setBounds(8, 8, 150, 400);
				p2IconPane.setBounds(Main.displayWidth-150-8, 8, 150, 400);
				p1NamePane.setBounds(8, 400+8+2, 150, 50);
				p2NamePane.setBounds(Main.displayWidth-150-8, 400+8+2, 150, 50);
				p1IconPane.setOpaque(false);
				p1IconPane.getViewport().setOpaque(false);
				p2IconPane.setOpaque(false);
				p2IconPane.getViewport().setOpaque(false);
				p1NamePane.setOpaque(false);
				p1NamePane.getViewport().setOpaque(false);
				p2NamePane.setOpaque(false);
				p2NamePane.getViewport().setOpaque(false);
				
				JScrollPane p1DialogPane=new JScrollPane(p1Dialog);
				JScrollPane p2DialogPane=new JScrollPane(p2Dialog);
				add(p1DialogPane);
				add(p2DialogPane);
				p1DialogPane.setBounds(8, 460+8, 150, Main.displayHeight-468-8);
				p2DialogPane.setBounds(Main.displayWidth-150-8, 460+8, 150, Main.displayHeight-468-8);
				p1Dialog.setFont(Main.textFont);
				p1Dialog.setForeground(Main.boxTextColor);
				p2Dialog.setFont(Main.textFont);
				p2Dialog.setForeground(Main.boxTextColor);
				p1DialogPane.setBorder(BorderFactory.createEmptyBorder());
				p2DialogPane.setBorder(BorderFactory.createEmptyBorder());
				p1DialogPane.setOpaque(false);
				p1DialogPane.getViewport().setOpaque(false);
				p2DialogPane.setOpaque(false);
				p2DialogPane.getViewport().setOpaque(false);
			}
			
			{
				add(mapPanel);
				mapPanel.setBounds(150+8+4, 170, Main.displayWidth-300-16-8, Main.displayHeight-170-8-4);
				mapPanel.setOpaque(false);
			}
			
			{
				add(unitPanel);
				//unitPanel.setBounds(150+8+4, 8, Main.displayWidth-300-16-8, 160);
				unitPanel.setBounds(169, 5, 628, 186);
				unitPanel.initLayout();
			}
		}
		myGame=Main.myGame;
		p1Icon.setIcon(new ImageIcon(myGame.p1Leader.mapScreenImage));
		p2Icon.setIcon(new ImageIcon(myGame.p2Leader.mapScreenImage));
		p1Name.setText(myGame.p1Leader.name);
		p2Name.setText(myGame.p2Leader.name);
		mapPanel.removeAll(); //Prefer to put this in any method that abandons this screen.
		mapPanel.add(myGame.myMap);
		myGame.myMap.addMouseListener(this);
		myGame.myMap.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		myGame.myMap.setAlignmentY(JPanel.CENTER_ALIGNMENT);
		
		//unitPanel.setUnit(Piece.newPiece(null, null, PieceType.Blaster, SubType.Arcane, 1));
	}

	protected Point lastLOn=new Point();
	protected Point lastROn=new Point();
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public static MapScreen getMapParent(Object O)
	{
		if(!(O instanceof Component)) return null;
		Container c=((Component)O).getParent();
		while(c!=null && !(c instanceof MapScreen))
			c=c.getParent();
		return (MapScreen)c;
	}
	
	public void mousePressed(MouseEvent e)
	{
		Point lastOn;
		if(e.getButton()==MouseEvent.BUTTON1)
			lastOn=lastLOn;
		else if(e.getButton()==MouseEvent.BUTTON3)
			lastOn=lastROn;
		else
			return;
		int x=e.getPoint().x;
		int y=e.getPoint().y-GameMap.extraHeight;
		lastOn.x=(x%(TileType.tileSize.width+1)==0?-1:x/(TileType.tileSize.width+1));
		lastOn.y=(y%(TileType.tileSize.height+1)==0?-1:y/(TileType.tileSize.height+1));
	}

	public void mouseReleased(MouseEvent e)
	{
		Point lastOn;
		if(e.getButton()==MouseEvent.BUTTON1)
			lastOn=lastLOn;
		else if(e.getButton()==MouseEvent.BUTTON3)
			lastOn=lastROn;
		else
			return;
		int x=e.getPoint().x;
		int y=e.getPoint().y-GameMap.extraHeight;
		x=(x%(TileType.tileSize.width+1)==0?-1:x/(TileType.tileSize.width+1));
		y=(y%(TileType.tileSize.height+1)==0?-1:y/(TileType.tileSize.height+1));
		if(x==lastOn.x && y==lastOn.y && myGame.myMap.okSpot(x, y))
		{
			if(e.getButton()==MouseEvent.BUTTON3)
				clickSelect(x, y);
			else
				clickTarget(x, y);
		}
	}
	
	public boolean AIWholeAction(int tileX, int tileY, SelectAction action, int targetX, int targetY)
	{
		AISelect(tileX, tileY);
		Point[] options=AIAction(action);
		for(Point option : options)
		{
			if(option.x == targetX && option.y == targetY)
			{
				AITarget(targetX, targetY);
				return true;
			}
		}
		return false;
	}
	public void AISelect(int tileX, int tileY)
	{
		if(selectUnit(tileX, tileY))
		{
			setAction(null);
			try{Thread.sleep(1000);}catch(InterruptedException e){}
		}
	}
	public Point[] AIAction(SelectAction action)
	{
		if(setAction(action))
			try{Thread.sleep(1000);}catch(InterruptedException e){}
		return validActionTargets;
		
	}
	public void AITarget(int tileX, int tileY)
	{
		Point target=null;
		for(Point P : validActionTargets)
			if(tileX==P.x && tileY==P.y)
			{
				target=P;
				break;
			}
		if(target==null) return;
		selectedAction.doAction(this, selectedPiece, target);
		refreshAll();
		try{Thread.sleep(1000);}catch(InterruptedException e){}
	}
	
	protected void clickSelect(int tileX, int tileY)
	{
		if(myGame.isComputer[myGame.playerTurn-1])
		{
			unitPanel.setUnit(myGame.myMap.getPieceAt(tileX, tileY));
			return;
		}
		selectUnit(tileX, tileY);
		if(selectedPiece!=null)
			setAction(SelectAction.Move);
		else
			setAction(null);
	}
	public boolean selectUnit(int x, int y)
	{
		return selectUnit(myGame.myMap.getPieceAt(x, y));
	}
	public boolean selectUnit(Piece unit)
	{
		boolean changed=(selectedPiece!=unit);
		selectedPiece=unit;
		unitPanel.setUnit(unit);
		return changed;
	}
	protected void clickTarget(int tileX, int tileY)
	{
		if(selectedAction==null || validActionTargets==null) return;
		if(myGame.isComputer[myGame.playerTurn-1]) return;
		Point target=null;
		for(Point P : validActionTargets)
			if(tileX==P.x && tileY==P.y)
			{
				target=P;
				break;
			}
		if(target==null) return;
		selectedAction.doAction(this, selectedPiece, target);
		refreshAll();
	}
	public void refreshAll()
	{
		if(selectedPiece!=null)
		{
			int x=selectedPiece.x();
			int y=selectedPiece.y();
			if(myGame.myMap.okSpot(x, y))
				selectUnit(myGame.myMap.getPieceAt(x, y));
			else
				selectUnit(null);
		}
		setAction(selectedAction);
		//repaint();
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(e.getScrollType()!=MouseWheelEvent.WHEEL_UNIT_SCROLL) return;
		if(selectedPiece==null) return;
		if(e.getWheelRotation()==0) return;
		if(selectedAction==null)
		{
			if(e.getWheelRotation()>0)
				setAction(SelectAction.Move);
			else
				setAction(SelectAction.Special);
		}
		else if(selectedAction==SelectAction.Special)
		{
			if(e.getWheelRotation()>0)
				setAction(SelectAction.Move);
			else
				setAction(SelectAction.Attack);
		}
		else if(selectedAction==SelectAction.Move)
		{
			if(e.getWheelRotation()>0)
				setAction(SelectAction.Attack);
			else
				setAction(SelectAction.Special);
		}
		else if(selectedAction==SelectAction.Attack)
		{
			if(e.getWheelRotation()>0)
				setAction(SelectAction.Special);
			else
				setAction(SelectAction.Move);
		}
	}
	public boolean setAction(SelectAction action)
	{
		boolean changed=(action!=selectedAction);
		Color paintColor;
		if(action==SelectAction.Move)
		{
			paintColor=Color.yellow;
			//unitPanel.moveButtonBox.setBackground(paintColor);
			//unitPanel.attackButtonBox.setBackground(null);
			//unitPanel.specialButtonBox.setBackground(null);
		}
		else if(action==SelectAction.Attack)
		{
			paintColor=Color.red;
			//unitPanel.moveButtonBox.setBackground(null);
			//unitPanel.attackButtonBox.setBackground(paintColor);
			//unitPanel.specialButtonBox.setBackground(null);
		}
		else if(action==SelectAction.Special)
		{
			paintColor=Color.blue;
			//unitPanel.moveButtonBox.setBackground(null);
			//unitPanel.attackButtonBox.setBackground(null);
			//unitPanel.specialButtonBox.setBackground(paintColor);
		}
		else
		{
			paintColor=Color.gray;
			//unitPanel.moveButtonBox.setBackground(null);
			//unitPanel.attackButtonBox.setBackground(null);
			//unitPanel.specialButtonBox.setBackground(null);
		}
		
		if(action==null)
		{
			validActionTargets=null;
		}
		else
		{
			validActionTargets=action.validSpots(this, selectedPiece);
		}
		selectedAction=action;
		myGame.myMap.hilightTargets(validActionTargets, paintColor);
		repaint();
		return changed;
	}

	public void paintComponent(Graphics g)
	{
		g.drawImage(background, 0, 0, null);
	}
}
