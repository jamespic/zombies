package player;

import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class Priest implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        return Move.NORTH;
    }
}