package Utils;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Semaphore;

public class CustomLocalReceiver implements VerificationCodeReceiver {

    private HttpServer server;
    private String code;
    private String error;
    private int port;
    private final Semaphore signal = new Semaphore(0);

    @Override
    public String getRedirectUri() throws IOException {
        // Pick a free port
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/Callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2) {
                        if ("code".equals(kv[0]))  code  = kv[1];
                        if ("error".equals(kv[0])) error = kv[1];
                    }
                }
            }
            String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                    + "<title>Hirely</title></head>"
                    + "<body style='font-family:sans-serif;text-align:center;padding-top:100px'>"
                    + "<h2 style='color:#2ecc71'>&#10003; Login successful!</h2>"
                    + "<p>You are now logged in. You can close this tab and go back to Hirely.</p>"
                    + "</body></html>";
            byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
            signal.release();
        });
        server.start();
        return "http://localhost:" + port + "/Callback";
    }

    @Override
    public String waitForCode() throws IOException {
        signal.acquireUninterruptibly();
        if (error != null) throw new IOException("Google auth error: " + error);
        return code;
    }

    @Override
    public void stop() throws IOException {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
