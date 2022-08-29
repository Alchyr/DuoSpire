package duospire.patches.init;

import basemod.Pair;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.desktop.DesktopLauncher;
import duospire.patches.gen.EnumPlace;

import java.util.ArrayList;
import java.util.List;

@SpirePatch(
        clz = DesktopLauncher.class,
        method = "main"
)
public class PostStandardInit {
    @SpirePrefixPatch
    public static void init(String[] whatever) {
        registerEnumColors();
        registerColors();
    }

    public static void registerColors() {

    }

    private static final List<Pair<String, Enum<?>>> generatedEnums = new ArrayList<>();
    public static void registerEnumLater(String origin, Enum<?> value) {
        generatedEnums.add(new Pair<>(origin, value));
    }
    private static void registerEnumColors() {
        for (Pair<String, Enum<?>> enumPair : generatedEnums)
            EnumPlace.registerValue(enumPair.getKey(), enumPair.getValue());
    }
}
