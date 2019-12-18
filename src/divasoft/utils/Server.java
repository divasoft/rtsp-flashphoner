package divasoft.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 *
 * @author pnzdevelop
 */
public class Server {

    private int port = 443;
    private String host = "127.0.0.1";
    private String url = "";
    // TODO: protocol

    public String serviceCheck(String serviceName) {
        try {
            Process processDuration = new ProcessBuilder("service", serviceName, "status").redirectErrorStream(true).start();

            StringBuilder strBuild = new StringBuilder();
            try ( BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(processDuration.getInputStream(), Charset.defaultCharset()));) {
                String line;
                while ((line = processOutputReader.readLine()) != null) {
                    strBuild.append(line + System.lineSeparator());
                }
            }
            String outputJson = strBuild.toString().trim();
            return outputJson;

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return "";
    }

    public X509Certificate getSSLUserSert() {
        try {

            URL destinationURL = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();

            if (certs == null || certs.length == 0 || (!(certs[0] instanceof X509Certificate))) {
                throw new SSLPeerUnverifiedException("No server's end-entity certificate");
            }

            return ((X509Certificate) certs[0]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    @Override
    public String toString() {
        return this.getUrl();
    }

    public Date getSSLDateTo() {
        X509Certificate sert = this.getSSLUserSert();
        return sert.getNotAfter();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        this.updUrl();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
        this.updUrl();
    }

    private void updUrl() {
        this.url = "https://" + this.host + ":" + this.port;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
