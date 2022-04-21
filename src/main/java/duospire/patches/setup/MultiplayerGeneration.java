package duospire.patches.setup;

import basemod.BaseMod;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import duospire.DuoSpire;
import javassist.*;
import javassist.bytecode.Descriptor;
import javassist.expr.*;
import org.clapper.util.classutil.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MultiplayerGeneration {
    private static final String PACKAGE_PREFIX = "duospire.";
    private static final String NAME_PREFIX = "DuoSpire";

    private static final HashMap<String, CtClass> classReplacements = new HashMap<>();
    private static final ClassMap classMap = new ClassMap();

    private static final List<CtClass> generated = new ArrayList<>();
    private static final List<CtClass> generatedCards = new ArrayList<>(); //Cards *only* go in this list as they need specific modifications.

    private static Field isModified; //This is only part of CtClassType but the class is package-private making it inconvenient to reference directly.

    //private static CtClass ctAbstractCard = null;

    private static void registerReplacement(String original, CtClass replacement) {
        classReplacements.put(original, replacement);
        classMap.put(original, replacement.getName());
    }

    public static void patch(ClassFinder finder, ClassPool pool) throws NotFoundException {
        System.out.println("Generating multiplayer card and relic variants.");

        classReplacements.clear();
        classMap.clear();

        ClassFilter cardFilter = new AndClassFilter(
                new NotClassFilter(new InterfaceOnlyClassFilter()),
                new SubclassClassFilter(AbstractCard.class)
        );
        ClassFilter actionFilter = new AndClassFilter(
                new NotClassFilter(new InterfaceOnlyClassFilter()),
                new SubclassClassFilter(AbstractGameAction.class)
        );

        //ctAbstractCard = pool.get(AbstractCard.class.getName());

        ArrayList<ClassInfo> actionClasses = new ArrayList<>();
        finder.findClasses(actionClasses, actionFilter);

        generateActions(pool, actionClasses);

        ArrayList<ClassInfo> cardClasses = new ArrayList<>();
        finder.findClasses(cardClasses, cardFilter);

        generateCards(pool, cardClasses);

        replaceClasses(pool);

        registerCards(pool);
    }

    private static void generateActions(ClassPool pool, List<ClassInfo> actionClasses) throws NotFoundException {
        System.out.println("\t- Generating action copies.");

        Collection<String> references;

        outer:
        for (ClassInfo classInfo : actionClasses)
        {
            CtClass actionClass = pool.get(classInfo.getClassName());

            try
            {
                references = actionClass.getRefClasses();
                for (String s : references) {
                    CtClass reference = pool.getOrNull(s);
                    if (reference == null) {
                        System.out.println("\t\t- Class " + actionClass.getSimpleName() + " refers to an unloaded class, " + s + ", and will be skipped.");
                        continue outer;
                    }
                }

                String origName = actionClass.getName();
                String newName = actionClass.getPackageName();
                if (newName == null)
                    newName = PACKAGE_PREFIX;
                else
                    newName = PACKAGE_PREFIX + newName + ".";
                newName += NAME_PREFIX + actionClass.getSimpleName();

                actionClass.setName(newName);

                try {
                    if (isModified == null) {
                        isModified = actionClass.getClass().getDeclaredField("wasChanged");
                        isModified.setAccessible(true);
                    }
                    isModified.set(actionClass, true);
                    generated.add(actionClass);
                    registerReplacement(origName, actionClass);
                    //I also might be creating modified constructors, so I'll need to add the modified parameter info as well.
                }
                catch (NoSuchFieldException | IllegalAccessException e) {
                    System.out.println("\t\t- Failed to mark new action as modified: " + actionClass.getSimpleName());
                } //This ensures it will be compiled by ModTheSpire.
            } catch(Exception e) {
                System.out.println("\t\t- Error occurred while generating copy of class: " + actionClass.getSimpleName() + "\n");
                e.printStackTrace();
            }
        }
    }

    private static void generateCards(ClassPool pool, List<ClassInfo> cardClasses) throws NotFoundException {
        System.out.println("\t- Generating card copies.");

        Collection<String> references;

        outer:
        for (ClassInfo classInfo : cardClasses)
        {
            CtClass cardClass = pool.get(classInfo.getClassName());

            try
            {
                references = cardClass.getRefClasses();
                for (String s : references) {
                    CtClass reference = pool.getOrNull(s);
                    if (reference == null) {
                        System.out.println("\t\t- Class " + cardClass.getSimpleName() + " refers to an unloaded class, " + s + ", and will be skipped.");
                        continue outer;
                    }
                }

                String origName = cardClass.getName();
                String newName = cardClass.getPackageName();
                if (newName == null)
                    newName = PACKAGE_PREFIX;
                else
                    newName = PACKAGE_PREFIX + newName + ".";
                newName += NAME_PREFIX + cardClass.getSimpleName();

                cardClass.setName(newName);

                try {
                    if (isModified == null) {
                        isModified = cardClass.getClass().getDeclaredField("wasChanged");
                        isModified.setAccessible(true);
                    }
                    isModified.set(cardClass, true);
                    generatedCards.add(cardClass);
                    registerReplacement(origName, cardClass);
                }
                catch (NoSuchFieldException | IllegalAccessException e) {
                    System.out.println("\t\t- Failed to mark new card as modified: " + cardClass.getSimpleName());
                } //This ensures it will be compiled by ModTheSpire.
            } catch(Exception e) {
                System.out.println("\t\t- Error occurred while generating copy of class: " + cardClass.getSimpleName() + "\n");
                e.printStackTrace();
            }
        }
    }

    private static void replaceClasses(ClassPool pool) {
        System.out.println("\t- Replacing references.");
        //need to replace any references to the original actions in the actions themselves as well

        ClassReplacer generalAdjuster = new ClassReplacer(classReplacements);
        CardAdjuster cardAdjuster = new CardAdjuster(classReplacements);

        adjustFieldsAndSignatures(generated);
        adjustFieldsAndSignatures(generatedCards);

        adjust(generated, generalAdjuster);
        adjust(generatedCards, cardAdjuster);
    }

    private static void adjustFieldsAndSignatures(List<CtClass> toAdjust) {
        CtField[] fields;
        CtConstructor[] constructors;
        CtMethod[] methods;

        for (CtClass clz : toAdjust) {
            try {
                //Fields
                {
                    fields = clz.getDeclaredFields();
                    for (CtField field : fields) {
                        String fieldType = field.getType().getName();
                        CtClass replacement = classReplacements.get(fieldType);
                        if (replacement != null) {
                            String name = field.getName();
                            int modifiers = field.getModifiers();
                            clz.removeField(field);
                            CtField alt = new CtField(replacement, name, clz);
                            alt.setModifiers(modifiers);
                            clz.addField(alt);
                            System.out.println("\t\t- Changed type of field " + name + " to " + replacement.getSimpleName());
                        }
                    }
                }
                //Constructors
                {
                    constructors = clz.getDeclaredConstructors();

                    for (CtConstructor con : constructors) {
                        if (con.isClassInitializer())
                            continue;

                        CtClass[] params = con.getParameterTypes();
                        if (params == null)
                            continue;

                        CtClass[] altParams = null;
                        for (int i = 0; i < params.length; ++i) {
                            CtClass replacement = classReplacements.get(params[i].getName());
                            if (replacement != null) {
                                if (altParams == null) {
                                    altParams = new CtClass[params.length];
                                    System.arraycopy(params, 0, altParams, 0, i);
                                }
                                altParams[i] = replacement;
                            }
                            else if (altParams != null) {
                                altParams[i] = params[i];
                            }
                        }

                        if (altParams != null) {
                            clz.removeConstructor(con);
                            CtConstructor replacement = new CtConstructor(altParams, clz);
                            replacement.setModifiers(con.getModifiers());
                            replacement.setExceptionTypes(con.getExceptionTypes());
                            replacement.setBody(con, classMap);
                            clz.addConstructor(replacement);
                        }
                    }
                }
                //Methods
                {
                    methods = clz.getDeclaredMethods();

                    for (CtMethod mth : methods) {
                        CtClass[] params = mth.getParameterTypes();
                        if (params == null)
                            continue;

                        CtClass[] altParams = null;
                        for (int i = 0; i < params.length; ++i) {
                            CtClass replacement = classReplacements.get(params[i].getName());
                            if (replacement != null) {
                                if (altParams == null) {
                                    altParams = new CtClass[params.length];
                                    System.arraycopy(params, 0, altParams, 0, i);
                                }
                                altParams[i] = replacement;
                            }
                            else if (altParams != null) {
                                altParams[i] = params[i];
                            }
                        }

                        if (altParams != null) {
                            clz.removeMethod(mth);
                            //Test:
                            CtMethod replacement = new CtMethod(mth, clz, classMap);
                            clz.addMethod(replacement);
                        /*CtMethod replacement = new CtMethod(altParams, clz);
                        replacement.setModifiers(con.getModifiers());
                        replacement.setExceptionTypes(con.getExceptionTypes());
                        replacement.setBody(con, classMap);
                        clz.addConstructor(replacement);*/
                        }
                    }
                }
            } catch (NotFoundException | CannotCompileException e) {
                System.out.println("\t\t- Error occurred while patching class: " + clz.getSimpleName() + "\n");
                e.printStackTrace();
            }
        }
    }

    private static void adjust(List<CtClass> toAdjust, ClassReplacer adjuster) {
        CtConstructor[] constructors;
        CtMethod[] methods;

        for (CtClass clz : toAdjust) {
            try {
                constructors = clz.getDeclaredConstructors();
                adjuster.modifyConstructors(clz, constructors);

                methods = clz.getDeclaredMethods();
                adjuster.modifyMethods(clz, methods);
            } catch (CannotCompileException e) {
                System.out.println("\t\t- Error occurred while patching class: " + clz.getSimpleName() + "\n");
                e.printStackTrace();
            }
        }
    }

    private static class CardAdjuster extends ClassReplacer {
        public CardAdjuster(HashMap<String, CtClass> classReplacements) {
            super(classReplacements);
        }

        boolean callsSuper = false;
        @Override
        public void modifyConstructors(CtClass clz, CtConstructor[] constructors) throws CannotCompileException {
            for (CtConstructor constructor : constructors) {
                callsSuper = false;
                constructor.instrument(this);
                if (callsSuper) {
                    constructor.insertAfter("this.cardID = \"" + NAME_PREFIX + ":\" + this.cardID;");
                }
            }
            //I could theoretically go through the constructor calls and figure out what parameter is used as ID and then modify it, but that sounds like a pain in the ass
        }

        @Override
        public void edit(ConstructorCall c) throws CannotCompileException {
            super.edit(c);
            callsSuper = c.isSuper();
        }
    }

    private static class ClassReplacer extends ExprEditor {
        final HashMap<String, CtClass> classReplacements;

        public ClassReplacer(HashMap<String, CtClass> classReplacements) {
            this.classReplacements = classReplacements;
        }

        public void modifyConstructors(CtClass clz, CtConstructor[] constructors) throws CannotCompileException {
            for (CtConstructor constructor : constructors) {
                constructor.instrument(this);
            }
        }

        public void modifyMethods(CtClass clz, CtMethod[] methods) throws CannotCompileException {
            for (CtMethod method : methods) {
                method.instrument(this);
            }
        }

        //Constructor call to `this(something)` or `super(something)` in a constructor.
        @Override
        public void edit(ConstructorCall c) throws CannotCompileException {
            super.edit(c);
        }

        @Override
        public void edit(NewExpr e) throws CannotCompileException {
            CtClass replacement = classReplacements.get(e.getClassName());

            if (replacement != null) {
                e.replace("$_ = new " + replacement.getName() + "($$);");
            }
        }

        @Override
        public void edit(NewArray a) throws CannotCompileException {
            CtClass replacement;
            try {
                replacement = classReplacements.get(a.getComponentType().getName());
            } catch (NotFoundException e) {
                e.printStackTrace();
                return;
            }

            if (replacement != null) {
                int dims = a.getCreatedDimensions();
                StringBuilder arrDef = new StringBuilder("$_ = new ").append(replacement.getName());
                for (int i = 0; i < dims; ++i) {
                    arrDef.append('[').append('$').append(i + 1).append(']');
                }
                arrDef.append(';');
                a.replace(arrDef.toString());
            }
        }

        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            super.edit(m);
        }

        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
            super.edit(f);
        }

        @Override
        public void edit(Instanceof i) throws CannotCompileException {
            CtClass replacement;
            try {
                replacement = classReplacements.get(i.getType().getName());
            } catch (NotFoundException e) {
                e.printStackTrace();
                return;
            }

            if (replacement != null) {
                i.replace("$_ = $1 instanceof " + replacement.getName() + ";");
            }
        }

        @Override
        public void edit(Cast c) throws CannotCompileException {
            CtClass replacement;
            try {
                replacement = classReplacements.get(c.getType().getName());
            } catch (NotFoundException e) {
                e.printStackTrace();
                return;
            }

            if (replacement != null) {
                c.replace("$_ = (" + replacement.getName() + ") $1;");
            }
        }
    }

    private static void registerCards(ClassPool pool) {
        try {
            CtClass duoSpireBase = pool.get(DuoSpire.class.getName());

            CtMethod registerCards = duoSpireBase.getDeclaredMethod("receiveEditCards");

            StringBuilder cardRegistration = new StringBuilder("{");

            String noParams = Descriptor.ofParameters(new CtClass[0]);

            for (CtClass cardClass : generatedCards) {
                try {
                    for (CtConstructor con : cardClass.getConstructors()) {
                        if (con.isConstructor() && con.getMethodInfo2().getDescriptor().startsWith(noParams)) {
                            //Found no-parameter constructor. Safe to register.
                            cardRegistration.append(BaseMod.class.getName()).append(".addCard(new ")
                                    .append(cardClass.getName()).append("());");
                        }
                    }
                }
                catch (Exception e) {
                    System.out.println("\t- Error occurred while generating card registration for " + cardClass.getSimpleName());
                    e.printStackTrace();
                }
            }
            cardRegistration.append('}');

            registerCards.setBody(cardRegistration.toString());
        } catch (NotFoundException | CannotCompileException e) {
            System.out.println("\t- Error occurred while attempted to generate card registration.");
            e.printStackTrace();
        }
    }
}
