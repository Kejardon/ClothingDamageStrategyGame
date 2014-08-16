
import java.awt.Point;
import java.util.*;

/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */

/*
 * AI is not very flexible on some points:
 *  Aim type assumed to have only 2 actions
 *  Blast type assumed to have only 1 action
 *  Charger speed and jump are the same
 */

public class AI extends Thread
{
	//TODO LATER: Blast AI used to spread damage and not only snipe.
	
	//Game objects
	public final MapScreen screen;
	public final GameInstance game;
	public final GameMap map;
	
	//Plan results
	//public static final int MAX_PATHS=100;
	//public SortedArrayList<PlanIssue> otherPaths = new SortedArrayList(MAX_PATHS+1);
	//public HashSet<WholeTurnPlan> triedPlans = new HashSet();
	//public WholeTurnPlan bestPlan;
	//public int bestPlanScore;
	//Analysis for pieces that have done some action already
	public HashMap<Piece,AnalysisForPiece> partialPotentials=new HashMap();
	
	//Analysis for pieces that have not acted yet
	public AnalysisForPiece[] myPotential;
	public AnalysisForPiece[] enemyPotential;
	
	//Timer for when AI has to wrap up its plans.
	public long allowedTime;
	//public long lastStart;
	
	//Reused 2D arrays to keep track of current logic - clear after use.
	private Point[][] pointMap;
	private ArrayList<PlanPointPartial[][]> jumpPointMaps=new ArrayList();
	protected boolean[][] mapOfTakenSpots;
	
	//Type checking for effects
	public static final Class<Piece.AffectedPiece> aimEffect;
	public static final Class<Piece.AffectedPiece> blockEffect;
	static {
		aimEffect=Piece.affectTypes.get("AimingPiece");
		blockEffect=Piece.affectTypes.get("BlockingPiece");
	}
	public AI(MapScreen screen, long permittedTime)
	{
		this.screen=screen;
		game=screen.myGame;
		map=game.myMap;
		pointMap = new Point[map.width][map.height];
		allowedTime=permittedTime;
	}
	protected class AnalysisForPiece
	{
		//SubType type;
		Piece piece;
		Special special;
		
		//Damage ranges
		int[] multi; //how many times it can hit at this range (main attack). Includes archer's aim special
		int[] shot; //how many times Leader shot ability can hit
		int[] blast; //how many times blaster's special can hit at this range. Considers diagonals - straights do not reach as far as this array claim (increase index by 1 if straight)
		//Enemy-indexed data
		int[] damageToTarget; //theoreticalDamageToPieceIndex;
		//damageToTarget may be negative - signifies damage via special instead
		//of normal attack.
		//HashMap<ArrayList<ExpectedChange>, Integer> realDamageToTarget=new HashMap();
		int maxJumps=0; //Number of jumps this piece can perform, at most.
		ArrayList<PlanPointScore>[][] moveScorePointMap; //Map of useful spots to move to, and for approaching which enemy.
		
		Object tempRef; //lastMultiplier for DamageTo function
		
		AggregateVPP vpps; //Overall moveplan information. Null for enemy pieces.
		
		//Start analysis, for my pieces
		public AnalysisForPiece(Piece p, AnalysisForPiece[] enemies)
		{
			this(p);
			damageToTarget=new int[enemies.length];
			for(int i=0;i<enemies.length;i++)
				 damageToTarget[i] = DamageTo(enemies[i].piece);
			moveOptions();
		}
		//Shallow analysis. Called directly for enemy pieces, otherwise subset of my pieces
		public AnalysisForPiece(Piece p)
		{
			//type=p.subType();
			piece=p;
			special=p.pieceType().special;
			int actions=(p.player()==game.playerTurn)?(p.actions()):(p.maxActions()); //Actions this piece can(or will be able to) do
			int minAttackRange=p.attackMinRange(); //Min range of normal attack
			int maxAttackRange=p.attackMaxRange(); //Max range of normal attack (without special)
			int move=p.moveRange();
			boolean hasRangeBoost=false;
			boolean canRangeBoost=false;
			int energy=p.energy();
			int maxRange=maxAttackRange+(actions-1)*move; //The farthest this piece's main attack can reach this turn.
			if(special==Special.Aim)
			{
				if(actions>1 && energy>0)
					canRangeBoost=true;
				if(!Piece.hasEffect(p, aimEffect))
				{
					if(canRangeBoost)
						maxRange+=2;
				}
				else
				{
					hasRangeBoost=true;
					maxAttackRange-=2;
				}
			}
			else if(special==Special.Shot && energy>0)
			{
				shot=new int[(actions-1)*move+3];
				fillOutArray(shot, 2, 3, actions, move);
				if(energy<actions) for(int i=0;i<shot.length;i++)
					shot[i]=Math.min(shot[i], energy);
			}
			else if(special==Special.Blast && energy>0)
			{
				blast=new int[(actions-1)*move+5];
				fillOutArray(blast, 1, 5, actions, move);
				if(energy<actions) for(int i=0;i<blast.length;i++)
					blast[i]=Math.min(blast[i], energy);
			}
			else if(special==Special.Jump)
			{
				maxJumps=Math.min(piece.actions(),piece.energy());
			}
			multi=new int[maxRange];
			fillOutArray(multi, minAttackRange, maxAttackRange, actions, move);
			if(hasRangeBoost && canRangeBoost && actions>2)
				multi[multi.length-3]++;
		}
		
		//Fills out the multiplier array with appropriate values.
		//Assumes that the size of the array is correct (last value is at least 1) - Special.Aim uses this
		//Usable for any sort of attack; doesn't take energy into account (cap by Energy)
		protected final void fillOutArray(int[] multi, int minRange, int maxRange, int actions, int move)
		{
			for(int i=maxRange-1;i>=0;i--)
			{
				int m=actions;
				if(i+1 < minRange)
					m-=(((minRange-1)-(i+1))/move) +1;
				multi[i]=m;
			}
			if(actions==1) for(int i=maxRange;i<multi.length;i++)
				multi[i]=1;
			else
			{
				for(int i=maxRange;i<multi.length;i++)
				{
					int m=actions;
					m-=((i-maxRange)/move) +1;
					if(m<1) m=1;
					multi[i]=m;
				}
			}
		}
		public MoveRank bestMoveTowards(WholeTurnPlan plan, boolean[][] finalMap, int index)
		{
			if(vpps.turnsToReach==null) new GenerateWholeMapMove();
			ArrayList<PlanPointScore> scores=new GenerateBestMoveToward(plan).results;
			if(scores.isEmpty()) return null;
			MoveRank rank=new MoveRank(scores.get(0), index);
			//For now just return first.
			return rank;
		}
		protected class GenerateWholeMapMove
		{
			int energy=(special==Special.Jump)?(piece.energy()):0;
			//ArrayList<PlanPointThin> options=new ArrayList();
			PlanPointThin source;
			//PlanPointPartial sourceBack;
			int actions=piece.actions();
			int moves=0;
			int jumps=0;
			int maxActions=actions+1;
			int stopMove=piece.moveRange();
			int unfoundEnemies=enemyPotential.length;
			int enemyI;
			
