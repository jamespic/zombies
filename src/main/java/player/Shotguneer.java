package player;

import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class Shotguneer implements Player {

    @Override
    public Action doTurn(PlayerContext context) {

        double sdistance=1000;

        if (context.getBullets() > 0) {
            for (PlayerId player: context.shootablePlayers()) {
                switch(player.getName()) {
                    case "Gunner":
                    case "ZombieRightsActivist":
                    case "ZombieHater":
                        return new Shoot(player);
                    default:
                        break;
                }
            }
            boolean zombies=false;
            PlayerId TargetZombie = context.getId();
            for (int x = -3; x < +4; x++) {
            for (int y = -3; y < +4; y++) {
                double distance = sqrt(pow(x,2)+pow(y,2));
                PlayerId playerAtLocation = context.getPlayField()[x + CENTRE_OF_VISION][y + CENTRE_OF_VISION];
                if (playerAtLocation != null && playerAtLocation.getName().equals("Zombie") && (distance < sdistance ||zombies==false)) {
                    sdistance = distance;
                    zombies=true;
                    TargetZombie=playerAtLocation;
                }
                if ((playerAtLocation != null && (playerAtLocation.getName().equals("Priest")||playerAtLocation.getName().equals("ZombieRightsActivist"))) && distance < 2 &&zombies==false) {
                    TargetZombie=playerAtLocation;
                }
            }}
            if (zombies || sdistance<3) {
                return new Shoot(TargetZombie);
            }
        }

        if (context.getPlayField()[CENTRE_OF_VISION+1][CENTRE_OF_VISION+1]==null){
            return Move.SOUTHEAST;  
        } else if (context.getPlayField()[CENTRE_OF_VISION][CENTRE_OF_VISION+1]==null){
            return Move.SOUTH;
        } else {
            return Move.EAST;
        }

    }

}