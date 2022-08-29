package duospire.patches.menu;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import javassist.CannotCompileException;
import javassist.CtBehavior;

@SpirePatch2(
        clz = CharacterOption.class,
        method = "updateHitbox"
)
public class AltCharSelect {
    @SpireInsertPatch(
            locator = Locator.class
    )
    public static SpireReturn<Void> altSelect(CharacterOption __instance) {
        if (CoopMenu.screen != null && CoopMenu.screen.viewingLobby()) {
            if (!__instance.locked) {
                CoopMenu.screen.selectCharOption(__instance);
                __instance.c.doCharSelectScreenSelectEffect();
            }

            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator
    {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException
        {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(CharacterOption.class, "selected");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
