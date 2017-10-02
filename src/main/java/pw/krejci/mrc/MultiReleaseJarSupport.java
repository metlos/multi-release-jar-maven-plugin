package pw.krejci.mrc;

/**
 * @author Lukas Krejci
 */
final class MultiReleaseJarSupport {
    private static final boolean AVAILABLE;

    static {
        String javaVersion = System.getProperty("java.version");
        AVAILABLE = !javaVersion.startsWith("1.");
    }

    private MultiReleaseJarSupport() {
        throw new AssertionError();
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }
}
