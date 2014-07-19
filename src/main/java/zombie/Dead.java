package zombie;

import static zombie.Constants.*;
import static java.lang.Math.*;
import java.util.concurrent.ThreadLocalRandom;

public enum Dead implements Player {
    DEADBODY {
        @Override
        public Action doTurn(PlayerContext context) {
            return Move.STAY;
        }
    },
    ZOMBIE {
        @Override
        public Action doTurn(PlayerContext context) {
            // Find nearest non-zombie player
            Move bestDirection = Move.randomMove();
            int bestDistance = Integer.MAX_VALUE;
            for (int x = 0; x < VISION_WIDTH; x++) {
                for (int y = 0; y < VISION_WIDTH; y++) {
                    int distance = max(abs(x - CENTRE_OF_VISION), abs(y - CENTRE_OF_VISION));
                    PlayerId playerAtLocation = context.getPlayField()[x][y];
                    if (playerAtLocation != null
                            && !PlayerRegistry.isDeadOrUndead(playerAtLocation.getName())
                            && distance < bestDistance) {
                        bestDistance = distance;
                        bestDirection = Move.inDirection(x - CENTRE_OF_VISION, y - CENTRE_OF_VISION);
                    }
                }
            }
            return bestDirection;
        }
    };
    
    public static final String DEADBODYNAME = "DeadBody";
    public static final String ZOMBIENAME = "Zombie";
}