			ArrayList<PlanPointPartial> optionsBack=new ArrayList();
			//int jumps=0;
			public GenerateWholeMapMove()
			{
				/*
				 * Current thoughts
				 * I can freely mess with reachableFrom at this point.
				 * 
				 * Let's spam up till we've reached every enemy, +actions. Full reachableFrom
				 * (+actions to account for nearby alternate routes)
				 * 
				 * The midpoint I want is a set of points (x/y) with scores (move+action*maxMove)
				 * and minimum energy (jumps) to reach target, and are theoretically reachable
				 * for the current moving piece.
				 * After that I need to calculate which points are actually reachable. I should
				 * look into SecondPlanPoints for this, use/refit them as practical.
				 * 
				 * The first move I want to expand original points really, do full
				 * reachableFroms for everything
				 * Every move after that I want 
				 */
				//moveTowardsTargets=new SortedArrayList();
				vpps.turnsToReach = new int[enemyPotential.length];
				moveScorePointMap=new ArrayList[map.width][map.height];
				//int last=0;
				//int lastNormal=0;
				maxActions=piece.actions()+1; //because I would need +1 elsewhere otherwise
				//int startNormal, start;
				//options.add(new PlanPointThin(piece.x(), piece.y(), actions, 0));
				//Fills pointMap with least-jump-moves
				MovementLooper<PlanPointThin> data = new MovementLooper<PlanPointThin>()
				{
					@Override
					public void addPosition(int x, int y)
					{
						PlanPointThin P;
						if((P=(PlanPointThin)pointMap[x][y])!=null)
						{
							if(P.jumpsLeft>=source.jumpsLeft)
							{
								P.reachableFlags|=PlanPointThin.inverseFlagIndex[fromIndex];
								return;
							}
							//else if((P.actionsLeft < actions+maxActions)||(P.actionsLeft == actions+maxActions && P.movesLeft<=moves)){}
							int oldFlags=P.reachableFlags;
							P=new PlanPointThin(P.x, P.y, P.actionsLeft, P.movesLeft);
							P.reachableFlags=oldFlags;
						}
						else
						{
							P=new PlanPointThin(x, y, actions, moves); //, source.jumpsLeft //actions-1?
							for(int i=enemyPotential.length-1;i>=0;i--)
							{
								if(vpps.turnsToReach[i]!=0) continue;
								Piece enemy=enemyPotential[i].piece;
								if(enemy.x()==x && enemy.y()==y)
								{
									vpps.turnsToReach[i]=piece.actions()-actions;
									//vpps.jumpsToReach[i]=source.jumpsLeft;
									unfoundEnemies--;
									break;
								}
							}
						}
						P.jumpsLeft=source.jumpsLeft;
						P.reachableFlags|=PlanPointThin.inverseFlagIndex[fromIndex];
						options.add(P);
						pointMap[x][y]=P;
					}
					int maxActionCounter=0;
					//int maxAction=0;
					int actions;
					@Override
					public boolean checkLoop()
					{
						actions--;
						if(unfoundEnemies>0) return true;
						//if(maxAction==0) { maxAction=piece.maxActions()+1; maxActionCounter=maxAction; }
						maxActionCounter--;
						return maxActionCounter>=0;
					}
					@Override
					public void init(Object data) { maxActionCounter=((Piece)data).maxActions()+1; actions=maxActionCounter-1; }
				};
				data.init(piece);
				data.doLoop(new PlanPointThin(piece.x(), piece.y(), actions, 0),piece,game,energy>0);
				ArrayList<PlanPointThin> options = data.options; //Just to be able to clear the PointMap a bit more efficiently
				/*
				while(unfoundEnemies>0)
				{
					actions--;
					start=options.size()-1;
					for(moves=piece.moveRange()-1;moves>=0;moves--)
					{
						startNormal=options.size()-1;
						for(int j=startNormal;j>=lastNormal;j--)
						{
							source=options.get(j);
							for(int k=0;k<4;k++)
								doAddEverPositionThin(k); //, data
						}
						lastNormal=startNormal+1;
					}
					if(energy>0) for(int j=start;j>=last;j--)
					{
						source=options.get(j);
						if(source.jumpsLeft==0) continue;
						source.jumpsLeft--;
						for(int k=4;k<12;k++)
							doAddEverPositionThin(k); //, data
						source.jumpsLeft++;
					}
					last=start+1;
				}
				for(int i=maxActions;i>0;i--)
				{
					actions--;
					start=options.size()-1;
					for(moves=piece.moveRange()-1;moves>=0;moves--)
					{
						startNormal=options.size()-1;
						for(int j=startNormal;j>=lastNormal;j--)
						{
							source=options.get(j);
							for(int k=0;k<4;k++)
								doAddEverPositionThin(k); //, data
						}
						lastNormal=startNormal+1;
					}
					if(energy>0) for(int j=start;j>=last;j--)
					{
						source=options.get(j);
						if(source.jumpsLeft==0) continue;
						source.jumpsLeft--;
						for(int k=4;k<12;k++)
							doAddEverPositionThin(k); //, data
						source.jumpsLeft++;
					}
					last=start+1;
				}
				*/
				//vpps.wholeMapPoints=options.toArray(new PlanPointThin[options.size()]);
				//if(energy>0)
				//	for(int i=0;i<vpps.jumpsToReach.length;i++)
				//		vpps.jumpsToReach[i]=energy-vpps.jumpsToReach[i];
				while(jumpPointMaps.size()<=energy)
					jumpPointMaps.add(new PlanPointPartial[map.width][map.height]);
				for(enemyI=0;enemyI<enemyPotential.length;enemyI++)
				{
					AnalysisForPiece enemy=enemyPotential[enemyI];
					PlanPointThin enemyStart=(PlanPointThin)pointMap[enemy.piece.x()][enemy.piece.y()];
					enemyStart.jumpsLeft=0;
					optionsBack.add(enemyStart);
					source=enemyStart; //getSource(enemyStart);
					//actions=0;
					moves=0;
					jumps=0;
					int lastNormal=0; int last=0;
					actions=-vpps.turnsToReach[enemyI]+1;
					for(int i=0;i<4;i++)
						doAddEverPositionBack(i);
					/* Start at enemyStart
					 * Move out by existing reachableFlag and pointMap
					 * Max score of (piece.actions+turnsToReach)*moves or simply (piece.actions+turnsToReach) actions
					 *   +1 for case of target piece blocking jump
					 * Need to think through jump/action count logic
					 * Return path has actionsLeft from piece.actions-1 to piece.actions-1-turnsToReach;
					 * Note that jumpsLeft is actually count of jumpsUsed for this loop
					 * 
					 * Path is a dead end if?
					 */
					//int stopActions=vpps.turnsToReach[enemyI]+1;
					for(;actions<maxActions;actions++)//actions=-stopActions
					{
						int start=optionsBack.size()-1;
						for(moves=0;moves<stopMove;moves++)
						{
							int startNormal=optionsBack.size()-1;
							for(int j=startNormal;j>=lastNormal;j--)
							{
								getSource(optionsBack.get(j));
								jumps=source.jumpsLeft;
								for(int k=0;k<4;k++)
									doAddEverPositionBack(k); //, data
							}
							lastNormal=startNormal+1;
						}
						if(energy>0) for(int j=start;j>=last;j--)
						{
							PlanPointPartial sourceBack=optionsBack.get(j);
							if(sourceBack.jumpsLeft>=energy) continue;
							jumps=sourceBack.jumpsLeft+1;
							getSource(sourceBack);
							for(int k=4;k<12;k++)
								doAddEverPositionBack(k); //, data
						}
						last=start+1;
					}
					for(PlanPointPartial back : optionsBack)
						jumpPointMaps.get(back.jumpsLeft)[back.x][back.y]=null;
					optionsBack.clear();
				}
				
				for(PlanPointThin point : options)
					pointMap[point.x][point.y]=null;
			}
			protected final void getSource(Point P)
			{
				source=(PlanPointThin)pointMap[P.x][P.y];
			}
			//jumpPointMaps is used simply to skip places that have already been reached,
			//but allows repeats that have been reached with fewer jumps now
			//target is a beyond-ideal best-case-reach (minimum jumps AND minimum move needed to reach a spot)
			protected final void doAddEverPositionBack(int fromIndex)
			{
				if((source.reachableFlags & (1<<fromIndex)) == 0) return;
				int x=source.x+PlanPointThin.dX[fromIndex];
				int y=source.y+PlanPointThin.dY[fromIndex];
				int z=jumps;
				PlanPointPartial P;
				//if(!MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) return;
				//if(x<0 || x>=map.width || y<0 || y>=map.height) return;
				while((P=(PlanPointPartial)jumpPointMaps.get(z)[x][y])==null && --z>=0);
				if(P!=null) return;
				PlanPointThin target=(PlanPointThin)pointMap[x][y];
				//target.actionsLeft can be a big number because of jumps. This needs some complicated logic to work right, TODO
				//if(target.actionsLeft>actions || (target.actionsLeft==actions && moves>target.movesLeft))
				//	return;
				P=new PlanPointPartial(x, y, actions, moves, jumps);
				jumpPointMaps.get(jumps)[x][y]=P;
				optionsBack.add(P);
				if(target.actionsLeft>=-1) //if(actions>=0 && target.actionsLeft>=-1) //TODO: Figure out if this should be -1 or 0
				{
					//+vpps.turnsToReach[enemyI] to fix score offset to be comparable between enemies
					//TODO: Doublecheck this actions is correct.
					PlanPointScore score=new PlanPointScore(x, y, (actions+vpps.turnsToReach[enemyI])*stopMove - moves, jumps, enemyI, actions);
					ArrayList<PlanPointScore> list=moveScorePointMap[x][y];
					if(list==null)
					{
						list=new ArrayList();
						moveScorePointMap[x][y]=list;
					}
					list.add(score);
				}
			}
			/*
			protected final void doAddEverPositionThin(int fromIndex)
			{
				int x=source.x+PlanPointThin.dX[fromIndex];
				int y=source.y+PlanPointThin.dY[fromIndex];
				PlanPointThin P;
				if(!MechanicsLibrary.mayEverPositionAt(game, piece, x, y))
					return;
				else if((P=(PlanPointThin)pointMap[x][y])!=null)
				{
					if(P.jumpsLeft>=source.jumpsLeft)
					{
						P.reachableFlags|=PlanPointThin.inverseFlagIndex[fromIndex];
						return;
					}
					//else if((P.actionsLeft < actions+maxActions)||(P.actionsLeft == actions+maxActions && P.movesLeft<=moves)){}
					int oldFlags=P.reachableFlags;
					P=new PlanPointThin(P.x, P.y, P.actionsLeft, P.movesLeft);
					P.reachableFlags=oldFlags;
				}
				else
				{
					P=new PlanPointThin(x, y, actions-1, moves); //, source.jumpsLeft
					for(int i=enemyPotential.length-1;i>=0;i--)
					{
						if(vpps.turnsToReach[i]!=0) continue;
						Piece enemy=enemyPotential[i].piece;
						if(enemy.x()==x && enemy.y()==y)
						{
							vpps.turnsToReach[i]=piece.actions()-actions;
							//vpps.jumpsToReach[i]=source.jumpsLeft;
							unfoundEnemies--;
							break;
						}
					}
				}
				P.jumpsLeft=source.jumpsLeft;
				P.reachableFlags|=PlanPointThin.inverseFlagIndex[fromIndex];
				options.add(P);
				pointMap[x][y]=P;
			}
			*/
		}
		protected class GenerateBestMoveToward
		{
			//TODO: Finish this. Needs to create a MoveTask or something.
			//int energy=(special==Special.Jump)?(piece.energy()):0;
			//ArrayList<PlanPointPath> options=new ArrayList();
			ArrayList<PlanPointScore> results=new ArrayList();
			//PlanPointPath source;
			//int actions=piece.actions();
			//int moves=0;
			//int jumps=0;
			//int bestScore=Integer.MAX_VALUE; //Lower is better
			WholeTurnPlan plan;
			public GenerateBestMoveToward(WholeTurnPlan thePlan)
			{
				plan=thePlan;
				//int last=0;
				//int lastNormal=0;
				//int startNormal, start;
				
				MovementLooper<PlanPointPath> looper = new MovementLooper<PlanPointPath>()
				{
					int actions;
					int energy;
					int bestScore=Integer.MAX_VALUE; //Lower is better
					@Override
					public void init(Object data)
					{
						piece = (Piece)data;
						actions = piece.actions();
						energy=(special==Special.Jump)?(piece.energy()):0;
					}
					@Override
					public boolean checkLoop()
					{
						return actions-->=0;
					}
					@Override
					public void addPosition(int x, int y)
					{
						PlanPointPath P=(PlanPointPath)pointMap[x][y];
						ExpectedChange PChange;
						if(P!=null)
						{
							if(P.jumpsLeft>=jumps)
								return;
						}
						else if((PChange=plan.getChange(x, y))!=null)
						{
							if(PChange.addedPiece!=null)
								return;
						}

						P=new PlanPointPath(x, y, actions-1, moves, jumps);
						options.add(P);
						pointMap[x][y]=P;
						P.previous=source;

						ArrayList<PlanPointScore> scores=moveScorePointMap[x][y];
						if(scores!=null)
						{
							for(int i=scores.size()-1;i>=0;i--)
							{
								PlanPointScore score=scores.get(i);
								if(bestScore<score.score) continue;
								if(score.jumpsLeft > (energy-jumps)) continue;
								//TODO NOW: Fix score's actions?
								Integer lifeLeft=plan.remainingLife.get(enemyPotential[score.target].piece);
								if(lifeLeft!=null && lifeLeft.intValue()==0) continue;
								if(score.score<bestScore)
								{
									results.clear();
									bestScore=score.score;
								}
								score.previous=P;
								results.add(score);
							}
						}
					}
				};
				int actions=piece.actions();
				int energy=(special==Special.Jump)?(piece.energy()):0;
				looper.init(piece);
				looper.doLoop(new PlanPointPath(piece.x(), piece.y(), actions, 0, 0), piece, game, energy>0);
				/*
				options.add(new PlanPointPath(piece.x(), piece.y(), actions, 0, 0));
				while(actions-->=0)
				{
					start=options.size()-1;
					for(moves=piece.moveRange()-1;moves>=0;moves--)
					{
						startNormal=options.size()-1;
						for(int j=startNormal;j>=lastNormal;j--)
						{
							source=options.get(j);
							jumps=source.jumpsLeft;
							for(int k=0;k<4;k++)
								doAddEverPositionThin(k); //, data
						}
						lastNormal=startNormal+1;
					}
					if(energy>0) for(int j=start;j>=last;j--)
					{
						source=options.get(j);
						if(source.jumpsLeft==0) continue;
						jumps=source.jumpsLeft-1;
						for(int k=4;k<12;k++)
							doAddEverPositionThin(k); //, data
					}
					last=start+1;
				}
				*/
				//while(jumpPointMaps.size()<=energy) //Why was this here?
				//	jumpPointMaps.add(new PlanPointPartial[map.width][map.height]);
				
				for(PlanPointPartial point : looper.options)
					pointMap[point.x][point.y]=null;
			}
			/*
			protected final void doAddEverPositionThin(int fromIndex)
			{
				int x=source.x+PlanPointThin.dX[fromIndex];
				int y=source.y+PlanPointThin.dY[fromIndex];
				if(!MechanicsLibrary.mayPositionAt(game, piece, x, y))
					return;
				PlanPointPath P=(PlanPointPath)pointMap[x][y];
				ExpectedChange PChange;
				if(P!=null)
				{
					if(P.jumpsLeft>=jumps)
						return;
				}
				else if((PChange=plan.getChange(x, y))!=null)
				{
					if(PChange.addedPiece!=null)
						return;
				}
				
				P=new PlanPointPath(x, y, actions-1, moves, jumps);
				options.add(P);
				pointMap[x][y]=P;
				P.previous=source;
				
				ArrayList<PlanPointScore> scores=moveScorePointMap[x][y];
				if(scores!=null)
				{
					for(int i=scores.size()-1;i>=0;i--)
					{
						PlanPointScore score=scores.get(i);
						if(bestScore<score.score) continue;
						if(score.jumpsLeft > (energy-jumps)) continue;
						Integer lifeLeft=plan.remainingLife.get(enemyPotential[score.target].piece);
						if(lifeLeft!=null && lifeLeft.intValue()==0) continue;
						if(score.score<bestScore)
						{
							results.clear();
							bestScore=score.score;
						}
						score.previous=P;
						results.add(score);
					}
				}
			}
			*/
		}
		//Calculates or loads the best path to attack the target for the given layout. Small exception: Blast pieces choose the best spot to fire at.
		public FinalSecondPlanPoint BestPoint(int target, ArrayList<ExpectedChange> changes, boolean[][] finalMap)
		{
			HashMap<ArrayList<ExpectedChange>, FinalSecondPlanPoint> damageMap=vpps.plans[target].realDamageToTarget;
			FinalSecondPlanPoint finalPoint=damageMap.get(changes);
			boolean[][] interestMap=vpps.plans[target].interestingPoint;
			if(finalPoint!=null)
				return finalPoint;
			ArrayList<ExpectedChange> altChanges=null;
			for(int i=0;i<changes.size();i++)
			{
				ExpectedChange ec=changes.get(i);
				if(interestMap[ec.x][ec.y])
				{
					if(altChanges!=null)
						altChanges.add(ec);
					continue;
					//Handled interesting change
				}
				//Skip uninteresting change
				//Also, this initializes the list if there ARE uninteresting changes (i.e. altChanges is worthwhile to consider)
				if(altChanges==null)
				{
					altChanges=new ArrayList(changes.size());
					for(int j=0;j<i;j++)
						altChanges.add(changes.get(j));
				}
			}
			if(altChanges!=null)
			{
				finalPoint=damageMap.get(altChanges);
				if(finalPoint!=null)
				{
					damageMap.put(changes, finalPoint);
					return finalPoint;
				}
			}
			int bestDamage=0;
			FinalSecondPlanPoint bestPoint=null;
			Piece targetPiece=map.getPiece(game.playerTurn^3, target);
			for(SecondPlanPoint destination : vpps.plans[target].spps)
			{
				if(destination==null) continue; //Clearly no opportunity for this mixup
				FinalSecondPlanPoint point=destination.checkPath(finalMap);
				if(point == null) continue; //Failed to reach destination.
				if(point.finalDamage==0)
					point.finalDamage=DamageTo(targetPiece, destination, point.finalActions);
				if(point.finalDamage==0) continue; //Can't deal damage to target?
				if(point.finalDamage<0 && special==Special.Blast)
				{
					//TODO: This is probably better as a whole separate score analysis
					ArrayList<ScoredPoint> points=new ArrayList(9);
					Point myPieceSpot=new Point(piece.x(), piece.y());
					SquareIterator targetIter=new SquareIterator(targetPiece.x(), targetPiece.y(), 0, 1);
					if(!vpps.plans[target].hasBlast) //If blast calculations haven't been done yet, do them now
					{
						vpps.plans[target].hasBlast=true;
						while(targetIter.next())
						{
							if(!map.okSpot(targetIter.cX(), targetIter.cY())) continue;
							int distance=PlanPoint.distanceTo(myPieceSpot, targetIter.cX(), targetIter.cY());
							if(targetIter.cX()==myPieceSpot.x || targetIter.cY()==myPieceSpot.y)
								distance++;
							if(distance>5) continue;
							for(SquareIterator hitIter=new SquareIterator(targetIter.cX(), targetIter.cY(), 0, 1);hitIter.next();)
								if(map.okSpot(hitIter.cX(), hitIter.cY()))
									interestMap[hitIter.cX()][hitIter.cY()]=true;
						}
						if(altChanges!=null) //Null if no changes, empty if any changes (because blasters have 1 action).
						{ //Don't bother processing if no changes (or theoretically if all changes already interesting)
							altChanges.clear(); //Clear anyways incase blasters theoretically get more actions
							for(int i=0;i<changes.size();i++)
							{
								ExpectedChange ec=changes.get(i);
								if(interestMap[ec.x][ec.y])
									altChanges.add(ec);
							}
						}
					}
					int bestX=0;
					int bestY=0;
					int bestScore=0;
					targetLoop:
					while(targetIter.next())
					{
						if(!map.okSpot(targetIter.cX(), targetIter.cY())) continue;
						int distance=PlanPoint.distanceTo(myPieceSpot, targetIter.cX(), targetIter.cY());
						if(targetIter.cX()==myPieceSpot.x || targetIter.cY()==myPieceSpot.y)
							distance++;
						if(distance>5) continue;
						int score=0;
						SquareIterator hitIter=new SquareIterator(targetIter.cX(), targetIter.cY(), 0, 1);
						while(hitIter.next())
						{
							if(!map.okSpot(hitIter.cX(), hitIter.cY())) continue;
							if(!finalMap[hitIter.cX()][hitIter.cY()]) //Is there a piece at this time?
								continue;
							Piece hitPiece=map.getPieceAt(hitIter.cX(), hitIter.cY());
							if(hitPiece==null || hitPiece.player()==game.playerTurn) //Was there not a piece before?
								continue targetLoop; //It's our piece, this would be friendly fire.
							score+=BlastDamageTo(hitPiece, 1);
						}
						if(score>bestScore)
						{
							bestScore=score;
							bestX=targetIter.cX();
							bestY=targetIter.cY();
						}
					}
					if(bestScore==0)
						continue;
					point=new BlastFSPP(point, new Point(bestX, bestY), bestScore);
				}
				if(Math.abs(point.finalDamage)>bestDamage)
				{
					bestPoint=point;
					bestDamage=Math.abs(point.finalDamage);
					continue;
				}
				if(Math.abs(point.finalDamage)==bestDamage &&
				  bestPoint.finalActions<point.finalActions)
					bestPoint=point;
			}
			tempRef=bestPoint;
			damageMap.put((ArrayList<ExpectedChange>)changes.clone(), bestPoint);
			if(altChanges!=null)
				damageMap.put(altChanges, bestPoint);
			return bestPoint;
		}
		public int DamageTo(Piece target, PlanPoint fromHere)
		{
			return DamageTo(target, fromHere, fromHere.actionsLeft);
		}
		public int DamageTo(Piece target, Point fromHere, int multiplier)
		{
			int distance=Math.abs(target.x()-fromHere.x)+Math.abs(target.y()-fromHere.y);
			int damage=0;
			if(distance==0) return 0;
			if(distance<=piece.attackMaxRange() && distance>=piece.attackMinRange())
				damage=MainDamageTo(target, multiplier);
			if(shot!=null && distance<=shot.length)
			{
				int min=shot[distance-1];
				if(min>multiplier) min=multiplier;
				int altDamage=ShotDamageTo(target, min);
				if(altDamage>damage) damage=-altDamage;
			}
			else if(blast!=null)
			{
				int altDistance=distance;
				if(target.x()==piece.x() || target.y()==piece.y())
					altDistance++;
				if(altDistance<=blast.length)
				{
					int min=blast[distance-1];
					if(min>multiplier) min=multiplier;
					int altDamage=BlastDamageTo(target, min);
					if(altDamage>damage) damage=-altDamage;
				}
			}
			else if(distance>piece.attackMaxRange()) //For niche case of unboosted marksmen boosting then firing.
			{
				damage=MainDamageTo(target, 1);
			}
			return damage;
		}
		//Calculates the max theoretical damage possible, from normal, shot, and blast. If special is used, returns negative damage.
		public final int DamageTo(Piece target)
		{
			int distance=Math.abs(target.x()-piece.x())+Math.abs(target.y()-piece.y());
			int damage=0;
			if(distance==0) return 0;
			if(distance<=multi.length) damage=MainDamageTo(target, multi[distance-1]);
			if(shot!=null && distance<=shot.length)
			{
				int altDamage=ShotDamageTo(target, shot[distance-1]);
				if(altDamage>damage) damage=-altDamage;
			}
			else if(blast!=null)
			{
				int altDistance=distance;
				if(target.x()==piece.x() || target.y()==piece.y())
					altDistance++;
				if(altDistance<=blast.length)
				{
					int altDamage=BlastDamageTo(target, blast[altDistance-1]);
					if(altDamage>damage) damage=-altDamage;
				}
			}
			return damage;
		}
		//Calculates the damage done if this piece uses Shot on target, times multiplier
		public int ShotDamageTo(Piece target, int multiplier)
		{
			int baseDamage=2;
			if(piece instanceof Piece.LeaderPiece)
			{
				Piece.LeaderPiece leader=(Piece.LeaderPiece)piece;
				if(leader.trait1==Piece.LeaderPiece.Trait_SpecialDamage || leader.trait2==Piece.LeaderPiece.Trait_SpecialDamage)
					baseDamage+=1;
			}
			if(Piece.hasEffect(target, blockEffect))
				baseDamage-=2;
			int damage=multiplier*baseDamage;
			if(damage<0) damage=0;
			return damage;
		}
		//Calculates the damage done if this piece uses Blast on/near target, times multiplier
		public int BlastDamageTo(Piece target, int multiplier)
		{
			int baseDamage=1;
			baseDamage+=MechanicsLibrary.subTypeBonus(game, piece, target);
			if(Piece.hasEffect(target, blockEffect))
				baseDamage-=2;
			int damage=multiplier*(baseDamage);
			if(damage<0) damage=0;
			return damage;
		}
		//Calculates the damage done if this piece directly attacks target, times multiplier
		public int MainDamageTo(Piece target, int multiplier)
		{
			int baseDamage=piece.attackDamage();
			baseDamage+=MechanicsLibrary.subTypeBonus(game, piece, target);
			if(Piece.hasEffect(target, blockEffect))
				baseDamage-=2;
			int damage=multiplier*(baseDamage);
			if(damage<0) damage=0;
			return damage;
		}
		//Create the vpps (general move plans) for this piece.
		public final AggregateVPP moveOptions()
		{
			if(vpps==null)
			{
				{
					//MakeMoveOptions data=new MakeMoveOptions();
					MovementLooper<PlanPoint> data = new MovementLooper<PlanPoint>()
					{
						int actions;
						@Override
						public void addPosition(int x, int y)
						{
							PlanPoint P=(PlanPoint)pointMap[x][y];
							if(P!=null)
							{
								P.reachableFrom.add(source);
								return;
							}
							P=new PlanPoint(x, y, actions-1, moves); //, source.jumpsLeft
							P.reachableFrom.add(source);
							options.add(P);
							pointMap[x][y]=P;
						}
						@Override
						public void init(Object data) { actions = ((Piece)data).actions()+1; }
						@Override
						public boolean checkLoop()
						{
							actions--;
							return actions>0;
						}
					};
					data.init(piece);
					data.doLoop(new PlanPoint(piece.x(), piece.y(), piece.actions(), 0), piece, game, true);

					vpps=new AggregateVPP();
					vpps.reachablePoints=data.options.toArray(new PlanPoint[data.options.size()]);
					for(PlanPoint p : vpps.reachablePoints) //Important cleanup for above MovementLooper
						pointMap[p.x][p.y]=null;
				}
				vpps.plans=new VaguePiecePlan[map.numPiece(game.playerTurn^3)];
				int minRange = piece.attackMinRange();
				int maxRange = piece.attackMaxRange();
				vpps.canBoost = (special == Special.Aim)
								&& (!Piece.hasEffect(piece, aimEffect))
								&& (piece.energy()>0);
				vpps.canBlast = (special == Special.Blast)
								&& (piece.energy()>0);
				vpps.canShot = (special == Special.Shot)
								&& (piece.energy()>0);
				for(int i=0;i<vpps.plans.length;i++)
				{
					if(damageToTarget[i]==0) continue;
					Piece target=map.getPiece(game.playerTurn^3, i);
					int xHit = target.x();
					int yHit = target.y();
					ArrayList<PlanPoint> goodDestinations=new ArrayList();
					for(PlanPoint possibleDestination : vpps.reachablePoints) //Find points that can reach current target.
					{
						int actions=possibleDestination.actionsLeft;
						if(actions < 1) continue;
						int range = Math.abs(xHit-possibleDestination.x) + Math.abs(yHit-possibleDestination.y);
						if( (range >= minRange && range <= maxRange)
						  || (vpps.canBlast && (range < 5 || (range == 5 && xHit !=possibleDestination.x && yHit != possibleDestination.y)))
						  || (vpps.canShot && range >= 2 && range <=3) )
							goodDestinations.add(possibleDestination);
						//This is a niche enough case I might put it elsewhere. Only for marksmen that have not moved.
						else if(vpps.canBoost && actions>1 && range >= minRange && range <= maxRange+2)
						{
							//NOTE: I need to keep this situation in mind. Also, part of 2-action-marksman assumptions
							PlanPoint clone=(PlanPoint)possibleDestination.clone();
							clone.actionsLeft-=1;
							goodDestinations.add(clone);
						}
					}
					int maxDamage=0;
					for(int j=goodDestinations.size()-1;j>=0;j--)
					{
						PlanPoint point=goodDestinations.get(j);
						int damage=DamageTo(target, point);
						if(damage==0)
							goodDestinations.remove(j);
						if(Math.abs(damage) > Math.abs(maxDamage))
							maxDamage=damage;
					}
					damageToTarget[i]=maxDamage;
					if(maxDamage==0)
						continue;
					
					//TreeSet<SecondPlanPoint> interestingSpots=new TreeSet();
					vpps.plans[i] = new VaguePiecePlan();
					vpps.plans[i].interestingPoint=new boolean[map.width][map.height];
					
					maxDamage=0;
					vpps.plans[i].spps=new SecondPlanPoint[goodDestinations.size()];
					for(PlanPoint destination : goodDestinations)
					{
						int multip=vpps.plans[i].findPathsTo(destination, this);
						if(multip==0)
							continue;
						int damage=DamageTo(target, destination, multip);
						if(Math.abs(maxDamage)<Math.abs(damage))
							maxDamage=damage;
					}
					damageToTarget[i]=maxDamage;
					if(maxDamage==0)
						continue;
				}
			}
			return vpps;
		}

