package org.krawn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class CronJobConfig {
    final public String name;
    final public CronBit schedule;
    final public String command;
    final public long timeoutms;
    final public boolean exclusive;
    final Map<String, String> env;

    public CronJobConfig(final Config c) {
        this.name = c.getString("name");
        this.schedule = new CronBit(c.getString("schedule"));
        this.command = c.getString("command");
        this.timeoutms = c.getDuration("timeout", TimeUnit.MILLISECONDS);
        this.exclusive = c.getBoolean("exclusive");

        env = new HashMap();
        if (c.hasPath("env")) {
            Set<Map.Entry<String, ConfigValue>> s = c.getConfig("env").entrySet();
            for (Map.Entry<String, ConfigValue> e : s) {
                env.put(e.getKey(), c.getConfig("env").getString(e.getKey()));
            }
        }

    }

    @Override
    public String toString() {
        return "CronJobConfig [name=" + name + ", schedule=" + schedule + ", command=" + command + ", timeout=" + timeoutms + ", exclusive=" + exclusive + "]";
    }
}