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
	private static final Set<PlayerId> shooting = new HashSet<PlayerId>();
	private final Point CurrentLocation = new Point(CENTRE_OF_VISION, CENTRE_OF_VISION);
	private final Point North = new Point(CENTRE_OF_VISION,0);
	private final Point East = new Point(VISION_WIDTH-1,CENTRE_OF_VISION);
	private final Point South = new Point(CENTRE_OF_VISION,VISION_WIDTH-1);
	private final Point West = new Point(0,CENTRE_OF_VISION);	
	
	private static int _lastGameTurn = -1;
	
	// DEBUG
	private static boolean _DEBUG = true;
	private static int agressiveKills;
	private static int zombieKills;
	private static int wallsBuilt;
	////////
	
    private class Point{
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
			return X*100 + Y;          
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
		int gameTurn = context.getGameClock();	
		
		if(gameTurn != _lastGameTurn){
			_lastGameTurn = gameTurn;				
		}
		
        PlayerId[][] field = context.getPlayField();         
        int bullets = context.getBullets();
		
		Action action;
		
		// 1) Handle immediate threats to life, have to be dealt before anything else
		action = AssessThreats(field, bullets);
        if(action != null) return action;
       		
		if(gameTurn < 6) {
			//action = EvadeAgressivePlayers(field);
			//if(action != null) return action; 
			
			action = ShootAgressivePlayers(field, bullets);
			if(action != null) return action;             
		}
		    
		
        int currentWallCount = countNumberOfSurroundingWalls(field, CENTRE_OF_VISION, CENTRE_OF_VISION);

        switch(currentWallCount) {  
            case 8:		
				action = ShootAgressivePlayers(field, bullets);
				if(action != null) return action; 
				return Move.STAY; // no more moving					
            case 7:
				action = ShootAgressivePlayers(field, bullets);
				if(action != null) return action; 
				
				action = ExpandWall(field, bullets);
                if(action != null) return action; 	                  
                    
                return Move.STAY; // no more moving		
            case 6:
            case 5:
            case 4:
				action = ExpandWall(field, bullets);
                if(action != null) return action;          
            case 3:  
			case 2: 
			case 1: 
            default:
				 
                // Try to complete the wall
                break;
        }                       

        Set<Point> optimalLocations = findOptimalPositions(field, currentWallCount);

        if(!optimalLocations.isEmpty()) {       
            Move moveAction = findShortestPath(field, CurrentLocation, optimalLocations);

            if(moveAction == Move.STAY) {
                action = ExpandWall(field, bullets);
                if(action != null) return action;  
				
				action = ShootAgressivePlayers(field, bullets);
				if(action != null) return action; 
            }          
			
			return moveAction;
        }
		
		// Move in a direction where there are the most walls
		if(currentWallCount < 3){
				
			// Set<Point> directions = new HashSet<Point>();
			// directions.add(North);
			// directions.add(East);
			// directions.add(West);
			// directions.add(South);
			
			// Move moveAction = findShortestPath(field, CurrentLocation, directions);
			// return moveAction;
			//return Move.randomMove();
		}
		
		action = ShootAgressivePlayers(field, bullets);
		if(action != null) return action; 
		return Move.STAY;
    }
		
	private Action EvadeAgressivePlayers(PlayerId[][] field, int width) {		
		int sumX = 0;
		int sumY = 0;	
	
		int aggressors = 0;
		for(int x = CENTRE_OF_VISION - width; x <= CENTRE_OF_VISION + width; x++) {
			for(int y = CENTRE_OF_VISION - width; y <= CENTRE_OF_VISION + width; y++) {
				PlayerId player = field[x][y];
				if(isAgressive(player)) {     		
					sumX += CENTRE_OF_VISION - x;
					sumY += CENTRE_OF_VISION - y;	
					aggressors++;					
				}
			}
		}
		
		if(aggressors == 0)
			return null;
			
		return Move.inDirection(sumX, sumY);     
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

    private Action ExpandWall(PlayerId[][] field, int bullets) {
        if(bullets > 0) {
            for(int x = CENTRE_OF_VISION - 1; x <= CENTRE_OF_VISION + 1; x++) {
                for(int y = CENTRE_OF_VISION - 1; y <= CENTRE_OF_VISION + 1; y++) {
                    PlayerId player = field[x][y];
                    if(isEnemy(player) && shouldShoot(player)) {    
						if(_DEBUG) System.out.println("["+_lastGameTurn+"] Expanding Wall: "+(++wallsBuilt));			
                        return new Shoot(player);
                    }
                }
            }
        }
        return null;
    }

	private boolean shouldShoot(PlayerId player) {
		return shooting.add(player);		
	}
	
	private boolean canShoot(PlayerId player) {
		return !shooting.contains(player);		
	}
		
    private Action AssessThreats(PlayerId[][] field, int bullets){          
        if(bullets > 0) {
           
		    PlayerId bestZombie = null;
			int smallestDistance = MaximumDistanceToShootZombie+1;
			// Check for zombies approaching
            for(int x = CENTRE_OF_VISION - MaximumDistanceToShootZombie; x <= CENTRE_OF_VISION + MaximumDistanceToShootZombie; x++) {
                for(int y = CENTRE_OF_VISION - MaximumDistanceToShootZombie; y <= CENTRE_OF_VISION + MaximumDistanceToShootZombie; y++) {
                    PlayerId zombie = field[x][y];
                    if(isZombie(zombie)){                   
                        LinkedList<Point> path = findShortestPath_astar(field, new Point(x,y), CurrentLocation);
                        if(path.isEmpty()) 
                            continue;                       
                        if(path.size() <= MaximumDistanceToShootZombie && canShoot(zombie) && path.size() < smallestDistance) {		
							bestZombie = zombie;
							smallestDistance = path.size();												                           
                        }           
                    }               
                }       
            }
			
			if(bestZombie != null && shouldShoot(bestZombie)) {			
				if(_DEBUG) System.out.println("["+_lastGameTurn+"] Shooting Zombie: "+(++zombieKills));				
				return new Shoot(bestZombie);		
			}
        }   
			

        return null;        
    }

    // Implementation of the A* path finding algorithm
    private LinkedList<Point> findShortestPath_astar(PlayerId[][] field, Point startingPoint, Point finalPoint) {   

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
            LinkedList<Point> path = findShortestPath_astar(field, startingPoint, finalPoint);

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

        return Move.inDirection(pointToMoveTo.X - startingPoint.X, pointToMoveTo.Y - startingPoint.Y);          
    }

    private Set<Point> findOptimalPositions(PlayerId[][] field, int currentWallCount) {     
        int bestWallCount = currentWallCount + 1;
        Set<Point> bestLocations = new HashSet<Point>();

        for(int x = 0; x < VISION_WIDTH; x++){
            for(int y = 0; y < VISION_WIDTH; y++){
                int walls = countNumberOfSurroundingWalls(field, x, y);
                if(walls >= bestWallCount) {                
                    if(walls > bestWallCount) {
                        bestLocations.clear();
                    }
                    bestLocations.add(new Point(x, y));
                    bestWallCount = walls;                  
                }
            }
        }
        return bestLocations;
    }

    private int countNumberOfSurroundingWalls(PlayerId[][] field, int x, int y) {
        PlayerId currentPosition = field[x][y];

        // Ignore places that are already walls.
        if(isWall(currentPosition)){ 
            return 0;
        }

        int wallCount = 0;
        for(int i = x - 1; i <= x + 1; i++) {
            if(i < 0 || i >= VISION_WIDTH){
                continue;
            }
            for(int j = y - 1; j <= y + 1; j++) {
                if(j<0 || j >= VISION_WIDTH) {
                    continue;
                }
                if(isWall(field[i][j])) {
                    wallCount++;
                }
            }
        }

        return wallCount;           
    }
  
    private static boolean isZombie(PlayerId player) {
        return (player != null && player.getName().equals("Zombie"));
    }

    private static boolean isWall(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {           
            case "DeadBody":  			
			case "StandStill":
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
			case "Coward":
                return true;
            default:
                return false;
        }
    }
}