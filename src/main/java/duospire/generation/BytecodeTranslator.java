package duospire.generation;

import javassist.*;
import javassist.bytecode.*;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static javassist.bytecode.Opcode.*;

public class BytecodeTranslator {
    private static final String UNDF = "UNDEFINED", NULL = "NULL";

    public static String getOpCodes(MethodInfo mi, int perLine) throws BadBytecode {
        CodeAttribute ca = mi.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        StringBuilder bytecode = new StringBuilder();
        int currentLine = 0;
        while (ci.hasNext()) {
            int index = ci.next();
            int op = ci.byteAt(index);
            String hexOp = Integer.toHexString(op);
            if (hexOp.length() == 1)
                hexOp = '0' + hexOp;

            bytecode.append(hexOp).append(' ').append(String.format("%-14.14s", Mnemonic.OPCODE[op]));
            ++currentLine;
            if (currentLine >= perLine) {
                currentLine = 0;
                bytecode.append('\n');
            }
        }
        return bytecode.toString();
    }

    public static void translate(ClassPool pool, String className, String methodName) {
        try {
            CtClass clz = pool.get(className);
            CtMethod method = clz.getDeclaredMethod(methodName);
            System.out.println(new BytecodeTranslator(method).translate());
        } catch (NotFoundException e) {
            System.out.println("BytecodeTranslator: Method + " + className + "." + methodName + " not found.");
        } catch (BadBytecode badBytecode) {
            badBytecode.printStackTrace();
        }
    }

    private final MethodInfo mi;
    private final ConstPool cp;

    private final CodeAttribute ca;
    private final CodeIterator ci;

    private final StringBuilder sb;

    private Deque<Object> stack = new IntedetermineDeque();

    private final Map<Integer, Object> vars = new HashMap<>();

    private final boolean isStatic;
    private final CtClass[] paramTypes;

    private int arrCount = 0;

    public BytecodeTranslator(CtMethod method) throws NotFoundException {
        this(method.getMethodInfo(), Modifier.isStatic(method.getModifiers()), method.getParameterTypes());
    }
    public BytecodeTranslator(CtConstructor constructor) throws NotFoundException {
        this(constructor.getMethodInfo(), Modifier.isStatic(constructor.getModifiers()), constructor.getParameterTypes());
    }
    public BytecodeTranslator(MethodInfo mi, boolean isStatic, CtClass[] paramtypes) {
        this.mi = mi;
        this.cp = mi.getConstPool();
        this.sb = new StringBuilder();
        this.ca = mi.getCodeAttribute();
        this.ci = ca.iterator();

        this.isStatic = isStatic;
        this.paramTypes = paramtypes;
    }
    public String translate() throws BadBytecode {
        sb.setLength(0);
        stack.clear();
        for (int i = 0; i < 99; ++i)
            stack.push(UNDF);
        vars.clear();

        int off = 0;
        if (!mi.isStaticInitializer() && !isStatic) {
            vars.put(0, "this");
            off = 1;
        }
        for (int i = 0; i < paramTypes.length; ++i) {
            vars.put(i + off, paramTypes[i].getSimpleName());
        }

        ci.begin();
        while (ci.hasNext()) {
            opToString(ci);
        }
        stack.clear();
        vars.clear();
        return sb.toString();
    }

