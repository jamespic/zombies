package player;

import zombie.*;

public class MoveRandomly implements Player {
    @Override
    public Action doTurn(PlayerContext context) {
        return Move.randomMove();
    }
}
