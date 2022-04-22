package duospire.patches.gen;

import basemod.BaseMod;
import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import duospire.DuoSpire;
import duospire.util.BytecodeTranslator;
import duospire.util.NotDeprecatedFilter;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.MethodInfo;
import javassist.expr.*;
import org.clapper.util.classutil.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class MultiplayerGeneration {
    private static final String PACKAGE_PREFIX = "duospire.";
    private static final String NAME_PREFIX = "DuoSpire";

    private static final Set<String> noReplace = new HashSet<>();
    static {
        noReplace.add(AbstractCard.class.getName());
        //noReplace.add(CustomCard.class.getName());
        noReplace.add(AbstractGameAction.class.getName());
        noReplace.add(Object.class.getName()); //hopefully nothing gets this far.
    }
    private static final Map<String, CtClass> classReplacements = new HashMap<>();
    private static final Map<CtClass, String> superclasses = new HashMap<>();
    private static final ClassMap classMap = new ClassMap();

    private static final Map<String, List<CtClass>> dependent = new HashMap<>();

    private static final List<CtClass> generated = new ArrayList<>();
    private static final List<CtClass> generatedCards = new ArrayList<>(); //Cards *only* go in this list as they need specific modifications.

    private static Field isModified; //This is only part of CtClassType but the class is package-private making it inconvenient to reference directly.

    //private static CtClass ctAbstractCard = null;

    private static void registerReplacement(String original, CtClass replacement) {
        if (original.equals(replacement.getName())) {
            System.out.println("\t- UNCHANGED NAME?");
        }
        classReplacements.put(original, replacement);
        classMap.put(original, replacement.getName());
    }

    public static void patch(ClassFinder finder, ClassPool pool) throws NotFoundException {
        System.out.println("Generating multiplayer card and relic variants.");

        classReplacements.clear();
        classMap.clear();

        ClassFilter cardFilter = new AndClassFilter(
                new NotClassFilter(new InterfaceOnlyClassFilter()),
                new NotDeprecatedFilter(),
                new SubclassClassFilter(AbstractCard.class)
        );
        ClassFilter actionFilter = new AndClassFilter(
                new NotClassFilter(new InterfaceOnlyClassFilter()),
                new NotDeprecatedFilter(),
                new SubclassClassFilter(AbstractGameAction.class)
        );

        //ctAbstractCard = pool.get(AbstractCard.class.getName());

        ArrayList<ClassInfo> actionClasses = new ArrayList<>();
        finder.findClasses(actionClasses, actionFilter);

        generateClasses(pool, actionClasses, generated);

        ArrayList<ClassInfo> cardClasses = new ArrayList<>();
        finder.findClasses(cardClasses, cardFilter);

        generateClasses(pool, cardClasses, generatedCards);

        if (!dependent.isEmpty()) {
            System.out.println("\t- Superclasses for " + dependent.size() + " classes were not generated.");
        }

        ///TODO: If classes extend another class, mark them as incomplete.
        //I'll have to generate a new class extending the altered base class first, then transfer everything to that new class to make it a copy of the original.
        //Mark them as "dependent" on their base class.
        //Then update them as their dependencies are dealt with.

        //Specifically: For each class, check its base class. If that base class requires processing, put it in a hashmap of base class -> list of classes
        //Whenever a class is processed, check for any dependencies that are now resolved and update those next.

        replaceClasses(pool);

        registerCards(pool);

    }

    private static boolean handled(CtClass clz) {
        return classReplacements.containsKey(clz.getName()) || noReplace.contains(clz.getName());
    }

    private static void generateClasses(ClassPool pool, List<ClassInfo> baseClasses, List<CtClass> generated) throws NotFoundException {
        System.out.println("\t- Generating action copies.");

        for (ClassInfo classInfo : baseClasses)
        {
            CtClass clz = pool.get(classInfo.getClassName());

            generateClass(pool, clz, generated);
        }
    }

    private static void generateClass(ClassPool pool, CtClass clz, List<CtClass> generated) {
        try
        {
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
                    dependent.compute(superclass.getName(), (k, v)-> {
                        if (v == null)
                            v = new ArrayList<>();
                        v.add(clz);
                        return v;
                    });
                    return;
                }
                else if (classReplacements.containsKey(superclass.getName())) {
                    String newName = clz.getPackageName();
                    if (newName == null)
                        newName = PACKAGE_PREFIX;
                    else
                        newName = PACKAGE_PREFIX + newName + ".";
                    newName += NAME_PREFIX + clz.getSimpleName();

                    clz.setName(newName);
                    clz.setSuperclass(classReplacements.get(superclass.getName()));
                    superclasses.put(clz, superclass.getName());
                }
                else {
                    //Non-replaced superclass
                    String newName = clz.getPackageName();
                    if (newName == null)
                        newName = PACKAGE_PREFIX;
                    else
                        newName = PACKAGE_PREFIX + newName + ".";
                    newName += NAME_PREFIX + clz.getSimpleName();

                    clz.setName(newName);
                }
            }
            else {
                //No superclass...?
                String newName = clz.getPackageName();
                if (newName == null)
                    newName = PACKAGE_PREFIX;
                else
                    newName = PACKAGE_PREFIX + newName + ".";
                newName += NAME_PREFIX + clz.getSimpleName();

                clz.setName(newName);
            }


            try {
                if (isModified == null) {
                    isModified = clz.getClass().getDeclaredField("wasChanged");
                    isModified.setAccessible(true);
                }
                isModified.set(clz, true);

                generated.add(clz);
                registerReplacement(origName, clz);
                //I also might be creating modified constructors, so I'll need to add the modified parameter info as well.

                List<CtClass> dependencies = dependent.remove(origName);
                if (dependencies != null) {
                    for (CtClass dependency : dependencies) {
                        generateClass(pool, dependency, generated);
                    }
                }
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                System.out.println("\t\t- Failed to mark new class as modified: " + clz.getSimpleName());
            } //This ensures it will be compiled by ModTheSpire.
        } catch(Exception e) {
            System.out.println("\t\t- Error occurred while generating copy of class: " + clz.getSimpleName() + "\n");
            e.printStackTrace();
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
                            CtConstructor replacement = new CtConstructor(con, clz, classMap);
                            /*CtConstructor replacement = new CtConstructor(altParams, clz);
                            replacement.setModifiers(con.getModifiers());
                            replacement.setExceptionTypes(con.getExceptionTypes());
                            replacement.setBody(con, classMap);*/
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
                            CtMethod replacement = new CtMethod(mth, clz, classMap);
                            clz.addMethod(replacement);
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
            } catch (CannotCompileException | BadBytecode | NotFoundException e) {
                System.out.println("\t\t- Error occurred while patching class: " + clz.getSimpleName() + "\n");
                e.printStackTrace();
            }
        }
    }

    private static class CardAdjuster extends ClassReplacer {
        public CardAdjuster(Map<String, CtClass> classReplacements) {
            super(classReplacements);
        }

        boolean callsSuper = false;
        @Override
        public void modifyConstructors(CtClass clz, CtConstructor[] constructors) throws CannotCompileException {
            currentClass = clz;
            for (CtConstructor constructor : constructors) {
                currentMember = constructor;
                callsSuper = false;
                constructor.instrument(this);
                if (callsSuper) {
                    constructor.insertAfter(
                        "{" +
                                "this.cardID = \"" + NAME_PREFIX + ":\" + this.cardID;" +
                                "this.color = " + EnumPlace.class.getName() + ".getAltColor(this.color);" +
                        "}");

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
        final Map<String, CtClass> classReplacements;
        CtClass currentClass = null;
        CtMember currentMember = null;
        Set<CtMember> bytecodeLater = new HashSet<>();

        public ClassReplacer(Map<String, CtClass> classReplacements) {
            this.classReplacements = classReplacements;
        }

        public void modifyConstructors(CtClass clz, CtConstructor[] constructors) throws CannotCompileException, NotFoundException, BadBytecode {
            currentClass = clz;
            bytecodeLater.clear();
            for (CtConstructor constructor : constructors) {
                currentMember = constructor;
                constructor.instrument(this);
            }

            for (CtMember member : bytecodeLater) {
                if (member instanceof CtConstructor) {
                    fixBody(clz, (CtConstructor) member);
                }
            }
        }

        public void modifyMethods(CtClass clz, CtMethod[] methods) throws CannotCompileException, BadBytecode, NotFoundException {
            currentClass = clz;
            bytecodeLater.clear();
            for (CtMethod method : methods) {
                currentMember = method;
                method.instrument(this);
            }

            for (CtMember member : bytecodeLater) {
                if (member instanceof CtMethod) {
                    fixBody(clz, (CtMethod) member);
                }
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
            CtClass replacement = classReplacements.get(m.getClassName());
            if (replacement != null) {
                CtMethod method;
                try {
                    method = m.getMethod();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                if (Modifier.isStatic(method.getModifiers())) {
                    System.out.println("\t- Unmodified static method call: " + method.getLongName());
                    //m.replace("$_ = " + replacement.getName() + "." + method.getName() + "($$);");
                }
                else if (m.isSuper()) {
                    System.out.println("\t- Super call to incorrect class.");
                    /*if (currentClass != null) {
                        String origSuper = superclasses.get(currentClass);
                        m.replace("$_ = " + replacement.getName() + ".this." + method.getName() + "($$);");
                    }*/
                    bytecodeLater.add(currentMember);
                }
                else {
                    //System.out.println("\t- Unhandled method call to incorrect class.");
                    m.replace("$_ = $proceed($$);");
                }
            }
        }

        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
            CtClass replacement = classReplacements.get(f.getClassName());
            if (replacement != null) {
                //System.out.println("\t- Unhandled field access to incorrect class.");
                f.replace("$_ = $proceed($$);");
            }
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

        protected void fixBody(CtClass clz, CtMethod method) throws BadBytecode, NotFoundException {
            //MethodInfo info = method.getMethodInfo();
            System.out.println(clz.getName() + '.' + method.getName());
            System.out.println(new BytecodeTranslator(method).translate());
            //Determining the called method/class requires the const pool
            //methodInfo.getConstPool();
        }
        protected void fixBody(CtClass clz, CtConstructor constructor) throws BadBytecode, NotFoundException {
            //MethodInfo info = method.getMethodInfo();
            System.out.println(clz.getName() + '.' + constructor.getName());
            System.out.println(new BytecodeTranslator(constructor).translate());
            //Determining the called method/class requires the const pool
            //methodInfo.getConstPool();
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