    private void opToString(CodeIterator ci) throws BadBytecode {
        Object a, b, c, d;
        boolean staticMethod = false;
        int index = ci.next();
        int op = ci.byteAt(index);
        String hexOp = Integer.toHexString(op);
        if (hexOp.length() == 1)
            hexOp = '0' + hexOp;

        sb.append(hexOp).append(' ').append(String.format("%-15.15s", Mnemonic.OPCODE[op])).append(" | ");
        switch (op) {
            case ACONST_NULL:   push("null"); break;
            case ICONST_M1:     push(-1); break;
            case ICONST_0:      push(0); break;
            case ICONST_1:      push(1); break;
            case ICONST_2:      push(2); break;
            case ICONST_3:      push(3); break;
            case ICONST_4:      push(4); break;
            case ICONST_5:      push(5); break;
            case LCONST_0:      pushSuf(0L, 'L'); break;
            case LCONST_1:      pushSuf(1L, 'L'); break;
            case FCONST_0:      pushSuf(0F, "F"); break;
            case FCONST_1:      pushSuf(1F, "F"); break;
            case FCONST_2:      pushSuf(2F, "F"); break;
            case DCONST_0:      pushSuf(0D, "D"); break;
            case DCONST_1:      pushSuf(1D, "D"); break;
            case BIPUSH:        push(bVal(index + 1)); break;
            case SIPUSH:        push(uVal(index + 1)); break;
            case LDC:           push(cp.getLdcValue(bVal(index + 1))); break;
            case LDC_W:
            case LDC2_W:        push(cp.getLdcValue(uVal(index + 1))); break;
            case ILOAD:
            case ALOAD:         pushSuf(var(bVal(index + 1)), " [var", bVal(index + 1), ']'); break;
            case LLOAD:         pushSuf(var(bVal(index + 1)), "_L [var", bVal(index + 1), ']'); break;
            case FLOAD:         pushSuf(var(bVal(index + 1)), "_F [var", bVal(index + 1), ']'); break;
            case DLOAD:         pushSuf(var(bVal(index + 1)), "_D [var", bVal(index + 1), ']'); break;
            case ILOAD_0:
            case ILOAD_1:
            case ILOAD_2:
            case ILOAD_3:       pushSuf(var(op - ILOAD_0), " [var", op - ILOAD_0, ']'); break;
            case LLOAD_0:
            case LLOAD_1:
            case LLOAD_2:
            case LLOAD_3:       pushSuf(var(op - LLOAD_0), "_L [var", op - LLOAD_0, ']'); break;
            case FLOAD_0:
            case FLOAD_1:
            case FLOAD_2:
            case FLOAD_3:       pushSuf(var(op - FLOAD_0), "_F [var", op - FLOAD_0, ']'); break;
            case DLOAD_0:
            case DLOAD_1:
            case DLOAD_2:
            case DLOAD_3:       pushSuf(var(op - DLOAD_0), "_D [var", op - DLOAD_0, ']'); break;
            case ALOAD_0:
            case ALOAD_1:
            case ALOAD_2:
            case ALOAD_3:       pushSuf(var(op - ALOAD_0), " [var", op - ALOAD_0, ']'); break;
            case IALOAD:        push(readArr(int.class)); break;
            case LALOAD:        push(readArr(long.class)); break;
            case FALOAD:        push(readArr(float.class)); break;
            case DALOAD:        push(readArr(double.class)); break;
            case AALOAD:        push(readArr(Object.class)); break;
            case BALOAD:        push(readArr(byte.class)); break;
            case CALOAD:        push(readArr(char.class)); break;
            case SALOAD:        push(readArr(short.class)); break;
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:        store(bVal(index + 1)); break;
            case ISTORE_0:
            case ISTORE_1:
            case ISTORE_2:
            case ISTORE_3:      store(op - ISTORE_0); break;
            case LSTORE_0:
            case LSTORE_1:
            case LSTORE_2:
            case LSTORE_3:      store(op - LSTORE_0); break;
            case FSTORE_0:
            case FSTORE_1:
            case FSTORE_2:
            case FSTORE_3:      store(op - FSTORE_0); break;
            case DSTORE_0:
            case DSTORE_1:
            case DSTORE_2:
            case DSTORE_3:      store(op - DSTORE_0); break;
            case ASTORE_0:
            case ASTORE_1:
            case ASTORE_2:
            case ASTORE_3:      store(op - ASTORE_0); break;
            case IASTORE:       storeArr(int.class); break;
            case LASTORE:       storeArr(long.class); break;
            case FASTORE:       storeArr(float.class); break;
            case DASTORE:       storeArr(double.class); break;
            case AASTORE:       storeArr(Object.class); break;
            case BASTORE:       storeArr(byte.class); break;
            case CASTORE:       storeArr(char.class); break;
            case SASTORE:       storeArr(short.class); break;
            case POP:           endPopStr(stack.pop()); break;
            case POP2:
                endPopStr(stack.pop(), stack.pop()); //SHOULD BE: Only one pop if double/long?
                break;
            case DUP:
                popStr(a = stack.pop());
                multiPush(a, a);
                break;
            case DUP_X1:
                popStr(a = stack.pop(), b = stack.pop());
                multiPush(a, b, a);
                break;
            case DUP_X2:
                //SHOULD BE: Only two values if value2 is double/long?
                popStr(a = stack.pop(), b = stack.pop(), c = stack.pop());
                multiPush(a, c, b, a);
                break;
            case DUP2:
                //SHOULD BE: Only one value if it is a double/long?
                popStr(a = stack.pop(), b = stack.pop());
                multiPush(b, a, b, a);
                break;
            case DUP2_X1:
                popStr(a = stack.pop(), b = stack.pop(), c = stack.pop());
                multiPush(b, a, c, b, a);
                break;
            case DUP2_X2:
                popStr(a = stack.pop(), b = stack.pop(), c = stack.pop(), d = stack.pop());
                multiPush(b, a, d, c, b, a);
                break;
            case SWAP:
                popStr(a = stack.pop(), b = stack.pop());
                multiPush(b, a);
                break;
            case IADD: //-----------------Addition-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, Integer::sum, (x,y)->x+ " + " + y);
                break;
            case LADD:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, Long::sum, (x,y)->x + " + " + y, 'L');
                break;
            case FADD:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue, Float::sum, (x,y)->x + " + " + y, 'F');
                break;
            case DADD:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue, Double::sum, (x,y)->x + " + " + y, 'D');
                break;
            case ISUB: //-----------------Subtraction-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x-y, (x,y)->x + " - " + y);
                break;
            case LSUB:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x-y, (x,y)->x + " - " + y, 'L');
                break;
            case FSUB:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue, (x, y)->x-y, (x,y)->x + " - " + y, 'F');
                break;
            case DSUB:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue, (x, y)->x-y, (x,y)->x + " - " + y, 'D');
                break;
            case IMUL: //-----------------Multiplication-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x*y, (x,y)->x + " * " + y);
                break;
            case LMUL:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x*y, (x,y)->x + " * " + y, 'L');
                break;
            case FMUL:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue, (x, y)->x*y, (x,y)->x + " * " + y, 'F');
                break;
            case DMUL:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue, (x, y)->x*y, (x,y)->x + " * " + y, 'D');
                break;
            case IDIV: //-----------------Division-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x/y, (x,y)->x + " / " + y);
                break;
            case LDIV:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x/y, (x,y)->x + " / " + y, 'L');
                break;
            case FDIV:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue, (x, y)->x/y, (x,y)->x + " / " + y, 'F');
                break;
            case DDIV:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue, (x, y)->x/y, (x,y)->x + " / " + y, 'D');
                break;
            case IREM: //-----------------Remainder-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x%y, (x,y)->x + " % " + y);
                break;
            case LREM:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x%y, (x,y)->x + " % " + y, 'L');
                break;
            case FREM:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue, (x, y)->x%y, (x,y)->x + " % " + y, 'F');
                break;
            case DREM:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue, (x, y)->x%y, (x,y)->x + " % " + y, 'D');
                break;
            case INEG: //-----------------Negation-----------------
                popStr(a = stack.pop());
                arith(a, combine(Number::intValue, x->-x), (x)->"-" + x);
                break;
            case LNEG:
                popStr(a = stack.pop());
                arith(a, combine(Number::longValue, x->-x), (x)->"-" + x, 'L');
                break;
            case FNEG:
                popStr(a = stack.pop());
                arith(a, combine(Number::floatValue, x->-x), (x)->"-" + x, 'F');
                break;
            case DNEG:
                popStr(a = stack.pop());
                arith(a, combine(Number::doubleValue, x->-x), (x)->"-" + x, 'D');
                break;
            case ISHL: //-----------------Arithmetic LShift-----------------
                popStr(a = stack.pop(), b = stack.pop());
                shift(a, b, Number::intValue, (x, y)->x<<y, (x,y)->x + " << " + y);
                break;
            case LSHL:
                popStr(a = stack.pop(), b = stack.pop());
                shift(a, b, Number::longValue, (x, y)->x<<y, (x,y)->x + " << " + y, 'L');
                break;
            case ISHR: //-----------------Arithmetic (Signed) RShift-----------------
                popStr(a = stack.pop(), b = stack.pop());
                shift(a, b, Number::intValue, (x, y)->x>>y, (x,y)->x + " >> " + y);
                break;
            case LSHR:
                popStr(a = stack.pop(), b = stack.pop());
                shift(a, b, Number::longValue, (x, y)->x>>y, (x,y)->x + " >> " + y, 'L');
                break;
            case IUSHR: //-----------------Logical (Unsigned) RShift-----------------
                popStr(a = stack.pop(), b = stack.pop());
                shift(a, b, Number::intValue, (x, y)->x>>>y, (x,y)->x + " >>> " + y);
                break;
            case LUSHR:
                popStr(a = stack.pop(), b = stack.pop());
                shift(a, b, Number::longValue, (x, y)->x>>>y, (x,y)->x + " >>> " + y, 'L');
                break;
            case IAND: //-----------------Bitwise &-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x&y, (x,y)->x + " & " + y);
                break;
            case LAND:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x&y, (x,y)->x + " & " + y, 'L');
                break;
            case IOR: //-----------------Bitwise |-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x|y, (x,y)->x + " | " + y);
                break;
            case LOR:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x|y, (x,y)->x + " | " + y, 'L');
                break;
            case IXOR: //-----------------Bitwise ^-----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::intValue, (x, y)->x^y, (x,y)->x + " ^ " + y);
                break;
            case LXOR:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, (x, y)->x^y, (x,y)->x + " ^ " + y, 'L');
                break;
            case IINC:          iinc(bVal(index + 1), bVal(index + 2));
            case L2I: //-----------------Type Conversion-----------------
            case F2I:
            case D2I:
                popStr(a = stack.pop());
                arith(a, Number::intValue);
                break;
            case I2L:
            case F2L:
            case D2L:
                popStr(a = stack.pop());
                arith(a, Number::longValue);
                break;
            case I2F:
            case L2F:
            case D2F:
                popStr(a = stack.pop());
                arith(a, Number::floatValue);
                break;
            case I2D:
            case L2D:
            case F2D:
                popStr(a = stack.pop());
                arith(a, Number::doubleValue);
                break;
            case I2B:
                popStr(a = stack.pop());
                arith(a, Number::byteValue);
                break;
            case I2C:
                popStr(a = stack.pop());
                arith(a, (x)->(char)x.intValue());
                break;
            case I2S:
                popStr(a = stack.pop());
                arith(a, Number::shortValue);
                break;
            case LCMP: //----------------Comparison----------------
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::longValue, Long::compare, (x,y)->"cmp(" + x + ", " + y + ")", 'L');
                break;
            case FCMPL:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue,
                        (x, y)->{
                            if (x.isNaN() || y.isNaN())
                                return -1;
                            return Float.compare(x, y);
                        }, (x,y)->"cmp(" + x + ", " + y + ")", 'F');
                break;
            case FCMPG:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::floatValue,
                        (x, y)->{
                            if (x.isNaN() || y.isNaN())
                                return 1;
                            return Float.compare(x, y);
                        }, (x,y)->"cmp(" + x + ", " + y + ")", 'F');
                break;
            case DCMPL:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue,
                        (x, y)->{
                            if (x.isNaN() || y.isNaN())
                                return -1;
                            return Double.compare(x, y);
                        }, (x,y)->"cmp(" + x + ", " + y + ")", 'D');
                break;
            case DCMPG:
                popStr(a = stack.pop(), b = stack.pop());
                arith(a, b, Number::doubleValue,
                        (x, y)->{
                            if (x.isNaN() || y.isNaN())
                                return 1;
                            return Double.compare(x, y);
                        }, (x,y)->"cmp(" + x + ", " + y + ")", 'D');
                break;
            case IFEQ:
                popFormatted("if (%s == 0) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IFNE:
                popFormatted("if (%s != 0) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IFLT:
                popFormatted("if (%s < 0) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IFGE:
                popFormatted("if (%s >= 0) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IFGT:
                popFormatted("if (%s > 0) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IFLE:
                popFormatted("if (%s <= 0) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IF_ICMPEQ:
            case IF_ACMPEQ:
                popFormatted("if (%s == %s) go to offset ", 2);
                sb.append(uVal(index + 1));
                break;
            case IF_ICMPNE:
            case IF_ACMPNE:
                popFormatted("if (%s != %s) go to offset ", 2);
                sb.append(uVal(index + 1));
                break;
            case IF_ICMPLT:
                popFormatted("if (%s < %s) go to offset ", 2);
                sb.append(uVal(index + 1));
                break;
            case IF_ICMPGE:
                popFormatted("if (%s >= %s) go to offset ", 2);
                sb.append(uVal(index + 1));
                break;
            case IF_ICMPGT:
                popFormatted("if (%s > %s) go to offset ", 2);
                sb.append(uVal(index + 1));
                break;
            case IF_ICMPLE:
                popFormatted("if (%s <= %s) go to offset ", 2);
                sb.append(uVal(index + 1));
                break;
            case GOTO:
                sb.append("go to offset ").append(uVal(index + 1));
                break;
            case JSR: //Might want to implement this at some point?
            case RET:
                sb.append("UNSUPPORTED");
                break;
            case TABLESWITCH: //Could use more work.
            case LOOKUPSWITCH:
                endPopStr(stack.pop());
                break;
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
                endPopStr(stack.pop());
                sb.append(' ');
            case RETURN:
                sb.append("[Empty Stack]");
                stack.clear();
                break;
            case GETSTATIC:
                int getStaticIndex = uVal(index + 1);
                String getStaticField = cp.getFieldrefName(getStaticIndex);
                if (getStaticField == null) {
                    sb.append("{FIELD NOT FOUND} ");
                    stack.push(UNDF);
                }
                else {
                    sb.append(cp.getFieldrefClassName(getStaticIndex)).append('.').append(getStaticField).append(' ');
                    push(getStaticField);
                }
                break;
            case PUTSTATIC:
                popStr(stack.pop());
                sb.append("=> ");
                int putStaticIndex = uVal(index + 1);
                String putStaticField = cp.getFieldrefName(putStaticIndex);
                if (putStaticField == null)
                    sb.append("{FIELD NOT FOUND}");
                else
                    sb.append("Field ").append(putStaticField)
                            .append(" [").append(cp.getFieldrefClassName(putStaticIndex)).append(']');
                break;
            case GETFIELD:
                popFormatted("%1$s -> %1$s.", 1);
                int getFieldIndex = uVal(index + 1);
                String getField = cp.getFieldrefName(getFieldIndex);
                if (getField == null) {
                    sb.append("{FIELD NOT FOUND}");
                    stack.push(UNDF);
                }
                else {
                    sb.append(getField).append(" [").append(cp.getFieldrefClassName(getFieldIndex)).append(']');
                    stack.push(getField);
                }
                break;
            case PUTFIELD:
                popFormatted("%1$s, %2$s => %1$s.", 2);
                int putFieldIndex = uVal(index + 1);
                String putField = cp.getFieldrefName(putFieldIndex);
                if (putField == null)
                    sb.append("{FIELD NOT FOUND}");
                else
                    sb.append(putField).append(" [").append(cp.getFieldrefClassName(putFieldIndex)).append(']');
                break;
            case INVOKESTATIC: //A static method.
                staticMethod = true;
            case INVOKEVIRTUAL: //Normally calling a method
            case INVOKESPECIAL: //Usually a super call
            case INVOKEINTERFACE: //An interface method. Has two additional bytes of info, but they're not important for this.
                int virtualMethIndex = uVal(index + 1);
                String virtualMethName = cp.getMethodrefName(virtualMethIndex);
                if (virtualMethName == null) {
                    sb.append("{METHOD NOT FOUND}");
                }
                else {
                    processDescriptor(virtualMethName, cp.getMethodrefType(virtualMethIndex), staticMethod, cp.getMethodrefClassName(virtualMethIndex));
                }
                break;
            case INVOKEDYNAMIC:
                //Gets a symbolic reference to a CallSite resolved to a reference to a bound instance of CallSite for this specific instruction.
                //Then method indicated by CallSite is invoked.
                break;
            case NEW:
                int type = uVal(index + 1);
                String defClass = cp.getClassInfo(type);
                if (defClass == null) {
                    sb.append("{CLASS NOT FOUND}");
                    push(UNDF);
                }
                else {
                    pushSuf(defClass, " new");
                }
                break;
            case NEWARRAY:
                a = stack.pop();
                if (a instanceof Integer) {
                    popStr(a);
                    push(newArray(bVal(index + 1), (Integer) a));
                }
                else {
                    sb.append("{NO ARRAY SIZE} ").append(a);
                    push(newArray(bVal(index + 1), 1));
                }
                break;
            case ANEWARRAY:
                a = stack.pop();
                String arrClassInfo = cp.getClassInfo(uVal(index + 1));
                if (arrClassInfo != null) {
                    if (a instanceof Integer) {
                        popStr(a);
                        push(new ArrayRepresentation("arr" + arrCount++, arrClassInfo, ()->null, (Integer) a));
                    }
                    else {
                        sb.append("{NO ARRAY SIZE} ").append(a);
                        push(new ArrayRepresentation("arr" + arrCount++, arrClassInfo, ()->null, 1));
                    }
                }
                else {
                    sb.append("{NO CLASS FOUND AT INDEX ").append(uVal(index + 1)).append('}');
                }
                break;
            case ARRAYLENGTH:
                popStr(a = stack.pop());
                if (a == null)
                    push(UNDF);
                else
                    push(a + ".length");
                break;
            case ATHROW:
                endPopStr(a = stack.pop());
                stack.clear();
                stack.push(a);
                sb.append(" [Empty Stack], ");
                sb.append(a);
                break;
            case CHECKCAST:
                String checkCast = cp.getClassInfo(uVal(index + 1));
                if (checkCast == null) checkCast = UNDF;
                popFormatted("%s instanceof " + checkCast + " ", a = stack.pop());
                push(a);
                break;
            case INSTANCEOF:
                String instanceOf = cp.getClassInfo(uVal(index + 1));
                if (instanceOf == null) instanceOf = UNDF;
                popStr(a = stack.pop());
                push(a.toString() + " instanceof " + instanceOf);
                break;
            case MONITORENTER:
                popFormatted("synchronized on %s ->", 1);
                break;
            case MONITOREXIT:
                popFormatted("end synchronized on %s ->", 1);
                break;
            case WIDE:
                break;
            case MULTIANEWARRAY:
                String multiArrClassInfo = cp.getClassInfo(uVal(index + 1));
                if (multiArrClassInfo != null) {
                    int numDimensions = bVal(index + 3);
                    Object[] dimensions = new Object[numDimensions];
                    for (int i = 0; i < numDimensions; ++i)
                        dimensions[i] = stack.pop();
                    popStr(dimensions);
                    push(new ArrayRepresentation("arr" + arrCount++, multiArrClassInfo, ()->null, dimensions));
                }
                else {
                    sb.append("{NO CLASS FOUND AT INDEX ").append(uVal(index + 1)).append('}');
                }
                break;
            case IFNULL:
                popFormatted("if (%s == null) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case IFNONNULL:
                popFormatted("if (%s != null) go to offset ", 1);
                sb.append(uVal(index + 1));
                break;
            case GOTO_W:
                break;
            case JSR_W:
                break;
            case NOP:
            default: //breakpoint, reserved, impdep1, impdep2
                break;
        }
        sb.append('\n');
    }

    private int bVal(int index) {
        return ci.byteAt(index);
    }
    private int uVal(int index) {
        return ci.u16bitAt(index);
    }
    private int wVal(int index) {
        return ci.s32bitAt(index);
    }
    private Object var(int index) {
        if (index < 0)
            return UNDF;
        return vars.getOrDefault(index, UNDF);
    }

    private void push(Object val) {
        if (val == null)
            val = NULL;
        stack.push(val);
        sb.append("-> ").append(val);
    }
    private void pushSuf(Object val, Object... suffix) {
        StringBuilder str = new StringBuilder();
        str.append(val);
        for (Object o : suffix) str.append(o == null ? NULL : o);
        stack.push(val == null ? NULL : val);
        sb.append("-> ").append(str);
    }
    private void multiPush(Object... vals) {
        boolean first = true;
        sb.append("-> ");
        for (Object o : vals) {
            if (o == null)
                o = NULL;
            stack.push(o);
            if (first) {
                sb.append(o);
                first = false;
            }
            else {
                sb.append(", ").append(o);
            }
        }
    }

    private void popStr(Object... popped) {
        boolean first = true;
        for (int i = popped.length - 1; i >= 0; --i) {
            Object o = popped[i];
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(o == null ? NULL : o);
        }
        sb.append(' ');
    }
    private void popAmt(int amt) {
        if (amt <= 0)
            return;
        Object[] popped = new Object[amt];
        for (int i = 0; i < amt; ++i) {
            popped[i] = stack.pop();
        }
        popStr(popped);
    }
    private void popFormatted(String format, int amt) {
        if (amt <= 0)
            return;
        Object[] popped = new Object[amt];
        for (int i = amt - 1; i >= 0; --i) {
            popped[i] = stack.pop();
            if (popped[i] != null)
                popped[i] = popped[i].toString();
        }
        sb.append(String.format(format, popped));
    }
    private void popFormatted(String format, Object... popped) {
        Object[] reverse = new Object[popped.length];
        for (int i = 0; i < popped.length; ++i) {
            reverse[i] = popped[popped.length - (i + 1)];
            if (reverse[i] != null)
                reverse[i] = reverse[i].toString();
        }
        sb.append(String.format(format, reverse));
    }
    private void endPopStr(Object... popped) {
        boolean first = true;
        for (int i = popped.length - 1; i >= 0; --i) {
            Object o = popped[i];
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(o == null ? NULL : o);
        }
        sb.append(" ->");
    }

    private void store(int index) {
        Object o = stack.pop();
        sb.append(o).append(" => ");
        sb.append("var").append(index);
        vars.put(index, o);
    }
    private void iinc(int index, int inc) {
        Object val = var(index);
        if (val instanceof Integer) {
            sb.append("var").append(index).append(" = ").append(val).append(" + ").append(inc);
            vars.put(index, (Integer) val + inc);
        }
        else {
            String result = val.toString() + " + " + inc;
            sb.append("var").append(index).append(" = ").append(result);
            vars.put(index, result);
        }
    }

    private Object readArr(Class<?> arrType) {
        Object index = stack.pop();
        if (index instanceof Integer) {
            Object arr = stack.pop();
            if (arr instanceof ArrayRepresentation) {
                if (arrType.equals(Object.class) || ((ArrayRepresentation) arr).dataType.equals(arrType.getName())) {
                    Object val = ((ArrayRepresentation) arr).get((Integer) index);
                    sb.append(arr).append(", ").append(index).append(' ');
                    return val;
                }
                else {
                    sb.append("WRONG ARRAY TYPE ").append(arr).append(' ');
                }
            }
            else {
                sb.append(arr).append(' ');
            }
        }
        else {
            stack.pop();
            sb.append(UNDF);
        }
        return UNDF;
    }
    private void storeArr(Class<?> arrType) {
        Object val = stack.pop();
        Object index = stack.pop();
        if (index instanceof Integer) {
            Object arr = stack.pop();
            if (arr instanceof ArrayRepresentation) {
                if (!arrType.equals(Object.class) && !((ArrayRepresentation) arr).dataType.equals(arrType.getName())) {
                    sb.append("INVALID ARRAY TYPE ").append(((ArrayRepresentation) arr).dataType);
                    return;
                }

                sb.append(arr).append(", ").append(index).append(", ").append(val);
                sb.append(" => ");
                sb.append(arr).append('[').append(index).append("] = ").append(val);
                if (!((ArrayRepresentation) arr).set(val, (Integer) index)) {
                    sb.append(" INDEX OUT OF BOUNDS");
                }
            }
            else {
                if (arr == null)
                    arr = NULL;
                sb.append(arr).append(", ").append(index).append(", ").append(val);
                sb.append(" => ");
                sb.append(arr).append('[').append(index).append("] = ").append(val);
            }
        }
        else {
            sb.append(val).append(", ").append(index).append(", ").append(stack.pop()).append(" -> ");
        }
    }

    private ArrayRepresentation newArray(int aType, int... size) {
        switch (aType) {
            case T_BOOLEAN:
                return new ArrayRepresentation("arr" + arrCount++, boolean.class, ()->false, size);
            case T_CHAR:
                return new ArrayRepresentation("arr" + arrCount++, char.class, ()->(char)0, size);
            case T_FLOAT:
                return new ArrayRepresentation("arr" + arrCount++, float.class, ()->0.0f, size);
            case T_DOUBLE:
                return new ArrayRepresentation("arr" + arrCount++, double.class, ()->0.0, size);
            case T_BYTE:
                return new ArrayRepresentation("arr" + arrCount++, byte.class, ()->(byte)0, size);
            case T_SHORT:
                return new ArrayRepresentation("arr" + arrCount++, short.class, ()->(short)0, size);
            case T_INT:
                return new ArrayRepresentation("arr" + arrCount++, int.class, ()->0, size);
            case T_LONG:
                return new ArrayRepresentation("arr" + arrCount++, long.class, ()->0L, size);
            default:
                sb.append("INVALID ARRAY TYPE ").append(aType);
                return new ArrayRepresentation("arr" + arrCount++, Object.class, ()->null, size);
        }
    }

    private <T> Function<Number, T> combine(Function<Number, T> conv, Function<T, T> math) {
        return (x) -> {
                T result = conv.apply(x);
                result = math.apply(result);
                return result;
        };
    }

    private <T> void arith(Object a, Function<Number, T> math) {
        arith(a, math, x->x, (Object[]) null);
    }
    private <T> void arith(Object a, Function<Number, T> math, Function<String, String> strConv) {
        arith(a, math, strConv, (Object[]) null);
    }
    private <T> void arith(Object a, Function<Number, T> math, Function<String, String> strConv, Object... suffix) {
        if (a instanceof Number) {
            if (suffix != null && suffix.length > 0)
                pushSuf(math.apply((Number) a), suffix);
            else
                push(math.apply((Number) a));
        }
        else {
            if (a == null)
                push(NULL);
            else
                push(strConv.apply(a.toString()));
        }
    }

    private <T> void arith(Object a, Object b, Function<Number, T> conv, BiFunction<T, T, ?> math) {
        arith(a, b, conv, math, (x,y)->(x + ", " + y), (Object[]) null);
    }
    private <T> void arith(Object a, Object b, Function<Number, T> conv, BiFunction<T, T, ?> math, BiFunction<String, String, String> strConv) {
        arith(a, b, conv, math, strConv, (Object[]) null);
    }
    private <T> void arith(Object a, Object b, Function<Number, T> conv, BiFunction<T, T, ?> math, BiFunction<String, String, String> strConv, Object... suffix) {
        if (a == null)
            a = UNDF;
        if (b == null)
            b = UNDF;

        if (!a.getClass().equals(b.getClass())) {
            push(strConv.apply(a.toString(), b.toString()));
            return;
        }

        if (a instanceof Number && b instanceof Number) {
            if (suffix != null && suffix.length > 0)
                pushSuf(math.apply(conv.apply((Number) a), conv.apply((Number) b)), suffix);
            else
                push(math.apply(conv.apply((Number) a), conv.apply((Number) b)));
        }
        else {
            push(strConv.apply(a.toString(), b.toString()));
        }
    }

    private <T> void shift(Object a, Object b, Function<Number, T> aConv, BiFunction<T, Integer, ?> shift, BiFunction<String, String, String> strConv) {
        shift(a, b, aConv, shift, strConv, (Object[]) null);
    }
    private <T> void shift(Object a, Object b, Function<Number, T> aConv, BiFunction<T, Integer, ?> shift, BiFunction<String, String, String> strConv, Object... suffix) {
        if (!(a instanceof Number) || !(b instanceof Integer)) {
            push(strConv.apply(a == null ? UNDF : a.toString(), b == null ? UNDF : b.toString()));
            return;
        }

        T aVal = aConv.apply((Number) a);
        int dist = (Integer) b & 0x3f;
        Object result = shift.apply(aVal, dist);
        if (suffix != null && suffix.length > 0)
            pushSuf(result, suffix);
        else
            push(result);
    }

    private void processDescriptor(String name, String descriptor, boolean isStatic, String className) {
        if (descriptor == null || descriptor.length() < 3) {
            sb.append(name).append(" [").append(className).append("]");
            return;
        }

        char c;
        int i = 1;
        int params = isStatic ? 0 : 1;
        outer:
        for (; i < descriptor.length(); ++i) { //First character is just opening parentheses
            c = descriptor.charAt(i);
            switch (c) {
                case ')':
                    ++i;
                    break outer;
                case 'L':
                    ++i;
                    while (i < descriptor.length()) {
                        if (descriptor.charAt(i) == ';')
                            break;
                        ++i;
                    }
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 'Z':
                    ++params;
                    break;
                case '[':
                    break;
            }
        }

        StringBuilder format = new StringBuilder(isStatic ? className : "%s");
        format.append('.').append(name);
        if (!isStatic) {
            sb.append('[').append(className).append('.').append(name).append("] ");
        }
        int n = isStatic ? 0 : 1;
        if (n < params)
            format.append('(');
        for (; n < params - 1; ++n)
            format.append("%s, ");
        if (n < params)
            format.append("%s)");

        if (params == 0) {
            sb.append(format);
        }
        else {
            popFormatted(format.toString(), params);
        }
        sb.append(' ');

        if (i < descriptor.length() && descriptor.charAt(i) != 'V') {
            format.setLength(0);
            for (; i < descriptor.length(); ++i) { //First character is just opening parentheses
                c = descriptor.charAt(i);
                switch (c) {
                    case 'L':
                        int start = Math.max(i, descriptor.lastIndexOf('/')) + 1;
                        if (start >= descriptor.length() - 1) {
                            push(UNDF);
                            return;
                        }
                        push(descriptor.substring(start, descriptor.length() - 1) + format);
                        return;
                    case 'B':
                        push("byte" + format); return;
                    case 'C':
                        push("char" + format); return;
                    case 'D':
                        push("double" + format); return;
                    case 'F':
                        push("float" + format); return;
                    case 'I':
                        push("int" + format); return;
                    case 'J':
                        push("long" + format); return;
                    case 'S':
                        push("short" + format); return;
                    case 'Z':
                        push("boolean" + format); return;
                    case '[':
                        format.append("[]");
                        break;
                }
            }
        }
        else if (params == 0) {
            sb.append("void"); //no params, no return (no change to stack)
        }
        else {
            sb.append("->"); //Removed params from stack
        }
    }

    private static class ArrayRepresentation {
        final String identity;
        final String dataType;
        final int[] dimensions;
        final int[] dimensionMultipliers;
        final Object[] arr;
        final boolean determinate;

        public <T> ArrayRepresentation(String identity, Class<T> clazz, Supplier<T> defaultVal, int[] dimensions) {
            this.identity = identity;
            this.dataType = clazz.getName();
            this.dimensions = dimensions;
            this.dimensionMultipliers = new int[dimensions.length + 1]; //The last value will never be used.
            dimensionMultipliers[0] = 1;
            determinate = true;

            int totalSize = 1;
            for (int i = 0; i < dimensions.length; ++i) {
                totalSize *= dimensions[i];
                dimensionMultipliers[i + 1] = dimensions[i];
            }
            arr = new Object[totalSize];
            for (int i = 0; i < totalSize; ++i)
                arr[i] = defaultVal.get();
        }
        public <T> ArrayRepresentation(String identity, String typeName, Supplier<T> defaultVal, Object... dimensions) {
            this.identity = identity;
            this.dataType = typeName;
            this.dimensions = new int[dimensions.length];
            this.dimensionMultipliers = new int[dimensions.length + 1]; //The last value will never be used.
            dimensionMultipliers[0] = 1;

            boolean determinate = true;

            int totalSize = 1;
            for (int i = 0; i < dimensions.length; ++i) {
                Object o = dimensions[i];
                if (o instanceof Integer) {
                    totalSize *= (int) o;
                    this.dimensions[i] = (int) o;
                    dimensionMultipliers[i + 1] = (int) o;
                }
                else {
                    determinate = false;
                    totalSize = 0;
                    for (; i < dimensions.length; ++i)
                        dimensionMultipliers[i + 1] = 1;
                }
            }
            this.determinate = determinate;
            if (determinate) {
                arr = new Object[totalSize];
                for (int i = 0; i < totalSize; ++i)
                    arr[i] = defaultVal.get();
            }
            else {
                arr = null;
            }
        }

        public Object get(int... indices) {
            if (indices.length != dimensions.length)
                return "ARRAY INDICES MISMATCH";

            if (!determinate) {
                StringBuilder sb = new StringBuilder(identity);
                for (int i = 0; i < indices.length; ++i)
                    sb.append('[').append(indices[i]).append(']');
                return sb.toString();
            }

            Integer index = translateIndices(indices);
            if (index == null)
                return "ARRAY INDEX OUT OF BOUNDS";

            return arr[index];
        }

        public boolean set(Object val, int... indices) {
            if (!determinate) {
                return true;
            }

            Integer index = translateIndices(indices);
            if (index == null)
                return false;

            arr[index] = val;
            return true;
        }

        private Integer translateIndices(int... indices) {
            int index = 0;
            for (int i = 0; i < indices.length; ++i) {
                if (indices[i] < 0 || indices[i] > dimensions[i])
                    return null;
                index += indices[i] * dimensionMultipliers[i];
            }
            return index;
        }

        @Override
        public String toString() {
            return identity + "_" + dataType;
        }
    }

    //The only flexibility is between primitives and their wrappers.
    public static boolean strictTestAssignable(Class<?> target, Class<?> src) {
        if (target == null)
            return false;
        if (src == null) {
            return !target.isPrimitive();
        }
        if (target.isAssignableFrom(src))
            return true;

        switch (target.getName())
        {
            case "java.lang.Byte":
                target = byte.class;
                break;
            case "java.lang.Character":
                target = char.class;
                break;
            case "java.lang.Short":
                target = short.class;
                break;
            case "java.lang.Integer":
                target = int.class;
                break;
            case "java.lang.Long":
                target = long.class;
                break;
            case "java.lang.Float":
                target = float.class;
                break;
            case "java.lang.Double":
                target = double.class;
                break;
            case "java.lang.Boolean":
                target = boolean.class;
                break;
        }

        //The exceptional cases are primitives, which require equality for isAssignableFrom to return true.
        if (target.isPrimitive()) {
            //convert src from primitive wrapper to primitive
            switch (src.getName())
            {
                case "java.lang.Byte":
                    src = byte.class;
                    break;
                case "java.lang.Character":
                    src = char.class;
                    break;
                case "java.lang.Short":
                    src = short.class;
                    break;
                case "java.lang.Integer":
                    src = int.class;
                    break;
                case "java.lang.Long":
                    src = long.class;
                    break;
                case "java.lang.Float":
                    src = float.class;
                    break;
                case "java.lang.Double":
                    src = double.class;
                    break;
                case "java.lang.Boolean":
                    src = boolean.class;
                    break;
            }
            if (!src.isPrimitive()) //can't assign non-primitives to primitives.
                return false;

            return target == src;
        }

        return false;
    }


    private static class IntedetermineDeque implements Deque<Object> {
        private Deque<Object> subQueue = new ArrayDeque<>();

        public void setSub(Deque<Object> stack) {
            subQueue = stack;
        }

        @Override
        public void addFirst(Object o) {
            subQueue.addFirst(o);
        }
        @Override
        public void addLast(Object o) {
            subQueue.addLast(o);
        }
        @Override
        public boolean offerFirst(Object o) {
            return subQueue.offerFirst(o);
        }

        @Override
        public boolean offerLast(Object o) {
            return subQueue.offerLast(o);
        }

        @Override
        public Object removeFirst() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.removeFirst();
        }

        @Override
        public Object removeLast() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.removeLast();
        }

        @Override
        public Object pollFirst() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.pollFirst();
        }

        @Override
        public Object pollLast() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.pollLast();
        }

        @Override
        public Object getFirst() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.getFirst();
        }

        @Override
        public Object getLast() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.getLast();
        }

        @Override
        public Object peekFirst() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.peekFirst();
        }

        @Override
        public Object peekLast() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.peekLast();
        }

        @Override
        public boolean removeFirstOccurrence(Object o) {
            return subQueue.removeFirstOccurrence(o);
        }

        @Override
        public boolean removeLastOccurrence(Object o) {
            return subQueue.removeLastOccurrence(o);
        }

        @Override
        public boolean add(Object o) {
            return subQueue.add(o);
        }

        @Override
        public boolean offer(Object o) {
            return subQueue.offer(o);
        }

        @Override
        public Object remove() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.remove();
        }

        @Override
        public Object poll() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.poll();
        }

        @Override
        public Object element() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.element();
        }

        @Override
        public Object peek() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.peek();
        }

        @Override
        public void push(Object o) {
            subQueue.push(o);
        }

        @Override
        public Object pop() {
            if (subQueue.isEmpty())
                return UNDF;
            return subQueue.pop();
        }

        @Override
        public boolean remove(Object o) {
            return subQueue.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return subQueue.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<?> c) {
            return subQueue.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return subQueue.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return subQueue.retainAll(c);
        }

        @Override
        public void clear() {
            subQueue.clear();
        }

        @Override
        public boolean contains(Object o) {
            return subQueue.contains(o);
        }

        @Override
        public int size() {
            return subQueue.size();
        }

        @Override
        public boolean isEmpty() {
            return subQueue.isEmpty();
        }

        @Override
        public Iterator<Object> iterator() {
            return subQueue.iterator();
        }

        @Override
        public Object[] toArray() {
            return subQueue.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return subQueue.toArray(a);
        }

        @Override
        public Iterator<Object> descendingIterator() {
            return subQueue.descendingIterator();
        }
    }
}
