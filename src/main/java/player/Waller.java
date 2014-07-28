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

    private class Point{
        public int X;
        public int Y;

        public Point(int x, int y) {
            X = x;
            Y = y;
        }

        public boolean SameLocation(Point otherPoint) {
            return (X == otherPoint.X && Y == otherPoint.Y);
        }

        public int GetDistance(Point point) {
            return Math.max(Math.abs(X - point.X), Math.abs(Y - point.Y));
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + this.X;
            hash = 71 * hash + this.Y;

            return hash;
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

    public final Point CurrentLocation = new Point(CENTRE_OF_VISION, CENTRE_OF_VISION);;

    @Override
    public Action doTurn(PlayerContext context) {
        PlayerId[][] field = context.getPlayField();
        int bullets = context.getBullets();

        Action action = AssessThreats(field, bullets);
        if(action != null)
        {
            return action;
        }

        int currentWallCount = countNumberOfSurroundingWalls(field, CENTRE_OF_VISION, CENTRE_OF_VISION);

        switch(currentWallCount) {
            case 8:
                return Move.STAY; // no more moving
            case 7:
            case 6:
            case 5:
            case 4:
            case 3:
            case 2:
                action = ExpandWall(field, bullets);
                if(action != null) {
                    return action;
                }
                break;
            // Try to complete the wall
            default:
                break;
        }

        Set<Point> optimalLocations = findOptimalPositions(field, currentWallCount);

        if(!optimalLocations.isEmpty()) {
            Move moveAction = findShortestPath(field, CurrentLocation, optimalLocations);

            if(moveAction == Move.STAY) {
                action = ExpandWall(field, bullets);
                if(action != null)
                    return action;
            }

            return moveAction;
        }

        return Move.STAY;
    }

    private Action ExpandWall(PlayerId[][] field, int bullets) {
        if(bullets > 0) {
            for(int x = CENTRE_OF_VISION - 1; x <= CENTRE_OF_VISION + 1; x++) {
                for(int y = CENTRE_OF_VISION - 1; y <= CENTRE_OF_VISION + 1; y++) {
                    PlayerId player = field[x][y];
                    if(isEnemy(player)) {
                        return new Shoot(player);
                    }
                }
            }
        }
        return null;
    }

    private Action AssessThreats(PlayerId[][] field, int bullets){
        // Will need to improve (i.e. zombies aren't a threat if they cannot get to an adjacent square in one turn (unless there are multiple))
        if(bullets > 0) {
            List<PlayerId> zombies = new ArrayList<PlayerId>();

            for(int x = CENTRE_OF_VISION - 4; x <= CENTRE_OF_VISION + 4; x++) {
                for(int y = CENTRE_OF_VISION - 4; y <= CENTRE_OF_VISION + 4; y++) {
                    PlayerId player = field[x][y];
                    if(isZombie(player)){
                        LinkedList<Point> path = findShortestPath_astar(field, new Point(x,y), CurrentLocation);
                        if(path.isEmpty())
                            continue;
                        if(path.size() <= 2) {
                            zombies.add(player);
                        }
                    }
                }
            }

            if(zombies.size() > 0) {
                return new Shoot(zombies.get(0));
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
            for(int x = currentPoint.X - 1; x <= currentPoint.X + 1; x++) {
                for(int y = currentPoint.Y - 1; y <= currentPoint.Y + 1; y++) {
                    Point pointToAdd = new Point(x, y);
                    if(!validatePoint(field, pointToAdd) || isWall(field[x][y]) || closedSet.contains(pointToAdd))
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

    private boolean validatePoint(PlayerId[][] field, Point point) {
        if(point == null)
            return false;
        int x = point.X;
        int y = point.Y;
        if(x < 0 || x >= VISION_WIDTH || y < 0 || y >= VISION_WIDTH) {
            return false;
        }
        return true;
    }

    private boolean isZombie(PlayerId player) {
        return (player != null && player.getName().equals("Zombie"));
    }

    private boolean isWall(PlayerId player) {
        if(player == null)
            return false;
        switch (player.getName()) {
            case "DeadBody":
                return true;
            default:
                return false;
        }
    }

    private boolean isEnemy(PlayerId player) {
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

    private final int getDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    private final int getDistance(int x, int y) {
        return Math.max(Math.abs(CENTRE_OF_VISION - x), Math.abs(CENTRE_OF_VISION - y));
    }
}