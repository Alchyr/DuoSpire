package duospire.statics;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import duospire.DuoSpire;

public class Players {
    //0 is host, others are other players.
    public static AbstractPlayer[] players;
    public static int localPlayer = 0;

    public static AbstractPlayer getAltPlayer(AbstractPlayer p) {
        //see generateAltPlayerGetters in MultiplayerGeneration
        return null;
    }
    public static void setCoopPlayers(int localIndex, AbstractPlayer... players) {
        Players.players = players;
        localPlayer = localIndex;
        if (localIndex >= players.length)
            DuoSpire.logger.error("Local player index (" + localIndex + ") out of bounds of players (" + players.length + ")");
    }

    //If custom static methods must be generated, put them here
}