		/*
		protected class MakeMoveOptions
		{
			int actions=piece.actions(); //piece.actions()-1;
			int energy=(special==Special.Jump)?(piece.energy()):0;
			ArrayList<PlanPoint> options=new ArrayList();
			int pieceX=piece.x();
			int pieceY=piece.y();
			int last=0;
			int lastNormal=0;
			PlanPoint source;
			public MakeMoveOptions()
			{
				options.add(new PlanPoint(pieceX, pieceY, actions, 0)); //, energy
				pointMap[pieceX][pieceY]=options.get(0);
				for(;actions>0;actions--)
				{
					int start=options.size()-1;
					for(int i=piece.moveRange()-1;i>=0;i--)
					{
						int startNormal=options.size()-1;
						for(int j=startNormal;j>=lastNormal;j--)
						{
							source=options.get(j);
							doAddEverPosition(source.x+1, source.y, i); //, data
							doAddEverPosition(source.x, source.y+1, i); //, data
							doAddEverPosition(source.x-1, source.y, i); //, data
							doAddEverPosition(source.x, source.y-1, i); //, data
						}
						lastNormal=startNormal+1;
					}
					if(energy>0)
					{
						for(int j=start;j>=last;j--)
						{
							source=options.get(j);
							//if(source.jumpsLeft==0) continue;
							//source.jumpsLeft--;
							DiamondIterator iter=new DiamondIterator(source.x, source.y, 2, 2);
							while(iter.next())
								doAddEverPosition(iter.cX(), iter.cY(), 0); //, data
							//source.jumpsLeft++;
						}
					}
					last=start+1;
				}
				for (PlanPoint P : options)
					pointMap[P.x][P.y]=null;
			}
			protected final void doAddEverPosition(int x, int y, int moves)
			{
				if(!MechanicsLibrary.mayEverPositionAt(game, piece, x, y))
					return;
				PlanPoint P=(PlanPoint)pointMap[x][y];
				if(P!=null)
				{
					P.reachableFrom.add(source);
					return;
				}
				P=new PlanPoint(x, y, actions-1, moves); //, source.jumpsLeft
				P.reachableFrom.add(source);
				options.add(P);
				pointMap[x][y]=P;
			}
		}
		*/
	}
	@Override
	public void run()
	{
		try{
		int myPlayer=game.playerTurn;
		myPotential=new AnalysisForPiece[map.numPiece(myPlayer)];
		enemyPotential=new AnalysisForPiece[map.numPiece(myPlayer^3)];
		Piece.LeaderPiece enemyLeader=null;
		Piece eLC=null;
		int enemyLeaderIndex=-1;
		for(int i=0;i<enemyPotential.length;i++)
		{
			Piece enemyPiece=map.getPiece(myPlayer^3, i);
			enemyPotential[i]=new AnalysisForPiece(enemyPiece);
			Piece other=Piece.unaffectedPiece(enemyPiece);
			if(other instanceof Piece.LeaderPiece)
			{
				eLC=enemyPiece;
				enemyLeader=(Piece.LeaderPiece)other;
				enemyLeaderIndex=i;
				break;
			}
		}
		ArrayList<ExpectedChange> changes=new ArrayList();
		mapOfTakenSpots=new boolean[map.width][map.height];
		pointMap=new Point[map.width][map.height];
		for(int x=0;x<map.width;x++)
			for(int y=0;y<map.height;y++)
			{
				//For now the piece type doesn't matter, so sending null
				mapOfTakenSpots[x][y]=!MechanicsLibrary.mayPositionAt(game, null, x, y);
			}
		//ArrayMap<Piece, Integer> movesLeft=new ArrayMap();
		for(int index=0;index<myPotential.length;index++)
		{
			AnalysisForPiece pot=new AnalysisForPiece(map.getPiece(myPlayer, index), enemyPotential);
			myPotential[index] = pot;
			//movesLeft.put(pot.piece, pot.piece.actions());
		}
		WholeTurnPlan firstPlan=new WholeTurnPlan();
		//firstPlan.moves=new DoubleArray(new Action[myPotential.length][]);
		firstPlan.changes=changes;
		ActionRank bestRank;
		ArrayList<AnalysisForPiece> myPotentialLeft=new ArrayList(myPotential.length);
		myPotentialLeft.addAll(Arrays.asList(myPotential));
		while(myPotentialLeft.size()>0)
		{
			//Get all possible attacks. Choose a 'best' attack.
			{ DamageRank dRank=null; for(int index=0;index<myPotentialLeft.size();index++) {
				//DEAL WITH changes ARRAY
				AnalysisForPiece pot = myPotentialLeft.get(index);
				int i=0;
				if(dRank==null)
				for(;i<enemyPotential.length;i++)
				{
					if(pot.damageToTarget[i]==0) continue;
					Integer left=firstPlan.remainingLife.get(enemyPotential[i].piece);
					if(left!=null && left.intValue()==0) continue; //omae wa mo shindeiru

					FinalSecondPlanPoint point=pot.BestPoint(i, changes, mapOfTakenSpots);
					if(point==null) continue;
					float ratio=((float)Math.abs(point.finalDamage))/Math.abs(pot.damageToTarget[i]);
					dRank=new DamageRank(ratio, point, index, i);
					i++;
					break;
				}
				for(;i<enemyPotential.length;i++)
				{
					if(pot.damageToTarget[i]==0) continue;
					Integer left=firstPlan.remainingLife.get(enemyPotential[i].piece);
					if(left!=null && left.intValue()==0) continue; //omae wa mo shindeiru

					FinalSecondPlanPoint point=pot.BestPoint(i, changes, mapOfTakenSpots);
					if(point==null) continue;
					if(Math.abs(point.finalDamage)<Math.abs(dRank.startPoint.finalDamage)) continue;
					float ratio=((float)Math.abs(point.finalDamage))/Math.abs(pot.damageToTarget[i]);
					if(Math.abs(point.finalDamage)>Math.abs(dRank.startPoint.finalDamage) ||
						ratio > dRank.damageRatio ||
						(ratio==dRank.damageRatio && point.finalActions>dRank.startPoint.finalActions))
					{
						dRank.damageRatio=ratio;
						dRank.startPoint=point;
						dRank.enemyPieceIndex=i;
					}
				}
			} bestRank=dRank; }
			if(bestRank==null)
			{ MoveRank mRank=null; for(int index=0;index<myPotentialLeft.size();index++) {
				AnalysisForPiece myPiece=myPotentialLeft.get(index);
				//TODO: You are here.
				mRank=myPiece.bestMoveTowards(firstPlan, mapOfTakenSpots, index);
			} bestRank=mRank; }
			//TODO: MAKE SURE bestRank IS SET BEFORE THIS
			//Actually no. If there is no bestRank left that means there's not especially anything left I *want* to move.
			if(bestRank!=null)
			{
				bestRank.planAction(firstPlan, myPotentialLeft);
				myPotentialLeft.remove(bestRank.myPieceIndex);
				firstPlan.moves.add(bestRank);
			}
			else
			{
				//TODO: I think this is what I want to do?
				break;
			}
		}
		
		for(ActionRank action : firstPlan.moves)
		{
			action.doAction();
		}
		
		}
		finally{MechanicsLibrary.endTurn(screen);}
	}
	protected static class ScoredPoint extends Point
	{
		int score;
	}
	protected static abstract class ActionRank implements Comparable<ActionRank>
	{
		int myPieceIndex;
		public abstract void planAction(WholeTurnPlan firstPlan, ArrayList<AnalysisForPiece> myPotentialLeft);
		public abstract void doAction();
		@Override
		public abstract int hashCode();
		//@Override
		//public boolean equals(Object O){return super.equals(O);}
	}
	protected class MoveRank extends ActionRank
	{
		PlanPointScore target;
		Piece piece;
		public MoveRank(PlanPointScore t, int i)
		{target=t;myPieceIndex=i;}
		@Override
		public int compareTo(ActionRank rank)
		{
			//TODO
			//MoveRank other=(MoveRank)rank;
			return 0;
		}
		@Override
		public void planAction(WholeTurnPlan firstPlan, ArrayList<AnalysisForPiece> myPotentialLeft)
		{
			AnalysisForPiece myPiece=myPotentialLeft.get(myPieceIndex);
			ArrayList<ExpectedChange> changes=firstPlan.changes;
			piece = myPiece.piece;
			int x=piece.x();
			int y=piece.y();
			changes.add(new ExpectedChange(x, y));
			changes.add(new ExpectedChange(target.x, target.y, piece));
			mapOfTakenSpots[x][y]=false;
			mapOfTakenSpots[target.x][target.y]=true;
			
			int moveRemaining=target.actionsLeft;
			if(moveRemaining>0)
			{
				Piece leftoverPiece = piece.copy();
				Piece.DefaultPiece rootPiece=leftoverPiece.rootPiece();
				rootPiece.x=target.x;
				rootPiece.y=target.y;
				rootPiece.actions=moveRemaining;
				if(rootPiece.pieceType.special==Special.Jump)
					rootPiece.energy=target.jumpsLeft;
				
				AnalysisForPiece leftoverAnalysis=partialPotentials.get(leftoverPiece);
				if(leftoverAnalysis==null)
				{
					leftoverAnalysis=new AnalysisForPiece(leftoverPiece, enemyPotential);
					partialPotentials.put(leftoverPiece, leftoverAnalysis);
				}
				myPotentialLeft.add(leftoverAnalysis);
			}
		}
		@Override
		public void doAction()
		{
			piece=map.getPieceAt(piece.x(), piece.y());
			ArrayList<PlanPointPath> path=target.path();
			int current=path.size()-1;
			if(current <= 0) return;
			int step=piece.moveRange()-1;
			ArrayList<PlanPointPath> targets=new ArrayList();
			ArrayList<SelectAction> actions=new ArrayList();
			//Walk through path, mark each destination tile and type.
			for(int i=0;current>0;i++)
			{
				PlanPointPath source=path.get(current);
				PlanPointPath dest=path.get(current-1);
				if(dest.distanceTo(source)>1)
				{
					//Jump
					if(i>0)
					{
						targets.add(source);
						actions.add(SelectAction.Move);
					}
					targets.add(dest);
					actions.add(SelectAction.Special);
					current--;
					i=-1;
					continue;
				}
				if((--current == 0 && i > 0) || (i == step))
				{
					targets.add(dest);
					actions.add(SelectAction.Move);
					i=-1;
				}
			}
			outerLoop:
			for(int i=0;i<targets.size();i++)
			{
				Point dest=targets.get(i);
				if(screen.AIWholeAction(piece.x(), piece.y(), actions.get(i), dest.x, dest.y)) continue;
				throw new RuntimeException("Desired target is invalid!");
			}
		}
		@Override
		public int hashCode()
		{
			int hash=target.x;
			hash^=(piece.x()<<8);
			hash^=(target.y<<16);
			hash^=(piece.y()<<24);
			return hash;
		}
	}
	protected class DamageRank extends ActionRank
	{
		FinalSecondPlanPoint startPoint;
		Piece piece;
		float damageRatio;
		int enemyPieceIndex;
		Piece enemyPiece;
		public DamageRank(float f, FinalSecondPlanPoint s, int m, int e)
		{damageRatio=f;startPoint=s;myPieceIndex=m;enemyPieceIndex=e;}
		@Override
		public int compareTo(ActionRank rank)
		{
			DamageRank other=(DamageRank)rank;
			int diffDam=startPoint.finalDamage-other.startPoint.finalDamage;
			if(diffDam!=0) return diffDam;
			float diff=damageRatio-other.damageRatio;
			if(diff<0) return -1;
			if(diff>0) return 1;
			return startPoint.finalActions-other.startPoint.finalActions;
		}
		@Override
		public void planAction(WholeTurnPlan firstPlan, ArrayList<AnalysisForPiece> myPotentialLeft)
		{
			AnalysisForPiece myPiece=myPotentialLeft.get(myPieceIndex);
			piece=myPiece.piece;
			ArrayList<ExpectedChange> changes=firstPlan.changes;
			SecondPlanPoint destination=startPoint.destination();
			{
				int x=piece.x();
				int y=piece.y();
				if(!(destination.x==x && destination.y==y))
				{
					changes.add(new ExpectedChange(x, y));
					changes.add(new ExpectedChange(destination.x, destination.y, piece));
					mapOfTakenSpots[x][y]=false;
					mapOfTakenSpots[destination.x][destination.y]=true;
				}
			}

			enemyPiece=map.getPiece(game.playerTurn^3, enemyPieceIndex);
			int lifeLeft;
			{
				Integer tempInt=firstPlan.remainingLife.get(enemyPiece);
				lifeLeft=(tempInt==null)?enemyPiece.health():tempInt.intValue();
				//TODO: Alternative blast AI would go here. Probably.
				if(startPoint instanceof BlastFSPP)
				{

				}
				else
				{
					lifeLeft-=Math.abs(startPoint.finalDamage);
					if(lifeLeft<=0)
					{
						//TODO: Need to be sure to only use current values
						int x=enemyPiece.x();
						int y=enemyPiece.y();
						changes.add(new ExpectedChange(x, y));
						mapOfTakenSpots[x][y]=false;
						int moveRemaining=startPoint.finalActions;
						if(tempInt!=null)
						{
							moveRemaining-=Math.ceil(moveRemaining*tempInt.doubleValue()/Math.abs(startPoint.finalDamage));
							firstPlan.remainingLife.put(enemyPiece,Integer.valueOf(0));
						}
						else
							moveRemaining-=Math.ceil(moveRemaining*((double)enemyPiece.health())/Math.abs(startPoint.finalDamage));
						if(moveRemaining>0)
						{
							Piece leftoverPiece = piece.copy();
							if(aimEffect.isInstance(leftoverPiece))
								leftoverPiece=((Piece.AffectedPiece)leftoverPiece).heldPiece;
							Piece.DefaultPiece rootPiece=leftoverPiece.rootPiece();
							rootPiece.x=startPoint.destination().x;
							rootPiece.y=startPoint.destination().y;
							rootPiece.actions=moveRemaining;
							//TODO: energy
							AnalysisForPiece leftoverAnalysis=partialPotentials.get(leftoverPiece);
							if(leftoverAnalysis==null)
							{
								leftoverAnalysis=new AnalysisForPiece(leftoverPiece, enemyPotential);
								partialPotentials.put(leftoverPiece, leftoverAnalysis);
							}
							myPotentialLeft.add(leftoverAnalysis);
						}
						lifeLeft=0;
					}
					firstPlan.remainingLife.put(enemyPiece, Integer.valueOf(lifeLeft));
				}
			}
		}
		@Override
		public void doAction()
		{
			piece=map.getPieceAt(piece.x(), piece.y());
			//SecondPlanPoint source=startPoint;
			Point source=new Point(piece.x(), piece.y());
			SecondPlanPoint target=startPoint;
			int step=piece.moveRange()-1;
			ArrayList<Point> targets=new ArrayList();
			ArrayList<SelectAction> actions=new ArrayList();
			//Walk through path, mark each destination tile and type.
			for(int i=0;target!=null;i++)
			{
				if(PlanPoint.distanceTo(target, source)>1)
				{
					//Jump
					if(i>0)
					{
						targets.add(source);
						actions.add(SelectAction.Move);
					}
					targets.add(target);
					actions.add(SelectAction.Special);
					source=target;
					target=target.toHere;
					i=-1;
					continue;
				}
				source=target;
				if(((target=target.toHere) == null && i > 0) || (i == step))
				{
					targets.add(source);
					actions.add(SelectAction.Move);
					i=-1;
				}
			}
			outerLoop:
			for(int i=0;i<targets.size();i++)
			{
				Point dest=targets.get(i);
				if (screen.AIWholeAction(piece.x(), piece.y(), actions.get(i), dest.x, dest.y)) continue;
				throw new RuntimeException("Desired target is invalid!");
			}
			if(startPoint instanceof BlastFSPP)
			{
				Point blastTarget = ((BlastFSPP)startPoint).blastTarget;
				screen.AISelect(piece.x(), piece.y());
				Point[] options=screen.AIAction(SelectAction.Special);
				for(Point option : options)
				{
					if(blastTarget.equals(option))
					{
						screen.AITarget(blastTarget.x, blastTarget.y);
						return;
					}
				}
				throw new RuntimeException("Desired target is invalid!");
			}
			int distance=PlanPoint.distanceTo(piece.x(), piece.y(), enemyPiece.x(), enemyPiece.y());
			while(enemyPiece.health()>0 && piece.actions()>0)
			{
				if(piece.attackMaxRange() < distance)
				{
					if(piece.energy()==0)
					{
						System.out.println("Warning: " + piece + " tried to attack " + enemyPiece);
						break;
					}
					if(piece.pieceType().special == Special.Aim)
					{
						if(screen.AIWholeAction(piece.x(), piece.y(), SelectAction.Special, piece.x(), piece.y())) continue;
						throw new RuntimeException("Desired target is invalid (Aim)!");
					}
					else if(piece.pieceType().special == Special.Shot)
					{
						if(screen.AIWholeAction(piece.x(), piece.y(), SelectAction.Special, enemyPiece.x(), enemyPiece.y())) continue;
						throw new RuntimeException("Desired target is invalid (Shot)!");
					}
					else throw new RuntimeException("Impossible to reach target!");
				}
				if(screen.AIWholeAction(piece.x(), piece.y(), SelectAction.Attack, enemyPiece.x(), enemyPiece.y())) continue;
				else throw new RuntimeException("Impossible to attack target?");
			}
		}
		@Override
		public int hashCode()
		{
			Point target=startPoint.destination();
			int hash=target.x;
			hash^=(piece.x()<<8);
			hash^=(target.y<<16);
			hash^=(piece.y()<<24);
			hash^=(enemyPieceIndex<<12);
			return hash;
		}
	}
	protected static class PlanPointPartial extends Point
	{
		public PlanPointPartial(){}
		public PlanPointPartial(int x, int y){super(x, y);}
		public PlanPointPartial(int x, int y, int a, int m, int e){super(x, y);movesLeft=m;actionsLeft=a;jumpsLeft=e;}
		public PlanPointPartial(Point P){super(P);}
		public int actionsLeft;
		public int movesLeft;
		public int jumpsLeft;
		public int distanceTo(Point P)
		{
			return Math.abs(x-P.x)+Math.abs(y-P.y);
		}
		public int distanceTo(int Px, int Py)
		{
			return Math.abs(x-Px)+Math.abs(y-Py);
		}
	}
	protected static class PlanPointPath extends PlanPointPartial
	{
		public PlanPointPath(){}
		public PlanPointPath(int x, int y){super(x, y);}
		public PlanPointPath(int x, int y, int a, int m, int e){super(x, y, a, m, e);}
		public PlanPointPath(Point P){super(P);}
		public PlanPointPath previous;
		public ArrayList<PlanPointPath> path()
		{
			ArrayList<PlanPointPath> path = new ArrayList();
			path.add(this);
			PlanPointPath source=previous;
			while(source!=null)
			{
				path.add(source);
				source=source.previous;
			}
			return path;
		}
	}
	protected static class PlanPointScore extends PlanPointPath implements Comparable<PlanPointScore>
	{
		public int score;
		public int target;
		public PlanPointScore(){}
		public PlanPointScore(int x, int y){super(x, y);}
		public PlanPointScore(int x, int y, int score, int jumps, int target, int actions)
		{super(x, y);this.score=score;actionsLeft=actions;jumpsLeft=jumps;this.target=target;}
		public PlanPointScore(Point P){super(P);}
		@Override
		public int compareTo(PlanPointScore o)
		{
			int value=score-o.score;
			if(value!=0) return value;
			value=o.jumpsLeft-jumpsLeft;
			return value;
		}
	}
	protected static class PlanPointThin extends PlanPointPartial
	{
		public PlanPointThin(){}
		public PlanPointThin(int x, int y){super(x, y);}
		public PlanPointThin(Point P){super(P);}
		public PlanPointThin(int x, int y, int a, int m){super(x, y);movesLeft=m;actionsLeft=a;}
		
