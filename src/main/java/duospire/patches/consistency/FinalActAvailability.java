package duospire.patches.consistency;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import duospire.DuoSpire;

@SpirePatch(
        clz = Settings.class,
        method = "setFinalActAvailability"
)
public class FinalActAvailability {
    @SpirePostfixPatch
    public static void alter() {
        if (DuoSpire.inMultiplayer()) {
            //Todo - make this a lobby option
            Settings.isFinalActAvailable = true;
        }
    }
}
