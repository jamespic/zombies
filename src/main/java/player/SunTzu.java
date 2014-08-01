package player;

import static zombie.Constants.CENTRE_OF_VISION;
import static zombie.Constants.SHOOT_RANGE;
import static zombie.Constants.VISION_RANGE;

import java.awt.Point;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import zombie.Action;
import zombie.Move;
import zombie.Player;
import zombie.PlayerContext;
import zombie.PlayerId;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

public class SunTzu implements Player {
    private TreeBasedTable<Integer, Integer, Integer> dangerZone;
    private final static int IN_ENEMY_RANGE = 5;
    private Set<Point> safeSpots;
    @SuppressWarnings("unused")
    @Override
    public Action doTurn(PlayerContext context) {
        int ammo =context.getBullets();
        int gameTurn =context.getGameClock();
        int boardSize = context.getBoardSize();
        PlayerId myId = context.getId();
        PlayerId[][] localArea = context.getPlayField();
        dangerZone = TreeBasedTable.create();
        Set<PlayerId> shootable = context.shootablePlayers();

        for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
            for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                PlayerId playerId = localArea[x][y];
                if (playerId != null) {
                    calculateDangerZone(x,y,playerId);
                }
            }
        }
        Point p = (Point) safeSpots.toArray()[0]==null?new Point(Move.randomMove().x,Move.randomMove().y):(Point)safeSpots.toArray()[0];
        return Move.inDirection(p.x, p.y);
    }

    private void calculateDangerZone(int x, int y, PlayerId playerId) {
        int dangerLevel = deriveDanger(playerId);
        updateDangerZone(x, y, dangerLevel);
        safeSpots = getSafeSpots();
    }

    @SuppressWarnings("rawtypes")
    private Set<Point> getSafeSpots() {
        Set<Point> safeSpots = new HashSet<>();
        TreeSet<Cell> spots = new TreeSet<>(cellValueComparator());
        for (Cell<Integer, Integer, Integer> cell : dangerZone.cellSet()) {
            spots.add(cell);
        }
        final Cell safeCell = spots.isEmpty()?null:Collections.min(spots,cellValueComparator());
        Function<Cell,Point> pointFromCell = new Function<Cell,Point>() {
            public Point apply(final Cell arg0) {return new Point((int)arg0.getRowKey(), (int)arg0.getColumnKey());};
        };

        if (safeCell!=null) {
            safeSpots.addAll(Collections2.transform(
                    Collections2.filter(spots, new Predicate<Cell>() {

                        @Override
                        public boolean apply(Cell arg0) {
                            return (arg0.getValue() == safeCell.getValue());
                        }
                    }), pointFromCell));
        }
        return safeSpots;
    }

    private Comparator<Cell> cellValueComparator() {
        return new Comparator<Cell>() {
            @Override
            public int compare(Cell o1, Cell o2) {
                return (int)o1.getValue()- (int)o2.getValue();
            }
        };
    }

    private int deriveDanger(PlayerId playerId) {
        int dangerLevel = 0;
        switch (playerId.getName()) {
        case "Gunner":
        case "Fox":
        case "HideyTwitchy":
        case "Shotguneer":
        case "ZombieRightsActivist":
            dangerLevel = IN_ENEMY_RANGE;
            break;

        default:
            dangerLevel = -1;
            break;
        }
        return dangerLevel;
    }

    private void updateDangerZone(int x, int y, int dangerLevel) {
        for (int i = Math.max(x-SHOOT_RANGE, 0); i < Math.min(SHOOT_RANGE+x,VISION_RANGE); i++) {
            for (int j = Math.max(y-SHOOT_RANGE, 0); j < Math.min(SHOOT_RANGE+y,VISION_RANGE); j++) {
                int cardinalityFactor = (i+1)*(j+1);
                Integer previousDangerLevel = dangerZone.get(i, j) ;
                int currentDangerLevel = dangerLevel;
                if (previousDangerLevel != null) {
                    currentDangerLevel = Math.max(previousDangerLevel, dangerLevel);
                } dangerZone.put(x, y, currentDangerLevel * cardinalityFactor);
            }
        }
    }

}