		/*     4
		 *  11 0 5
		 *10 3   1 6
		 *   9 2 7
		 *     8
		 * 
		 */
		public static int[] dX=new int[]{0,1,0,-1,0,1,2,1,0,-1,-2,-1};
		public static int[] dY=new int[]{-1,0,1,0,-2,-1,0,1,2,1,0,-1};
		public static int[] flagIndex=new int[]{
			1, //1<<0
			1<<1,
			1<<2,
			1<<3,
			1<<4,
			1<<5,
			1<<6,
			1<<7,
			1<<8,
			1<<9,
			1<<10,
			1<<11,
		};
		public static int[] inverseFlagIndex=new int[]{
			1<<2,
			1<<3,
			1, //1<<0
			1<<1,
			1<<8,
			1<<9,
			1<<10,
			1<<11,
			1<<4,
			1<<5,
			1<<6,
			1<<7,
		};
		public int reachableFlags=0;
	}
	protected static class PlanPoint extends PlanPointPartial
	{
		public PlanPoint(){}
		public PlanPoint(int x, int y){super(x, y);}
		public PlanPoint(Point P){super(P);}
		//public PlanPoint(int a){movesLeft=a;}
		public PlanPoint(int x, int y, int a, int m){super(x, y);movesLeft=m;actionsLeft=a;}
		public PlanPoint(int x, int y, int a, int m, int e){super(x, y);movesLeft=m;actionsLeft=a;jumpsLeft=e;}
		//public PlanPoint(Point P, int a){super(P);movesLeft=a;}
		public ArrayList<PlanPoint> reachableFrom=new ArrayList();
		public PlanPoint[] reachableArray;
		public static int distanceTo(Point P, Point P2)
		{
			return Math.abs(P2.x-P.x)+Math.abs(P2.y-P.y);
		}
		public static int distanceTo(Point P, int x, int y)
		{
			return Math.abs(x-P.x)+Math.abs(y-P.y);
		}
		public static int distanceTo(int x1, int y1, int x, int y)
		{
			return Math.abs(x-x1)+Math.abs(y-y1);
		}
	}
	protected static class AggregateVPP
	{
		//public static ExpectedChange[] NoChange=new ExpectedChange[0];
		VaguePiecePlan[] plans; //Target specific movement plans
		PlanPoint[] reachablePoints; //Full list of positions this piece can theoretically end at this turn
		int[] turnsToReach; //Used for whole-map-move. How many actions it takes to move next to the target, indexed by enemy index.
		//For a charger(3 action 2 move), if this were indexed by distance starting at 0, this will look like 0,1,1,2,2,3,3...
		//except it takes map obstructions into account (not pieces, but permanent rocks or something yes)
		boolean canBoost;
		boolean canShot;
		boolean canBlast;
	}
	//Collection of plans, unique to one of my pieces attacking one enemy piece.
	protected static class VaguePiecePlan
	{
		HashMap<ArrayList<ExpectedChange>, FinalSecondPlanPoint> realDamageToTarget=new HashMap(); //Collection of actually possible options for dealing damage to a target
		int maxActionsLeftover; //Temp variable used for each piece to calculate the maximum times it can attack that piece.
		boolean[][] interestingPoint;
		//PlanPoint[] target;
		SecondPlanPoint[] spps; //List of destinations to move to to attack an enemy from.
		int sppsSize=0;
		boolean hasBlast=false;
		public void addSPP(SecondPlanPoint point)
		{
			spps[sppsSize++]=point;
		}
		//Note: Marksman has a special-case destination, 1 less move to reserve for using special.
		//Start with the destination to attack a piece from, backtrack to find theoretical starting points/paths
		//Populates spps array. Returns maximum number of moves leftover possible.
		public int findPathsTo(PlanPoint destination, AnalysisForPiece piece)
		{
			//list of points to ignore because you could have reached them more easily
			ArrayList<PlanPoint> shortCircuitable=new ArrayList();
			//If we're calling this routine, minInteresting are not set yet. Using them for extra temp memory.
			
			if(piece.piece.x()==destination.x && piece.piece.y()==destination.y)
			{
				addSPP(new FinalSecondPlanPoint(destination.x, destination.y, destination.actionsLeft, 0, this));
				int result=destination.actionsLeft;
				//finishPathsTo(destination, piece);
				return result;
			}
			
			PlanPoint[] from=destination.reachableFrom.toArray(new PlanPoint[destination.reachableFrom.size()]);
			//int added=0;
			for(int i=0;i<from.length;i++)
			{
				PlanPoint point=from[i];
				if(point.distanceTo(destination)>1)
					continue;
				else if(piece.piece.x()==point.x && piece.piece.y()==point.y)
				{
					//shortCircuitable.clear();
					addSPP(new FinalSecondPlanPoint(destination.x, destination.y, destination.actionsLeft-1, 0, this));
					int result=destination.actionsLeft-1;
					//finishPathsTo(destination, piece);
					return result;
				}
				shortCircuitable.add(point);
			}
			//boolean success=false;
			maxActionsLeftover=0;
			SecondPlanPoint firstPoint=null;
			for(int i=0;i<from.length;i++)
			{
				PlanPoint point=from[i];
				boolean jumpSpot=point.distanceTo(destination)>1;
				SecondPlanPoint result=recursePath(0, 0, piece.maxJumps, jumpSpot, piece, point, shortCircuitable);
				if(result==null) continue;
				if(firstPoint==null)
				{
					//success=true;
					firstPoint=new SecondPlanPoint(destination.x, destination.y, from.length-i, this);
					addSPP(firstPoint);
				}
				firstPoint.add(result);
			}
			//finishPathsTo(destination, piece);
			return maxActionsLeftover;
		}
		//Return null if this point cannot be tracked back to a finalsecondplanpoint. Else return a solved
		//SecondPlanPoint.
		public SecondPlanPoint recursePath(int moves, int actions, int jumps, boolean jumpSpot, AnalysisForPiece piece, PlanPoint fromHere, ArrayList<PlanPoint> shortCircuitable)
		{
			if(jumpSpot)
			{
				moves=0;
				actions++;
				jumps--;
				if(jumps<0) return null;
				if(piece.piece.x()==fromHere.x && piece.piece.y()==fromHere.y)
				{
					int actionsLeft=fromHere.actionsLeft-actions;
					//if(moves>fromHere.movesLeft) actionsLeft--;
					if(maxActionsLeftover<actionsLeft) maxActionsLeftover=actionsLeft;
					return new FinalSecondPlanPoint(fromHere.x, fromHere.y, actionsLeft, piece.maxJumps-jumps, this);
				}
			}
			else
			{
				moves++;
				if(moves==1) actions++;
				if(moves==piece.piece.moveRange())
					moves=0;
			}
			if( actions>fromHere.actionsLeft
			  //||(actions==fromHere.actionsLeft && moves>fromHere.movesLeft) //Not really proper logic. TODO: Refine this.
			  )
				return null;
			
			PlanPoint[] from=fromHere.reachableFrom.toArray(new PlanPoint[fromHere.reachableFrom.size()]);
			int i=0;
			int added=0;
			for(;i<from.length;i++)
			{
				PlanPoint point=from[i];
				if(shortCircuitable.contains(point))
				{
					from[i]=null;
					continue;
				}
				if(point.distanceTo(fromHere)>1)
				{
					continue;
				}
				if(piece.piece.x()==point.x && piece.piece.y()==point.y)
				{
					while(added-->0) shortCircuitable.remove(shortCircuitable.size()-1);
					int actionsLeft=point.actionsLeft-actions;
					if(moves==0) actionsLeft--;
					if(maxActionsLeftover<actionsLeft) maxActionsLeftover=actionsLeft;
					return new FinalSecondPlanPoint(fromHere.x, fromHere.y, actionsLeft, piece.maxJumps-jumps, this);
				}
				shortCircuitable.add(point);
				added++;
			}
			//boolean success=false;
			SecondPlanPoint thisSPP=null;// //, moves, actions, jumps);
			
			for(i=0;i<from.length;i++)
			{
				PlanPoint point=from[i];
				if(point==null) continue;
				boolean subJumpSpot=point.distanceTo(fromHere)>1;
				SecondPlanPoint result=recursePath(moves, actions, jumps, subJumpSpot, piece, point, shortCircuitable);
				if(result==null) continue;
				if(thisSPP==null)
				{
					thisSPP=new SecondPlanPoint(fromHere.x, fromHere.y, from.length-i, this);
					//success=true;
				}
				thisSPP.add(result);
			}
			int size=shortCircuitable.size();
			for(Point point:from)
				if(point!=null && fromHere.distanceTo(point)<2) shortCircuitable.remove(--size);
			return thisSPP;
		}
	}
	protected static class BlastFSPP extends FinalSecondPlanPoint
	{
		Point blastTarget;
		public BlastFSPP(FinalSecondPlanPoint original, Point target, int bestDamage)
		{
			this.x=original.x;
			this.y=original.y;
			this.finalActions=original.finalActions;
			this.finalDamage=-bestDamage;
			this.usedJumps=original.usedJumps;
			this.toHere=original.toHere;
			blastTarget=target;
			fromHere=emptyList;
		}
	}
	// 'First step' of a path to a tile to attack an enemy from. No fromHere, can follow toHere to destination.
	protected static class FinalSecondPlanPoint extends SecondPlanPoint
	{
		int finalActions;
		int usedJumps;
		int finalDamage; //This is lazily initialized when requested elsewhere.
		public FinalSecondPlanPoint(){}
		//Marks its spot as an interesting point on the plan. Tracks jumps used (more = worse) and actions left (more=better)
		public FinalSecondPlanPoint(int x, int y, int actions, int jumps, VaguePiecePlan a)
		{
			this.x=x;
			this.y=y;
			finalActions=actions;
			usedJumps=jumps;
			fromHere=emptyList;
			a.interestingPoint[x][y]=true;
		}
		@Override
		public FinalSecondPlanPoint checkPath(boolean[][] mapPassable)
		{
			if(mapPassable[x][y]) return null;
			return this;
		}
		public SecondPlanPoint destination()
		{
			SecondPlanPoint destination=this;
			while(destination.toHere!=null) destination=destination.toHere;
			return destination;
		}
	}
	
