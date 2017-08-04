package org.krawn;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

public class CronJobConfig {
    final public String name;
    final public CronBit schedule;
    final public String command;
    final public long timeoutms;
    final public boolean exclusive;

    public CronJobConfig(final Config c) {
        this.name = c.getString("name");
        this.schedule = new CronBit(c.getString("schedule"));
        this.command = c.getString("command");
        this.timeoutms = c.getDuration("timeout", TimeUnit.MILLISECONDS);
        this.exclusive = c.getBoolean("exclusive");
    }

    @Override
    public String toString() {
        return "CronJobConfig [name=" + name + ", schedule=" + schedule + ", command=" + command + ", timeout=" + timeoutms + ", exclusive=" + exclusive + "]";
    }
}