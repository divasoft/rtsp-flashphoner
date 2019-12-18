package divasoft.utils;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import divasoft.rtsp.Rtsp;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pnzdevelop
 */
public class HttpService {

    private String host;
    private int port;
    private String login;
    private String pwd;
    private List<String> contextNames;

    private HttpServer server;

    public HttpService(String host, int port, String login, String pwd) {
        this.host = host;
        this.port = port;
        this.login = login;
        this.pwd = pwd;

        this.start();
    }

    private void addContext(String name, HttpHandler service) {
        contextNames.add(name);
        server.createContext(name, service).setAuthenticator(new Auth(login, pwd));
    }

    public String getContexts() {
        return String.join("\n", contextNames);
    }

    private void start() {
        try {
            Log.msg("Start HTTP service on " + this.host + ":" + this.port + "");
            server = HttpServer.create(new InetSocketAddress(this.host, this.port), 0);
            contextNames = new ArrayList<>();
            this.addContext("/start", new HttpStartService());
            this.addContext("/status", new HttpStatusService());
            this.addContext("/report", new HttpReportService());
            this.addContext("/log", new HttpLogService());
            this.addContext("/flashphoner/ssl", new HttpFlashphonerSSLService());
            this.addContext("/flashphoner/service", new HttpFlashphonerService());
            this.addContext("/", new HttpHelpService(this.getContexts()));

            // TODO: Stop
            // TODO: Restart
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (Exception e) {
            Log.err(e.getMessage());
        }
    }

}

class HttpHelpService extends HttpServiceBase {

    private String serverContexts;

    public HttpHelpService(String serverContexts) {
        this.serverContexts = serverContexts;
    }

    @Override
    public String response() {
        return Rtsp.PROGRAM_TITLE + "\n" + "Commands avalible:\n" + serverContexts;
    }

}

class Auth extends BasicAuthenticator {

    private String login;
    private String pwd;

    public Auth(String login, String pwd) {
        super("get");
        this.login = login;
        this.pwd = pwd;
    }

    @Override
    public boolean checkCredentials(String user, String password) {
        return user.equals(login) && password.equals(pwd);
    }
}

abstract class HttpServiceBase implements HttpHandler {

    abstract public String response();

    @Override
    public void handle(HttpExchange t) {
        try {
            String response = this.response();

            byte[] bs = response.getBytes("UTF-8");
            t.sendResponseHeaders(200, bs.length);
            OutputStream os = t.getResponseBody();
            os.write(bs);

            /*t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());*/
            os.close();
        } catch (Exception e) {
            Log.err(e.getMessage());
        }

    }
}

class HttpStartService extends HttpServiceBase {

    @Override
    public String response() {
        String response = "{\"status\":\"start\"}";
        if (!Rtsp.is_work()) {
            Rtsp.getLog().stop();
            Rtsp.getLog().start();
            Rtsp.start();
        } else {
            response = "{\"status\":\"progress\", \"msg\":\"" + Rtsp.getStat() + "\"}";
        }
        return response;
    }
}

class HttpStatusService extends HttpServiceBase {

    @Override
    public String response() {
        String response = "{\"status\":\"stop\"}";
        if (Rtsp.is_work()) {
            response = "{\"status\":\"progress\", \"msg\":\"" + Rtsp.getStat() + "\"}";
        }
        return response;
    }
}

class HttpReportService extends HttpServiceBase {

    @Override
    public String response() {
        return Rtsp.getReport();
    }

}

class HttpLogService extends HttpServiceBase {

    @Override
    public String response() {
        return Rtsp.getLog().get();
    }

}

class HttpFlashphonerSSLService extends HttpServiceBase {

    @Override
    public String response() {
        Server f = new Server();
        f.setUrl(Rtsp.FLASHPHONER_URL);
        return f.getSSLDateTo().toString();
    }

}

class HttpFlashphonerService extends HttpServiceBase {

    @Override
    public String response() {
        return Rtsp.checkPortServices();
    }

}
