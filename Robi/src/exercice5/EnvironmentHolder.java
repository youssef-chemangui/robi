package exercice5;

public class EnvironmentHolder {
    private static Environment instance;

    public static void setInstance(Environment env) {
        instance = env;
    }

    public static Environment getInstance() {
        if (instance == null)
            throw new Error("Environment not initialized!");
        return instance;
    }
}