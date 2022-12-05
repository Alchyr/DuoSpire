package duospire.generation;

import basemod.Pair;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireOverride;
import com.evacipated.cardcrawl.modthespire.lib.SpireSuper;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.evacipated.cardcrawl.modthespire.patcher.javassist.MyCodeConverter;
import duospire.DuoSpire;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.clapper.util.classutil.ClassInfo;
import org.scannotation.AnnotationDB;

import java.lang.reflect.Field;
import java.util.*;

import static duospire.generation.MultiplayerGeneration.registerReplacement;

public class ClassGenerator {
    private static final String PACKAGE_PREFIX = "duospire.";
    private static final String NAME_PREFIX = "DuoSpire";

    private final Map<String, CtClass> classReplacements;
    private final Set<String> noReplace;
    private final Map<String, List<Pair<CtClass, List<CtClass>>>> dependent = new HashMap<>();

    private final Set<String> spireOverrideClasses = new HashSet<>();

    private Field isModified = null; //This is only part of CtClassType but the class is package-private making it inconvenient to reference directly.

    public ClassGenerator(Map<String, CtClass> classReplacements, Set<String> noReplace) {
        this.classReplacements = classReplacements;
        this.noReplace = noReplace;

        for (AnnotationDB db : Patcher.annotationDBMap.values()) {
            Set<String> classNames = db.getAnnotationIndex().get(SpireOverride.class.getName());
            if (classNames != null) {
                spireOverrideClasses.addAll(classNames);
            }
        }
    }


    public CtClass getReplacement(Class<?> original) {
        return classReplacements.get(original.getName());
    }
    public CtClass getReplacement(String originalName) {
        return classReplacements.get(originalName);
    }





    //Generation

    private boolean handled(CtClass clz) {
        return classReplacements.containsKey(clz.getName()) || noReplace.contains(clz.getName());
    }

    public boolean clean() {
        return dependent.isEmpty();
    }

    public int incomplete() {
        return dependent.size();
    }

    public void generateClasses(ClassPool pool, List<ClassInfo> baseClasses, List<CtClass> generated) throws NotFoundException {
        for (ClassInfo classInfo : baseClasses)
        {
            CtClass clz = pool.get(classInfo.getClassName());

            generateClass(pool, clz, generated);
        }
    }

    public void generateClass(ClassPool pool, CtClass clz, List<CtClass> generated) {
        try
        {
            if (handled(clz))
                return;

            Collection<?> references = clz.getRefClasses();
            for (Object o : references) {
                CtClass reference = pool.getOrNull(o.toString());
                if (reference == null) {
                    System.out.println("\t\t- Class " + clz.getSimpleName() + " refers to an unloaded class, " + o + ", and will be skipped.");
                    return;
                }
            }

            CtClass superclass = clz.getSuperclass();

            String origName = clz.getName();

            if (superclass != null) {
                if (!handled(superclass)) {
                    if (DuoSpire.FULL_DEBUG_LOGGING)
                        System.out.println("\t\t- Class " + clz.getSimpleName() + " waiting for " + superclass.getSimpleName() + ".");
                    dependent.compute(superclass.getName(), (k, v) -> {
                        if (v == null)
                            v = new ArrayList<>();
                        v.add(new Pair<>(clz, generated));
                        return v;
                    });
                    return;
                }
            }

            String newName = clz.getPackageName();
            if (newName == null)
                newName = PACKAGE_PREFIX;
            else
                newName = PACKAGE_PREFIX + newName + ".";
            newName += NAME_PREFIX + clz.getSimpleName();

            CtClass newClz = pool.getAndRename(clz.getName(), newName);

            if (superclass != null) {
                if (classReplacements.containsKey(superclass.getName())) {
                    newClz.setSuperclass(classReplacements.get(superclass.getName()));
                }
                if (spireOverrideClasses.contains(origName)) {
                    processSpireSuper(newClz);
                }
            }

            try {
                if (isModified == null) {
                    isModified = newClz.getClass().getDeclaredField("wasChanged");
                    isModified.setAccessible(true);
                }
                isModified.set(newClz, true);

                generated.add(newClz);

                if (DuoSpire.FULL_DEBUG_LOGGING)
                    System.out.println("\t\t- Copy generated: " + newClz.getSimpleName());

                registerReplacement(origName, newClz);
                //I also might be creating modified constructors, so I'll need to add the modified parameter info as well.

                List<Pair<CtClass, List<CtClass>>> dependencies = dependent.remove(origName);
                if (dependencies != null) {
                    for (Pair<CtClass, List<CtClass>> dependency : dependencies) {
                        generateClass(pool, dependency.getKey(), dependency.getValue());
                    }
                }
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                System.out.println("\t\t- Failed to mark new class as modified: " + clz.getSimpleName());
            } //This ensures it will be compiled by ModTheSpire.
        } catch (Exception e) {
            System.out.println("\t\t- Error occurred while generating copy of class: " + clz.getSimpleName() + "\n");
            e.printStackTrace();
        }
    }


