package player;
import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class ZombieRightsActivist implements Player {

@Override
public Action doTurn(PlayerContext context) {
    if (context.getBullets() > 0) {
        for (PlayerId player: context.shootablePlayers()) {
            switch(player.getName()) {
                case "ZombieRightsActivist":
                case "DeadBody":
                case "Zombie":   
                    break;
                default:
                    return new Shoot(player);//Kill the non-believers
            }
        }
    }
    double farthest=0;
    Move move=Move.randomMove();
    for (int x = 0; x < VISION_WIDTH; x++) {//Find a lonely zombie and give it a hug
        for (int y = 0; y < VISION_WIDTH; y++) {
            PlayerId friend = context.getPlayField()[x][y];
            if (friend!= null && (friend.getName().equals("Zombie"))) {
                double distance=sqrt(pow(x-context.getX(),2)+pow(y-context.getY(),2));
                if (distance>farthest){
                    farthest = distance;
                    move = Move.inDirection(x - CENTRE_OF_VISION, y -CENTRE_OF_VISION);
                }
            }
        }
    }
    return move;
}

}