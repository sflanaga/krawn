package org.krawn;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.krawn.KrawnManager.JobInfoTrack;
import org.krawn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.process.ProcessUtil;
import org.zeroturnaround.process.Processes;

public class ReaperThread extends Thread {
    static Logger log = LoggerFactory.getLogger(ReaperThread.class);

    final long sleepTime;
    
    public ReaperThread(long sleeptime) {
        this.sleepTime = sleeptime;
    }
    
    @Override
    public void run() {

        while (true) {

            try {
                if (KrawnManager.running != null) {
                    long now = System.currentTimeMillis();
                    ArrayList<JobInfoTrack> tocheck = new ArrayList<>();
                    KrawnManager.lock.lock();
                    try {
                        for (JobInfoTrack job : KrawnManager.running.values())
                            if ( job.startedProc != null )
                                tocheck.add(job);

                    } finally {
                        KrawnManager.lock.unlock();
                    }
                    for (JobInfoTrack job : tocheck) {
                        long delta = now - job.startTime;
                        if (delta > job.cron.timeoutms) {
                            log.warn("job name: " + job.cron.name + " timing out at life time: " + Util.longSpanToStringShort(delta, 2));
                            try {
                                ProcessUtil.destroyGracefullyOrForcefullyAndWait(Processes.newStandardProcess(job.startedProc.getProcess()), 5000, TimeUnit.MILLISECONDS, 1000,
                                        TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                log.error("error trying to kill process: " + job.cron.name, e);
                            }
                        } else {
                            if ( log.isDebugEnabled() )
                                log.debug("skipping reap on " + job.cron.name + " age: " + Util.longSpanToStringShort(delta, 2));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("error ", e);
            } catch (Throwable e) {
                log.error("SERVER error ", e);
            }
            
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
