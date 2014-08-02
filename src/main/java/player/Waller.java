package player;

import java.lang.Math.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import zombie.*;
import static zombie.Constants.*;

public class Waller implements Player {

    private static final int MaximumDistanceToShootZombie = 2;
    private static final int PointsPerWall = 3;
    private static final int PointsPerLoot = 3;
    private static final int PointsPerZombie = -500;
    private static final int PointsPerAggressor = -500;  

    private static final Set<PlayerId> shooting = new HashSet<PlayerId>();
    private static final Set<PlayerId> dontLoot = new HashSet<PlayerId>();
    private static final Set<Point> zombieLocations = new HashSet<Point>();
    private Point CurrentLocation = new Point(CENTRE_OF_VISION, CENTRE_OF_VISION);

    private static int _lastGameTurn = -1;

    // DEBUG
    private static boolean _DEBUG = false;
    private static int agressiveKills;
    private static int zombieKills;
    private static int wallsBuilt;
    ////////

    private static class Point{
        public int X;
        public int Y;
        public PlayerId Player;
        public int Distance;

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

        public List<Point> getAdjacentPoints(PlayerId[][] field, int distance, boolean includeSelf) {
            List<Point> points = new ArrayList<Point>();
            for(int x = X - distance; x <= X + distance; x++) {
                for(int y = Y - distance; y <= Y + distance; y++) { 
                    if(!includeSelf && x == X && y == Y)
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

        @Override
        public String toString(){
            return "("+X+","+Y+")";
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

        // Mark all adjacent dead players as already been looted
        for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, 1)){
            if(point.Player.getName().equals("DeadBody")) 
                dontLoot.add(point.Player);  
        }

        int x = context.getX();
        int y = context.getY();
        int boardSize = context.getBoardSize();
        List<Point> newZombies = new ArrayList<Point>();
        for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, VISION_RANGE)){     
            Point absolutePoint = GetNewTorusPoint(x + point.X - CENTRE_OF_VISION , y + point.Y - CENTRE_OF_VISION, boardSize);         
            if(point.Player.getName().equals("DeadBody") && zombieLocations.contains(absolutePoint)) 
                dontLoot.add(point.Player);  // new zombie kill
            if(isZombie(point.Player))
                newZombies.add(absolutePoint);
        }
        zombieLocations.clear();
        zombieLocations.addAll(newZombies);

        Action action;  

        // 1) Handle immediate threats to life, have to be dealt before anything else
        action = AssessThreats(field, bullets);
        if(action != null) return action;

        //2) Early turn avoidance
        if(gameTurn < 5) {
            action = EarlyTurn(field, bullets, context);
            if(action != null) return action;
        }

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
            case 6: 
            case 5:              
            case 4: 
                // action = ExpandWall(field, bullets, 2);
                // if(action != null) return action; 
                // break;
            case 2: 
            case 1: 
            default:                                    
                break;
        }                       

        // 2) Score each possible square and find the best possible location(s)
        Set<Point> optimalLocations = scoreSquares(field);  

        action = findShortestPath(field, CurrentLocation, optimalLocations);
        if(action != null) return action;

        action = ShootAgressivePlayers(field, bullets);
        if(action != null) return action;   

        action = ExpandWall(field, bullets, 1);
        if(action != null) return action;    

