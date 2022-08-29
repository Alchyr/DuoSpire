package duospire.patches.menu;

import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import duospire.ui.CoopMenuScreen;

import java.lang.reflect.Field;

public class CoopMenu {
    @SpireEnum
    public static MenuButton.ClickResult DUOSPIRE_COOP;

    public static CoopMenuScreen screen = null;

    public static String buttonText = "";

    @SpirePatch(
            clz=MenuButton.class,
            method="setLabel"
    )
    public static class SetLabel
    {
        @SpirePostfixPatch
        public static void Postfix(MenuButton __instance)
        {
            try {
                if (__instance.result == DUOSPIRE_COOP) {
                    Field f_label = MenuButton.class.getDeclaredField("label");
                    f_label.setAccessible(true);
                    f_label.set(__instance, buttonText);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SpirePatch(
            clz=MenuButton.class,
            method="buttonEffect"
    )
    public static class ButtonEffect
    {
        @SpirePostfixPatch
        public static void Postfix(MenuButton __instance)
        {
            if (__instance.result == DUOSPIRE_COOP) {
                screen.open();
            }
        }
    }
}
