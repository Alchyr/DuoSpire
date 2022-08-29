package duospire.patches.menu;

import basemod.ReflectionHacks;
import basemod.patches.com.megacrit.cardcrawl.screens.mainMenu.ColorTabBar.ColorTabBarFix;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.compendium.CardLibraryScreen;
import com.megacrit.cardcrawl.screens.mainMenu.ColorTabBar;
import duospire.patches.gen.EnumPlace;
import javassist.CtBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static duospire.DuoSpire.makeID;

@SpirePatch2(
        clz = ColorTabBarFix.Render.class,
        method = "Insert"
)
public class LocalizedColorNamesInLibrary {
    private static final String TAB_NAME = CardCrawlGame.languagePack.getUIString(makeID("LibraryTabs")).TEXT[0];
    private static ArrayList<ColorTabBarFix.ModColorTab> modTabs = null;
    private static final Map<AbstractCard.CardColor, String> colorNames = new HashMap<>();

    @SpireInsertPatch(
            locator = Locator.class,
            localvars = { "i", "tabName" }
    )
    public static void changeTabName(int i, @ByRef String[] tabName)
    {
        if (modTabs == null)
        {
            modTabs = ReflectionHacks.getPrivateStatic(ColorTabBarFix.Fields.class, "modTabs");
        }
        ColorTabBarFix.ModColorTab modTab = modTabs.get(i);
        AbstractCard.CardColor orig = EnumPlace.getOrigColor(modTab.color);
        if (orig != modTab.color) {
            String name = colorNames.get(modTab.color);
            if (name != null) {
                tabName[0] = name;
            }
            else {
                switch (orig) {
                    case RED:
                        tabName[0] = String.format(TAB_NAME, CardLibraryScreen.TEXT[1]);
                        break;
                    case GREEN:
                        tabName[0] = String.format(TAB_NAME, CardLibraryScreen.TEXT[2]);
                        break;
                    case BLUE:
                        tabName[0] = String.format(TAB_NAME, CardLibraryScreen.TEXT[3]);
                        break;
                    case PURPLE:
                        tabName[0] = String.format(TAB_NAME, CardLibraryScreen.TEXT[8]);
                        break;
                    case COLORLESS:
                        tabName[0] = String.format(TAB_NAME, CardLibraryScreen.TEXT[4]);
                        break;
                    case CURSE:
                        tabName[0] = String.format(TAB_NAME, CardLibraryScreen.TEXT[5]);
                        break;
                    default:
                        for (AbstractPlayer character : CardCrawlGame.characterManager.getAllCharacters()) {
                            if (character.getCardColor() == orig) {
                                tabName[0] = String.format(TAB_NAME, character.getLocalizedCharacterName());
                                break;
                            }
                        }
                        break;
                }
                colorNames.put(modTab.color, tabName[0]);
            }
        }
    }

    private static class Locator extends SpireInsertLocator
    {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception
        {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(FontHelper.class, "renderFontCentered");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
