package player;

import zombie.*;
import static zombie.Constants.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class Sokie implements Player {

    public static Map<Point, Sokie> myPack = new HashMap<>();
    private PlayerContext context;
    private Move moveDirection;
    private final int PLAYER_X = 8;
    private final int PLAYER_Y = 8;

    private enum DANGER {
        SAFE(0), PROBABLY_SAFE(1), UNSAFE(2), DANGER(3);

        private int value;

        private DANGER(int value) {
            this.value = value;
        }
    }

    @Override
    public Action doTurn(PlayerContext context) {
        Point p = new Point(context.getX(), context.getY());
        myPack.put(p, this);
        this.context = context;

        int friends = 0;
        int deadbodyDistance = Integer.MAX_VALUE;
        Move deadbodyDirection = null;
        Point deadBodyPosition = null;
        Move friendsDirection = Move.SOUTHWEST;

        // Find the closest friend to whom we can move
        int maxDistance = Integer.MAX_VALUE;
        for (Sokie bp : myPack.values()) {
            // Skip ourselves
            if (bp.context.equals(context)) {
                continue;
            }
            Point pos = bp.getPosition();
            int x = pos.x;
            int y = pos.y;
            int distance = Math.max(Math.abs(context.getX() - x),
                    Math.abs(context.getY() - y));
            if (distance < maxDistance) {
                if (canMove(context, (int) Math.signum(x), (int) Math.signum(y))
                        && !isDangerous(context, (int) Math.signum(x),
                        (int) Math.signum(y))) {
                    maxDistance = distance;
                    friendsDirection = Move.inDirection((int) Math.signum(x),
                            (int) Math.signum(y));
                } else {
                    if (canMove(context, (int) Math.signum(0),
                            (int) Math.signum(y))
                            && !isDangerous(context, (int) Math.signum(x),
                            (int) Math.signum(y))) {
                        maxDistance = distance;
                        friendsDirection = Move.inDirection(
                                (int) Math.signum(0), (int) Math.signum(y));
                    } else if (canMove(context, (int) Math.signum(x),
                            (int) Math.signum(0))
                            && !isDangerous(context, (int) Math.signum(x),
                            (int) Math.signum(y))) {
                        maxDistance = distance;
                        friendsDirection = Move.inDirection(
                                (int) Math.signum(x), (int) Math.signum(0));
                    }
                }
            }
        }

        // Find how many friends we have in close vicinity
        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                PlayerId playerAtLocation = context.getPlayField()[x][y];
                if (playerAtLocation != null
                        && playerAtLocation.getName().equals("Sokie")) {
                    friends++;
                }
            }
        }

        // Search for dead bodies
        for (int y = 1; y < VISION_WIDTH - 1; y++) {
            for (int x = 1; x < VISION_WIDTH - 1; x++) {

                PlayerId playerAtLocation = context.getPlayField()[x][y];
                // find a dead body
                if ((playerAtLocation != null)
                        && "DeadBody".equals(playerAtLocation.getName())) {
                    // check adjacent squares for an empty square
                    for (int yy = -1; yy <= +1; yy++) {
                        for (int xx = -1; xx <= +1; xx++) {
                            PlayerId playerNearby = context.getPlayField()[x
                                    + xx][y + yy];
                            if (playerNearby == null) {
                                int distance = max(abs(xx + x
                                        - CENTRE_OF_VISION), abs(yy + y
                                        - CENTRE_OF_VISION));
                                if (distance < deadbodyDistance) {
                                    deadbodyDistance = distance;
                                    deadBodyPosition = getAbsolutePosition(
                                            context, x + xx, y + yy);
                                    deadbodyDirection = Move.inDirection(xx + x
                                            - CENTRE_OF_VISION, yy + y
                                            - CENTRE_OF_VISION);
                                }
                            }
                        }
                    }
                }
            }
        }

        // If we have atleast 2 people close, stay or try to shoot
        // otherwise move randomly, try to find bodies and packs
        if (friends >= 2) {
            // Shoot anybody close
            if (context.getBullets() > 0) {
                int distEnemy = VISION_WIDTH;
                int distZombie = VISION_WIDTH;
                PlayerId targetEnemy = null;
                PlayerId targetZombie = null;
                for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION
                        + SHOOT_RANGE; x++) {
                    for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION
                            + SHOOT_RANGE; y++) {
                        PlayerId player = context.getPlayField()[x][y];
                        if (player != null) {
                            int dist = getDistance(x, y);
                            if (player.getName().equals("Zombie")) {
                                if (dist < distZombie) {
                                    distZombie = dist;
                                    targetZombie = player;
                                }
                            } else if (isEnemy(player.getName())
                                    && dist <= distEnemy) {
                                distEnemy = dist;
                                targetEnemy = context.getPlayField()[x][y];
                            }
                        }
                    }
                }

                if (targetZombie != null && distZombie <= 2) {
                    return new Shoot(targetZombie);
                } else if (targetEnemy != null && distEnemy <= 5) {
                    return new Shoot(targetEnemy);
                }
            }

            for (Sokie bp : myPack.values()) {
                // If someone in the pack has ammo, stay
                if (bp.getAmmo() > 0) {
                    return Move.STAY;
                }
            }

            // If there are bodies close, try to reach them
            int bodyDistance = deadbodyDistance;
            if (deadbodyDistance <= 5) {
                for (Sokie bp : myPack.values()) {
                    int distanceBody = Math.max(
                            Math.abs(deadBodyPosition.x - bp.context.getX()),
                            Math.abs(deadBodyPosition.y - bp.context.getY()));
                    if (deadbodyDistance > distanceBody) {
                        bodyDistance = distanceBody;
                    }
                }
            }
            // If we are not the closest to the body, stay
            if (bodyDistance < deadbodyDistance) {
                return Move.STAY;
            } else {
                return deadbodyDirection;
            }
        } else {
            // We try to reach our closest friend
            // If we are in danger, either fight or run
            if (areWeInDanger(context, PLAYER_X, PLAYER_Y)) {
                if (context.getBullets() > 0) {
                    int distEnemy = VISION_WIDTH;
                    int distZombie = VISION_WIDTH;
                    PlayerId targetEnemy = null;
                    PlayerId targetZombie = null;
                    for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION
                            + SHOOT_RANGE; x++) {
                        for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION
                                + SHOOT_RANGE; y++) {
                            PlayerId player = context.getPlayField()[x][y];
                            if (player != null) {
                                int dist = getDistance(x, y);
                                if (player.getName().equals("Zombie")) {
                                    if (dist < distZombie) {
                                        distZombie = dist;
                                        targetZombie = player;
                                    }
                                } else if (isEnemy(player.getName())
                                        && dist <= distEnemy) {
                                    distEnemy = dist;
                                    targetEnemy = context.getPlayField()[x][y];
                                }
                            }
                        }
                    }

                    if (targetZombie != null && distZombie <= 2) {
                        return new Shoot(targetZombie);
                    } else if (targetEnemy != null && distEnemy <= 5) {
                        return new Shoot(targetEnemy);
                    }
                } else {
                    DANGER danger = DANGER.DANGER;
                    Point position = null;
                    for (int i = -1; i < 1; i++) {
                        for (int j = -1; j < 1; j++) {
                            DANGER positionDanger = getDangerLevel(context,
                                    PLAYER_X + i, PLAYER_Y + j);
                            if (positionDanger.value < danger.value) {
                                if (canMove(context, PLAYER_X + i, PLAYER_Y + j)) {
                                    position = new Point(PLAYER_X + i, PLAYER_Y
                                            + j);
                                }
                            }
                        }
                    }

                    if (position != null) {
                        return Move.inDirection(position.x, position.y);
                    } else {
                        return Move.randomMove();
                    }
                }
            } else {
                return friendsDirection;
            }
        }
        return Move.randomMove();
    }

    private DANGER getDangerLevel(PlayerContext context, int posX, int posY) {
        DANGER danger = DANGER.SAFE;

        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                PlayerId playerAtLocation = context.getPlayField()[x][y];

                if (playerAtLocation != null
                        && isEnemy(playerAtLocation.getName())) {
                    int distanceToPlayer = max(abs(x - posX), abs(y - posY));
                    if (playerAtLocation.getName().equals("Zombie")) {
                        DANGER currentDanger = null;
                        if (distanceToPlayer <= 2) {
                            currentDanger = DANGER.DANGER;
                        } else if (distanceToPlayer <= 5) {
                            currentDanger = DANGER.PROBABLY_SAFE;
                        } else if (distanceToPlayer > 5) {
                            currentDanger = DANGER.SAFE;
                        }
                        if (currentDanger.value > danger.value) {
                            danger = currentDanger;
                        }
                    } else {
                        DANGER currentDanger = null;
                        if (distanceToPlayer <= 5) {
                            currentDanger = DANGER.DANGER;
                        } else if (distanceToPlayer > 5) {
                            currentDanger = DANGER.PROBABLY_SAFE;
                        }
                        if (currentDanger.value > danger.value) {
                            danger = currentDanger;
                        }
                    }
                }
            }
        }
        return danger;
    }

    private boolean isDangerous(PlayerContext context, int posX, int posY) {

        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                PlayerId playerAtLocation = context.getPlayField()[x][y];

                if (playerAtLocation != null
                        && isEnemy(playerAtLocation.getName())) {
                    int distanceToPlayer = max(abs(x - posX), abs(y - posY));
                    if (playerAtLocation.getName().equals("Zombie")) {
                        if (distanceToPlayer <= 2) {
                            return true;
                        }
                    } else {
                        if (distanceToPlayer <= 5) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;

    }

    // calculates absolute position, from XY in our field of view
    private Point getAbsolutePosition(PlayerContext context, int relativeX,
                                      int relativeY) {
        int playerX = context.getX();
        int playerY = context.getY();

        return new Point(playerX + (relativeX - PLAYER_X), playerY
                + (relativeY - PLAYER_Y));
    }

    // Gets distance on the field
    private int getDistance(int x, int y) {
        return Math.max(Math.abs(PLAYER_X - x), Math.abs(PLAYER_Y - y));
    }

    public int getAmmo() {
        return context.getBullets();
    }

    public Point getPosition() {
        Point p = new Point(context.getX(), context.getY());
        return p;
    }

    public Move getMoveDirection() {
        return moveDirection;
    }

    // Quick check for dangers around us
    private boolean areWeInDanger(PlayerContext context, int posX, int posY) {
        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                PlayerId playerAtLocation = context.getPlayField()[x][y];

                if (playerAtLocation != null
                        && isEnemy(playerAtLocation.getName())) {
                    int distanceToPlayer = max(abs(x - posX), abs(y - posY));
                    if (playerAtLocation.getName().equals("Zombie")) {
                        if (distanceToPlayer <= 2) {
                            return true;
                        }
                    } else {
                        if (distanceToPlayer <= 5) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean canMove(PlayerContext context, int posX, int posY) {
        PlayerId playerAtLocation = context.getPlayField()[posX][posY];
        if (playerAtLocation == null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isEnemy(String name) {
        switch (name) {
            case "Sokie":
            case "DeadBody":
            case "GordonFreeman":
            case "EmoWolfWithAGun":
            case "HuddleWolf":
            case "ThePriest":
            case "Shotguneer":
            case "StandStill":
                return false;
            default:
                return true;
        }
    }

}