# krawn
Java replacement for cron.

```
cron : {
	reaperPollTime = 1s
	configPollTime = 500ms
	jobs: [
		{ 
			name: simple_windows_example      # name of job - must be unique for exclusive tracking
			schedule="5 * * * * *"            # cron schedule spec with seconds added at front and day of week is 1 to 7
			command="dir /s c:\\"             # shell command to run: either on the end of cmd.exe /c or bash -c
			timeout=5s                        # how long before the krawn will reap/kill a process running too long
			exclusive=true                    # no LOCKING needed - will only run 1 instances of this command at a time if true
		}
		{ 
			name: simple_unix_example
			schedule="45 * * * * *"
			command="wc krawn.conf"
			timeout=5s
			exclusive=true
		}
	]
}
```

This is a relatively small program intended to replace cron.  If for some reason you do not have cron access but need to run scheduled scripts then this might be the program for you.

Running example:

`nohup java -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=3 -XX:GCLogFileSize=16m -Xloggc:log/gc.log -verbose:gc -XX:+PrintGCDateStamps -Xmx128m -jar krawn.jar > it 2>&1 &`


Krawn will capture std error and std out to the logback file for you so you don't necessarily need to pipe output to a separate log file.

Krawn supports "second" level timing.  The first entry in the schedule spec is for seconds, so you have a job that runs every 5 seconds if you need.

The schedule spec works almost like old unix cron (not vixie cron as is standard on linux now):

```
 +--------------- second (0 - 59)
 | +------------- minute (0 - 59)
 | | +------------- hour (0 - 23)
 | | | +------------- day of month (1 - 31)
 | | | | +------------- month (1 - 12)
 | | | | | +------------- day of week (1- 7 (Sunday to Saturday;
 | | | | | |                                  (day of week is different from unix cron)
 | | | | | | 
 | | | | | | 
 * * * * * *
```


