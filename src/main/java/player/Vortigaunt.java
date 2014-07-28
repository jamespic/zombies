package player;

import java.util.ArrayList;

import zombie.*;

public class Vortigaunt implements Player {
    class PlayerLocation {
        private int x;
        int y;
        PlayerId player;

        public PlayerLocation(int x, int y, PlayerId id) {
            this.x = x;
            this.y = y;
            this.player = id;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public PlayerId getPlayer() {
            return player;
        }
    }
    @Override
    public Action doTurn(PlayerContext context) {
        PlayerId[][] field = context.getPlayField();
        PlayerLocation me = new PlayerLocation(context.getX(), context.getY(), context.getId());
        ArrayList<PlayerLocation> freemans = findFreeman(field);
        PlayerLocation nearestFreeman = getNearestFreeman(freemans, me);
        if (nearestFreeman == null) {
            return Move.randomMove();
        } else {
            return Move.inDirection(nearestFreeman.getX(), nearestFreeman.getY());
        }
    }

    private PlayerLocation getNearestFreeman(ArrayList<PlayerLocation> freemans, PlayerLocation me) {
        double nearestDistance = Integer.MAX_VALUE;
        PlayerLocation nearestFreeman = null;
        for (PlayerLocation freeman : freemans) {
            int x = freeman.getX() - me.getX();
            int y = freeman.getY() - me.getY();
            double distance = (int)Math.sqrt((double)(x * x + y * y));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestFreeman = freeman;
            }
        }
        return nearestFreeman;
    }

    private ArrayList<PlayerLocation> findFreeman(PlayerId[][] field) {
        ArrayList<PlayerLocation> freemans = new ArrayList<PlayerLocation>();
        for (int x = field.length - 1; x >= 0; x -= 1) {
            for (int y = field[x].length - 1; y >= 0; y -= 1) {
                if (field[x][y] != null && field[x][y].getName().equals("GordonFreeman")) {
                    freemans.add(new PlayerLocation(x, y, field[x][y]));
                }
            }
        }
        return freemans;
    }

}