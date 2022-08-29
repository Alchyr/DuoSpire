package duospire.patches.menu;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.characters.CharacterManager;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import javassist.CannotCompileException;
import javassist.CtBehavior;

@SpirePatch(
        clz= MainMenuScreen.class,
        method="setMainMenuButtons"
)
public class MainMenuItem {
    @SpireInsertPatch(
            locator=Locator.class,
            localvars={"index"}
    )
    public static void Insert(Object __obj_instance, @ByRef int[] index)
    {
        MainMenuScreen __instance = (MainMenuScreen)__obj_instance;
        __instance.buttons.add(new MenuButton(CoopMenu.DUOSPIRE_COOP, index[0]++));
    }

    private static class Locator extends SpireInsertLocator
    {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException
        {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(CharacterManager.class, "anySaveFileExists");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