	//A point leading to a destination with multiple possible paths leading to it
	protected static class SecondPlanPoint extends Point implements Comparable<SecondPlanPoint>
	{
		public static final SecondPlanPoint[] emptyList=new SecondPlanPoint[0];
		public SecondPlanPoint(){super();}
		int size=0;
		SecondPlanPoint[] fromHere;
		SecondPlanPoint toHere;
		
		//max = How many points might lead to this one. Marks its spot as an interesting point on the plan.
		public SecondPlanPoint(int x, int y, int max, VaguePiecePlan a){
			super(x, y);
			fromHere=new SecondPlanPoint[max];
			a.interestingPoint[x][y]=true;
		}
		
		public void add(SecondPlanPoint newFromHere)
		{
			fromHere[size++]=newFromHere;
			newFromHere.toHere=this;
		}
		@Override
		public int compareTo(SecondPlanPoint other)
		{
			int diff=x-other.x;
			if(diff!=0) return diff;
			diff=y-other.y;
			return diff;
		}
		//Check all paths to this position to see if this spot can be currently reached. Return best path found.
		//Currently only chooses the best (fewest moves no matter how many jumps) option.
		public FinalSecondPlanPoint checkPath(boolean[][] mapPassable)
		{
			if(mapPassable[x][y]) return null;
			//int best=0;
			FinalSecondPlanPoint bestPoint=null;
			for(int i=0;i<size;i++)
			{
				SecondPlanPoint next = fromHere[i];
				FinalSecondPlanPoint nextPoint=next.checkPath(mapPassable);
				if(nextPoint==null) continue;
				if(bestPoint==null)
				{
					if(nextPoint.finalActions>0) bestPoint=nextPoint;
				}
				else if(nextPoint.finalActions>bestPoint.finalActions ||
					(nextPoint.finalActions==bestPoint.finalActions&&nextPoint.usedJumps<bestPoint.usedJumps))
				{
					bestPoint=nextPoint;
					//best=bestPoint.finalActions;
				}
			}
			return bestPoint;
		}
	}
	protected static class ExpectedChange extends Point implements Comparable<ExpectedChange>
	{
		//boolean addPiece=false; //removePiece=true;
		Piece addedPiece=null;
		public ExpectedChange() {
			super(0, 0);
		}
		public ExpectedChange(Point p) {
			super(p.x, p.y);
		}
		public ExpectedChange(int x, int y) {
			super(x, y);
		}
		public ExpectedChange(Point p, Piece addPiece) {
			super(p.x, p.y);
			addedPiece=addPiece;
		}
		public ExpectedChange(int x, int y, Piece addPiece) {
			super(x, y);
			addedPiece=addPiece;
		}
		@Override
		public int hashCode()
		{
			return super.hashCode()^(addedPiece==null?0:addedPiece.hashCode()*31);
		}
		@Override
		public int compareTo(ExpectedChange other)
		{
			int diff=x-other.x;
			if(diff!=0) return diff;
			diff=y-other.y;
			if(diff!=0) return diff;
			diff=(addedPiece==null?0:1)-(other.addedPiece==null?0:1);
			return diff;
		}
		@Override
		public boolean equals(Object other)
		{
			if(!(other instanceof ExpectedChange)) return false;
			ExpectedChange o=(ExpectedChange)other;
			return (o.x==x && o.y==y && o.addedPiece==addedPiece);
		}
	}
	
