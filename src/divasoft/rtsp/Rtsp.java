package divasoft.rtsp;

import divasoft.utils.Server;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import divasoft.utils.Log;
import divasoft.utils.HttpService;
import divasoft.utils.Web;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.ini4j.Wini;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author pnzdevelop
 */
public class Rtsp {

    public static String LOCATION = "./";
    public static String SAVE_DIR = "/tmp/";
    public static String REPORT_FILE = "report.xml";
    public static String CONFIG_FILE = "config.ini";

    public static List<RtspBean> LIST_CHECK = new ArrayList<RtspBean>();

    public static int THREADS_COUNT = Runtime.getRuntime().availableProcessors();

    public static int PING_TIMEOUT = 1000;
    public static int PORT_TIMEOUT = 1000;
    public static int WEB_CLIENT_TIMEOUT = 1000;
    public static int PROCESS_TIMEOUT = 30000;
    public static int TRY_GET_IMAGE_CNT = 2;
    public static int TRY_GET_IMAGE_TIME = 5000;
    
    public static String INTERNAL_WEB_SERVER_HOST = "127.0.0.1";
    public static int INTERNAL_WEB_SERVER_PORT = 8081;
    public static String INTERNAL_WEB_SERVER_LOGIN = "admin";
    public static String INTERNAL_WEB_SERVER_PWD = "admin";

    public static String FLASHPHONER_PORTS = "1935|3478|5349|80|8080|8081|8082|443|8443|8444|8445|2000|9091|8888";
    public static String FLASHPHONER_SERVICES = "webcallserver|haproxy|turnserver";
    public static String FLASHPHONER_URL = "https://video.divasoft.ru";

    public static String LIST_URL = "";
    public static String LIST_FILE = "";
    public static String SINGLE_URL = "";

    public static final String PROGRAM_TITLE = "Divasoft RTSP Tester 1.0";

    private static boolean is_work = false;

    private static final String OS = System.getProperty("os.name").toLowerCase();
    //private static long timeStart = System.currentTimeMillis();
    private static Log log = new Log();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Rtsp.LOCATION = Rtsp.getFileLocation(Rtsp.class.getProtectionDomain().getCodeSource());

        Rtsp.SAVE_DIR = Rtsp.LOCATION + "report/";
        Rtsp.REPORT_FILE = Rtsp.LOCATION + Rtsp.REPORT_FILE;
        

        saveConfig();
        loadConfig();

        // TODO: Время старта/окончания
        // Настройки
        CommandLine commandLine;
        Options options = new Options();

        Option option_File = Option.builder("F")
                .hasArg()
                .desc("File path to list rtsp")
                .longOpt("file")
                .build();

        Option option_Url = Option.builder("U")
                .hasArg()
                .desc("URL to list rtsp")
                .longOpt("url")
                .build();

        Option option_Single = Option.builder("S")
                .hasArg()
                .desc("RTSP single url")
                .longOpt("single")
                .build();

        Option option_Debug = Option.builder("D")
                .desc("Debug ON")
                .longOpt("debug")
                .build();

        Option option_Help = Option.builder("H")
                .desc("Help info")
                .longOpt("help")
                .build();

        options.addOption(option_File);
        options.addOption(option_Url);
        options.addOption(option_Single);
        options.addOption(option_Help);
        options.addOption(option_Debug);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            Log.msg(PROGRAM_TITLE);

            commandLine = parser.parse(options, args);

            if ((!commandLine.hasOption("F") && !commandLine.hasOption("U") && !commandLine.hasOption("S")) || commandLine.hasOption("H")) {
                formatter.printHelp("rtsp", options);
                System.exit(0);
            }

            if (commandLine.hasOption("D")) {
                Log.debug = true;
            }

            if (commandLine.hasOption("F")) {
                LIST_FILE = commandLine.getOptionValue("F");
            }

            if (commandLine.hasOption("U")) {
                LIST_URL = commandLine.getOptionValue("U");
            }

            if (commandLine.hasOption("S")) {
                SINGLE_URL = commandLine.getOptionValue("S");
            }

