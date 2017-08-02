package org.krawn;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.StartedProcess;

public class ProcessManager {
    static Logger log = LoggerFactory.getLogger(ProcessManager.class);

    static AtomicReference<TreeMap<String, CronJobConfig>> listholder = new AtomicReference<TreeMap<String, CronJobConfig>>();

    static ArrayList<CronJobConfig> list = new ArrayList<>();
    static long lastmod = 0L;

    static public class JobInfoTrack {
        public StartedProcess startedProc;
        public final long startTime;
        public final CronJobConfig cron;

        public JobInfoTrack(long startTime, CronJobConfig cron) {
            super();
            this.startTime = startTime;
            this.cron = cron;
        }
    }

    static final ReentrantLock lock = new ReentrantLock();
    static TreeMap<String, JobInfoTrack> pending = new TreeMap<>();
    static TreeMap<String, JobInfoTrack> running = new TreeMap<>();


    public static void main(String[] args) {
        final AtomicBoolean keepRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                log.warn("got shutdown signal");
                keepRunning.set(false);
            }
        });
        log.info("STARTING krawn");
        ReadConfigThread readerthread = new ReadConfigThread("./test.conf");
        readerthread.setDaemon(true);
        readerthread.start();

        KrawnThread krawnthread = new KrawnThread();
        krawnthread.setDaemon(true);
        krawnthread.start();
        
        ReaperThread reaperThread = new ReaperThread();
        reaperThread.setDaemon(true);
        reaperThread.start();

        while (keepRunning.get()) {
            // log.info("tick");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                log.warn("Shutting down...");
                break;

            }
        }
        try {
            log.info("interupting children");
            readerthread.interrupt();
            krawnthread.interrupt();
        } catch(Throwable ee) {
            log.error("error during shutdown so exit(1)", ee);
            System.exit(1);
        }
    }
}
