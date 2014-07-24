package player;

import static java.lang.Math.*;
import java.awt.Point;

import zombie.*;
import static zombie.Constants.*;

public class HideyTwitchy implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        Action action = Move.randomMove();

        Point playerP = getClosestPlayerPoint(context);
        Point corpseP = getClosestCorpsePoint(context);
        Point enemyP = getClosestEnemyPoint(context);

        if (isWithinArea(playerP, Constants.SHOOT_RANGE, Constants.SHOOT_RANGE)) {
            //player spotted within 5x5
            if (context.getBullets() > 0) {
                action = getShootAction(playerP, context); //shoot!
            } else {
                action = getMoveAwayFromPoint(playerP); //run!
            }
        } else if (isWithinArea(enemyP, Constants.VISION_RANGE, Constants.VISION_RANGE)) {
            //players or zombie spotted within 8x8
            action = getMoveAwayFromPoint(enemyP); //run!
        } else if (isWithinArea(corpseP, Constants.VISION_RANGE, Constants.VISION_RANGE)) {
            //corpse spotted within 8x8
            if (context.getBullets() == 0) {
                action = getMoveTowardsPoint(corpseP); //loot corpse!
            }
        } else {
            //randomly move
            action = Move.randomMove();
        }

        return action;
    }

    private Move getMoveTowardsPoint(Point p) {
        return Move.inDirection((int)p.getX() - CENTRE_OF_VISION, (int)p.getY() - CENTRE_OF_VISION);
    }

    private Move getMoveAwayFromPoint(Point p) {
        return Move.inDirection(CENTRE_OF_VISION - (int)p.getX(), CENTRE_OF_VISION - (int)p.getY());
    }

    private Shoot getShootAction(Point p, PlayerContext context) {
        PlayerId id = context.getPlayField()[(int) p.getX()][(int) p.getY()];
        Shoot shootAction = new Shoot(id);

        return shootAction;
    }

    private boolean isWithinArea(Point p, int x, int y) {
        return p != null
                && abs(CENTRE_OF_VISION - p.getX()) <= x
                && abs(CENTRE_OF_VISION - p.getY()) <= y;
    }

    private Point getClosestEnemyPoint(PlayerContext context) {
        String[] lookFor = {};
        String[] avoid = {Dead.DEADBODYNAME};
        Point p = getClosestEntity(context, lookFor, avoid);

        return p;
    }

    private Point getClosestPlayerPoint(PlayerContext context) {
        String[] lookFor = {};
        String[] avoid = {Dead.DEADBODYNAME, Dead.ZOMBIENAME};
        Point p = getClosestEntity(context, lookFor, avoid);

        return p;
    }

    private Point getClosestCorpsePoint(PlayerContext context) {
        String[] lookFor = {Dead.DEADBODYNAME};
        String[] avoid = {Dead.ZOMBIENAME};
        Point p = getClosestEntity(context, lookFor, avoid);

        return p;
    }

    private Point getClosestEntity(PlayerContext context, String[] lookFor, String[] avoid) {

        int bestDistance = Integer.MAX_VALUE;
        Point closestPoint = null;

        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {


                PlayerId playerAtLocation = context.getPlayField()[x][y];

                if (playerAtLocation != null && !playerAtLocation.equals(context.getId())) {
                    //not empty and not me

                    boolean conditionsMet = true;
                    for (String lookForName : lookFor) {
                        conditionsMet |= playerAtLocation.getName().equals(lookForName);
                    }

                    for (String avoidName : avoid) {
                        conditionsMet &= !playerAtLocation.getName().equals(avoidName);
                    }

                    if (conditionsMet) {
                        int distance = max(abs(x - CENTRE_OF_VISION), abs(y - CENTRE_OF_VISION));
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            closestPoint = new Point(x, y);
                        }
                    }
                }
            }
        }

        return closestPoint;
    }
}