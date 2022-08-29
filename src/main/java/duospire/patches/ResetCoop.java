package duospire.patches;

import duospire.networking.matchmaking.Matchmaking;

public class ResetCoop {
    public static void stopAllMultiplayer() {
        Matchmaking.leave();
        //P2P
    }

    public static void thing() {

    }
}
