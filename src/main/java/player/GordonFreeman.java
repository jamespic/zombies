package player;

import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class GordonFreeman implements Player {
    @Override
    public Action doTurn(PlayerContext context){
        int ammo = context.getBullets();
        // if I have bullets, shoot some zombies
        if(ammo > 0){
            for(PlayerId player: context.shootablePlayers()){
                switch(player.getName()){
                    case "Zombie":
                       return new Shoot(player);
                    default:
                       break;
                }
            }
        }
        // if no bullets, find a dead body and scavenge
        Move bestDirection = Move.STAY;
        int bestDistance = Integer.MAX_VALUE;
        for(int y = 1; y < VISION_WIDTH - 1; y++) {
            for(int x = 1; x < VISION_WIDTH - 1; x++) {

                PlayerId playerAtLocation = context.getPlayField()[x][y];
                // find a dead body
                if((playerAtLocation != null) && "DeadBody".equals(playerAtLocation.getName())){
                    // check adjacent squares for an empty square
                    for(int yy=-1; yy <= +1; yy++){
                        for(int xx=-1; xx <= +1; xx++){
                            PlayerId playerNearby = context.getPlayField()[x + xx][y + yy];
                            if(playerNearby == null){
                                int distance = max(abs(xx + x - CENTRE_OF_VISION), abs(yy + y - CENTRE_OF_VISION));
                                if(distance < bestDistance){
                                    bestDistance = distance;
                                    bestDirection = Move.inDirection(xx + x - CENTRE_OF_VISION, yy + y - CENTRE_OF_VISION);
                                }
                            }
                        }
                    }
                }
            }
        }
        return bestDirection;
    }
}