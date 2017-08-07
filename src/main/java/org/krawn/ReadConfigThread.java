package org.krawn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

    public void updateConfig() throws IOException {
        thistime = Files.getLastModifiedTime(Paths.get(filename)).toMillis();
        if (thistime != KrawnManager.lastmod) {
            log.info("conf file new - reading...");
            KrawnManager.lastmod = thistime;
            KrawnManager.listholder.set(readConfig(filename));
            log.info("Config ACCEPTED - job list updated {}", KrawnManager.listholder.get().size());
        }      
    }
    long thistime = 0L;
    @Override
    public void run() {

        
        while (true) {
            try {
                if (!Files.exists(Paths.get(filename))) {
                    KrawnManager.lastmod = thistime;
                    KrawnManager.log.error(filename + " config file is missing");
                } else {
                    updateConfig();
                }
                Thread.sleep(KrawnManager.configPollTime);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                log.error("error reading config - NOT ACCEPTED {}", e.toString());
            } catch (Throwable e) {
                log.error("SERVER error {}, {}, {} ", e.getMessage(), thistime, KrawnManager.lastmod);
            }
        }
    }
    public static TreeMap<String, CronJobConfig> readConfig(String filename) {
        TreeMap<String, CronJobConfig> newlist = new TreeMap<>();

        long start = System.currentTimeMillis();
        Config conf = ConfigFactory.load(ConfigFactory.parseFile(new File(filename))); // confraw.resolve();
        long readend = System.currentTimeMillis();
        log.info("config read time: " + Util.longSpanToStringShort(readend - start,1));

        KrawnManager.reaperPollTime = conf.getDuration("cron.reaperPollTime", TimeUnit.MILLISECONDS); 
        KrawnManager.configPollTime = conf.getDuration("cron.configPollTime", TimeUnit.MILLISECONDS); 

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