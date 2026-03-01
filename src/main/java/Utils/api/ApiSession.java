package Utils.api;

public class ApiSession {
    private static String token;

    public static String getToken() { return token; }
    public static void setToken(String t) { token = t; }
    public static void clear() { token = null; }
}