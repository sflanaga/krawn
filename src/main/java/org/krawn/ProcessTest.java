package org.krawn;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import org.zeroturnaround.process.ProcessUtil;
import org.zeroturnaround.process.Processes;
import org.zeroturnaround.process.SystemProcess;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ProcessTest {
    static Logger log = LoggerFactory.getLogger(ProcessTest.class);

    static AtomicReference<ArrayList<CronJobConfig>> listholder = new AtomicReference<ArrayList<CronJobConfig>>();
    static ArrayList<CronJobConfig> list = new ArrayList<>();
    static long lastmod = 0L;

    public static class ReadConfig extends Thread {
        @Override
        public void run() {

            long thistime = 1L;
            while (true) {
                try {
                    thistime = Files.getLastModifiedTime(Paths.get("./test.conf")).toMillis();
                    if (thistime != lastmod) {
                        log.info("conf file new - reading...");
                        lastmod = thistime;
                        listholder.set(getList("./test.conf"));
                        log.info("job list updated {}", listholder.get().size());
                    }
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    log.error("error {}, {}, {} ", e.getMessage(), thistime, lastmod);
                } catch (Throwable e) {
                    log.error("SERVER error {}, {}, {} ", e.getMessage(), thistime, lastmod);
                }
            }
        }
    }

    public static ArrayList<CronJobConfig> getList(String filename) {
        ArrayList<CronJobConfig> newlist = new ArrayList<>();

        long start = System.currentTimeMillis();
        Config conf = ConfigFactory.load(ConfigFactory.parseFile(new File(filename))); // confraw.resolve();
        long readend = System.currentTimeMillis();
        System.out.println("read time: " + (readend - start));

        for (Config cfg : conf.getConfigList("cron.jobs"))
            newlist.add(new CronJobConfig(cfg));
        long end = System.currentTimeMillis();

        System.out.println("interpret time: " + (end - readend));
        return newlist;

    }

    public static void main(String[] args) {
        try {

            ReadConfig readerthread = new ReadConfig();
            readerthread.setDaemon(true);
            readerthread.start();

            log.info("Howdy");
            String output = "";
            // output = new ProcessExecutor().command("java", "-version")
            // .readOutput(true).execute()
            // .outputUTF8();
            // System.out.println(output);

            // output = new ProcessExecutor().command("cmd /C dir c:\\")
            // .readOutput(true).execute()
            // .outputUTF8();
            // System.out.println(output);

            // output = new ProcessExecutor().command("cmd", "/C", "dir", "c:\"").readOutput(true).execute().outputUTF8();
            // System.out.println(output);
            //
            // output = new ProcessExecutor().command("cmd", "/C", "dir", "c:\"").readOutput(true).execute().outputUTF8();

            StartedProcess startProc1 = new ProcessExecutor().command("cmd", "/C", "dir", "c:\\").redirectOutput(Slf4jStream.of("dude1").asInfo())
                    .redirectError(Slf4jStream.of("dude1").asError()).start();
            StartedProcess startProc2 = new ProcessExecutor().command("cmd", "/C", "dir", "c:\\").redirectOutput(Slf4jStream.of("dude2").asInfo())
                    .redirectError(Slf4jStream.of("dude2").asError()).start();

            // Future<ProcessResult> future2 = new ProcessExecutor().command("cmd", "/C", "dir", "/s", "c:\\").redirectOutput(Slf4jStream.of("dude2").asInfo()).start().getFuture();
            // Future<ProcessResult> future3 = new ProcessExecutor().command("cmd", "/C", "dir", "/s", "c:\\").redirectOutput(Slf4jStream.of("dude3").asInfo()).start().getFuture();
            // Future<ProcessResult> future4 = new ProcessExecutor().command("cmd", "/C", "dir", "/s", "c:\\").redirectOutput(Slf4jStream.of("dude4").asInfo()).start().getFuture();

            SystemProcess proc1 = Processes.newStandardProcess(startProc1.getProcess());
            SystemProcess proc2 = Processes.newStandardProcess(startProc2.getProcess());
            
            
            
            
            Thread.sleep(1000L);

            if ( proc1.isAlive() )
                ProcessUtil.destroyGracefullyOrForcefullyAndWait(proc1, 5000, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);
            else 
                log.info("already dead");

            Thread.sleep(1000L);

            ProcessUtil.destroyGracefullyOrForcefullyAndWait(proc2, 5000, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);

            // res = future2.get(2, TimeUnit.SECONDS);
            // log.info("BOOM2");
            // res = future3.get(2, TimeUnit.SECONDS);
            // log.info("BOOM3");
            // res = future4.get(2, TimeUnit.SECONDS);
            // log.info("BOOM4");

            // while(true) {
            // //log.info("tick");
            // try { Thread.sleep(1000L); } catch(InterruptedException e) { break; }
            // }

            System.out.println("DONE");
            System.out.flush();
            System.err.flush();

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
            }

            System.exit(0);

        } catch (Exception e) {
            log.error("ex:", e);
        }
    }
}
