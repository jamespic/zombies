package player;

import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class SOS implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        if (context.getBullets() > 0) {
            for (PlayerId player: context.shootablePlayers()) {
                switch(player.getName()) {
                    case "Gunner":
                    case "Zombie":
                    case "ZombieRightsActivist":
                        return new Shoot(player);
                    default:
                        break;
                }
            }
        }
        Move bestDirection = Move.NORTH;
        int bestDistance = Integer.MAX_VALUE;
        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                int distance = max(abs(x - CENTRE_OF_VISION), abs(y - CENTRE_OF_VISION));
                PlayerId playerAtLocation = context.getPlayField()[x][y];
                if (playerAtLocation != null
                        && !(playerAtLocation.getName().equals("Zombie"))
                        && !(playerAtLocation.getName().equals("Gunner"))
                        && !(playerAtLocation.getName().equals("ZombieRightsActivist"))
                        && !(playerAtLocation.getName().equals("ZombieHater"))
                        && !(playerAtLocation.equals(context.getId()))
                        && distance < bestDistance) {
                    bestDistance = distance;
                    bestDirection = Move.inDirection(x - CENTRE_OF_VISION, y -CENTRE_OF_VISION);
                }
            }
        }
        return bestDirection;
    }
}