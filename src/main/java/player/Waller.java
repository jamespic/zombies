package player;

import java.lang.Math.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import zombie.*;
import static zombie.Constants.*;

public class Waller implements Player {

	private static final int MaximumDistanceToShootZombie = 2;
	private static final int PointsPerWall = 3;
	private static final int PointsPerLoot = 2;
	private static final int PointsPerZombie = -50;
	private static final int PointsPerAggressor = -30;	

	private static final Set<PlayerId> shooting = new HashSet<PlayerId>();
	private static final Set<PlayerId> dontLoot = new HashSet<PlayerId>();
	private Point CurrentLocation = new Point(CENTRE_OF_VISION, CENTRE_OF_VISION);
	
	private static int _lastGameTurn = -1;
	
	// DEBUG
	private static boolean _DEBUG = true;
	private static int agressiveKills;
	private static int zombieKills;
	private static int wallsBuilt;
	////////

    private final class Point{
        public int X;
        public int Y;
        public PlayerId Player;

        public Point(int x, int y) {
            X = x;
            Y = y;
        }

        public Point(int x, int y, PlayerId player) {
            X = x;
            Y = y;
            Player = player;
        }

        public boolean SameLocation(Point otherPoint) {
            return X == otherPoint.X && Y == otherPoint.Y;
        }

        public List<Point> getAdjacentPoints(PlayerId[][] field) {
            List<Point> points = new ArrayList<Point>();
            for(int x = X - 1; x <= X + 1; x++) {
                for(int y = Y - 1; y <= Y + 1; y++) { 
                    if(x == X && y == Y)
                        continue;
                    Point pointToAdd = new Point(x, y);                 
                    if(pointToAdd.isValid()) {
                        pointToAdd.Player = field[x][y];
                        points.add(pointToAdd);
                    }
                }
            }                   
            return points;
        }


        public int GetDistance(Point point) {
            return Math.max(Math.abs(X - point.X), Math.abs(Y - point.Y));
        }

        private boolean isValid() { 
            return X >= 0 && X < VISION_WIDTH && Y >= 0 && Y < VISION_WIDTH;
        }

        @Override
        public int hashCode() {
			return (X*100) + Y;  
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Point))
                return false;
            if (obj == this)
                return true;

