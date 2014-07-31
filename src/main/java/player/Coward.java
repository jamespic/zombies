package player;

import java.lang.Math.*;
import java.util.Set;
import java.util.HashSet;
import zombie.*;
import static zombie.Constants.*;

public class Coward implements Player {

    private static final Set<PlayerId> killed = new HashSet<>();
    private static final Set<PlayerId> looted = new HashSet<>();

    @Override
    public Action doTurn(PlayerContext context) {

            PlayerId[][] field = context.getPlayField();

            // Panic and shoot
            if (context.getBullets() > 0) {
                    int distEnemy = VISION_WIDTH;
                    int distZombie = VISION_WIDTH;
                    PlayerId targetEnemy = null;
                    PlayerId targetZombie = null;
                    for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
                            for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                                    PlayerId player = field[x][y];
                                    if (player != null && !killed.contains(player)) {
                                            int dist = getDistance(x, y);
                                            if (player.getName().equals("Zombie")) {
                                                    if( dist < distZombie ) {
                                                            distZombie = dist;
                                                            targetZombie = player;
                                                    }
                                            } else if (isEnemy(player.getName()) && dist <= distEnemy ) {
                                                    distEnemy = dist;
                                                    targetEnemy = field[x][y];
                                            }
                                    }
                            }
                    }

                    if (targetZombie != null && distZombie <= 3) {
                            killed.add(targetZombie);
                            return new Shoot( targetZombie );
                    } else if (targetEnemy != null && distEnemy <= 5 ) {
                            killed.add(targetEnemy);
                            return new Shoot( targetEnemy );
                    }
            }

            // Run away
            int bestScore = -10000000;
            Move bestMove = Move.randomMove();

            for( int x = -1; x <= 1; x++ ) {
                    for( int y = -1; y <= 1; y++ ) {
                            PlayerId center = field[CENTRE_OF_VISION+x][CENTRE_OF_VISION+y];
                            if( center != null && !looted.contains(center) && center.getName().equals("DeadBody")) {
                                    looted.add(center);
                            }
                            if( center == null ) {
                                    int thisScore = 0;
                                    for( int xx = CENTRE_OF_VISION+x-VISION_RANGE+1; xx < CENTRE_OF_VISION+x+VISION_RANGE; xx++ ) {
                                            for( int yy = CENTRE_OF_VISION+y-VISION_RANGE+1; yy < CENTRE_OF_VISION+y+VISION_RANGE; yy++ ) {
                                                    PlayerId player = field[xx][yy];
                                                    if( player != null) {
                                                            int dist = getDistance(xx-x,yy-y);

                                                            if( player.getName().equals("Coward")) { // Prefer lose groups
                                                                    if( dist >= 3 && dist <= 6 ) {
                                                                            thisScore += 32;
                                                                    } else if( dist > 3 ) {
                                                                            thisScore += 16;
                                                                    }
                                                            } else if( player.getName().equals("DeadBody")) { // Visit dead bodies on the route
                                                                    if( !looted.contains(player)) {
                                                                            thisScore += 32*(VISION_RANGE-dist);
                                                                    }
                                                            } else if( player.getName().equals("Zombie")) { // Avoid zombies
                                                                    if( dist <= 2 ) {
                                                                            thisScore -= 10000;
                                                                    } else if( dist <= 3 ) {
                                                                            thisScore -= 1000;
                                                                    } else if( dist <= 4 ) {
                                                                            thisScore -= 100;
                                                                    }
                                                            } else if( isEnemy(player.getName())) { // Avoid strangers
                                                                    if( dist == 7 ) {
                                                                            thisScore -= 100;
                                                                    } else if( dist <= 6 ) {
                                                                            thisScore -= 1000;
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                                    if( thisScore > bestScore ) {
                                            bestScore = thisScore;
                                            bestMove = Move.inDirection( x, y );
                                    }
                            }
                    }
            }

            return bestMove;
    }

    private boolean isEnemy(String name) {
        switch (name) {
            case "Coward":
            case "DeadBody":
            case "GordonFreeman":
            case "EmoWolfWithAGun":
            case "HuddleWolf":
            case "ThePriest":
            case "Shotguneer":
            case "Vortigaunt":
            case "Fox":
            case "Cocoon":
            case "SuperCoward":
                return false;
            default:
                return true;
        }
    }

    private int getDistance(int x, int y) {
        return Math.max(Math.abs(CENTRE_OF_VISION - x), Math.abs(CENTRE_OF_VISION - y));
    }
}