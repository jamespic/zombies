package player;

import zombie.*;

public class Gunner implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        if (context.getBullets() > 0) {
            for (PlayerId player: context.shootablePlayers()) {
                switch(player.getName()) {
                    case "Gunner":
                    case "DeadBody":
                        break;
                    default:
                        return new Shoot(player);
                }
            }
        }
        return Move.randomMove();
    }
    
}
