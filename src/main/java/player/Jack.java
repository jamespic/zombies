package player;
import zombie.*;
import static zombie.Constants.*;
//import static java.lang.Math.*;

public class Jack implements Player {
    @Override
    public Action doTurn(PlayerContext context) {
        int[] Ideal = {1,5,8,7,2,2,2,2,1,5,1,2,1,1,7,2,7,7,7,0,2,3,1,7};
        int[] Threat = {1,4,8,8,1,1,1,1,2,2,2,1,2,0,6,2,6,6,6,1,1,2,6,6};
        int[] Importance = {1,2,2,2,1,1,1,1,3,1,3,1,3,3,1,2,1,1,1,10,2,2,3,2};

        PlayerId Target = context.getId();
        int[][] Bob = {{400-2*Math.max(0,context.getGameClock()),200-Math.max(0,context.getGameClock()),400-Math.max(0,context.getGameClock())},{0,0,0},{0,0,0}};
        double maxDanger=0;
        int zombies=0;

        for (int x = -8; x < +8; x++) {
        for (int y = -8; y < +8; y++) {
            PlayerId playerAtLocation = context.getPlayField()[x + CENTRE_OF_VISION][y + CENTRE_OF_VISION];
            if (playerAtLocation != null && x*y+x+Math.abs(y) != 0){
                if (Math.abs(x)*Math.abs(y)==1 || Math.abs(x) + Math.abs(y)==1){
                    Bob[x+1][y+1]-=100000;
                }
                int dist = Math.max(Math.abs(x),Math.abs(y));
                int Ident = Dats(playerAtLocation);
                double Danger = (Threat[Ident]-dist)*Importance[Ident];
                if(Ident==1 && dist<Threat[Ident]){
                    zombies++;
                    if(context.getPlayField()[TFSAE(x)-1 + CENTRE_OF_VISION][TFSAE(y) -1+ CENTRE_OF_VISION]!=null){ 
                    Danger=0;
                                } else if(dist==2){Danger+=4;} 
                }
                if(Danger>maxDanger && dist<6){
                    maxDanger=Danger;
                    Target=playerAtLocation;
                }
                if(dist != Ideal[Ident]){

                    Bob[TFSAE(x)][TFSAE(y)] += Math.round(200*Importance[Ident]/(dist-Ideal[Ident]));

                    if(TFSAE(x) ==1) {
                        Bob[0][TFSAE(y)] += Math.round(100*Importance[Ident]/(dist-Ideal[Ident]));
                        Bob[2][TFSAE(y)] += Math.round(100*Importance[Ident]/(dist-Ideal[Ident]));
                    } else {
                        Bob[1][TFSAE(y)] += Math.round(100*Importance[Ident]/(dist-Ideal[Ident]));
                    }

                    if(TFSAE(y) ==1) {
                        Bob[TFSAE(x)][0] += Math.round(100*Importance[Ident]/(dist-Ideal[Ident]));
                        Bob[TFSAE(x)][2] += Math.round(100*Importance[Ident]/(dist-Ideal[Ident]));
                    } else {
                        Bob[TFSAE(x)][1] += Math.round(100*Importance[Ident]/(dist-Ideal[Ident]));
                    }
                }
            }
        }}

        if (context.getBullets()>1 && maxDanger>0){
            return new Shoot(Target);
        } else if (context.getBullets()==1 && zombies>3){
            return new Shoot(context.getId());
        } else if (context.getBullets()==1 && maxDanger>7){
            return new Shoot(Target);
        }

        int Xmax=0;
        int Ymax=0;

        for (int x = 0; x < 3; x++) {
        for (int y = 0; y < 3; y++) {
            if (Bob[x][y]>=Bob[Xmax][Ymax]){
                Xmax=x;
                Ymax=y;
            }
        }}
        return Move.inDirection(Xmax-1, Ymax-1);

    }

    private int Dats (PlayerId WhoDat){
        switch (WhoDat.getName()){
            case "DeadBody": return 0;
            case "Zombie": return 1;
            case "Fox": return 2;
            case "Coward": return 3;
            case "Shotguneer": return 4;
            case "HuddleWolf": return 5;
            case "Sokie": return 6;
            case "GordonFreeman": return 7;
            case "Vortigaunt": return 8;
            case "SuperCoward": return 9;
            case "StandStill": return 10;
            case "JohnNash": return 11;
            case "MoveRandomly": return 12;
            case "Waller": return 13;
            case "HideyTwitchy": return 14;
            case "Bee": return 15;
            case "ZombieHater": return 16;
            case "ZombieRightsActivist": return 17;
            case "Gunner": return 18;
            case "EmoWolfWithAGun": return 19;
            case "Jack": return 20;
              case "SOS": return 21;
              case "SunTzu": return 22;
            default: return 23;
        }

    }
    private int TFSAE(int TBN){
        if(TBN==0){return 1;
        } else if(TBN>0){return 2;}

        return 0;
    }
}