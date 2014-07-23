package player;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import zombie.*;
import static zombie.Constants.*;

public class ZombieHater implements Player {
    private static final Set<PlayerId> emptyDeadBodies = new HashSet<>();
    private static final Map<PlayerId, Point> lastPos = new HashMap<>();

    @Override
    public Action doTurn(PlayerContext context) {
        PlayerId[][] field = context.getPlayField();
        Point myPos = new Point(context.getX(), context.getY());
        PlayerId myId = context.getId();

        // update dead bodies with the new empty ones
        addEmptyBodies(field);

        // shoot nearest zombie if possible
        if (context.getBullets() > 0) {
            PlayerId nearestZombie = getNearestEnemy(field);
            if (nearestZombie != null) {
                lastPos.remove(myId);
                return new Shoot(nearestZombie);
            }
        }

        // stuck, mostly because of dead body
        if (lastPos.containsKey(myId) && lastPos.get(myId).equals(myPos)) {
            return Move.randomMove();
        }

        // walk towards dead bodies
        Point nearestDeadBody = getNearestDeadBody(field);
        if (nearestDeadBody != null) {
            Move move = Move.inDirection(nearestDeadBody.x - CENTRE_OF_VISION, nearestDeadBody.y - CENTRE_OF_VISION);
            lastPos.put(myId, myPos);
            return move;
        }

        lastPos.remove(myId);
        return Move.randomMove();
    }

    // add surrounding dead bodies to empty bodies
    private void addEmptyBodies(PlayerId[][] field) {
        for (Move move : Move.values()) {
            PlayerId player = field[CENTRE_OF_VISION + move.x][CENTRE_OF_VISION + move.y];
            if (player != null && "DeadBody".equals(player.getName())) {
                emptyDeadBodies.add(player);
            }
        }
    }

    // distance from centre, for example 5 if x=7 and y=3
    private int distanceFromCentre(int x, int y) {
        int dx = Math.abs(CENTRE_OF_VISION - x);
        int dy = Math.abs(CENTRE_OF_VISION - y);
        return Math.max(dx, dy);
    }

    // return nearest enemy or null if none exists
    private PlayerId getNearestEnemy(PlayerId[][] field) {
        int minOffset = Integer.MAX_VALUE;
        PlayerId nearestEnemy = null;
        for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
            for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                int offset = distanceFromCentre(x, y);
                PlayerId player = field[x][y];
                if (player != null && isEnemy(player.getName()) && offset < minOffset) {
                    minOffset = offset;
                    nearestEnemy = field[x][y];
                }
            }
        }
        return nearestEnemy;
    }

   // return nearest dead body or null if none exists
    private Point getNearestDeadBody(PlayerId[][] field) {
        int minOffset = Integer.MAX_VALUE;
        Point nearestDeadBody = null;
        for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
            for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                int offset = distanceFromCentre(x, y);
                PlayerId player = field[x][y];
                if (player != null && "DeadBody".equals(player.getName()) && offset < minOffset && 
                        !emptyDeadBodies.contains(player)) {
                    minOffset = offset;
                    nearestDeadBody = new Point(x, y);
                }
            }
        }
        return nearestDeadBody;
    }

    private boolean isEnemy(String name) {
        switch (name) {
            case "ZombieHater":
            case "DeadBody":
            case "EmoWolfWithGun": // don't bother shooting him
                return false;
            default:
                return true;
        }
    }
}