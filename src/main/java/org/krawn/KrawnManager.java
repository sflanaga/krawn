package org.krawn;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.StartedProcess;

import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

public class KrawnManager {
    static Logger log = LoggerFactory.getLogger(KrawnManager.class);

    static AtomicReference<TreeMap<String, CronJobConfig>> listholder = new AtomicReference<TreeMap<String, CronJobConfig>>();

    static ArrayList<CronJobConfig> list = new ArrayList<>();
    static long lastmod = 0L;
    static long reaperPollTime=1000L;
    static long configPollTime=500L;

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
    static TreeMap<String, JobInfoTrack> running = new TreeMap<>();

    public static String[] cmd_setup;
    
    static void usage() {
        System.err.println("usage: ");
        System.err.println("nohup java -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=3 -XX:GCLogFileSize=16m -Xloggc:log/gc.log -verbose:gc -XX:+PrintGCDateStamps -Xmx128m -jar krawn.jar krawn.conf > it 2>&1 & ");
        System.exit(1);
    }
    // nohup java -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=3 -XX:GCLogFileSize=16m -Xloggc:log/gc.log -verbose:gc -XX:+PrintGCDateStamps -Xmx128m -jar krawn.jar > it 2>&1 &
    public static void main(String[] args) {
        try {
        if ( args.length != 1 ) {
            usage();
        }
        
        if ( System.getProperty("os.name").toLowerCase().contains("win") ) {
            cmd_setup = new String[] {"cmd.exe", "/C"};
        } else {
            cmd_setup = new String[] {"/bin/bash", "-c"};
        }
        
        log.info(System.getProperty("os.name"));
        
        
        if ( !Files.exists(Paths.get(args[0])) ) {
            System.err.println("Cannot find config file: " + args[0]);
            usage();
        }
        ReadConfigThread readerthread = new ReadConfigThread(args[0]);

        readerthread.updateConfig();
        
        final AtomicBoolean keepRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.warn("got shutdown signal");
                keepRunning.set(false);
            }
        });
        log.info("STARTING krawn");
        readerthread.setDaemon(true);
        readerthread.start();

        KrawnThread krawnthread = new KrawnThread();
        krawnthread.setDaemon(true);
        krawnthread.start();

        ReaperThread reaperThread = new ReaperThread(reaperPollTime);
        reaperThread.setDaemon(true);
        reaperThread.start();

        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();


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
        } catch (Throwable ee) {
            log.error("error during shutdown so exit(1)", ee);
            System.exit(1);
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
