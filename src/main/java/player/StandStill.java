package player;

import zombie.*;

public class StandStill implements Player {
    @Override
    public Action doTurn(PlayerContext context) {
        return Move.STAY;
    }
}
