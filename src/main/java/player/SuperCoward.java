package player;

import zombie.*;
import static zombie.Constants.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import java.awt.Point;

public class SuperCoward implements Player {

    private enum DANGER{
        SAFE(0),PROBABLY_SAFE(1),UNSAFE(2),DANGER(3);

        private int value;
        private DANGER(int value){
            this.value = value;
        }
    }

    private final int PLAYER_X = 8;
    private final int PLAYER_Y = 8;

    @Override
    public Action doTurn(PlayerContext context) {

        DANGER danger = DANGER.DANGER;
        Point position = null;
        for(int i=-1;i<1;i++){
            for(int j=-1;j<1;j++){
                DANGER positionDanger = isDangerous(context,PLAYER_X+i,PLAYER_Y+j);
                if(positionDanger.value < danger.value){
                    if(canMove(context,PLAYER_X+i,PLAYER_Y+j)){
                        position = new Point(PLAYER_X+i, PLAYER_Y+j);
                    }
                }
            }
        }

        if(position != null){
            return Move.inDirection(position.x, position.y);
        }else{
            return Move.STAY;
        }
    }

    private boolean canMove(PlayerContext context,int posX, int posY){
        PlayerId playerAtLocation = context.getPlayField()[posX][posY];
        if(playerAtLocation == null){
            return true;
        }else{
            return false;
        }
    }

    private DANGER isDangerous(PlayerContext context,int posX, int posY){
        DANGER danger = DANGER.SAFE;

        for (int x = 0; x < VISION_WIDTH; x++) {
            for (int y = 0; y < VISION_WIDTH; y++) {
                PlayerId playerAtLocation = context.getPlayField()[x][y];

                if(playerAtLocation != null && isEnemy(playerAtLocation.getName())){
                    int distanceToPlayer = max(abs(x - posX), abs(y - posY));
                    if(playerAtLocation.getName().equals("Zombie")){
                        DANGER currentDanger = null;
                        if(distanceToPlayer <=3){
                            currentDanger = DANGER.DANGER;
                        }else if(distanceToPlayer <=5){
                            currentDanger = DANGER.PROBABLY_SAFE;
                        }else if(distanceToPlayer >5){
                            currentDanger = DANGER.SAFE;
                        }
                        if(currentDanger.value > danger.value){
                            danger = currentDanger;
                        }
                    }else{
                        DANGER currentDanger = null;
                        if(distanceToPlayer <=5){
                            currentDanger = DANGER.DANGER;
                        }else if(distanceToPlayer >5){
                            currentDanger = DANGER.PROBABLY_SAFE;
                        }
                        if(currentDanger.value > danger.value){
                            danger = currentDanger;
                        }
                    }
                }
            }
        }
        return danger;
    }

    private boolean isEnemy(String name){
        switch(name) {
            case "DeadBody":
            case "GordonFreeman":
            case "EmoWolfWithAGun":
            case "HuddleWolf":
            case "ThePriest":
            case "Shotguneer":
            case "SuperCoward":
                return false;
            default:
                return true;
        }
    }
}