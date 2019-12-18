package divasoft.rtsp;

import static divasoft.rtsp.Rtsp.LIST_CHECK;
import static divasoft.rtsp.Rtsp.THREADS_COUNT;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import divasoft.utils.Log;
import java.io.File;

/**
 *
 * @author pnzdevelop
 */
public class Worker implements Runnable {

    @Override
    public void run() {
        try {
            long timeStart = System.currentTimeMillis();
            if (Rtsp.is_work()) {
                return;
            }
            
            Rtsp.set_work(true);
            Rtsp.deleteDirectory(new File(Rtsp.SAVE_DIR));
            Rtsp.initDir(Rtsp.SAVE_DIR);
            
            Log.msg("Pool size " + LIST_CHECK.size());
            Log.msg("Run " + THREADS_COUNT + " threads");
            ExecutorService executor = Executors.newFixedThreadPool(THREADS_COUNT);
            for (RtspBean rtspBean : LIST_CHECK) {
                executor.execute(rtspBean);
            }
            executor.shutdown();

            while (!executor.isTerminated()) {
                // Ждём пока всё отработает
            }
            
            Rtsp.set_work(false);
            Log.msg("Work time: [" + (System.currentTimeMillis()-timeStart) + "] ms");
            Thread.sleep(5000);
            Rtsp.report();
        } catch (Exception e) {
            Rtsp.set_work(false);
            Log.out(e.getLocalizedMessage());
        }
    }
}