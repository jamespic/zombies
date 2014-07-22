package player;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;
import static zombie.Constants.*;
import zombie.*;

public class ZombieHater implements Player {
    private static final Set<Point> emptyDeadBodies = new HashSet<>();

    @Override
    public Action doTurn(PlayerContext context) {
        Point centre = new Point(context.getX(), context.getY());
        PlayerId[][] field = context.getPlayField();

        // update dead bodies with the new empty ones
        addEmptyBodies(field, centre);

        // shoot nearest zombie if possible
        if (context.getBullets() > 0) {
            PlayerId nearestZombie = getNearestZombie(field);
            if (nearestZombie != null) {
                return new Shoot(nearestZombie);
            }
        }

        // walk towards dead bodies
        Point nearestDeadBody = getNearestDeadBody(field, centre);
        if (nearestDeadBody != null) {
            return Move.inDirection(nearestDeadBody.x - CENTRE_OF_VISION, nearestDeadBody.y - CENTRE_OF_VISION);
        }

        return Move.randomMove();
    }

    // add surrounding dead bodies to empty bodies
    private void addEmptyBodies(PlayerId[][] field, Point me) {
        for (Move move : Move.values()) {
            PlayerId player = field[CENTRE_OF_VISION + move.x][CENTRE_OF_VISION + move.y];
            if (player != null && "DeadBody".equals(player.getName())) {
                emptyDeadBodies.add(new Point(me.x + move.x, me.y + move.y));
            }
        }
    }

    // distance from centre, for example 5 if x=1 and y=3
    private int distanceFromCentre(int x, int y) {
        int dx = Math.abs(CENTRE_OF_VISION - x);
        int dy = Math.abs(CENTRE_OF_VISION - y);
        return Math.max(dx, dy);
    }

    // return nearest zombie or null if none exists
    private PlayerId getNearestZombie(PlayerId[][] field) {
        int minOffset = Integer.MAX_VALUE;
        PlayerId nearestZombie = null;
        for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
            for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                int offset = distanceFromCentre(x,y);
                if (field[x][y] != null && "Zombie".equals(field[x][y].getName()) && offset < minOffset) {
                    minOffset = offset;
                    nearestZombie = field[x][y];
                }
            }
        }
        return nearestZombie;
    }

   // return nearest dead body or null if none exists
    private Point getNearestDeadBody(PlayerId[][] field, Point centre) {
        int minOffset = Integer.MAX_VALUE;
        Point nearestDeadBody = null;
        for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
            for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                int offset = distanceFromCentre(x,y);
                Point absPos = new Point(centre.x + x - CENTRE_OF_VISION, centre.y + y - CENTRE_OF_VISION);
                if (field[x][y] != null && "DeadBody".equals(field[x][y].getName()) && offset < minOffset && 
                        !emptyDeadBodies.contains(absPos)) {
                    minOffset = offset;
                    nearestDeadBody = new Point(x,y);
                }
            }
        }
        return nearestDeadBody;
    }
}