            return SameLocation((Point) obj);       
        }

    }
      
    @Override
    public Action doTurn(PlayerContext context) {				
		CurrentLocation = new Point(CENTRE_OF_VISION, CENTRE_OF_VISION);
		int gameTurn = context.getGameClock();	
		
		if(gameTurn != _lastGameTurn){
			_lastGameTurn = gameTurn;				
		}
		
        PlayerId[][] field = context.getPlayField();         
        int bullets = context.getBullets();

		// Mark all adjacent dead players as already been looted
		for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, 1)){
			if(point.Player.getName().equals("DeadBody")) 
				dontLoot.add(point.Player);  
		}
		
		Action action;		

		// 1) Handle immediate threats to life, have to be dealt before anything else
		action = AssessThreats(field, bullets);
        if(action != null) return action;
	  			
        int currentWallCount = countNumberOfSurroundingWalls(field, CENTRE_OF_VISION, CENTRE_OF_VISION);
		
        switch(currentWallCount) {  
            case 8:		
				action = ShootAgressivePlayers(field, bullets);
				if(action != null) return action; 
				return Move.STAY; // no more moving					
            case 7:		
				action = ExpandWall(field, bullets, 1);
                if(action != null) return action;
				action = ShootAgressivePlayers(field, bullets);
				if(action != null) return action; 
                return Move.STAY; // no more moving		
         	case 6:	
			case 5:			     
            case 4:	
				action = ExpandWall(field, bullets, 1);
                if(action != null) return action; 	 
				break;     			
            case 3:  
				action = ExpandWall(field, bullets, 2);
                if(action != null) return action;    
				break;
			case 2: 
			case 1: 
            default:					
                // Try to complete the wall
                break;
        }                       
      
		// 2) Score each possible square and find the best possible location(s)
		Set<Point> optimalLocations = scoreSquares(field);
	  
        if(!optimalLocations.isEmpty()) {       
            Move moveAction = findShortestPath(field, CurrentLocation, optimalLocations);

            if(moveAction != Move.STAY) {
               return moveAction;
            }
        }		

		if(bullets < 3) {
			action = ExpandWall(field, bullets, 5);
			if(action != null) return action; 	      
		} else {
			action = ExpandWall(field, bullets, 2);
			if(action != null) return action; 	        
		}
		action = ShootAgressivePlayers(field, bullets);
		if(action != null) return action; 	
		return Move.STAY;
    }

    private Action ShootAgressivePlayers(PlayerId[][] field, int bullets) {
        if(bullets > 0) {
            for(int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
                for(int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                    PlayerId player = field[x][y];
                    if(isAgressive(player) && shouldShoot(player)) {     					
						if(_DEBUG) System.out.println("["+_lastGameTurn+"] Killing Aggressive: "+(++agressiveKills));	
                        return new Shoot(player);
                    }
                }
            }
        }
        return null;
    }

    private Action ExpandWall(PlayerId[][] field, int bullets, int distance) {
        if(bullets > 0) {
            for(int x = CENTRE_OF_VISION - distance; x <= CENTRE_OF_VISION + distance; x++) {
                for(int y = CENTRE_OF_VISION - distance; y <= CENTRE_OF_VISION + distance; y++) {
                    PlayerId player = field[x][y];
                    if(!isWall(player) && isEnemy(player) && !isZombie(player) && shouldShoot(player)) {    
						if(_DEBUG) System.out.println("["+_lastGameTurn+"] Expanding Wall: "+(++wallsBuilt));			
                        return new Shoot(player);
                    }
                }
            }
        }
        return null;
    }

	private boolean shouldShoot(PlayerId player) {
		boolean result = shooting.add(player);
		if(result && isZombie(player)){
			dontLoot.add(player);			
		} 		
		return result;		
	}
	
	private boolean canShoot(PlayerId player) {
		return !shooting.contains(player);		
	}
		
    private Action AssessThreats(PlayerId[][] field, int bullets){          
     
		// Find the most threatening zombie		
		PlayerId bestZombie = null;
		int smallestDistance = MaximumDistanceToShootZombie+1;		
		for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, MaximumDistanceToShootZombie)) {
			PlayerId zombie = point.Player;
			if(isZombie(zombie)) {				
				LinkedList<Point> path = findShortestPath_astar(field, CurrentLocation, point, false);				
				if(path.isEmpty()) 
					continue;  
				if(path.size() < smallestDistance && canShoot(zombie)) {		
					bestZombie = zombie;
					smallestDistance = path.size();												                           
				}    
			}
		}
		// No zombies
		if(bestZombie == null)
			return null;		
	
		// Do we have ammo?
		if(bullets > 0 && shouldShoot(bestZombie)) {					
			if(_DEBUG) System.out.println("["+_lastGameTurn+"] Shooting Zombie: "+(++zombieKills));				
			return new Shoot(bestZombie);					
        } 
		
		
		
		// We should flee		
		return null;				        

    }

    // Implementation of the A* path finding algorithm
    private LinkedList<Point> findShortestPath_astar(PlayerId[][] field, Point startingPoint, Point finalPoint, boolean includeScoring) {   

        LinkedList<Point> foundPath = new LinkedList<Point>();
        Set<Point> openSet = new HashSet<Point>();
        Set<Point> closedSet = new HashSet<Point>();
        Hashtable<Point, Integer> gScores = new Hashtable<Point, Integer>();
        Hashtable<Point, Point> cameFrom = new Hashtable<Point, Point>();

        gScores.put(startingPoint, 0);
        openSet.add(startingPoint);
        Point currentPoint = startingPoint;

        while(!openSet.isEmpty()) {

            // Find minimum F score
            int minF = 10000000;
            for(Point point : openSet) {
                int g = gScores.get(point);
				int h = point.GetDistance(finalPoint); // Assumes nothing in the way
				if(includeScoring){
					h += (int)-getScore(field,point.X,point.Y);
				}               
                int f = g + h;
                if(f < minF) {
                    minF = f;               
                    currentPoint = point;
                }           
            }

            // Found the final point
            if(currentPoint.SameLocation(finalPoint)) {                 
                Point curr = finalPoint;
                while(!curr.SameLocation(startingPoint)) {
                    foundPath.addFirst(curr);
                    curr = cameFrom.get(curr);
                }
                return foundPath;
            }

            openSet.remove(currentPoint);
            closedSet.add(currentPoint);            

            // Add neighbouring squares
			for(Point pointToAdd : currentPoint.getAdjacentPoints(field)){    
						
				if(isWall(pointToAdd.Player) || (pointToAdd.Player != null && !pointToAdd.SameLocation(finalPoint)) || closedSet.contains(pointToAdd)) 
					continue;
					
				int gScore = gScores.get(currentPoint) + 1; // distance should always be one (may change depending on environment)  
				boolean distIsBetter = false;   

				if(!openSet.contains(pointToAdd)) {
					openSet.add(pointToAdd);
					distIsBetter = true;
				} else if(gScore < gScores.get(pointToAdd)){                    
					distIsBetter = true;
				}
				if(distIsBetter) {
					gScores.put(pointToAdd, gScore);
					cameFrom.put(pointToAdd, currentPoint);                     
				}
			}  
        }

        return foundPath;   
    }

    private Move findShortestPath(PlayerId[][] field, Point startingPoint, Set<Point> finalPoints) {            
        int smallestPath = 10000;       
        Point pointToMoveTo = CurrentLocation;  

        for(Point finalPoint : finalPoints) {           
            LinkedList<Point> path = findShortestPath_astar(field, startingPoint, finalPoint, true);

            // No path between the two points
            if(path.isEmpty()){
                continue;
            }

            // Check if this is the smallest path
            if(path.size() < smallestPath) {                
                smallestPath = path.size();             
                pointToMoveTo = path.getFirst();                
            }           
        }   	

		if(pointToMoveTo.SameLocation(CurrentLocation))
			return Move.STAY;
	
        return Move.inDirection(pointToMoveTo.X - startingPoint.X, pointToMoveTo.Y - startingPoint.Y);          
    }
	
	private Set<Point> scoreSquares(PlayerId[][] field) {
		double bestScore = getScore(field, CENTRE_OF_VISION, CENTRE_OF_VISION) + 1;
		Set<Point> bestLocations = new HashSet<Point>();
		
		for(int x = 0; x < VISION_WIDTH; x++){
            for(int y = 0; y < VISION_WIDTH; y++){			
				if(isWall(field[x][y]) || field[x][y] != null) //|| !isMovableTo(field, x, y)
					continue;					
				double score = getScore(field, x, y);			
				if(score >= bestScore){
					if(score > bestScore) {
						bestLocations.clear();
						bestScore = score;   
					}
                    bestLocations.add(new Point(x, y));                      
				}
			}
		}
		//if(_DEBUG) System.out.println("Best Score: "+bestScore +" # of Spots: "+bestLocations.size());
		return bestLocations;
	}
			
	private double getScore(PlayerId[][] field, int x, int y) {
		int walls = countNumberOfSurroundingWalls(field, x, y);
		double score = Math.pow(PointsPerWall, walls);		
		int aggressors = countNumberOfSurroundingAggressions(field, x, y);
		score += aggressors * PointsPerAggressor;	
		int zombies = countNumberOfSurroundingZombies(field, x, y);
		score += zombies * PointsPerZombie;
		int loots = countNumberOfSurroundingLoots(field, x, y);
		score += Math.pow(PointsPerLoot, loots);
		return score;		
	}
	
	private int countNumberOfSurroundingZombies(PlayerId[][] field, int x, int y) {     
        int zombies = 0;
		Point currentPoint = new Point(x,y);
		for(Point point : getSurrounding(field, x, y, MaximumDistanceToShootZombie)){			
			if(isZombie(point.Player)){
				LinkedList<Point> path = findShortestPath_astar(field, currentPoint, point, false);
				if(path.isEmpty()) 
					continue; 
				if(path.size() < MaximumDistanceToShootZombie+1)
					zombies++;					
			}            
        }
        return zombies;           
    }
	
	private int countNumberOfSurroundingLoots(PlayerId[][] field, int x, int y) {     
		int loots = 0;	
		for(Point point : getSurrounding(field, x, y, 1)){
			PlayerId player = point.Player;
			if(player.getName().equals("DeadBody") && !dontLoot.contains(player)){	
				loots++;					
			}            
        }
        return loots; 	
	}
	
	private int countNumberOfSurroundingAggressions(PlayerId[][] field, int x, int y) {     
        int aggressors = 0;	
		for(Point point : getSurrounding(field, x, y, SHOOT_RANGE)){
			if(isAgressive(point.Player)){
				aggressors++;					
			}            
        }
        return aggressors;           
    }

    private int countNumberOfSurroundingWalls(PlayerId[][] field, int x, int y) {
		int walls = 0;		
		for(Point point : getSurrounding(field, x, y, 1)){
			if(isWall(point.Player)){
				walls++;					
			}            
        }

        return walls;
    }

    private static boolean isZombie(PlayerId player) {
        return player != null && player.getName().equals("Zombie");
    }

    private static boolean isWall(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {  
            case "DeadBody":  		
			case "StandStill":
			case "Waller":
                return true;
            default:
                return false;
        }
    }

    private static boolean isEnemy(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {  
            case "Waller":
            case "DeadBody":       
			case "StandStill":	
                return false;
            default:
                return true;
        }
    }

    private static boolean isAgressive(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {  
            case "Shotguneer":
            case "HideyTwitchy":   
			case "ZombieHater":
			case "Gunner":
			case "ZombieRightsActivist":	
			case "Fox":
			case "Sokie":			
			case "Coward":
                return true;
            default:
                return false;
        }
    }
	
	// Helper Functions
	
	private List<Point> getSurrounding(PlayerId[][] field, int x, int y, int distance) {
		int minX = coercePoint(x - distance);
		int maxX = coercePoint(x + distance);
		int minY = coercePoint(y - distance);
		int maxY = coercePoint(y + distance);	
        List<Point> players = new ArrayList<Point>();
        for(int i = minX; i <= maxX; i++) {         
            for(int j = minY; j <= maxY; j++) { 
				if(i == x && j == y)
					continue;
				if(field[i][j] != null) {								
					players.add(new Point(i,j,field[i][j]));
				}
            }
        }
		return players;
	}
		
	private static int coercePoint(int value) {
		if(value < 0)
			return 0;
		if(value >= VISION_WIDTH)
			return VISION_WIDTH-1;
		return value;
	}
	
	private static int getDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
}

