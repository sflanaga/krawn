package org.krawn;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

public class Test {
    public static void main(String[] args) {
        
        long x = 1L >>> 32L;
        System.out.printf("%x\n", Long.MAX_VALUE);
        
        
        
        long sum = 0;
        long start = System.currentTimeMillis();
        for(int i=0; i<10000000; i++) {
            MutableDateTime dt = new MutableDateTime(start+i, DateTimeZone.UTC);
            sum += dt.getSecondOfMinute();
            sum += dt.getMinuteOfHour();
            sum += dt.getHourOfDay();
            sum += dt.getDayOfMonth();
            sum += dt.getDayOfWeek();
        }
        long end = System.currentTimeMillis();
        System.out.printf("runtime %d\n", (end-start));
    }
}
