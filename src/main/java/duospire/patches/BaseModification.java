package duospire.patches;

import javassist.ClassPool;
import org.clapper.util.classutil.ClassFinder;

public class BaseModification {
    public static void patch(ClassFinder finder, ClassPool pool) {
        //Replace stuff like `AbstractDungeon.player.update` and `render` with
        //PlayerRepresentation.update
        //This will be an instance that is appropriately set up where the update method will call the appropriate methods
        //To avoid excessive branching checks everywhere in the code
        //It will have a default value of calling the methods on AbstractDungeon.player
        //It should never be null. This should avoid having any problems with standard gameplay.
        //The only concern is making sure to reset it when multiplayer gameplay ends.

        //hasRelic/getRelic usages. Will players be treated as having separate relics or the same relics?
        //Currently leaning towards separate.
        //As separate, hasRelic/getRelic checks must be player specific.
        //For non-specific stuff like Unceasing Top, the logic would have to be applied to both players individually.
    }
}
