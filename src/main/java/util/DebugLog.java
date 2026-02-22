package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DebugLog {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final boolean ENABLED = detectIdeRun();

    private DebugLog() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void info(String tag, String message) {
        if (!ENABLED) {
            return;
        }
        System.out.println(prefix(tag) + message);
    }

    public static void error(String tag, String message, Throwable ex) {
        if (!ENABLED) {
            return;
        }
        System.err.println(prefix(tag) + message);
        if (ex != null) {
            ex.printStackTrace(System.err);
        }
    }

    private static String prefix(String tag) {
        return "[DEBUG " + TS.format(LocalTime.now()) + "][" + tag + "] ";
    }

    private static boolean detectIdeRun() {
        String classPath = System.getProperty("java.class.path", "").toLowerCase(Locale.ROOT);
        String jvmArgs = System.getProperty("java.vm.info", "").toLowerCase(Locale.ROOT);
        String launcher = System.getProperty("sun.java.command", "").toLowerCase(Locale.ROOT);

        boolean intellij = classPath.contains("idea_rt.jar") || launcher.contains("com.intellij");
        boolean eclipse = classPath.contains("org.eclipse.jdt") || launcher.contains("org.eclipse");
        boolean netbeans = classPath.contains("netbeans") || launcher.contains("netbeans");
        boolean debugAgent = jvmArgs.contains("jdwp");

        return intellij || eclipse || netbeans || debugAgent;
    }
}
