package com.amresh.cortex;

import java.util.Calendar;

/** Works out how long a life has lasted, from a birth moment to a chosen moment. */
public class LifeCounter {

    public static class Result {
        public long totalDays;
        public long totalHours;
        public int years;
        public int months;
        public int days;
        public int hours;
        public int minutes;
    }

    private static final long MINUTE = 60L * 1000;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    /** Returns null when the birth moment is not set or is after {@code asOfMillis}. */
    public static Result compute(long birthMillis, long asOfMillis) {
        if (birthMillis <= 0 || asOfMillis < birthMillis) return null;

        Result r = new Result();
        long diff = asOfMillis - birthMillis;
        r.totalDays = diff / DAY;
        r.totalHours = diff / HOUR;

        Calendar birth = Calendar.getInstance();
        birth.setTimeInMillis(birthMillis);
        Calendar asOf = Calendar.getInstance();
        asOf.setTimeInMillis(asOfMillis);

        // Whole years.
        int years = asOf.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        Calendar t = (Calendar) birth.clone();
        t.add(Calendar.YEAR, years);
        if (t.after(asOf)) {
            years--;
            t = (Calendar) birth.clone();
            t.add(Calendar.YEAR, years);
        }
        r.years = years;

        // Whole months after those years.
        int months = 0;
        while (true) {
            Calendar next = (Calendar) t.clone();
            next.add(Calendar.MONTH, months + 1);
            if (next.after(asOf)) break;
            months++;
        }
        t.add(Calendar.MONTH, months);
        r.months = months;

        // The rest in days, hours and minutes.
        long rest = asOfMillis - t.getTimeInMillis();
        r.days = (int) (rest / DAY);
        rest -= r.days * DAY;
        r.hours = (int) (rest / HOUR);
        rest -= r.hours * HOUR;
        r.minutes = (int) (rest / MINUTE);
        return r;
    }
}
