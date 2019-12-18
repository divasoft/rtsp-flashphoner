package divasoft.utils;

import divasoft.rtsp.Rtsp;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 *
 * @author pnzdevelop
 */
public class Web {

    public static boolean check_ping(String host) {
        try {
            InetAddress geek = InetAddress.getByName(host);

            Log.out(host +" | send ping");
            if (geek.isReachable(Rtsp.PING_TIMEOUT)) {
                Log.out(host + " | ping OK");
                return true;
            } else {
                Log.out(host+" | ping FAIL");
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Crunchify's isAlive Utility
     *
     * @param hostName
     * @param port
     * @return boolean - true/false
     */
    public static boolean check_port(String hostName, int port) {
        boolean isAlive = false;

        // Creates a socket address from a hostname and a port number
        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();

        // Timeout required - it's in milliseconds
        int timeout = Rtsp.PORT_TIMEOUT;

        Log.out(hostName + ":" + port+" | try port");
        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAlive = true;
            Log.out(hostName + ":" + port+" | OK port");
        } catch (SocketTimeoutException exception) {
            Log.msg(hostName + ":" + port + " | SocketTimeout " + exception.getMessage());
        } catch (IOException exception) {
            Log.msg(hostName + ":" + port + "| Unable to connect" + exception.getMessage());
        }
        return isAlive;
    }
    
}
