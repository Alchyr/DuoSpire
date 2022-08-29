package duospire.patches.gen;

import basemod.BaseMod;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import duospire.DuoSpire;
import duospire.annotations.DynamicEnum;
import duospire.patches.init.PostStandardInit;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationImpl;
import javassist.bytecode.annotation.StringMemberValue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class CharacterColorEnumFields {
    private static final String ENUM_SUFFIX = "_DUOSPIRE";
    private static final String LIBRARY_SUFFIX = "_LIBRARY";

    //Step 1: Generate the fields in the enums
    //Importantly, this happens after all standard @SpireEnums have their fields generated, so it should be able to see them.
    public static void generate(ClassPool pool) throws NotFoundException, CannotCompileException {
        CtClass enumStorage = pool.get(EnumPlace.class.getName());
        CtClass cardColorClass = pool.get(AbstractCard.CardColor.class.getName());
        CtClass libraryTypeClass = pool.get(CardLibrary.LibraryType.class.getName());
        CtField[] enumFields = cardColorClass.getDeclaredFields();

        List<CtField> cardColorFields = new ArrayList<>();

        for (CtField colorEnum : enumFields) {
            if (!colorEnum.getName().equals("$VALUES") && colorEnum.getType().equals(cardColorClass)) {
                CtField created = generateCardColor(enumStorage, cardColorClass, libraryTypeClass, colorEnum.getName());
                if (created != null)
                    cardColorFields.add(created);
            }
        }

        //Step 2: Generate the code to register the created card colors.
        generateColorRegistration(pool, cardColorFields);
    }

    private static CtField generateCardColor(CtClass enumStorage, CtClass colorType, CtClass libraryType, String origName) throws NotFoundException, CannotCompileException {
        // Patch new field onto the enum
        try {
            ConstPool storagePool = enumStorage.getClassFile().getConstPool();

            //Add the color type field to enum storage to hold the instance+attached information
            CtField colorField = new CtField(colorType, origName + CharacterColorEnumFields.ENUM_SUFFIX, enumStorage);
            colorField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            addSpireEnumAnnotation(storagePool, colorField, origName + ENUM_SUFFIX, origName);
            enumStorage.addField(colorField);

            //Add the color type field to the color type enum
            CtField f = new CtField(colorType, origName + CharacterColorEnumFields.ENUM_SUFFIX, colorType);
            f.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL | Modifier.ENUM);
            annotateEnum(colorField, colorType, f);
            colorType.addField(f);

            //Add the library type field to enum storage
            CtField libraryField = new CtField(libraryType, origName + CharacterColorEnumFields.ENUM_SUFFIX + LIBRARY_SUFFIX, enumStorage);
            libraryField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            addSpireEnumAnnotation(storagePool, libraryField, origName + ENUM_SUFFIX, origName);
            enumStorage.addField(libraryField);

            //Add the library type field to the library type enum
            f = new CtField(libraryType, origName + CharacterColorEnumFields.ENUM_SUFFIX, libraryType);
            f.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL | Modifier.ENUM);
            annotateEnum(libraryField, libraryType, f);
            libraryType.addField(f);

            return colorField;
        } catch (DuplicateMemberException ignore) {
            // Field already exists
            System.out.printf("Warning: Enum %s %s is already defined.%n", colorType.getName(), origName);
        }
        return null;
    }

    private static void annotateEnum(CtField varField, CtClass enumType, CtField enumField) {
        ConstPool constPool = enumType.getClassFile().getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        for (Object a : varField.getAvailableAnnotations()) {
            if (Proxy.getInvocationHandler(a) instanceof AnnotationImpl) {
                AnnotationImpl impl = (AnnotationImpl) Proxy.getInvocationHandler(a);
                if (impl.getTypeName().equals(DynamicEnum.class.getName())) {
                    continue; //skip this one
                }
                Annotation annotation = new Annotation(impl.getTypeName(), constPool);
                if (impl.getAnnotation().getMemberNames() != null) {
                    for (Object memberName : impl.getAnnotation().getMemberNames()) {
                        annotation.addMemberValue((String) memberName, impl.getAnnotation().getMemberValue((String) memberName));
                    }
                }
                attr.addAnnotation(annotation);
            }
        }
        enumField.getFieldInfo().addAttribute(attr);
    }

    private static final String dynamicEnumAnnotation = "duospire/annotations/DynamicEnum";
    private static void addSpireEnumAnnotation(ConstPool constPool, CtField f, String enumName, String originalName) {
        Annotation dynamicEnum = new Annotation(dynamicEnumAnnotation, constPool);
        dynamicEnum.addMemberValue("origin", new StringMemberValue(originalName, constPool));
        if (enumName != null)
            dynamicEnum.addMemberValue("name", new StringMemberValue(enumName, constPool));

        AttributeInfo info = f.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
        AnnotationsAttribute attr;
        if (info != null) {
            attr = (AnnotationsAttribute) info;
        }
        else {
            attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        }
        attr.addAnnotation(dynamicEnum);
        f.getFieldInfo().addAttribute(attr);
    }

    private static final String COLOR_REGISTRATION_PRE =
            BaseMod.class.getName() + ".addColor(";
    private static final String COLOR_REGISTRATION_MID = ", " + CharacterColorEnumFields.class.getName() + ".getCharColor(";
    private static final String COLOR_REGISTRATION_POST = "), " +
                    "\"\", \"\", \"\", \"\", \"\", \"\", \"\", \"\", \"\");\n";
    private static void generateColorRegistration(ClassPool pool, List<CtField> cardColorFields) throws NotFoundException, CannotCompileException {
        CtClass registrationPlace = pool.get(PostStandardInit.class.getName());
        CtMethod registerColors = registrationPlace.getDeclaredMethod("registerColors");

        StringBuilder body = new StringBuilder("{");

        for (CtField cardColor : cardColorFields) {
            body.append(COLOR_REGISTRATION_PRE)
                    .append(cardColor.getDeclaringClass().getName())
                    .append('.')
                    .append(cardColor.getName())
                    .append(COLOR_REGISTRATION_MID)
                    .append(cardColor.getDeclaringClass().getName())
                    .append('.')
                    .append(cardColor.getName())
                    .append(COLOR_REGISTRATION_POST);
        }
        body.append("}");

        registerColors.setBody(body.toString());
    }

    @SuppressWarnings("unused")
    public static Color getCharColor(AbstractCard.CardColor cardColor) {
        AbstractCard.CardColor orig = EnumPlace.getOrigColor(cardColor);
        switch (orig) {
            case RED:
                return new Color(0.5F, 0.1F, 0.1F, 1.0F); //see ColorTabBar
            case GREEN:
                return new Color(0.25F, 0.55F, 0.0F, 1.0F);
            case BLUE:
                return new Color(0.01F, 0.34F, 0.52F, 1.0F);
            case PURPLE:
                return new Color(0.37F, 0.22F, 0.49F, 1.0F);
            case COLORLESS:
                return new Color(0.4F, 0.4F, 0.4F, 1.0F);
            case CURSE:
                return new Color(0.18F, 0.18F, 0.16F, 1.0F);
            default:
                Color c = BaseMod.getTrailVfxColor(orig);
                return c == null ? Color.WHITE.cpy() : c.cpy();
        }
    }
}