package duospire.patches;

import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.patcher.PatchInfo;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;

import java.lang.reflect.Field;
import java.util.TreeSet;

@SpirePatch(
        clz = CardCrawlGame.class,
        method = SpirePatch.CONSTRUCTOR
)
public class DummyInsertPatch {
    @SpireInsertPatch(
            locator = Locator.class
    )
    public static void patchMethod() { }

    public static class Locator extends SpireInsertLocator {
        static boolean addedFinalPatch = false;

        @Override
        public int[] Locate(CtBehavior ctBehavior) throws Exception {
            addedFinalPatch = true;
            System.out.println("Attempting to register final patch.");

            try {
                Field f = Patcher.class.getDeclaredField("patchInfos");
                f.setAccessible(true);
                TreeSet<PatchInfo> patchInfos = (TreeSet<PatchInfo>) f.get(null);

                patchInfos.add(new PatchInfo(null, null) {
                    @Override
                    public void doPatch() throws PatchingException {
                        try {
                            RawPatchTrigger.begin(ctBehavior);
                        } catch (NotFoundException | IllegalAccessException | NoSuchFieldException | ClassNotFoundException | CannotCompileException | BadBytecode e) {
                            throw new PatchingException(e);
                        }
                    }

                    @Override
                    public void debugPrint() {
                        System.out.println("Performing DuoSpire raw patches.");
                    }

                    @Override
                    protected String debugMsg() {
                        return "";
                    }

                    @Override
                    public int patchOrdering() {
                        return 99;
                    }
                });
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return new int[] { 0 };
        }
    }
}
