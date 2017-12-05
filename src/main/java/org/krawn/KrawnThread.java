package org.krawn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.krawn.KrawnManager.JobInfoTrack;
import org.krawn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

public class KrawnThread extends Thread {
    static Logger log = LoggerFactory.getLogger(KrawnThread.class);
    private final long slop = 800L;

    @Override
    public void run() {
        long lastrun = 0L;
        long baseSleepTime = 1000L;
        LinkedBlockingQueue<JobInfoTrack> jobQueue = new LinkedBlockingQueue<>();

        RunJobThread[] runthreads = new RunJobThread[4];
        for (int i = 0; i < runthreads.length; i++) {
            runthreads[i] = new RunJobThread(jobQueue);
            runthreads[i].setDaemon(true);
            runthreads[i].start();
        }

        while (true)

            try {
                long now = System.currentTimeMillis();

                if (KrawnManager.listholder.get() != null) {

                    int dead = 0, run = 0;
                    //
                    // clean out old expired - regular exited jobs
                    //
                    ArrayList<JobInfoTrack> deadProc = new ArrayList<>();
                    for (JobInfoTrack runningJob : KrawnManager.running.values()) {
                        if (runningJob.startedProc != null && !runningJob.startedProc.getProcess().isAlive()) {
                            deadProc.add(runningJob);
                        }
                    }

                    KrawnManager.lock.lock();
                    try {
                        for (JobInfoTrack deadJob : deadProc) {
                            log.info(deadJob.cron.name + " finished in " + Util.longSpanToStringShort(now - deadJob.startTime, 2) + " exit code: "
                                    + deadJob.startedProc.getProcess().exitValue());
                            KrawnManager.running.remove(deadJob.cron.name);
                            dead++;
                        }
                    } finally {
                        KrawnManager.lock.unlock();
                    }

                    //
                    // run jobs
                    //
                    for (CronJobConfig c : KrawnManager.listholder.get().values()) {
                        KrawnManager.lock.lock();
                        try {
                            //
                            // find jobs that need to be run now
                            //
                            JobInfoTrack runningJob = KrawnManager.running.get(c.name);
                            if (runningJob == null || !c.exclusive) {
                                CronBit.BitCheck bitCheck = new CronBit.BitCheck(new DateTime(now, c.tz));
                                if (c.schedule.hit(bitCheck)) {
                                    JobInfoTrack j = new JobInfoTrack(System.currentTimeMillis(), c);
                                    KrawnManager.running.put(c.name, j);
                                    log.info("Run: " + c.name + " cmd: " + c.command);
                                    jobQueue.put(j);
                                    run++;
                                }
                            }
                        } finally {
                            KrawnManager.lock.unlock();
                        }
                    }
                    if (dead > 0 || run > 0)
                        if (log.isDebugEnabled())
                            log.debug("Job state changes: dead: " + dead + " run: " + run + " running: " + KrawnManager.running.size());

                    long timeofsleep = System.currentTimeMillis();
                    long wakeuptime = ((timeofsleep / baseSleepTime + 1) * baseSleepTime);

                    if (log.isDebugEnabled())
                        log.debug("pass time: " + (timeofsleep - now));

                    //
                    // Thread.sleep can wake up early so you must spin until you get to the finish line
                    //
                    while (wakeuptime - timeofsleep > 0) {
                        if (log.isDebugEnabled())
                            log.debug("sleeping for " + (wakeuptime - timeofsleep));
                        Thread.sleep(wakeuptime - timeofsleep);
                        timeofsleep = System.currentTimeMillis();
                    }
                } else {
                    Thread.sleep(100L); // fast sleep waiting for initial config
                }

            } catch (InterruptedException e) {
                log.warn("interrupted so exiting Krawn thread");
                break;
            } catch (Exception e) {
                log.error("error ", e);
            } catch (Throwable e) {
                log.error("SERVER error ", e);
            }
    }

    //
    // these threads exist to keep the primary scheduler on time
    // when there are 1000 processes scheduled (only linux really supports that)
    //
    public static class RunJobThread extends Thread {
        private final LinkedBlockingQueue<JobInfoTrack> jobQueue;

        public RunJobThread(LinkedBlockingQueue<JobInfoTrack> jobQueue) {
            this.jobQueue = jobQueue;
        }

        @Override
        public void run() {
            while (true) {
                JobInfoTrack j = null;
                try {
                    j = jobQueue.take();
                    try {
                        String[] cmd = new String[3];
                        for (int i = 0; i < cmd.length - 1; i++) {
                            cmd[i] = KrawnManager.cmd_setup[i];
                        }
                        cmd[2] = j.cron.command;
                        StartedProcess startProc = null;
                        if ( j.cron.mapErrorToERROR ) 
                            startProc = new ProcessExecutor().directory(new File(j.cron.workingDir)).environment(j.cron.env).command(cmd)
                                .redirectOutput(Slf4jStream.of(j.cron.name).asInfo()).redirectError(Slf4jStream.of(j.cron.name).asError()).start();
                        else
                            startProc = new ProcessExecutor().directory(new File(j.cron.workingDir)).environment(j.cron.env).command(cmd)
                            .redirectOutput(Slf4jStream.of(j.cron.name).asInfo()).redirectError(Slf4jStream.of(j.cron.name).asInfo()).start();
                            
                        j.startedProc = startProc;
                    } catch (IOException e) {
                        log.error("IO except during proc start for: " + j.cron.name + " command: " + j.cron.command);
                    }
                } catch (Throwable e) {
                    log.error("Exceptiong during proc start for: " + j.cron.name + " command: " + j.cron.command);
                }
            }
        }
    }

}