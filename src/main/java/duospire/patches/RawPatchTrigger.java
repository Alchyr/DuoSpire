package duospire.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import duospire.patches.gen.CharacterColorEnumFields;
import duospire.patches.gen.MultiplayerGeneration;
import duospire.util.BytecodeTranslator;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import org.clapper.util.classutil.ClassFinder;

import java.io.File;
import java.net.URISyntaxException;

@SpirePatch(
        clz = CardCrawlGame.class,
        method = SpirePatch.CONSTRUCTOR
)
public class RawPatchTrigger {
    public static void Raw(CtBehavior ctBehavior) throws NotFoundException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException, CannotCompileException, BadBytecode {
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

        System.out.println("Generating card colors.");
        CharacterColorEnumFields.generate(pool);

        MultiplayerGeneration.patch(finder, pool);

        System.out.println("Raw patches complete.");

        //Testing bytecode translator
        BytecodeTranslator translator = new BytecodeTranslator(pool.get(RawPatchTrigger.class.getName()).getDeclaredMethod("TestMethod"));
        System.out.println(translator.translate());
    }

    public static void TestMethod() {
        int a = -1; //iconst_m1
        int b = 0; //iconst_0
        int c = 4; //iconst_4
        int d = 5; //iconst_5
        int e = 64; //bipush
        float f = 1; //dconst_1
        double g = 100; //ldc2_w
        int h = 0x7fffffff; //ldc

        b = c + d;
        f = e + f;

        int[] arr = new int[] { 128, 256 };
        a = arr[1];

        if (a > 0) {
            a = 0;
        }
        else {
            a = 999;
        }
    }

    public static void LongTest() {
        long a = 1;
        long b = 3483;
        long[] arr = new long[] { a, b };

        long c = a + b;

        int i = 1;
        ++c;
        ++i;
    }
}