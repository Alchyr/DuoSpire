package duospire.patches.visual;

import basemod.BaseMod;
import basemod.patches.com.megacrit.cardcrawl.screens.SingleCardViewPopup.BackgroundFix;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.cards.AbstractCard;
import duospire.patches.gen.EnumPlace;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

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

    //Next need to patch basemod rendering stuff in RenderFixSwitches
}