        // Stay still if nothing better to do
        return Move.STAY;
    }

    private Action EarlyTurn(PlayerId[][] field, int bullets, PlayerContext context) {
        Point bestPoint = CurrentLocation;
        double bestScore = 1000000;

        for(Point futurePoint : CurrentLocation.getAdjacentPoints(field, 1, true)) {            
            double score = 0;
            for(Point adjacentPoint : futurePoint.getAdjacentPoints(field, VISION_RANGE, false)) {
                if(isAgressive(adjacentPoint.Player)){
                    int dist = futurePoint.GetDistance(adjacentPoint);          
                    if(dist > 6){
                        score += 1;             
                    } else {
                        score += 10000;
                    }
                } else if(isZombie(adjacentPoint.Player)) {
                    int dist = futurePoint.GetDistance(adjacentPoint);      
                    if (dist <= 3)
                        score += 10000;
                } else if(isWall(adjacentPoint.Player)) {
                    score -= 2;
                }
            }   
            if(score < bestScore) {
                bestScore = score;
                bestPoint = futurePoint;
            }
        }                           

        //if(_DEBUG) System.out.println("["+_lastGameTurn+"] Best Score: "+bestScore +" point: "+context.getX()+","+context.getY());

        if(bestPoint == CurrentLocation) {
            Action action = ShootAgressivePlayers(field, bullets);
            if(action != null) return action;   
            return Move.STAY;
        }

        if(bestScore >= 1000) {
            Action action = ShootAgressivePlayers(field, bullets);
            if(action != null) return action;   
        }

        return Move.inDirection(bestPoint.X - CurrentLocation.X, bestPoint.Y - CurrentLocation.Y);      
    }

    private Action ShootAgressivePlayers(PlayerId[][] field, int bullets) {
        if(bullets > 0) {       
            for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, SHOOT_RANGE)) {
                PlayerId player = point.Player;
                if(isAgressive(player) && shouldShoot(player)) {
                    if(_DEBUG) System.out.println("["+_lastGameTurn+"] Killing Aggressive: "+(++agressiveKills));       
                    return new Shoot(player);
                }           
            }   
        }
        return null;
    }

    private Action ExpandWall(PlayerId[][] field, int bullets, int distance) {
        if(bullets > 0) {
            for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, distance)) {
                PlayerId player = point.Player;
                if(!isWall(player) && isEnemy(player) && !isZombie(player) && shouldShoot(player)) {
                    if(_DEBUG) System.out.println("["+_lastGameTurn+"] Expanding Wall: "+(++wallsBuilt)+" Dist: "+CurrentLocation.GetDistance(point));          
                    return new Shoot(player);
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
        List<Point> bestZombies = new ArrayList<Point>();
        int smallestDistance = MaximumDistanceToShootZombie+1;      
        for(Point point : getSurrounding(field, CENTRE_OF_VISION, CENTRE_OF_VISION, MaximumDistanceToShootZombie)) {
            PlayerId zombie = point.Player;
            if(isZombie(zombie)) {              
                LinkedList<Point> path = findShortestPath_astar(field, CurrentLocation, point, false, false);               
                if(path.isEmpty()) 
                    continue;  
                if(path.size() <= smallestDistance && canShoot(zombie)) {
                    if(path.size() < smallestDistance) {
                        smallestDistance = path.size();
                        bestZombies.clear();
                    }
                    bestZombies.add(point);                                                                                            
                }    
            }
        }

        // No zombies to worry about
        if(bestZombies.isEmpty())
            return null;

        if(bestZombies.size() > 1) {
            if(_DEBUG) System.out.println("["+_lastGameTurn+"] Multiple Zombies in striking range, wait them out?");        
            return MoveToBestSpot(field);   
        }

        Point zombie = bestZombies.get(0);

        // Do we have ammo?
        if(bullets > 0 && shouldShoot(zombie.Player)) { 
            if(_DEBUG) System.out.println("["+_lastGameTurn+"] Shooting Zombie: "+(++zombieKills));             
            return new Shoot(zombie.Player);
        } 

        if(_DEBUG) System.out.println("["+_lastGameTurn+"] No Bullets to Shoot Zombie! Should flee");           
        return MoveInDirection(field, CENTRE_OF_VISION - zombie.X, CENTRE_OF_VISION - zombie.Y);    
    }

    private Action MoveToBestSpot(PlayerId[][] field) { 
        int leastZombies = 100000;
        Point bestPoint = CurrentLocation;
        for(Point point : CurrentLocation.getAdjacentPoints(field, 1, false)) {
            if(point.Player == null) {
                int zombies = countNumberOfSurroundingZombies(field, point.X, point.Y);
                if(zombies < leastZombies) {
                    leastZombies = zombies;
                    bestPoint = point;
                }
            }
        }
        return Move.inDirection(bestPoint.X - CurrentLocation.X, bestPoint.Y - CurrentLocation.Y);
    }

    private Action MoveInDirection(PlayerId[][] field, int x, int y) {
        x = (int)Math.signum(x);
        y = (int)Math.signum(y);

        if(y == 0){
            if(field[CENTRE_OF_VISION+x][CENTRE_OF_VISION] != null)
                return Move.inDirection(x,0);
            if(field[CENTRE_OF_VISION+x][CENTRE_OF_VISION-1] != null)
                return Move.inDirection(x,-1);
            if(field[CENTRE_OF_VISION+x][CENTRE_OF_VISION+1] != null)
                return Move.inDirection(x,1);   
        } else if(x == 0){
            if(field[CENTRE_OF_VISION][CENTRE_OF_VISION+y] != null)
                return Move.inDirection(0,y);
            if(field[CENTRE_OF_VISION-1][CENTRE_OF_VISION+y] != null)
                return Move.inDirection(-1,y);
            if(field[CENTRE_OF_VISION+1][CENTRE_OF_VISION+y] != null)
                return Move.inDirection(1,y);   
        } else {        
            if(field[CENTRE_OF_VISION+x][CENTRE_OF_VISION+y] != null)
                return Move.inDirection(x,y);
            if(field[CENTRE_OF_VISION+x][CENTRE_OF_VISION] != null)
                return Move.inDirection(x,0);
            if(field[CENTRE_OF_VISION][CENTRE_OF_VISION+y] != null)
                return Move.inDirection(0,y);   
        }

        return Move.inDirection(0,0);   
    }

    // Implementation of the A* path finding algorithm
    private LinkedList<Point> findShortestPath_astar(PlayerId[][] field, Point startingPoint, Point finalPoint, boolean includeWeights, boolean considerPlayersAsWalls) {   
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
            for(Point pointToAdd : currentPoint.getAdjacentPoints(field, 1, false)){                            
                if(closedSet.contains(pointToAdd) || isWall(pointToAdd.Player) || (considerPlayersAsWalls && pointToAdd.Player != null && !pointToAdd.SameLocation(finalPoint) )) 
                    continue;

                int gScore = gScores.get(currentPoint) + 1; // distance should always be one (may change depending on environment)  
                // if(includeWeights){
                    // gScore += (int)-getScore(field,pointToAdd.X,pointToAdd.Y);
                // }   

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

    private Action findShortestPath(PlayerId[][] field, Point startingPoint, Set<Point> finalPoints) {    
        if(finalPoints.isEmpty())
            return null;
        int smallestPath = 10000;       
        Point pointToMoveTo = startingPoint;  

        for(Point finalPoint : finalPoints) {  
            if(finalPoint == startingPoint)
                return null;
            LinkedList<Point> path = findShortestPath_astar(field, startingPoint, finalPoint, true, true);

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

        if(pointToMoveTo == startingPoint)
            return null;

        double score = getScore(field, pointToMoveTo.X, pointToMoveTo.Y);
        if(score < -200) {
            if(_DEBUG) System.out.println("["+_lastGameTurn+"] Best Path leads to a bad spot: "+score);     
            return null;
        }

        return Move.inDirection(pointToMoveTo.X - startingPoint.X, pointToMoveTo.Y - startingPoint.Y);          
    }

    private Set<Point> scoreSquares(PlayerId[][] field) {
        double bestScore = getScore(field, CENTRE_OF_VISION, CENTRE_OF_VISION) + 1; // plus one to break ties, and would rather stay
        Set<Point> bestLocations = new HashSet<Point>();
        if(bestScore >= 0) {
            bestLocations.add(CurrentLocation);         
        } else {
            bestScore = 0;
        }

        for(int x = 0; x < VISION_WIDTH; x++){
            for(int y = 0; y < VISION_WIDTH; y++){   
                if(x == CENTRE_OF_VISION && y == CENTRE_OF_VISION) continue;
                if(field[x][y] == null) {                                 
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
        }       
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
        for(Point point : getSurrounding(field, x, y, MaximumDistanceToShootZombie+1)){         
            if(isZombie(point.Player)){
                LinkedList<Point> path = findShortestPath_astar(field, currentPoint, point, false, false);
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
            if(isWall(player) && !dontLoot.contains(player)){   
                loots++;                    
            }            
        }
        return loots;   
    }

    private int countNumberOfSurroundingAggressions(PlayerId[][] field, int x, int y) {     
        int aggressors = 0; 
        for(Point point : getSurrounding(field, x, y, SHOOT_RANGE+1)){
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
        return player != null && player.getName().equals("DeadBody");       
    }

    private static boolean isEnemy(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {  
            case "Waller":
            case "DeadBody": 
            case "EmoWolfWithAGun":
                return false;
            default:
                return true;
        }
    }

    private static boolean isAgressive(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {  
            case "Waller":
            case "DeadBody":   
            case "EmoWolfWithAGun":
            case "GordonFreeman":
            case "Vortigaunt": 
            case "StandStill":
            case "MoveRandomly":
            case "Zombie":
                return false;
            default:
                return true;
        }
    }

    // Helper Functions 

    private List<Point> getSurrounding(PlayerId[][] field, int x, int y, int maxDistance) {      
        final Point currentPoint = new Point(x,y);

        List<Point> players = new ArrayList<Point>();
        int minX = coercePoint(x - maxDistance);
        int maxX = coercePoint(x + maxDistance);
        int minY = coercePoint(y - maxDistance);
        int maxY = coercePoint(y + maxDistance);
        for(int i = minX; i <= maxX; i++){
            for(int j = minY; j <= maxY; j++) {
                if(i == x && j == y) continue;
                if(field[i][j] != null) {                
                    Point point = new Point(i,j,field[i][j]);
                    point.Distance = currentPoint.GetDistance(point);
                    players.add(point);
                }
            }
        }           

        Collections.sort(players, new Comparator<Point>() {
            public int compare(Point p1, Point p2) {
                return Integer.compare(p1.Distance, p2.Distance);          
            }});        

        return players;
    }

    private static int coercePoint(int value) {
        if(value < 0)
            return 0;
        if(value >= VISION_WIDTH)
            return VISION_WIDTH-1;
        return value;
    }

    public static Point GetNewTorusPoint(int x, int y, int boardSize) {
        if(x >= boardSize)
            x = boardSize - x;
        if(y >= boardSize)
            y = boardSize - y;
        return new Point(x,y);
    }

    private static int getDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
}