package duospire.generation;

import basemod.Pair;
import javassist.*;
import javassist.bytecode.*;

import java.util.HashMap;
import java.util.Map;

//Specifically for modifying method calls/field access to access the same method/field of a different class.
//Rather specific.
//The only changes are to the constant pool and the index reference in the const pool, which shouldn't require any significant changes.
//No changes to number of bytes. No changes to what opcodes are used.

/*
    ReferenceReplacer was version 2.
 */
@Deprecated
public class BytecodeModifier {
    private final MethodInfo mi;
    private final ConstPool cp;

    private final CodeAttribute ca;
    private final CodeIterator ci;

    public BytecodeModifier(CtMethod method) {
        this(method.getMethodInfo());
    }
    public BytecodeModifier(CtConstructor constructor) {
        this(constructor.getMethodInfo());
    }
    public BytecodeModifier(MethodInfo mi) {
        this.mi = mi;
        this.cp = mi.getConstPool();
        this.ca = mi.getCodeAttribute();
        this.ci = ca.iterator();
    }

    public void modify(BytecodeChanges changes) throws BadBytecode {
        int index, op;
        ci.begin();
        while (ci.hasNext()) {
            index = ci.next();
            op = ci.byteAt(index);

            if (op < Opcode.INVOKEVIRTUAL) {
                if (op >= Opcode.GETFIELD) { //get/setfield
                    int fieldIndex = uVal(index + 1);
                    String fieldName = cp.getFieldrefName(fieldIndex);
                    String fieldType = cp.getFieldrefType(fieldIndex);
                    String fieldClassName = cp.getFieldrefClassName(fieldIndex);
                    if (fieldName != null && fieldClassName != null) {
                        Pair<String, CtClass> change = changes.fieldChanges.get(fieldClassName);
                        if (change != null && change.getKey().equals(fieldName)) {
                            System.out.println("Found incorrect class field reference at index " + index);
                            int classRef = cp.addClassInfo(change.getValue());
                            int fieldRef = cp.addFieldrefInfo(classRef, fieldName, fieldType);
                            //TODO - Method type might not match if the field in the changed class was a changed type.
                            //Will probably need to pass it in as well in replace info.
                            //Or, get method from name of ctclass.
                            ci.write16bit(fieldRef, index + 1);
                        }
                    }
                }
            }
            else if (op <= Opcode.INVOKEINTERFACE) {
                int methodIndex = uVal(index + 1);
                String methodName = cp.getMethodrefName(methodIndex);
                String methodType = cp.getMethodrefType(methodIndex);
                String methodClassName = cp.getMethodrefClassName(methodIndex);
                if (methodName != null && methodType != null && methodClassName != null) {
                    Pair<String, CtClass> change = changes.methodChanges.get(methodClassName);
                    if (change != null && change.getKey().equals(methodName)) {
                        System.out.println("Found incorrect class method call at index " + index);
                        int classRef = cp.addClassInfo(change.getValue());
                        int methodRef = cp.addMethodrefInfo(classRef, methodName, methodType);
                        //TODO - Method type might not match if the method in the changed class returned a changed type.
                        ci.write16bit(methodRef, index + 1);
                    }
                }
            }
        }
    }

    private int bVal(int index) {
        return ci.byteAt(index);
    }
    private int uVal(int index) {
        return ci.u16bitAt(index);
    }


    public static class BytecodeChanges {
        //Class name -> Method/Field name, Replacement Class
        final Map<String, Pair<String, CtClass>> fieldChanges = new HashMap<>(), methodChanges = new HashMap<>();

        public void addFieldChange(String originalClass, String original, CtClass replacement) {
            fieldChanges.put(originalClass, new Pair<>(original, replacement));
        }
        public void addMethodChange(String originalClass, String original, CtClass replacement) {
            methodChanges.put(originalClass, new Pair<>(original, replacement));
        }
    }
}
