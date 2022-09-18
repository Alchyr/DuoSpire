package duospire.util;

import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class GeneralUtils {
    public static String arrToString(Object[] arr) {
        if (arr == null)
            return null;
        if (arr.length == 0)
            return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length - 1; ++i) {
            sb.append(arr[i]).append(", ");
        }
        sb.append(arr[arr.length - 1]);
        return sb.toString();
    }


    public static void logStackTrace(Logger l, Throwable e) {
        StringBuilder sb = new StringBuilder();
        logStackTrace(sb, e);
        l.error(sb.toString());
    }
    private static void logStackTrace(StringBuilder sb, Throwable e) {
        Set<Throwable> dejaVu =
                Collections.newSetFromMap(new IdentityHashMap<>());
        dejaVu.add(e);

        StackTraceElement[] trace = e.getStackTrace();

        sb.append(e).append('\n');
        for (StackTraceElement traceElement : trace)
            sb.append("\tat ").append(traceElement).append("\n");

        for (Throwable se : e.getSuppressed())
            logEnclosedStackTrace(sb, se, trace, "Suppressed: ", "\t", dejaVu);

        Throwable cause = e.getCause();
        if (cause != null) {
            logEnclosedStackTrace(sb, cause, trace, "Caused by: ", "", dejaVu);
        }
    }
    private static void logEnclosedStackTrace(StringBuilder sb, Throwable e, StackTraceElement[] enclosingTrace, String caption, String prefix, Set<Throwable> dejaVu) {
        if (dejaVu.contains(e)) {
            sb.append("\t[CIRCULAR REFERENCE:").append(e).append("]").append('\n');
        }
        else {
            dejaVu.add(e);

            // Compute number of frames in common between this and enclosing trace
            StackTraceElement[] trace = e.getStackTrace();
            int m = trace.length - 1;
            int n = enclosingTrace.length - 1;
            while (m >= 0 && n >=0 && trace[m].equals(enclosingTrace[n])) {
                m--; n--;
            }
            int framesInCommon = trace.length - 1 - m;

            // Print our stack trace
            sb.append(prefix).append(caption).append(e).append('\n');
            for (int i = 0; i <= m; i++)
                sb.append(prefix).append("\tat ").append(trace[i]).append('\n');
            if (framesInCommon != 0)
                sb.append(prefix).append("\t... ").append(framesInCommon).append(" more").append('\n');

            // Print suppressed exceptions, if any
            for (Throwable se : e.getSuppressed())
                logEnclosedStackTrace(sb, se, trace, "Suppressed: ", prefix + "\t", dejaVu);

            // Print cause, if any
            Throwable cause = e.getCause();
            if (cause != null)
                logEnclosedStackTrace(sb, cause, trace, "Caused by: ", prefix, dejaVu);
        }
    }
}
