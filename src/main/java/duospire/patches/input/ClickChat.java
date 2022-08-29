package duospire.patches.input;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import static duospire.DuoSpire.chat;

@SpirePatch2(
        clz = InputHelper.class,
        method = "updateFirst"
)
public class ClickChat {
    @SpireInsertPatch(
            locator = Locator.class
    )
    public static void stopChat() {
        chat.clicked(InputHelper.mX, InputHelper.mY);
    }

    private static class Locator extends SpireInsertLocator
    {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException
        {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(Settings.class, "isControllerMode");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
