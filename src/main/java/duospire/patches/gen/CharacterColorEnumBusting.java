package duospire.patches.gen;

import com.evacipated.cardcrawl.modthespire.*;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.evacipated.cardcrawl.modthespire.patcher.PatchInfo;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import duospire.annotations.DynamicEnum;
import duospire.patches.init.PostStandardInit;
import javassist.ClassPool;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeSet;

public class CharacterColorEnumBusting {
    public enum DUMMY {
        AMOGUS,
        SUS
    }
    @SpireEnum
    public static DUMMY IMPOSTER;
    //Used to trigger the class to be loaded at the correct time (during the enum process)

    private static Field enumBusterMapField;
    private static Field classLoaderField;

    static {
        try {
            enumBusterMapField = Patcher.class.getDeclaredField("enumBusterMap");
            enumBusterMapField.setAccessible(true);

            classLoaderField = MTSClassPool.class.getDeclaredField("classLoader");
            classLoaderField.setAccessible(true);

            Field poolField = Loader.class.getDeclaredField("POOL");
            poolField.setAccessible(true);

            generate((ClassPool) poolField.get(null));
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    public static void generate(ClassPool pool) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        ClassLoader loader;
        if (pool instanceof MTSClassPool) {
            loader = (ClassLoader) classLoaderField.get(pool);
        }
        else {
            System.out.println("Unable to obtain temporary patching class loader.");
            return;
        }

        Map<Class<?>, EnumBusterReflect> enumBusterMap = (Map<Class<?>, EnumBusterReflect>) enumBusterMapField.get(null);
        Class<?> enumStorage = duospire.patches.gen.EnumPlace.class; //loader.loadClass(EnumPlace.class.getName());

        for (Field field : enumStorage.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                DynamicEnum dynamicEnum = field.getDeclaredAnnotation(DynamicEnum.class);
                if (dynamicEnum != null) {
                    String enumName = field.getName();
                    if (dynamicEnum.origin().isEmpty())
                        continue;
                    String origin = dynamicEnum.origin();
                    if (!dynamicEnum.name().isEmpty())
                        enumName = dynamicEnum.name();

                    EnumBusterReflect buster;
                    if (enumBusterMap.containsKey(field.getType())) {
                        buster = enumBusterMap.get(field.getType());
                    } else {
                        buster = new EnumBusterReflect(loader, field.getType());
                        enumBusterMap.put(field.getType(), buster);
                    }
                    Enum<?> enumValue = buster.make(enumName);
                    buster.addByValue(enumValue);
                    try {
                        Field constantField = field.getType().getField(enumName);
                        ReflectionHelper.setStaticFinalField(constantField, enumValue);
                    } catch (NoSuchFieldException ignored) {
                        System.out.println("\t\t- Failed to initialize enum field " + enumName);
                    }

                    field.setAccessible(true);
                    field.set(null, enumValue);

                    PostStandardInit.registerEnumLater(origin, enumValue);
                }
            }
        }
    }
}
