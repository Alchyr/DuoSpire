package duospire.patches.menu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import duospire.ui.CoopMenuScreen;

public class CoopScreenUpdateAndRender {
    @SpirePatch(
            clz=MainMenuScreen.class,
            method="update"
    )
    public static class Update {
        @SpirePostfixPatch
        public static void Postfix(MainMenuScreen __instance) {
            if (__instance.screen == CoopMenuScreen.Enum.DUOSPIRE_COOP_MENU && CoopMenu.screen != null) {
                CoopMenu.screen.update();
            }
        }
    }

    @SpirePatch(
            clz=MainMenuScreen.class,
            method="render"
    )
    public static class Render {
        @SpirePostfixPatch
        public static void Postfix(MainMenuScreen __instance, SpriteBatch sb) {
            if (__instance.screen == CoopMenuScreen.Enum.DUOSPIRE_COOP_MENU && CoopMenu.screen != null) {
                CoopMenu.screen.render(sb);
            }
        }
    }
}
