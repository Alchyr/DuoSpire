package duospire.tests;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class CastingTestAction extends AbstractGameAction {
    private final AbstractCard card;

    public CastingTestAction(AbstractCard c, InvalidMethodCallCardTest otherCard) {
        this.card = c;

        otherCard.badParameter(new DamageAction(AbstractDungeon.player, new DamageInfo(AbstractDungeon.player, 3, DamageInfo.DamageType.HP_LOSS)));
    }

    @Override
    public void update() {
        if (card instanceof Strike_Red) {
            Strike_Red strike = (Strike_Red) card;
            strike.angle += 1;
        }


    }
}
