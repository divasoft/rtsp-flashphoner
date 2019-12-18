package divasoft.rtsp;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import divasoft.utils.Log;
import divasoft.utils.Web;

/**
 *
 * @author pnzdevelop
 */
public class RtspBean implements Runnable {

    private int admin_port = 80;
    private String url;
    private String host;
    private int port;

    private long workTime = 0;

    private boolean is_ping;
    private boolean is_port;
    private boolean is_admin_port;

    private WebClient webClient;

    private String imageEncoded = "";
    private String adminPageTitle = "";
    private String screenImageFileName = "";
    private String rtspImageLog = "";

    private int TRY_GET_IMAGE_CNT = 1;

    public RtspBean(String url) throws Exception {
        this.url = url;
        this.TRY_GET_IMAGE_CNT = Rtsp.TRY_GET_IMAGE_CNT;

        URI uri = new URI(this.url);
        this.host = uri.getHost();

        if (this.host == null) {
            throw new Exception("This is not url: " + this.url);
        }

        this.port = uri.getPort();
    }

    @Override
    public void run() {
        long timeStart = System.currentTimeMillis();
        Thread.currentThread().setName(url);
        Log.out(Thread.currentThread().getName() + " Start.");

        // Пропинговать камеру
        this.is_ping = Web.check_ping(this.host);

        // Проверить порт камеры
        this.is_port = Web.check_port(this.host, this.port);

        // Сделать скриншот
        screenImageFileName = Rtsp.SAVE_DIR + "" + Rtsp.getMD5(this.url);
        Log.out(this.url + " | Image = " + screenImageFileName);
        this.getScreenshot();

        // Проверить порт админки
        this.is_admin_port = Web.check_port(this.host, this.admin_port);

        // Зайти в админку
        this.auth();

        this.workTime = System.currentTimeMillis() - timeStart;
        Log.out(Thread.currentThread().getName() + " End [" + this.workTime + "] ms");
    }

    private void auth() {
        try {
            String adminPage = "http://" + host + ":" + admin_port;
            Log.out(adminPage + " | Get Admin page");
            webClient = new WebClient();
            webClient.getOptions().setTimeout(Rtsp.WEB_CLIENT_TIMEOUT);

            HtmlPage htmlAuthPage = webClient.getPage(adminPage);
            adminPageTitle = htmlAuthPage.getTitleText();
            Log.out(adminPage + " | Page title = " + adminPageTitle);
        } catch (Exception ex) {
            Log.err(ex.getLocalizedMessage());
        }
    }

    private void getImage() {
        try {

            // Запускаем процесс ffmpeg
            // ffmpeg -rtsp_transport tcp -i %cam% -f image2 -vframes 1 -pix_fmt yuvj420p "C:\video\test.jpg"
            File f = new File(screenImageFileName);
            if (!f.exists()) {
                Log.out("ffmpeg | Start process: " + this.url);
                Process processDuration = new ProcessBuilder("ffmpeg", "-stimeout", Rtsp.PROCESS_TIMEOUT * 1000 + "", "-rtsp_transport", "tcp", "-i", this.url, "-f", "image2", "-vframes", "1", "-pix_fmt", "yuvj420p", screenImageFileName).redirectErrorStream(true).start();

                StringBuilder strBuild = new StringBuilder();
                try ( BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(processDuration.getInputStream(), Charset.defaultCharset()));) {
                    String line;
                    while ((line = processOutputReader.readLine()) != null) {
                        //Log.out(line); // Выводим сразу всё что есть
                        strBuild.append(line + System.lineSeparator());
                    }
                    processDuration.waitFor(Rtsp.PROCESS_TIMEOUT * 2, TimeUnit.MILLISECONDS);
                }
                String outputJson = strBuild.toString().trim();
                rtspImageLog = outputJson;
                Log.msg("ffmpeg | Complite process" + this.url + "\n" + outputJson); // + outputJson // Или всё что накопится, но потом...
            } else {
                Log.out("File exist | "+ f.getPath());
            }
        } catch (Exception e) {
            Log.err(e.getMessage());
            e.printStackTrace();
        }
    }

    private void getScreenshot() {
        while (TRY_GET_IMAGE_CNT-- > 0) {
            this.getImage();
            // Проверить наличие картинки, запросить содержимое и закодировать в base64 7f690dcabc9e37781ad17d2fac9d7bd6
            try {
                File f = new File(screenImageFileName);
                if (f.exists()) {
                    byte[] fileContent = FileUtils.readFileToByteArray(f);
                    this.imageEncoded = Base64.getEncoder().encodeToString(fileContent);
                    Log.out(this.url + " | Image loaded");
                } else {
                    Log.out("Cant get image " + screenImageFileName + " | Try " + TRY_GET_IMAGE_CNT);
                    Thread.sleep(Rtsp.TRY_GET_IMAGE_TIME);
                    Log.out("Wait complete " + screenImageFileName);
                    this.getScreenshot();
                }

            } catch (Exception e) {
                Log.out(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public int getAdmin_port() {
        return admin_port;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean is_ping() {
        return is_ping;
    }

    public boolean is_port() {
        return is_port;
    }

    public boolean is_admin_port() {
        return is_admin_port;
    }

    public String getImageEncoded() {
        return imageEncoded;
    }

    public String getAdminPageTitle() {
        return adminPageTitle;
    }

    public long getWorkTime() {
        return workTime;
    }

    public String getRtspImageLog() {
        return rtspImageLog;
    }

}
