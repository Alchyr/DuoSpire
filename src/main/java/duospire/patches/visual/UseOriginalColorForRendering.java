package duospire.patches.visual;

import basemod.BaseMod;
import basemod.patches.com.megacrit.cardcrawl.cards.AbstractCard.CreateCardImageSwitch;
import basemod.patches.com.megacrit.cardcrawl.cards.AbstractCard.RenderFixSwitches;
import basemod.patches.com.megacrit.cardcrawl.screens.SingleCardViewPopup.BackgroundFix;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import duospire.patches.gen.EnumPlace;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

import java.lang.reflect.Field;

public class UseOriginalColorForRendering {
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "initializeDescription"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "initializeDescriptionCN"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "createCardImage"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "renderAttackBg"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "renderSkillBg"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "renderPowerBg"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "renderEnergy"
    )
    @SpirePatch2(
            clz = AbstractCard.class,
            method = "updateCost"
    )
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "renderCost"
    )
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "getCardBackAtlasRegion"
    )
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "getCardBackImg"
    )
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "allowUpgradePreview"
    )
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "canToggleBetaArt"
    )
    @SpirePatch2(
            clz = BaseMod.class,
            method = "getCardSmallEnergy",
            paramtypez = { AbstractCard.class }
    )
    @SpirePatch2(
            clz = BackgroundFix.EnergyOrbTexture.class,
            method = "getEnergyOrb"
    )
    @SpirePatch2(
            clz = BackgroundFix.BackgroundTexture.class,
            method = "Prefix"
    )
    @SpirePatch2(
            clz = CreateCardImageSwitch.CreateCardImage.class,
            method = "Prefix"
    )
    @SpirePatch2(
            clz = RenderFixSwitches.RenderBgSwitch.class,
            method = "Prefix"
    )
    @SpirePatch2(
            clz = RenderFixSwitches.RenderOuterGlowSwitch.class,
            method = "Insert"
    )
    @SpirePatch2(
            clz = RenderFixSwitches.RenderEnergySwitch.class,
            method = "getEnergyOrb"
    )
    @SpirePatch2(
            clz = RenderFixSwitches.RenderPortraitFrameSwitch.class,
            method = "Prefix"
    )
    @SpirePatch2(
            clz = RenderFixSwitches.RenderBannerSwitch.class,
            method = "Prefix"
    )
    public static class UseOriginal {
        @SpireInstrumentPatch
        public static ExprEditor edit() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.isReader() && f.getFieldName().equals("color") && f.getClassName().equals(AbstractCard.class.getName())) {
                        f.replace("$_ = " + EnumPlace.class.getName() + ".getOrigColor($proceed($$));");
                    }
                }
            };
        }
    }

    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "open",
            paramtypez = { AbstractCard.class, CardGroup.class }
    )
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "open",
            paramtypez = { AbstractCard.class }
    )
    public static class UseOriginalScv {
        private static final Field cardField;
        static {
            try {
                cardField = SingleCardViewPopup.class.getDeclaredField("card");
                cardField.setAccessible(true);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @SpirePostfixPatch
        public static void resetType(SingleCardViewPopup __instance) throws IllegalAccessException {
            AbstractCard c = (AbstractCard) cardField.get(__instance);
            if (c != null)
                c.color = EnumPlace.getOrigColor(c.color);
        }
    }
    @SpirePatch2(
            clz = SingleCardViewPopup.class,
            method = "render"
    )
    public static class StupidDangCopiesScv {
        @SpirePostfixPatch
        public static void resetType(SingleCardViewPopup __instance, AbstractCard ___card) {
            if (___card != null)
                ___card.color = EnumPlace.getOrigColor(___card.color);
        }
    }

    //Next need to patch basemod rendering stuff in RenderFixSwitches
}
