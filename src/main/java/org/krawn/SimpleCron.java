package org.krawn;
// $Id: $  


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SimpleTimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleCron {
    static Logger log = LoggerFactory.getLogger(SimpleCron.class);

    final String origCron;

    final HashSet<Integer> moy;
    final HashSet<Integer> doM;
    final HashSet<Integer> dow;
    final HashSet<Integer> seconds;
    final HashSet<Integer> minutes;
    final HashSet<Integer> hours;

    SimpleCron(String cronlike) {
        String timeParts[] = cronlike.split("\\s+");
        try {
            if (timeParts.length < 1)
                throw new RuntimeException("error creating cron for " + cronlike + " as it is missing one at least the seconds portion of the potential 4 time parts");

            seconds = parsePart(timeParts[0], 0, 59, "seconds");

            if (timeParts.length > 1)
                minutes = parsePart(timeParts[1], 0, 59, "minutes");
            else
                minutes = null;

            if (timeParts.length > 2)
                hours = parsePart(timeParts[2], 0, 23, "hours");
            else
                hours = null;

            if (timeParts.length > 3)
                dow = parsePart(timeParts[3], 0, 6, "daysofweek");
            else
                dow = null;
            
            if (timeParts.length > 4)
                doM = parsePart(timeParts[4], 0, 31, "daysofmonth");
            else
                doM = null;

            if (timeParts.length > 5)
                moy = parsePart(timeParts[5], 1, 12, "monthofyear");
            else
                moy = null;

        } catch (Exception e) {
            throw new RuntimeException("error creating cron for " + cronlike + " reason: " + e.getMessage(), e);
        }

        origCron = cronlike;
    }

    @Override
    public String toString() {
        final int maxLen = 15;
        return String.format("SimpCron [origCron=%s, seconds=%s, minutes=%s, hours=%s]", origCron, seconds != null ? toString(seconds, maxLen) : null,
                minutes != null ? toString(minutes, maxLen) : null, hours != null ? toString(hours, maxLen) : null);
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }

    protected HashSet<Integer> parsePart(String timeSpec, int lo_check, int hi_check, String msg) {
        if (timeSpec.equals("*")) {
            return null;
        }
        HashSet<Integer> tmp = new HashSet<>();
        try {
            String commaEntries[] = timeSpec.split(",");
            for (int i = 0; i < commaEntries.length; i++) {
                String rangeEntries[] = commaEntries[i].split("-");
                if (rangeEntries.length < 2) {
                    Integer rangeLo = Integer.decode(rangeEntries[0]);
                    if (rangeLo.intValue() > hi_check || rangeLo.intValue() < lo_check)
                        throw new RuntimeException(
                                "The timespec for " + msg + " has an entry outside the expect range of " + lo_check + " to " + hi_check + ", timespec:" + timeSpec);
                    tmp.add(rangeLo);
                } else if (rangeEntries.length == 2) {
                    Integer rangeLo = Integer.decode(rangeEntries[0]);
                    Integer rangeHi = Integer.decode(rangeEntries[1]);

                    if (rangeLo.intValue() > hi_check || rangeLo.intValue() < lo_check)
                        throw new RuntimeException(
                                "The timespec for " + msg + " has an entry outside the expect range of " + lo_check + " to " + hi_check + ", timespec:" + timeSpec);

                    if (rangeHi.intValue() > hi_check || rangeHi.intValue() < lo_check)
                        throw new RuntimeException(
                                "The timespec for " + msg + " has an entry outside the expect range of " + lo_check + " to " + hi_check + ", timespec:" + timeSpec);

                    if (rangeLo.intValue() >= rangeHi.intValue())
                        throw new RuntimeException("The timespec for " + msg + " has an entry in which the low entry is higher than the high entry");

                    populateSet(tmp, rangeLo.intValue(), rangeHi.intValue());
                } else
                    throw new RuntimeException("The timespec for " + msg + " has a range with only one entry, timespec:" + timeSpec);
            }
            return tmp;
        } catch (NumberFormatException ne) {
            throw new RuntimeException("The timespec for " + msg + " has a bad number in it, number:" + timeSpec, ne);
        }
    }

    private void populateSet(HashSet<Integer> tmp, int lo, int hi) {
        for (int i = lo; i < hi + 1; i++) {
            tmp.add(new Integer(i));
        }
    }

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    final static long msInSec = 1000L;
    final static long msInMin = 60 * msInSec;
    final static long msInHour = msInMin * 60L;
    final static long msInDay = msInHour * 24L;

    public long nextRun(long now) {
        MutableDateTime dt = new MutableDateTime(now, DateTimeZone.UTC);

        // printf("now: %1s  cron: %s\n", dt.toString(), origCron);

        if (hours != null)
            dt.setMinuteOfHour(0);

        if (minutes != null)
            dt.setSecondOfMinute(0);

        if (seconds != null)
            dt.setSecondOfMinute(0);
        
        if (dow != null)
            dt.setHourOfDay(0);

        if (doM != null)
            dt.setDayOfMonth(1);

        if (moy != null)
            dt.setMonthOfYear(1);

        long nextmoy = -1;
        long nextdoM = -1;
        long nextdow = -1;
        long nexthour = -1;
        long nextmin = -1;
        long nextsec = -1;
        long count = 0L;
        while (true) {
            long matchcount = 0;

            if (count > 10000)
                throw new RuntimeException("schedule look count high at " + count + " for " + origCron);

            count++;
            if (moy != null || nextmoy != -1) {
                // printf("comp hr: %1d\n", dt.getHourOfDay());
                if (!moy.contains(dt.getMonthOfYear())) {
                    dt.add(msInDay);
                    continue;
                } else {
                    nextmoy = dt.getDayOfMonth();
                    matchcount++;
                }
            } else
                matchcount++;

            if (doM != null || nextdoM != -1) {
                // printf("comp hr: %1d\n", dt.getHourOfDay());

                int lastday = -1;
                if ( doM.contains(0) )
                    lastday = dt.dayOfMonth().getMaximumValue();
                
                if (!doM.contains(dt.getDayOfMonth()) && lastday != dt.getDayOfMonth()) {
                    dt.add(msInDay);
                    continue;
                } else {
                    nextdoM = dt.getDayOfMonth();
                    matchcount++;
                }
            } else
                matchcount++;

            if (dow != null || nextdow != -1) {
                // printf("comp hr: %1d\n", dt.getHourOfDay());
                if (!dow.contains(dt.getDayOfWeek())) {
                    dt.add(msInDay);
                    continue;
                } else {
                    nextdow = dt.getDayOfWeek();
                    matchcount++;
                }
            } else
                matchcount++;

            if (hours != null || nexthour != -1) {
                // printf("comp hr: %1d\n", dt.getHourOfDay());
                if (!hours.contains(dt.getHourOfDay())) {
                    dt.add(msInHour);
                    continue;
                } else {
                    nexthour = dt.getHourOfDay();
                    matchcount++;
                }
            } else
                matchcount++;

            if (minutes != null || nextmin != -1) {
                if (!minutes.contains(dt.getMinuteOfHour())) {
                    dt.add(msInMin);
                    continue;
                } else {
                    nextmin = dt.getMinuteOfHour();
                    matchcount++;
                }
            } else
                matchcount++;

            if (seconds != null || nextsec != -1) {
                if (!seconds.contains(dt.getSecondOfMinute())) {
                    dt.add(msInSec);
                    continue;
                } else {
                    nextsec = dt.getSecondOfMinute();
                    matchcount++;
                }
            } else
                matchcount++;

            if (dt.getMillis() > now && matchcount == 6) {
                break;
            } else {
                nextmoy = -1;
                nextdow = -1;
                nexthour = -1;
                nextmin = -1;
                nextsec = -1;
                dt.add(msInSec);
            }

        }

        // printf("nh: %1d nm: %2d ns: %3d count: %4d nextnow: %5s\n", nexthour, nextmin, nextsec, count, dt.toString());
        // // printf("the: %s  count: %d\n", dt.toString(), count);
        long delta = dt.getMillis() - now;
        // // printf("future: hr: %s \n", Util.longSpanToStringShort(delta,3));
        return dt.getMillis();
    }

    static SimpleDateFormat dtf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    static {
        dtf.setTimeZone(SimpleTimeZone.getTimeZone("UTC"));
    }

    public static void testCase(String nowstr, String cron) throws ParseException {
        long now = dtf.parse(nowstr).getTime();

        SimpleCron sc = new SimpleCron(cron);
        sc.nextRun(now);
        // System.out.println(sc);
    }
    public static void testCase(long now, String cron) throws ParseException {
        SimpleCron sc = new SimpleCron(cron);
        sc.nextRun(now);
        // System.out.println(sc);
    }

    public static void main(String[] args) {
        try {
            long now = dtf.parse("2017/07/08-23:14:01").getTime();
//            for (long thenow = now; thenow <= now + 300 * 1000L; thenow += 5000L) {
//                testCase(thenow, "10-35,45-55 15");
//            }
//            
//            System.exit(1);
            
            testCase("2017/02/08-23:15:03", "10 15 4,12 * * *");
            testCase("2017/07/08-23:15:03", "2 15 * *");
            testCase("2017/07/08-23:15:03", "* * * *");

            testCase("2017/07/08-23:15:01", "*");
            testCase("2017/07/08-23:15:01", "* 15 23 4");
            testCase("2017/07/08-23:15:03", "* 15 23 *");
            testCase("2017/07/08-23:16:03", "* 15 23 *");

            SimpleCron sc = new SimpleCron("14 15 22 *");
            System.out.println(sc);
            // yyyy/MM/dd-HH:mm:ss

            for (long thenow = now; thenow <= now + 300 * 1000L; thenow += 5000L) {
                long it = sc.nextRun(thenow);
                System.out.println(it);
            }

            // long start = System.currentTimeMillis();
            // for (int i = 0; i < 100000; i++) {
            // it = sc.nextRun(now);
            // }
            // long end = System.currentTimeMillis();

            // printf("%d\n", (end-start));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
