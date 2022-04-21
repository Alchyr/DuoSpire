package duospire.patches.setup;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch(
        clz = BaseMod.class,
        method = "publishEditCards"
)
public class PostEditCards {
    @SpirePostfixPatch
    public static void generateMultiplayerCards() {
        //Goal:
        /*
            For each basegame card, generate a version of it (prefixed with DuoSpire:) for multiplayer.
            Any references to `AbstractDungeon.player` will have to be replaced.
            Skills that grant block and target none or self should use a CustomTargeting to target a player.
            If a card cannot target a player, they should reference the `p` variable passed into the method which will be
            the player who used the card.


            Attacks should work as normal.

            Step 1 is going through every card that has to be modified, then checking all the actions they reference they add.
            Then also all the actions *those* actions reference.

            AbstractDungeon.player will be the current "front" player.
            There will be two player instances. They are labeled with `I` and `II`. These will also be placed above enemy intents to show who they will damage.
         */

    }
}
