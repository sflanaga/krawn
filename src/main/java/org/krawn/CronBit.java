package org.krawn;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.SimpleTimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronBit {
    static Logger log = LoggerFactory.getLogger(CronBit.class);

    final String origCron;

    static long starbits = Long.MAX_VALUE;

    final long moy;
    final long doM;
    final long dow;
    final long seconds;
    final long minutes;
    final long hours;

    public CronBit(String cronlike) {
        String timeParts[] = cronlike.split("\\s+");
        try {
            if (timeParts.length < 1)
                throw new RuntimeException("error creating cron for " + cronlike + " as it is missing one at least the seconds portion of the potential 4 time parts");

            seconds = parsePart(timeParts[0], 0, 59, 1L, "seconds");

            if (timeParts.length > 1)
                minutes = parsePart(timeParts[1], 0, 59, 1L, "minutes");
            else
                minutes = starbits;

            if (timeParts.length > 2)
                hours = parsePart(timeParts[2], 0, 23, 1L, "hours");
            else
                hours = starbits;

            if (timeParts.length > 3)
                dow = parsePart(timeParts[3], 1, 7, 1L, "daysofweek");
            else
                dow = starbits;

            if (timeParts.length > 4)
                doM = parsePart(timeParts[4], 0, 31, 1L, "daysofmonth");
            else
                doM = starbits;

            if (timeParts.length > 5)
                moy = parsePart(timeParts[5], 1, 12, 1L, "monthofyear");
            else
                moy = starbits;

        } catch (Exception e) {
            throw new RuntimeException("error creating cron for " + cronlike + " reason: " + e.getMessage(), e);
        }

        origCron = cronlike;
    }

    protected long parsePart(String timeSpec, int lo_check, int hi_check, long base, String msg) {
        long tmp = 0;
        if (timeSpec.equals("*")) {
            return starbits;
        }
        try {
            String commaEntries[] = timeSpec.split(",");
            for (int i = 0; i < commaEntries.length; i++) {
                String intervaledEntries[] = commaEntries[i].split("/");
                int interval = 1;
                if (intervaledEntries.length == 2) {
                    interval = Integer.decode(intervaledEntries[1]);
                }
                String rangeEntries[] = intervaledEntries[0].split("-");
                if (rangeEntries.length < 2) {
                    if ( rangeEntries[0].equals("*") ) {
                        tmp += populateSet(lo_check, hi_check, base, interval);
                    } else {
                        Integer rangeLo = Integer.decode(rangeEntries[0]);
                        if (rangeLo.intValue() > hi_check || rangeLo.intValue() < lo_check)
                            throw new RuntimeException(
                                    "The timespec for " + msg + " has an entry outside the expect range of " + lo_check + " to " + hi_check + ", timespec:" + timeSpec);
                        tmp |= base << rangeLo;
                    }
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

                    tmp |= populateSet(rangeLo.intValue(), rangeHi.intValue(), base, interval);
                } else
                    throw new RuntimeException("The timespec for " + msg + " has a range with only one entry, timespec:" + timeSpec);
            }
            return tmp;
        } catch (NumberFormatException ne) {
            throw new RuntimeException("The timespec for " + msg + " has a bad number in it, number:" + timeSpec, ne);
        }
    }

    @Override
    public String toString() {
        return String.format("CronBit [origCron=%s, moy=%x, doM=%x, dow=%x, seconds=%x, minutes=%x, hours=%x]", origCron, moy, doM, dow, seconds, minutes, hours);
    }

    private long populateSet(int lo, int hi, long base, int interval) {
        long tmp = 0L;
        for (int i = lo; i < hi + 1; i+=interval) {
            tmp |= base << i;
        }
        return tmp;
    }

    static public class BitCheck {
        final long sec;
        final long min;
        final long month;
        final long hour;
        final long dom;
        final long moy;

        @Override
        public String toString() {
            return String.format("BitCheck [sec=%x, min=%x, month=%x, hour=%x, dom=%x, dow=%x, dom_normal=%s, dom_last=%s]", sec, min, month, hour, dom, dow, dom_normal, dom_last);
        }

        final long dow;
        final long dom_normal;
        final long dom_last;

        public BitCheck(DateTime dt) {
            sec = 1L << dt.getSecondOfMinute();
            min = 1L << dt.getMinuteOfHour();
            month = 1L << dt.getMonthOfYear();
            hour = 1L << dt.getHourOfDay();
            dom = 1L << dt.getDayOfMonth();
            dom_normal = dt.getDayOfMonth();
            dow = 1L << dt.getDayOfWeek();
            dom_last = dt.dayOfMonth().getMaximumValue();
            moy = 1L << dt.getMonthOfYear();
        }
    }

    public boolean hit(BitCheck now) {
        if (doM == 1) {
            // special check for last day of month which is zero
            if (now.dom_normal != now.dom_last)
                return false;
            // take out nromal dom bit check as last day worked for us here
            else if ((now.sec & seconds) > 0 && (now.min & minutes) > 0 && (now.hour & hours) > 0 && (now.dow & dow) > 0 && (now.moy & moy) > 0)
                return true;
            else
                return false;

            // else virtually true if other check out too
        } else if ((now.sec & seconds) > 0 && (now.min & minutes) > 0 && (now.hour & hours) > 0 && (now.dom & doM) > 0 && (now.dow & dow) > 0 && (now.moy & moy) > 0)
            return true;
        else
            return false;
    }

    static SimpleDateFormat dtf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    static {
        dtf.setTimeZone(SimpleTimeZone.getTimeZone("UTC"));
    }

    public static void testCase(String nowstr, String cron) throws ParseException {
        long now = dtf.parse(nowstr).getTime();
        BitCheck bc = new BitCheck(new DateTime(now, DateTimeZone.UTC));
        CronBit sc = new CronBit(cron);
        System.out.printf("%s\n%s\nresult = %s\n\n", bc, sc, sc.hit(bc));
    }

    public static void testCase(long now, String cron) throws ParseException {
        SimpleCron sc = new SimpleCron(cron);
        sc.nextRun(now);
        // System.out.println(sc);
    }

    public static void main(String[] args) {
        try {
            // testCase("2017/07/08-23:15:03", "* * * *");
            CronBit sc = new CronBit("5 10 4,5,6 *");
            long start = System.currentTimeMillis();
            long it =10000000;
            for( long i=0; i<it; i++ ) {
                BitCheck bc = new BitCheck(new DateTime(System.currentTimeMillis()+i, DateTimeZone.UTC));
                sc.hit(bc);
            }
            long end = System.currentTimeMillis();
            System.out.printf("%d / sec\n", (it*1000)/(end-start));
            testCase("2017/08/31-23:15:02", "2 15 23 * * 8");
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}