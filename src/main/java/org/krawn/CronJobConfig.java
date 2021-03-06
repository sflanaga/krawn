package org.krawn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTimeZone;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class CronJobConfig {
    final public boolean disable;
    final public String name;
    final public CronBit schedule;
    final public String command;
    final public long timeoutms;
    final public boolean exclusive;
    final public Path workingDir;
    final public DateTimeZone tz;
    final Map<String, String> env;
    final public boolean mapErrorToERROR;

    public CronJobConfig(final Config c) {
        if ( c.hasPath("disable") )
            disable = c.getBoolean("disable");
        else
            disable = false;

        this.name = c.getString("name");
        this.schedule = new CronBit(c.getString("schedule"));
        this.command = c.getString("command");
        this.timeoutms = c.getDuration("timeout", TimeUnit.MILLISECONDS);
        this.exclusive = c.getBoolean("exclusive");
        this.workingDir = Paths.get(c.getString("workingDir"));
        if ( !Files.exists(this.workingDir) )
            throw new RuntimeException("working directory does not exist: " + this.workingDir);
        else if ( !Files.isDirectory(this.workingDir) )
            throw new RuntimeException("working directory is not a directory: " + this.workingDir);
        
        this.tz = DateTimeZone.forID(c.getString("timezone"));
        // TODO: tz support

        env = new HashMap();
        if (c.hasPath("env")) {
            Set<Map.Entry<String, ConfigValue>> s = c.getConfig("env").entrySet();
            for (Map.Entry<String, ConfigValue> e : s) {
                env.put(e.getKey(), c.getConfig("env").getString(e.getKey()));
            }
        }

        if (c.hasPath("mapErrorToERROR"))
            mapErrorToERROR = c.getBoolean("mapErrorToERROR");
        else
            mapErrorToERROR = true;
        
    }

    @Override
    public String toString() {
        return "CronJobConfig [name=" + name + ", schedule=" + schedule + ", command=" + command + ", timeout=" + timeoutms + ", exclusive=" + exclusive + "]";
    }
}