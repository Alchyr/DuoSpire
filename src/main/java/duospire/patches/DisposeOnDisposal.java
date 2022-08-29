package duospire.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import duospire.networking.gameplay.P2P;
import duospire.networking.matchmaking.Matchmaking;

import java.util.function.Function;

@SpirePatch(
        clz = CardCrawlGame.class,
        method = "dispose"
)
public class DisposeOnDisposal {
    Function<Integer, Integer> thing;

    public DisposeOnDisposal(Function<Integer, Integer> thing) {
        this.thing = thing;
    }


    @SpirePrefixPatch
    public static void cleanup(CardCrawlGame __instance)
    {
        Matchmaking.dispose();
        P2P.dispose();

    }
}
