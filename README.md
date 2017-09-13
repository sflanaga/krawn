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
			workingDir=./                     # working direct or cwd of the process run
			env {                             # optionally env variables can be set per process spawned
				testvar="xyz"
			}
			timezone="America/Chicago"
		}
		{ 
			name: simple_unix_example
			schedule="45 * * * * *"
			command="wc krawn.conf"
			timeout=5s
			exclusive=true
			timezone="UTC"
		}
	]
}
```

This is a relatively small program intended to replace cron.  If for some reason you do not have cron access but need to run scheduled scripts then this might be the program for you.

Running example:

`nohup java -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=3 -XX:GCLogFileSize=16m -Xloggc:log/gc.log -verbose:gc -XX:+PrintGCDateStamps -Xmx128m -jar krawn.jar > it 2>&1 &`


Krawn will capture std error and std out to the logback file for you so you don't necessarily need to pipe output to a separate log file.

Krawn supports "second" level timing.  The first entry in the schedule spec is for seconds, so you can have a job that runs every 5 seconds if you need.

Each element of the schedule works much like vixie cron.

`5/*` =  run every 5 second

`5 1,2,3` = run on 1, 2 or 3rd minute at 5 seconds into the minute

`0 5-23` = run at minute 5 thru 23

`*` = run every second
 
`0 0 12` = run at noon

The schedule spec works most like vixie cron format, but any later schedule elements missing are assumed to be *.  

ORDER of the schedule spec is a bit different.  This order is thought to have most common things to be set first.

All time is GMT/UTC based - NOT local.  

```
 +--------------- second (0 - 59)
 | +------------- minute (0 - 59)
 | | +------------- hour (0 - 23)
 | | | +------------- day of week (1- 7 (Sunday to Saturday;
 | | | | +------------- day of month (1 - 31)
 | | | | | +------------- month (1 - 12)
 | | | | | |                                  
 | | | | | | 
 | | | | | | 
 * * * * * *
```

More examples:

Run a job once a day just 5 seconds after midnight:

`5 0 0`

Run a job every 5 seconds:

`*/5 *`

Run a job every other hour on sunday at 5 mins and 15 seconds into that hour:

`15 5 */2  1`

Run a job at 30 seconds, 20 minutes after midnight on the last day of the month.  Note the use of 0 for the last day of the month vs. -1 for some systems.

`30 20 0 * 0 *`



