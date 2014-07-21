package player;

import zombie.*;

public class EmoWolfWithAGun implements Player {

    @Override
    public Action doTurn(PlayerContext context) {
        PlayerId myself = context.getId();
        return new Shoot(myself);
    }

}