	protected static class PlanIssue implements Comparable<PlanIssue>
	{
		int totalScore;
		WholeTurnPlan myPlan;
		Issue myIssue;
		public PlanIssue(int score, WholeTurnPlan plan, Issue issue)
		{
			myPlan=plan;
			myIssue=issue;
			totalScore=score-issue.heuristicScoreLoss;
		}
		@Override
		public int compareTo(PlanIssue other)
		{
			return other.totalScore-totalScore;
		}
	}
	protected static abstract class Issue
	{
		int heuristicScoreLoss;
	}
	protected static class BlockedByFriendly extends Issue
	{
	}
	protected static class BlockedByMovedFriendly extends Issue
	{
	}
	protected static class BlockedByEnemy extends Issue
	{
	}
	protected static class HurtByEnemy extends Issue
	{
	}
	protected static class WholeTurnPlan
	{
		int hueristicScore;
		
		//DoubleArray<Action> moves;
		ArrayList<ActionRank> moves=new ArrayList();
		ArrayList<Piece> pieceOrder;
		
		ArrayList<ExpectedChange> changes;
		HashMap<Piece, Integer> remainingLife=new HashMap();
		
		@Override
		public int hashCode()
		{
			int hash=(moves==null)?(0):(moves.hashCode()*31);
			hash+=(pieceOrder==null)?(0):(pieceOrder.hashCode());
			return hash;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final WholeTurnPlan other = (WholeTurnPlan) obj;
			if (!other.moves.equals(moves) || !other.pieceOrder.equals(pieceOrder))
				return false;
			return true;
		}
		public ExpectedChange getChange(int x, int y)
		{
			ExpectedChange potential;
			for(int i = changes.size()-1;i>=0;i--)
				if((potential=changes.get(i)).x == x && potential.y == y)
					return potential;
			return null;
		}
	}
	protected static class DoubleArray<E>
	{
		E[][] array;
		public DoubleArray(E[][] newArray)
		{
			array=newArray;
		}
		@Override
		public int hashCode()
		{
			int hashCode=1;
			for(E[] innerArray : array)
			{
				int subHashCode=1;
				for(E e : innerArray)
				{
					subHashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
				}
				hashCode = 31*hashCode + subHashCode;
			}
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final DoubleArray<E> other = (DoubleArray<E>) obj;
			if (!Arrays.deepEquals(this.array, other.array)) {
				return false;
			}
			return true;
		}
	}
	protected static abstract class MovementLooper<T extends PlanPointPartial>
	{
		//int actions;
		int moves;
		int jumps;
		ArrayList<T> options;
		int start;
		int last;
		int startNormal;
		int lastNormal;
		T source;
		Piece piece;
		int fromIndex;
		
