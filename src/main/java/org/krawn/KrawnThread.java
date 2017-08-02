package org.krawn;

import java.io.IOException;
import java.util.ArrayList;

import org.krawn.ProcessManager.JobInfoTrack;
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
        while (true)

            try {
                long now = System.currentTimeMillis();
                if (ProcessManager.listholder.get() != null) {

                    int dead=0, pend=0, run=0;
                    //
                    // clean out old expired - self expired jobs
                    //
                    ArrayList<JobInfoTrack> deadProc = new ArrayList<>();
                    for (JobInfoTrack runningJob : ProcessManager.running.values()) {
                        if (runningJob.startedProc != null && !runningJob.startedProc.getProcess().isAlive()) {
                            deadProc.add(runningJob);
                        }
                    }

                    ProcessManager.lock.lock();
                    try {
                        for (JobInfoTrack deadJob : deadProc) {
                            log.info(deadJob.cron.name + " finished in " + Util.longSpanToStringShort(now-deadJob.startTime, 2) + " exit code: " + deadJob.startedProc.getProcess().exitValue());
                            ProcessManager.running.remove(deadJob.cron.name);
                            dead++;
                        }
                    } finally {
                        ProcessManager.lock.unlock();
                    }

                    for (CronJobConfig c : ProcessManager.listholder.get().values()) {
                        ProcessManager.lock.lock();
                        try {
                            //
                            // find jobs that need to be put into pending
                            // we use pending so we save the cost of time computation on
                            // later passes
                            //
                            JobInfoTrack pendJob = ProcessManager.pending.get(c.name);
                            if (pendJob == null) {
                                if (!ProcessManager.running.containsKey(c.name)) {
                                    long next = c.schedule.nextRun(now);
                                    JobInfoTrack j = new JobInfoTrack(next, c);
                                    if (Math.abs(next - now) < slop) {
                                        ProcessManager.running.put(c.name, j);
                                        log.info("immediate run: " + c.name);
                                        runJob(j);
                                        run++;
                                    } else {
                                        log.info("pending: " + c.name);
                                        ProcessManager.pending.put(c.name, j);
                                        pend++;
                                    }
                                }
                            } else {
                                if (!ProcessManager.running.containsKey(c.name)) {
                                    if (Math.abs(pendJob.startTime - now) < slop) {
                                        log.info("running pending: " + c.name);
                                        ProcessManager.pending.remove(pendJob.cron.name);
                                        ProcessManager.running.put(c.name, pendJob);
                                        runJob(pendJob);
                                        run++;
                                    }
                                }
                            }
                        } finally {
                            ProcessManager.lock.unlock();
                        }
                    }
                    if ( dead >0 || pend > 0 || run > 0 )
                        if ( log.isDebugEnabled())
                            log.debug("Job state changes: dead: " + dead + " pended: " + pend + " run: " + run + " pending: " + ProcessManager.pending.size() + " running: " + ProcessManager.running.size());
                    
                    long timeofsleep = System.currentTimeMillis();
                    long sleeptime = (timeofsleep / 1000L + 1) * 1000L - timeofsleep;
                    if ( log.isDebugEnabled() )
                        log.debug("sleeping for " + sleeptime + " pass time: " + (timeofsleep - now));

                    Thread.sleep(sleeptime);
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
    public static void runJob(JobInfoTrack j) {

        try {
            log.info("starting " + j.cron.name);
            StartedProcess startProc = new ProcessExecutor().command(j.cron.command).redirectOutput(Slf4jStream.of(j.cron.name).asInfo())
                    .redirectError(Slf4jStream.of(j.cron.name).asError()).start();
            j.startedProc = startProc;
        } catch (IOException e) {
            log.error("IO except during proc start for: " + j.cron.name + " command: " + j.cron.command);
        }

    }


}