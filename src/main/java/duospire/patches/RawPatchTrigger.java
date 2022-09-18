package duospire.patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.patcher.PatchInfo;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import duospire.patches.gen.BaseModification;
import duospire.patches.gen.CharacterColorEnumFields;
import duospire.generation.MultiplayerGeneration;
import duospire.generation.BytecodeTranslator;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import org.clapper.util.classutil.ClassFinder;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.TreeMap;
import java.util.TreeSet;

public class RawPatchTrigger {
    public static void begin(CtBehavior ctBehavior) throws NotFoundException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException, CannotCompileException, BadBytecode {
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
        BaseModification.patch(finder, pool);

        System.out.println("Raw patches complete.");

        //Testing bytecode translator
        BytecodeTranslator translator = new BytecodeTranslator(pool.get(RawPatchTrigger.class.getName()).getDeclaredMethod("BytecodeTest"));
        System.out.println(translator.translate());
    }

    public static void BytecodeTest() {
        AbstractPlayer.PlayerClass enumVar = AbstractPlayer.PlayerClass.IRONCLAD;
        enumVar = AbstractPlayer.PlayerClass.THE_SILENT;

        /*long a = 1;
        long b = 3483;
        long[] arr = new long[] { a, b };

        long c = a + b;

        int i = 1;
        ++c;
        ++i;*/

        AbstractGameAction anonymousClass = new AbstractGameAction() {
            @Override
            public void update() {
                this.isDone = true;
            }
        };
    }
}