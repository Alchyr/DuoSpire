package duospire.patches.gen;

import com.megacrit.cardcrawl.cards.AbstractCard;

import java.util.HashMap;

public class EnumPlace {
    public static final HashMap<AbstractCard.CardColor, AbstractCard.CardColor> cardColors = new HashMap<>();
    public static final HashMap<AbstractCard.CardColor, AbstractCard.CardColor> reverseColors = new HashMap<>();
    public static void registerValue(String origin, Enum<?> enumValue) {
        if (enumValue instanceof AbstractCard.CardColor) {
            try {
                AbstractCard.CardColor original = AbstractCard.CardColor.valueOf(origin);
                cardColors.put(original, (AbstractCard.CardColor) enumValue);
                reverseColors.put((AbstractCard.CardColor) enumValue, original);
            }
            catch (IllegalArgumentException e) {
                System.out.println("\t- Original card color " + origin + " does not exist.");
            }
        }
    }
    @SuppressWarnings("unused")
    public static AbstractCard.CardColor getAltColor(AbstractCard.CardColor base) {
        return cardColors.getOrDefault(base, base);
    }
    @SuppressWarnings("unused")
    public static AbstractCard.CardColor getOrigColor(AbstractCard.CardColor alt) {
        return reverseColors.getOrDefault(alt, alt);
    }

    //The enums for card colors are created here.
}