            if (INTERNAL_WEB_SERVER_PORT > 0 && !commandLine.hasOption("S")) {
                HttpService httpService = new HttpService(INTERNAL_WEB_SERVER_HOST, INTERNAL_WEB_SERVER_PORT, INTERNAL_WEB_SERVER_LOGIN, INTERNAL_WEB_SERVER_PWD);
            } else {
                Log.msg("If you need start HTTP service, change in config INTERNAL_WEB_SERVER_PORT");
                start();
                
                Log.msg(Rtsp.checkPortServices());
            }

        } catch (ParseException exception) {
            formatter.printHelp("rtsp", options);
            Log.out(exception.getMessage());
        }

    }

    public static void start() {
        LIST_CHECK = new ArrayList<RtspBean>();

        if (!LIST_FILE.isEmpty()) {
            getListFromFile(LIST_FILE);
        }
        if (!LIST_URL.isEmpty()) {
            getListFromUrl(LIST_URL);
        }

        if (!SINGLE_URL.isEmpty()) {
            addRtsp(SINGLE_URL);
        }

        if (LIST_CHECK.isEmpty()) {
            Log.msg("Nothing check...");
            return;
        }
        try {
            Thread worker = new Thread(new Worker());
            worker.start();
        } catch (Exception e) {
            Log.out(e.getLocalizedMessage());
        }
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static void report() {
        if (LIST_CHECK.isEmpty()) {
            Log.msg("Nothing report...");
            return;
        }
        saveXmlReport();
    }

    public static void addRtsp(String uri) {
        if (uri.isEmpty()) {
            return;
        }

        Log.out("Add url to check: " + uri);

        try {
            LIST_CHECK.add(new RtspBean(uri));
        } catch (Exception e) {
            Log.err(e.getMessage());
        }

    }

    public static void getListFromFile(String filePath) {
        try {

            Log.msg("Load from file: " + filePath);
            Scanner fs = new Scanner(new File(filePath), "UTF-8").useDelimiter("\n");

            while (fs.hasNextLine()) {
                addRtsp(fs.nextLine());
            }
        } catch (IOException ex) {
            Log.err(ex.getLocalizedMessage());
        }
    }

    public static void getListFromUrl(String uri) {
        try {

            Log.msg("Load from url: " + uri);
            Scanner fs = new Scanner(new URL(uri).openStream(), "UTF-8").useDelimiter("\n");

            while (fs.hasNextLine()) {
                addRtsp(fs.nextLine());
            }
        } catch (IOException ex) {
            Log.err(ex.getLocalizedMessage());
        }
    }

    public static void saveConfig() {
        try {
            File file = new File(LOCATION + CONFIG_FILE);
            if (!file.exists()) {
                Log.msg("Generate config file: " + file.getPath());
                file.createNewFile();
                Wini ini = new Wini(file);

                ini.put("main", "PING_TIMEOUT", PING_TIMEOUT);
                ini.put("main", "PORT_TIMEOUT", PORT_TIMEOUT);
                ini.put("main", "PROCESS_TIMEOUT", PROCESS_TIMEOUT);
                ini.put("main", "WEB_CLIENT_TIMEOUT", WEB_CLIENT_TIMEOUT);
                ini.put("main", "THREADS_COUNT", THREADS_COUNT);
                ini.put("main", "TRY_GET_IMAGE_CNT", TRY_GET_IMAGE_CNT);
                ini.put("main", "TRY_GET_IMAGE_TIME", TRY_GET_IMAGE_TIME);

                ini.put("web", "INTERNAL_WEB_SERVER_HOST", INTERNAL_WEB_SERVER_HOST);
                ini.put("web", "INTERNAL_WEB_SERVER_PORT", INTERNAL_WEB_SERVER_PORT);
                ini.put("web", "INTERNAL_WEB_SERVER_LOGIN", INTERNAL_WEB_SERVER_LOGIN);
                ini.put("web", "INTERNAL_WEB_SERVER_PWD", INTERNAL_WEB_SERVER_PWD);

                ini.put("flashphoner", "FLASHPHONER_URL", FLASHPHONER_URL);
                ini.put("flashphoner", "FLASHPHONER_SERVICES", FLASHPHONER_SERVICES);
                ini.put("flashphoner", "FLASHPHONER_PORTS", FLASHPHONER_PORTS);
                ini.store();
            }

        } catch (Exception ex) {
            Log.err(ex.getLocalizedMessage());
        }
    }

    public static void loadConfig() {
        try {
            File file = new File(LOCATION + CONFIG_FILE);
            Wini ini = new Wini(file);

            PING_TIMEOUT = ini.fetch("main", "PING_TIMEOUT", int.class);
            PORT_TIMEOUT = ini.fetch("main", "PORT_TIMEOUT", int.class);
            PROCESS_TIMEOUT = ini.fetch("main", "PROCESS_TIMEOUT", int.class);
            WEB_CLIENT_TIMEOUT = ini.fetch("main", "WEB_CLIENT_TIMEOUT", int.class);
            THREADS_COUNT = ini.fetch("main", "THREADS_COUNT", int.class);
            TRY_GET_IMAGE_CNT = ini.fetch("main", "TRY_GET_IMAGE_CNT", int.class);
            TRY_GET_IMAGE_TIME = ini.fetch("main", "TRY_GET_IMAGE_TIME", int.class);

            INTERNAL_WEB_SERVER_HOST = ini.fetch("web", "INTERNAL_WEB_SERVER_HOST", String.class);
            INTERNAL_WEB_SERVER_PORT = ini.fetch("web", "INTERNAL_WEB_SERVER_PORT", int.class);
            INTERNAL_WEB_SERVER_LOGIN = ini.fetch("web", "INTERNAL_WEB_SERVER_LOGIN", String.class);
            INTERNAL_WEB_SERVER_PWD = ini.fetch("web", "INTERNAL_WEB_SERVER_PWD", String.class);

            FLASHPHONER_URL = ini.fetch("flashphoner", "FLASHPHONER_URL", String.class);
            FLASHPHONER_SERVICES = ini.fetch("flashphoner", "FLASHPHONER_SERVICES", String.class);
            FLASHPHONER_PORTS = ini.fetch("flashphoner", "FLASHPHONER_PORTS", String.class);

            Log.msg("Settings loaded from: " + file.getPath());
        } catch (Exception ex) {
            Log.err(ex.getLocalizedMessage());
        }
    }

    public static void initDir(String dirPath) {
        try {
            Log.msg(dirPath);
            File dir = new File(dirPath);
            dir.mkdirs();
        } catch (Exception ex) {
            Log.err(ex.getLocalizedMessage());
        }
    }

    public static String getFileLocation(CodeSource codeSource) {
        try {
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            return jarFile.getParentFile().getPath() + "/";
        } catch (Exception ex) {
            Log.err(ex.getLocalizedMessage());
            return "./";
        }

    }

    public static String getMD5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(str));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception ex) {
            Log.err(ex.getLocalizedMessage());
        }
        return "";
    }

    static void saveXmlReport() {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("report");
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            rootElement.setAttribute("date", formatter.format(date));
            rootElement.setAttribute("items", LIST_CHECK.size() + "");
            // Добавить время генерации
            doc.appendChild(rootElement);

            // TODO: Статистика по работающим сервисам
            for (RtspBean rtspBean : LIST_CHECK) {
                Element item = doc.createElement("item");
                item.setAttribute("image_log", rtspBean.getRtspImageLog());
                item.setAttribute("time", rtspBean.getWorkTime() + "");
                item.setAttribute("url", rtspBean.getUrl());
                item.setAttribute("host", rtspBean.getHost());
                item.setAttribute("port", rtspBean.getPort() + "");
                item.setAttribute("admin_title", rtspBean.getAdminPageTitle() + "");
                item.setAttribute("is_ping", (rtspBean.is_ping()) ? "Y" : "N");
                item.setAttribute("is_port", (rtspBean.is_port()) ? "Y" : "N");
                item.setAttribute("is_admin_port", (rtspBean.is_admin_port()) ? "Y" : "N");
                item.setAttribute("is_image", (rtspBean.getImageEncoded().isEmpty()) ? "N" : "Y");
                item.setTextContent(rtspBean.getImageEncoded());
                rootElement.appendChild(item);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);

            File fileXml = new File(Rtsp.REPORT_FILE);
            fileXml.createNewFile();

            StreamResult result = new StreamResult(fileXml);
            transformer.transform(source, result);

            Log.msg("Save XML [" + Rtsp.REPORT_FILE + "]");

        } catch (Exception ex) {
            Log.err(ex.getMessage());
        }
    }

    public static String getReport() {
        try {
            File reportFile = new File(Rtsp.REPORT_FILE);
            if (reportFile.exists()) {
                Scanner fs = new Scanner(new File(Rtsp.REPORT_FILE), "UTF-8").useDelimiter("\n");
                StringBuilder strBuild = new StringBuilder();
                while (fs.hasNextLine()) {
                    strBuild.append(fs.nextLine());
                }
                return strBuild.toString().trim();
            }

        } catch (Exception e) {
            Log.err(e.getMessage());
        }
        return "";
    }

    public static String getStat() {
        int cnt = 0;
        for (RtspBean rtspBean : LIST_CHECK) {
            if (rtspBean.getWorkTime() > 0) {
                cnt++;
            }
        }
        return cnt + " from " + LIST_CHECK.size();
    }

    public static String checkPortServices() {
        Server f = new Server();
        StringBuilder ret = new StringBuilder();
        String host = Rtsp.INTERNAL_WEB_SERVER_HOST;
        
        try {
             host = new URL(Rtsp.FLASHPHONER_URL).getHost();
        } catch (Exception ex) {
            
        }
        
        String[] portName = Rtsp.FLASHPHONER_PORTS.split("\\|");
        for (String port : portName) {
            try {
                boolean check = Web.check_port(host, Integer.parseInt(port));
                ret.append("\nPort: "+host+ ":" + port + " - " + ((check) ? "OPEN" : "CLOSED"));
            } catch (Exception e) {
                Log.err(e.getMessage());
            }

        }

        String[] serviceName = Rtsp.FLASHPHONER_SERVICES.split("\\|");
        for (String service : serviceName) {
            ret.append("\n\n\n============================================\nServce: " + service + "\n");
            ret.append(f.serviceCheck(service));
        }

        return ret.toString().trim();
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists() && !"/".equals(directory.getPath())) {
            Log.out("Clear directory: " + directory.getPath());
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        //deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    public static Log getLog() {
        return log;
    }

    public static boolean is_work() {
        return is_work;
    }

    public static void set_work(boolean is_work) {
        Rtsp.is_work = is_work;
    }

}
