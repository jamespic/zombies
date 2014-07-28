package player;

import java.lang.Math.*;
import java.util.Set;
import java.util.HashSet;
import zombie.*;
import static zombie.Constants.*;

public class Fox implements Player {

    private static int lastround = -1;
    private static final Set<PlayerId> killed = new HashSet<>();
    private static final Set<PlayerId> looted = new HashSet<>();

    @Override
    public Action doTurn(PlayerContext context) {

        PlayerId[][] field = context.getPlayField();

        // Cleanup
        if (context.getGameClock() > lastround) {
            lastround = context.getGameClock();
            killed.clear();
        }

        // Snipe
        if (context.getBullets() > 0) {
            int distEnemy = 1;
            PlayerId targetEnemy = null;
            for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
                for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                    PlayerId player = field[x][y];
                    if (player != null && !killed.contains(player)) {
                        int dist = getDistance(x, y);
                        if (!player.getName().equals("Zombie") && isEnemy(player.getName()) && dist >= distEnemy ) {
                            distEnemy = dist;
                            targetEnemy = field[x][y];
                        }
                    }
                }
            }
            if (targetEnemy != null) {
                killed.add(targetEnemy);
                return new Shoot( targetEnemy );
            }
        }

        // Check Foxhole
        int foxhole = 0;
        PlayerId target = null;

        for( int x = -2; x <= 2; x++ ) {
            for( int y = -2; y <= 2; y++ ) {
                PlayerId player = field[CENTRE_OF_VISION+x][CENTRE_OF_VISION+y];
                if (player != null && getDistance(CENTRE_OF_VISION+x,CENTRE_OF_VISION+y) == 2) {
                    if (player.getName().equals("DeadBody") || player.getName().equals("Fox")) {
                        foxhole++;
                    }
                    if( player.getName().equals("Zombie")) {
                        target = player;
                    }
                }
            }
        }

        if (context.getBullets() + foxhole >= 16) {
            if (target!=null) {
                return new Shoot( target );
            } else {
                return Move.STAY;
            }
        }

        // Collect bullets
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

                                if( player.getName().equals("DeadBody")) {
                                    if( !looted.contains(player)) {
                                        thisScore += 32*(VISION_RANGE-dist);
                                    }
                                } else if( player.getName().equals("Zombie")) {
                                    if( dist < 3 ) {
                                        thisScore -= 10000;
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
            case "Fox":
            case "Coward":
            case "DeadBody":
            case "GordonFreeman":
            case "EmoWolfWithAGun":
            case "HuddleWolf":
            case "ThePriest":
//            case "Shotguneer":
            case "Vortigaunt":
                return false;
            default:
                return true;
        }
    }

    private int getDistance(int x, int y) {
        return Math.max(Math.abs(CENTRE_OF_VISION - x), Math.abs(CENTRE_OF_VISION - y));
    }
}