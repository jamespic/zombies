package player;

import static zombie.Constants.CENTRE_OF_VISION;
import static zombie.Constants.SHOOT_RANGE;
import static zombie.Constants.VISION_RANGE;

import java.awt.Point;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import zombie.Action;
import zombie.Move;
import zombie.Player;
import zombie.PlayerContext;
import zombie.PlayerId;
import zombie.Shoot;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

    public class SunTzu implements Player {
        private TreeBasedTable<Integer, Integer, Integer> dangerZone;
        private final static int IN_ENEMY_RANGE = 5;
        private static final int IN_LOOTED_RANGE = 4;
        private static final int FULL_MAGAZINE = 10;
        private static final int IN_ZOMBIE_RANGE = 10;
        private static final int NUM_PLAYERS = 40;
        private LinkedHashSet<Point> safeSpots;
        private PlayerId[][] localAreas;
        private Set<PlayerId> looted= new HashSet<>(50*NUM_PLAYERS);
        private int ammo;
        PlayerId biggestThreat;
        private Set<PlayerId> shootable;
        private PlayerId myId;
        @SuppressWarnings("unused")
        @Override
        public Action doTurn(PlayerContext context) {
            ammo = context.getBullets();
            int gameTurn =context.getGameClock();
            int boardSize = context.getBoardSize();
            myId = context.getId();
            localAreas = context.getPlayField();
            dangerZone = TreeBasedTable.create();
            shootable = context.shootablePlayers();
            updateAdjacentBodyState();

            for (int x = CENTRE_OF_VISION - SHOOT_RANGE; x <= CENTRE_OF_VISION + SHOOT_RANGE; x++) {
                for (int y = CENTRE_OF_VISION - SHOOT_RANGE; y <= CENTRE_OF_VISION + SHOOT_RANGE; y++) {
                    PlayerId playerId = localAreas[x][y];
                    if (playerId != null) {
                        calculateDangerZone(x,y,playerId);
                    }
                }
            }
            Action myAction = null;
            Iterator<Point> pIt = safeSpots.iterator();
            if (ammo>0&&!pIt.hasNext()&&getBiggestThreat()!=null) {
                return new Shoot(getBiggestThreat());
            } else if (pIt.hasNext()){
                Point p=pIt.next();
                return Move.inDirection(p.x, p.y);
            }else{
                return Move.randomMove();
            }
        }

        private PlayerId getBiggestThreat() {
            return biggestThreat==null?shootable.iterator().next():biggestThreat;
        }

        public void setBiggestThreat(PlayerId biggestThreat) {
            this.biggestThreat = biggestThreat;
        }
        private void updateAdjacentBodyState() {

            for( int x = -1; x <= 1; x++ ) {
                for( int y = -1; y <= 1; y++ ) {
                    PlayerId adjPlayerId = localAreas[CENTRE_OF_VISION+x][CENTRE_OF_VISION+y];
                    if( adjPlayerId != null && (!looted.contains(adjPlayerId) && adjPlayerId.getName().equals("DeadBody"))) {
                        looted.add(adjPlayerId);
                    }       
                }
            }
        }

        private void calculateDangerZone(int x, int y, PlayerId playerId) {
            deriveDanger(playerId, x, y);
            safeSpots = getSafeSpots();
        }

        @SuppressWarnings("rawtypes")
        private LinkedHashSet<Point> getSafeSpots() {
            LinkedHashSet<Point> safeSpots = new LinkedHashSet<>();
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
                        Collections2.filter(spots, sameCellValuePredicate(safeCell)), pointFromCell));
            }
            return safeSpots;
        }

        @SuppressWarnings("rawtypes")
        private Predicate<Cell> sameCellValuePredicate(final Cell safeCell) {
            return new Predicate<Cell>() {

                @Override
                public boolean apply(Cell arg0) {
                    return (arg0.getValue() == safeCell.getValue());
                }
            };
        }

        @SuppressWarnings("rawtypes")
        private Comparator<Cell> cellValueComparator() {
            return new Comparator<Cell>() {
                @Override
                public int compare(Cell o1, Cell o2) {
                    return (int)o1.getValue()- (int)o2.getValue();
                }
            };
        }

        private void deriveDanger(PlayerId playerId, int x, int y) {
            switch (playerId.getName()) {
            case "Gunner":
            case "Fox":
            case "HideyTwitchy":
            case "Shotguneer":
            case "ZombieRightsActivist":
            case "ZombieHater":
            case "SuperCoward":
            case "Sokie":
                updateDangerZoneWithEnemy(x, y);
                break;
            case "DeadBody":
            case "Zombie":
                updateDangerZoneWithBodies(x,y);
                break;
            default:
                break;
            }
        }

        private void updateDangerZoneWithBodies(int x, int y) {
            int dangerLevel=0;
            if(localAreas[x][y].getName().equalsIgnoreCase("Zombie")){
                dangerLevel = IN_ZOMBIE_RANGE;
            }
            else if(looted.contains(localAreas[x][y])){
                dangerLevel = IN_LOOTED_RANGE;
            }else{
                dangerLevel = Math.min(-1,-FULL_MAGAZINE+ammo);
            }
            for (int i = x-1; i < x+1; i++) {
                for (int j = y-1; j < y+1; j++) {
                    Integer previousDangerLevel = dangerZone.get(i, j) ;
                    int currentDangerLevel = dangerLevel;
                    if (previousDangerLevel != null) {
                        currentDangerLevel = previousDangerLevel+dangerLevel;
                    } 
                    dangerZone.put(x, y, currentDangerLevel);
                }
            }
        }

        private void updateDangerZoneWithEnemy(int x, int y) {
            int dangerLevel = IN_ENEMY_RANGE;
            playerShieldFound:
                for (int i = Math.max(x-SHOOT_RANGE, 0); i < Math.min(SHOOT_RANGE+x,VISION_RANGE); i++) {
                    for (int j = Math.max(y-SHOOT_RANGE, 0); j < Math.min(SHOOT_RANGE+y,VISION_RANGE); j++) {
                        int cardinalityFactor = (i+1)+(j+1);
                        Integer previousDangerLevel = dangerZone.get(i, j);
                        int currentDangerLevel = dangerLevel*cardinalityFactor;
                        PlayerId enemy = localAreas[x][y];
                        PlayerId target = localAreas[i][j];
                        if (target!=null) {
                            if (target != enemy) {
                                break playerShieldFound;
                            } else if (target.equals(myId)) {
                                setBiggestThreat(enemy);
                            }
                        }
                        if (previousDangerLevel != null) {
                            currentDangerLevel = Math.max(previousDangerLevel, dangerLevel);
                        } 
                        dangerZone.put(i, j, currentDangerLevel );
                    }
                }
        }

    }