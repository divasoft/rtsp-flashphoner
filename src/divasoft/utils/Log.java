package divasoft.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author pnzdevelop
 */
public class Log {

    private ByteArrayOutputStream baos;
    private PrintStream previous;
    private boolean capturing;
    public static boolean debug = false;

    public static void msg(String str) {
        System.out.println(str);
    }

    public static void err(String str) {
        System.err.println(str);
    }

    public static void out(String str) {
        if (debug) {
            msg(str);
        }
    }

    public String get() {
        if (!capturing) {
            return "";
        }
        return baos.toString();
    }

    public void start() {
        if (capturing) {
            return;
        }

        capturing = true;
        previous = System.out;
        baos = new ByteArrayOutputStream();

        OutputStream outputStreamCombiner
                = new OutputStreamCombiner(Arrays.asList(previous, baos));
        PrintStream custom = new PrintStream(outputStreamCombiner);

        System.setOut(custom);
    }

    public String stop() {
        if (!capturing) {
            return "";
        }

        System.setOut(previous);

        String capturedValue = baos.toString();

        baos = null;
        previous = null;
        capturing = false;

        return capturedValue;
    }

    private static class OutputStreamCombiner extends OutputStream {

        private List<OutputStream> outputStreams;

        public OutputStreamCombiner(List<OutputStream> outputStreams) {
            this.outputStreams = outputStreams;
        }

        public void write(int b) throws IOException {
            for (OutputStream os : outputStreams) {
                os.write(b);
            }
        }

        public void flush() throws IOException {
            for (OutputStream os : outputStreams) {
                os.flush();
            }
        }

        public void close() throws IOException {
            for (OutputStream os : outputStreams) {
                os.close();
            }
        }
    }
}
