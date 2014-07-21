package player;

import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class HuddleWolf implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        if (context.getBullets() > 0) {
            for (PlayerId player: context.shootablePlayers()) {
                switch(player.getName()) {
                    case "HuddleWolf":
                        break;
                    case "DeadBody":
                        break;
                    case "Gunner":
                        return new Shoot(player);
                    case "Zombie" :
                        return new Shoot(player);
                    default:
                        break;
                }
            }
        }
        Move bestDirection = Move.STAY;
        int bestDistance = Integer.MAX_VALUE;
        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                int distance = max(abs(x - CENTRE_OF_VISION), abs(y - CENTRE_OF_VISION));
                PlayerId playerAtLocation = context.getPlayField()[x][y];
                if (playerAtLocation != null
                        && !(playerAtLocation.getName().equals("Zombie"))
                        && !(playerAtLocation.getName().equals("Gunner"))
                        && distance < bestDistance) {
                    bestDistance = distance;
                    bestDirection = Move.inDirection(x - CENTRE_OF_VISION, y -CENTRE_OF_VISION);
                }
            }
        }
        return bestDirection;
    }
}