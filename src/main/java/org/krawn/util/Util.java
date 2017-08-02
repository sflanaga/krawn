package org.krawn.util;

public class Util {

    private static final long second = 1000L;
    private static final long minute = second * 60L;
    private static final long hour = minute * 60L;
    private static final long day = hour * 24L;

    public static String longSpanToStringShort(long time, int precision) {
        long d = 0L, h = 0L, m = 0L, s = 0L, millisec = 0L;

        d = time / day;
        h = (time % day) / hour;
        m = (time % hour) / minute;
        s = (time % minute) / second;

        millisec = time % second;

        //int precision = 2;

        StringBuilder b = new StringBuilder(12);
        if (d != 0 && precision > 0) {
            b.append(d).append("d");
            precision--;
        }
        if (h != 0 && precision > 0) {
            b.append(h).append("h");
            precision--;
        }
        if (m != 0 && precision > 0) {
            b.append(m).append("m");
            precision--;
        }
        if (s != 0 && precision > 0) {
            b.append(s).append("s");
            precision--;
        }
        if (millisec != 0 && precision > 0 && time < 10000L) {
            b.append(millisec).append("ms");
            precision--;
        }
        return b.toString();
    }

}
