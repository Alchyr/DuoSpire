package duospire.util;

import duospire.DuoSpire;
import javassist.*;
import javassist.bytecode.*;

import java.util.Collection;
import java.util.Map;

import static javassist.bytecode.Opcode.*;

//Specifically for modifying method calls/field access to access the same method/field of a different class.
//Rather specific.
//The only changes are to the constant pool and the index reference in the const pool, which shouldn't require any significant changes.
//No changes to number of bytes. No changes to what opcodes are used.
public class ReferenceReplacer {
    private final Map<String, CtClass> classReplacements;

    public ReferenceReplacer(Map<String, CtClass> classReplacements) {
        this.classReplacements = classReplacements;
    }

    public void replaceAll(Collection<CtClass> classes) throws BadBytecode {
        for (CtClass clz : classes) {
            if (DuoSpire.FULL_DEBUG_LOGGING)
                System.out.println("\t\t- Replacing references in " + clz.getSimpleName());

            for (CtConstructor c : clz.getDeclaredConstructors()) {
                replace(c.getMethodInfo());
            }

            for (CtMethod m : clz.getDeclaredMethods()) {
                if (!m.isEmpty())
                    replace(m.getMethodInfo());
            }
        }
    }

    public void replace(MethodInfo mi) throws BadBytecode {
        ConstPool cp = mi.getConstPool();
        CodeAttribute ca = mi.getCodeAttribute();
        CodeIterator ci = ca.iterator();

        modify(ci, cp);
    }

    /**
     * @param ci - CodeIterator of method
     * @param cp - ConstPool of class
     * @throws BadBytecode
     */
    private void modify(CodeIterator ci, ConstPool cp) throws BadBytecode {
        int index, op;
        ci.begin();
        while (ci.hasNext()) {
            index = ci.next();
            op = ci.byteAt(index);

            if (op < INVOKEVIRTUAL) {
                if (op >= GETFIELD) { //get/setfield
                    int fieldIndex = ci.u16bitAt(index + 1);
                    String fieldName = cp.getFieldrefName(fieldIndex);
                    String fieldType = cp.getFieldrefType(fieldIndex);
                    String fieldClassName = cp.getFieldrefClassName(fieldIndex);
                    if (fieldName != null && fieldType != null && fieldClassName != null) {
                        CtClass replacement = classReplacements.get(fieldClassName);
                        if (replacement != null) {
                            try {
                                CtField replacementField = replacement.getField(fieldName);
                                int classRef = cp.addClassInfo(replacement);
                                int fieldRef = cp.addFieldrefInfo(classRef,
                                        fieldName,
                                        replacementField.getFieldInfo().getDescriptor());
                                ci.write16bit(fieldRef, index + 1);
                            }
                            catch (NotFoundException e) {
                                System.out.println("ERROR: Failed to find replacement field " + fieldName + " in class " + replacement.getName());
                            }
                        }
                    }
                }
            }
            else if (op <= INVOKEINTERFACE) {
                int methodIndex = ci.u16bitAt(index + 1);
                String methodName = cp.getMethodrefName(methodIndex);
                String methodType = cp.getMethodrefType(methodIndex);
                String methodClassName = cp.getMethodrefClassName(methodIndex);
                if (methodName != null && methodType != null && methodClassName != null) {
                    CtClass replacement = classReplacements.get(methodClassName);
                    if (replacement != null) {
                        CtBehavior replacementMethod = findMethod(replacement, methodName, methodType);
                        if (replacementMethod != null) {
                            int classRef = cp.addClassInfo(replacement);
                            int methodRef = cp.addMethodrefInfo(classRef,
                                    methodName,
                                    replacementMethod.getMethodInfo().getDescriptor());
                            ci.write16bit(methodRef, index + 1);
                        }
                        else {
                            System.out.println("ERROR: Failed to find replacement method " + methodName + " in class " + replacement.getName());
                        }
                    }
                }
            }
            else if (op == NEW || op == NEWARRAY ||
                    op == CHECKCAST || op == INSTANCEOF ||
                    op == MULTIANEWARRAY) {
                int typeIndex = ci.u16bitAt(index + 1);
                String newClass = cp.getClassInfo(typeIndex);
                if (newClass != null) {
                    CtClass replacement = classReplacements.get(newClass);
                    if (replacement != null) {
                        int classRef = cp.addClassInfo(replacement);
                        ci.write16bit(classRef, index + 1);
                    }
                }
            }
        }
    }

    //Attempts to find a method in a class, first from non-private methods including inherited, then including private declared methods.
    //The return type is ignored.
    private static CtBehavior findMethod(CtClass clz, String name, String descriptor) {
        CtBehavior[] possible = clz.getMethods();
        for (CtBehavior m : possible) {
            if (name.equals(m.getName()) && descriptorMatchIgnoreReturn(m.getMethodInfo().getDescriptor(), descriptor))
                return m;
        }

        try {
            possible = clz.getDeclaredMethods(name);
            for (CtBehavior m : possible) {
                if (descriptorMatchIgnoreReturn(m.getMethodInfo().getDescriptor(), descriptor))
                    return m;
            }
        }
        catch (NotFoundException ignored) {
        }

        possible = clz.getDeclaredConstructors();
        for (CtBehavior m : possible) {
            if (name.equals(m.getMethodInfo().getName()) && descriptorMatchIgnoreReturn(m.getMethodInfo().getDescriptor(), descriptor))
                return m;
        }

        return null;
    }

    private static boolean descriptorMatchIgnoreReturn(String descriptorA, String descriptorB) {
        int max = Math.min(descriptorA.length(), descriptorB.length());
        for (int i = 0; i < max; ++i) {
            if (descriptorA.charAt(i) != descriptorB.charAt(i)) {
                return false;
            }
            else if (descriptorA.charAt(i) == ')') {
                return true;
            }
        }
        return true;
    }
}