    //Modified duplicate of original patchOverrides method
    private void processSpireSuper(CtClass clz) {
        try {
            for (CtMethod ctMethod : clz.getDeclaredMethods()) {
                if (ctMethod.hasAnnotation(SpireOverride.class)) {
                    CtMethod superMethod = findSuperMethod(ctMethod);
                    if (superMethod == null) {
                        throw new PatchingException(ctMethod, "Has no matching method signature in any superclass");
                    }

                    MyCodeConverter codeConverter = new MyCodeConverter();
                    codeConverter.redirectSpecialMethodCall(superMethod);
                    superMethod.getDeclaringClass().instrument(codeConverter);

                    ExprEditor exprEditor = new ExprEditor() {
                        @Override
                        public void edit(MethodCall m) throws CannotCompileException
                        {
                            try {
                                if (m.getClassName().equals(SpireSuper.class.getName())) {
                                    StringBuilder src = new StringBuilder(" { ");
                                    if (!ctMethod.getReturnType().equals(CtClass.voidType)) {
                                        src.append("$_ = ");
                                    }
                                    src.append("super.").append(ctMethod.getName()).append("(");
                                    for (int i=0; i<ctMethod.getParameterTypes().length; ++i) {
                                        if (i > 0) {
                                            src.append(", ");
                                        }
                                        src.append(makeObjectCastedString(ctMethod.getParameterTypes()[i], "$1[" + i + "]"));
                                    }
                                    src.append(");\n");
                                    if (ctMethod.getReturnType().equals(CtClass.voidType)) {
                                        src.append("$_ = null;");
                                    }
                                    src.append(" }");
                                    m.replace(src.toString());
                                }
                            } catch (NotFoundException e) {
                                throw new CannotCompileException(e);
                            }
                        }
                    };
                    ctMethod.instrument(exprEditor);
                }
            }
        } catch (NotFoundException | CannotCompileException | PatchingException e) {
            throw new RuntimeException(e);
        }
    }



    private static CtMethod findSuperMethod(CtMethod ctMethod) throws NotFoundException
    {
        CtClass superclass = ctMethod.getDeclaringClass().getSuperclass();

        while (superclass != null) {
            try {
                CtMethod superMethod = superclass.getDeclaredMethod(ctMethod.getName(), ctMethod.getParameterTypes());
                if (ctMethod.getReturnType().equals(superMethod.getReturnType())) {
                    return superMethod;
                }
            } catch (NotFoundException ignored) {
            }

            superclass = superclass.getSuperclass();
        }

        return null;
    }

    private static String makeObjectCastedString(CtClass ctType, String value)
    {
        String typename = ctType.getName();
        String extra = "";
        if (ctType.isPrimitive()) {
            if (ctType.equals(CtPrimitiveType.intType)) {
                typename = "Integer";
            } else if (ctType.equals(CtPrimitiveType.charType)) {
                typename = "Character";
            } else {
                typename = typename.substring(0, 1).toUpperCase() + typename.substring(1);
            }

            extra = "." + ctType.getName() + "Value()";
        }

        return "((" + typename + ") " + value + ")" + extra;
    }
}
