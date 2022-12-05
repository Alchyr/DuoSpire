package duospire.player;

import basemod.abstracts.CustomPlayer;
import basemod.animations.AbstractAnimation;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import duospire.statics.Players;

import java.util.ArrayList;

public class CoopPlayer extends CustomPlayer {
    public static class Enum {
        @SpireEnum
        public static PlayerClass coopPlayer;
    }

    public CoopPlayer() {
        super("", null, null, new AbstractAnimation() {
            @Override
            public Type type() {
                return Type.NONE;
            }
        });
    }

    @Override
    public String getTitle(PlayerClass playerClass) {
        return "";
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return null;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        //Return card from appropriate player for local player?
        return new Madness();
    }

    //Coop Implementation


    @Override
    public void update() {
        Players.players[0].update();
        Players.players[1].update();
    }

    @Override
    public void render(SpriteBatch sb) {
        Players.players[0].render(sb);
        Players.players[1].render(sb);
    }

    @Override
    public void dispose() {
        Players.players[0].dispose();
        Players.players[1].dispose();
    }

    //These probably shouldn't ever be called but are implemented anyways
    @Override
    public Color getCardRenderColor() {
        return null;
    }

    @Override
    public Color getCardTrailColor() {
        return null;
    }

    @Override
    public Color getSlashAttackColor() {
        return null;
    }

    //Below here *really* shouldn't ever be called
    @Override
    public ArrayList<String> getStartingDeck() {
        return new ArrayList<>();
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        return new ArrayList<>();
    }

    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo("", "", 0, 0, 0, 0, 0, new CoopPlayer(), getStartingRelics(), getStartingDeck(), false);
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 0;
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontRed;
    }

    @Override
    public void doCharSelectScreenSelectEffect() {

    }

    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        return "APPEAR";
    }

    @Override
    public String getLocalizedCharacterName() {
        return "";
    }

    @Override
    public AbstractPlayer newInstance() {
        return new CoopPlayer();
    }

    @Override
    public String getSpireHeartText() {
        return "";
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[0];
    }

    @Override
    public String getVampireText() {
        return "";
    }
}
