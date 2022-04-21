package duospire.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import duospire.patches.setup.MultiplayerGeneration;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.NotFoundException;
import org.clapper.util.classutil.ClassFinder;

import java.io.File;
import java.net.URISyntaxException;

@SpirePatch(
        clz = CardCrawlGame.class,
        method = SpirePatch.CONSTRUCTOR
)
public class RawPatchTrigger {
    public static void Raw(CtBehavior ctBehavior) throws NotFoundException {
        System.out.println("Starting raw patches.");

        ClassFinder finder = new ClassFinder();

        finder.add(new File(Loader.STS_JAR));

        for (ModInfo modInfo : Loader.MODINFOS) {
            if (modInfo.jarURL != null) {
                try {
                    finder.add(new File(modInfo.jarURL.toURI()));
                } catch (URISyntaxException e) {
                    // do nothing
                }
            }
        }

        ClassPool pool = ctBehavior.getDeclaringClass().getClassPool();
        MultiplayerGeneration.patch(finder, pool);

        System.out.println("Raw patches complete.");
    }
}