		public void doLoop(T point, Piece myPiece, GameInstance game, boolean doJumps)
		{
		//	doLoop(point, myPiece, game, doJumps, piece.actions());
		//}
		//public void doLoop(T point, Piece myPiece, GameInstance game, boolean doJumps, int startActions)
		//{
			source=point;
			piece=myPiece;
			options=new ArrayList();
			options.add(point);
			boolean stopAtZero=source.jumpsLeft!=0;
			
			for(;checkLoop();) //actions--
			{
				start=options.size()-1;
				for(moves=piece.moveRange()-1;moves>=0;moves--)
				{
					startNormal=options.size()-1;
					for(int j=startNormal;j>=lastNormal;j--)
					{
						source=options.get(j);
						jumps = source.jumpsLeft;
						int x=source.x+1;
						int y=source.y;
						fromIndex=1;
						if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y);
						x=x-2; fromIndex=3;
						if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y);
						x=x+1; y=y+1; fromIndex=2;
						if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y);
						y=y-2; fromIndex=0;
						if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y);
					}
					lastNormal=startNormal+1;
				}
				if(doJumps) for(int j=start;j>=last;j--)
				{
					source=options.get(j);
					if(stopAtZero && source.jumpsLeft==0) continue;
					jumps = source.jumpsLeft-1;
					int x=source.x+2;
					int y=source.y;
					fromIndex=6;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //Right
					x=x-4; fromIndex=10;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //Left
					x=x+1; y=y+1; fromIndex=9;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //DownLeft
					y=y-2; fromIndex=11;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //UpLeft
					x=x+1; y=y-1; fromIndex=4;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //Up
					y=y+4; fromIndex=8;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //Down
					x=x+1; y=y-1; fromIndex=7;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //DownRight
					y=y-2; fromIndex=5;
					if(MechanicsLibrary.mayEverPositionAt(game, piece, x, y)) addPosition(x, y); //UpRight
				}
				last=start+1;
			}

		}
		public abstract boolean checkLoop();
		public abstract void addPosition(int x, int y);
		public void init(Object data){}
	}
}
