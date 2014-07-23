package player;

import zombie.*;
import static zombie.Constants.*;
import static java.lang.Math.*;

public class ThePriest implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        return Move.NORTH;
    }
}