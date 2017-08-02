package org.krawn;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;

import org.krawn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ReadConfigThread extends Thread {
    static Logger log = LoggerFactory.getLogger(ReadConfigThread.class);

    private final String filename;

    public ReadConfigThread(String filename) {
        this.filename = filename;
    }

    @Override
    public void run() {

        long thistime = 0L;
        while (true) {
            try {
                if (!Files.exists(Paths.get(filename))) {
                    ProcessManager.lastmod = thistime;
                    ProcessManager.log.error(filename + " config file is missing");
                } else {
                    thistime = Files.getLastModifiedTime(Paths.get(filename)).toMillis();
                    if (thistime != ProcessManager.lastmod) {
                        log.info("conf file new - reading...");
                        ProcessManager.lastmod = thistime;
                        ProcessManager.listholder.set(getList(filename));
                        log.info("job list updated {}", ProcessManager.listholder.get().size());
                    }
                }
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                log.error("error reading config {}", e.toString());
            } catch (Throwable e) {
                log.error("SERVER error {}, {}, {} ", e.getMessage(), thistime, ProcessManager.lastmod);
            }
        }
    }
    public static TreeMap<String, CronJobConfig> getList(String filename) {
        TreeMap<String, CronJobConfig> newlist = new TreeMap<>();

        long start = System.currentTimeMillis();
        Config conf = ConfigFactory.load(ConfigFactory.parseFile(new File(filename))); // confraw.resolve();
        long readend = System.currentTimeMillis();
        log.info("config read time: " + Util.longSpanToStringShort(readend - start,1));

        for (Config cfg : conf.getConfigList("cron.jobs")) {
            CronJobConfig c = new CronJobConfig(cfg);
            if (newlist.containsKey(c.name))
                throw new RuntimeException("Duplicate job name: " + c.name);
            newlist.put(c.name, c);
        }
        long end = System.currentTimeMillis();

        log.info("interpret time: " + Util.longSpanToStringShort(end - readend,1));
        return newlist;

    }


}