package player;

import java.lang.Math.*;
import java.util.Set;
import java.util.HashSet;
import zombie.*;
import static zombie.Constants.*;

public class Coward implements Player {

    private static final Set<PlayerId> killed = new HashSet<>();

    @Override
    public Action doTurn(PlayerContext context) {

        PlayerId[][] field = context.getPlayField();

        // Panic and shoot
        if (context.getBullets() > 0) {
            int distEnemy = VISION_WIDTH;
            int distZombie = VISION_WIDTH;
            PlayerId targetEnemy = null;
            PlayerId targetZombie = null;
            int zombieCount = 0;
            for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
                for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                    PlayerId player = field[x][y];
                    if (player != null && !killed.contains(player)) {
                        int dist = getDistance(x, y);
                        if (player.getName().equals("Zombie")) {
                            if( dist < distZombie ) {
                                distZombie = dist;
                                targetZombie = player;
                                zombieCount = 1;
                            } else if( dist == distZombie ) {
                                zombieCount += 1;
                            }
                        } else if (isEnemy(player.getName()) && dist <= distEnemy ) {
                            distEnemy = dist;
                            targetEnemy = field[x][y];
                        }
                    }
                }
            }

            if (targetZombie != null && distZombie <= 3 && zombieCount == 1) {
                killed.add(targetZombie);
                return new Shoot( targetZombie );
            } else if (targetEnemy != null && distEnemy <= 5 ) {
                killed.add(targetEnemy);
                return new Shoot( targetEnemy );
            }
        }

        // Run away
        int bestScore = -999999;
        Move bestMove = Move.randomMove();

        for( int x = -1; x <= 1; x++ ) {
            for( int y = -1; y <= 1; y++ ) {
                int thisScore = 0;
                for( int xx = -(VISION_RANGE-1); xx < VISION_RANGE; xx++ ) {
                    for( int yy = -(VISION_RANGE-1); yy < VISION_RANGE; yy++ ) {
                        PlayerId player = field[CENTRE_OF_VISION+x+xx][CENTRE_OF_VISION+y+yy];
                        if( player != null && player != context.getId()) {
                            thisScore -= 8 * Math.abs(context.getX()+x-50); // Join the crusade!
                            thisScore -= 8 * Math.abs(context.getY()+y-50);
                            int dist = getDistance(xx-x,yy-y);

                            if( dist == 0 ) { // Dont run into stuff
                                thisScore = -999999;
                            } else if( player.getName().equals("Coward")) { // Prefer lose groups
                                if( dist >= 3 && dist <= 6 ) {
                                    thisScore += 16;
                                } else if( dist < 3 ) {
                                    thisScore += 8;
                                }
                            } else if( player.getName().equals("DeadBody") && dist == 1) { // Visit dead bodies on the route
                                thisScore += 1;
                            } else if( player.getName().equals("Zombie")) { // Avoid zombies
                                if( dist <= 3 ) {
                                    thisScore -= 999999;
                                } else if( dist <= 4 ) {
                                    thisScore -= 50000;
                                }
                            } else if( isEnemy(player.getName())) { // Avoid strangers
                                if( dist <= 6 ) {
                                    thisScore -= 100000;
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
                return false;
            default:
                return true;
        }
    }

    private int getDistance(int x, int y) {
        return Math.max(Math.abs(CENTRE_OF_VISION - x), Math.abs(CENTRE_OF_VISION - y));
    }
}