package duospire.patches.visual;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import duospire.patches.gen.EnumPlace;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public class UseOriginalColorForRendering {
    @SpirePatch(
            clz = AbstractCard.class,
            method = "initializeDescription"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "initializeDescriptionCN"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "createCardImage"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "renderAttackBg"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "renderSkillBg"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "renderPowerBg"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "renderEnergy"
    )
    @SpirePatch(
            clz = AbstractCard.class,
            method = "updateCost"
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
