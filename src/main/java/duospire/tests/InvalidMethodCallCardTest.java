package duospire.tests;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class InvalidMethodCallCardTest extends AbstractCard {
    public static final String ID = "Strike_B";
    private static final CardStrings cardStrings;

    public InvalidMethodCallCardTest() {
        super("Test", cardStrings.NAME, "blue/attack/strike", 1, cardStrings.DESCRIPTION, CardType.ATTACK, CardColor.BLUE, CardRarity.BASIC, CardTarget.ENEMY);
        this.baseDamage = 6;
        this.tags.add(CardTags.STRIKE);
        this.tags.add(CardTags.STARTER_STRIKE);
    }

    public void badParameter(DamageAction damageAction) {
        AbstractDungeon.actionManager.addToBottom(damageAction);
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
    }

    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(3);
        }
    }

    public AbstractCard makeCopy() {
        return new InvalidMethodCallCardTest();
    }

    static {
        cardStrings = CardCrawlGame.languagePack.getCardStrings("Strike_B");
    }
}
