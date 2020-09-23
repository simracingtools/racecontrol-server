package de.bausdorf.simracing.racecontrol.util;

/*-
 * #%L
 * tt-cloud-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class TimeTools {

    public static final String HH_MM_SS = "HH:mm:ss";
    public static final String HH_MM_SS_XXX = "HH:mm:ssxxx";
    public static final ZoneId GMT = ZoneId.of("GMT");
    private static final List<String> TIME_PATTERNS = new ArrayList<>();

    static {
        TIME_PATTERNS.add(HH_MM_SS);
        TIME_PATTERNS.add("HH:mm:s");
        TIME_PATTERNS.add("HH:m:ss");
        TIME_PATTERNS.add("HH:m:s");
        TIME_PATTERNS.add("H:mm:ss");
        TIME_PATTERNS.add("H:mm:s");
        TIME_PATTERNS.add("H:m:ss");
        TIME_PATTERNS.add("H:m:s");
        TIME_PATTERNS.add("HH:mm:ss.S");
        TIME_PATTERNS.add("HH:mm:ss.SS");
        TIME_PATTERNS.add("HH:mm:ss.SSS");
        TIME_PATTERNS.add("HH:m:ss.S");
        TIME_PATTERNS.add("HH:m:ss.SS");
        TIME_PATTERNS.add("HH:m:ss.SSS");
        TIME_PATTERNS.add("H:mm:ss.S");
        TIME_PATTERNS.add("H:mm:ss.SS");
        TIME_PATTERNS.add("H:mm:ss.SSS");
        TIME_PATTERNS.add("H:m:ss.S");
        TIME_PATTERNS.add("H:m:ss.SS");
        TIME_PATTERNS.add("H:m:ss.SSS");
        TIME_PATTERNS.add("HH:mm:s.S");
        TIME_PATTERNS.add("HH:mm:s.SS");
        TIME_PATTERNS.add("HH:mm:s.SSS");
        TIME_PATTERNS.add("HH:m:s.S");
        TIME_PATTERNS.add("HH:m:s.SS");
        TIME_PATTERNS.add("HH:m:s.SSS");
        TIME_PATTERNS.add("H:mm:s.S");
        TIME_PATTERNS.add("H:mm:s.SS");
        TIME_PATTERNS.add("H:mm:s.SSS");
        TIME_PATTERNS.add("H:m:s.S");
        TIME_PATTERNS.add("H:m:s.SS");
        TIME_PATTERNS.add("H:m:s.SSS");
    }

    private TimeTools() {
        super();
    }

    public static Duration durationFromPattern(String timestring, String pattern) {
        LocalTime time = LocalTime.parse(timestring, DateTimeFormatter.ofPattern(pattern));
        return Duration.between(LocalTime.MIN, time);
    }

    public static String longDurationString(Duration duration) {
        long h = duration.toHours();
        long m = duration.toMinutes() - (h * 60);
        double S = ((double)duration.toMillis() / 1000) - (m * 60) - (h * 3600);
        if (S < 0) {
            S = 0.0;
        }
        return String.format(Locale.US,"%d:%02d:%06.3f", h, m, S);
    }

    public static String longDurationDeltaString(Duration d1, Duration d2) {
        if (d1 == null || d2 == null) {
            return "--:--:--.---";
        }
        Duration delta = d1.minus(d2);
        String prefix = delta.isNegative() ? "-" : "";
        return prefix + longDurationString(delta.isNegative() ? d2.minus(d1) : delta);
    }

    public static String shortDurationString(final Duration duration) {
        String prefix = "";
        Duration d = duration;
        if (duration.isNegative()) {
            d = duration.multipliedBy(-1);
            prefix = "-";
        }
        long h = d.toHours();
        long m = d.toMinutes() - (h * 60);
        long S = d.getSeconds() - (m * 60) - (h * 3600);
        return prefix + String.format("%d:%02d:%02d", h, m, S);
    }

    public static LocalTime timeFromString(String time) {
        if( time == null ) {
            return LocalTime.MIN;
        }
        String timestring = normalizeTimeString(time);
        for (String pattern : TIME_PATTERNS) {
            try {
                return LocalTime.parse(timestring,
                        DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                log.debug("Pattern {} not successful", pattern);
            }
        }
        log.warn("Timestring {} could not be parsed", time);
        return LocalTime.MIN;
    }

    public static Duration durationFromString(String timestring) {
        if (timestring == null) {
            return Duration.ZERO;
        }
        Duration duration = Duration.ZERO;
        String[] timeParts = timestring.split(":");
        if (timeParts.length == 3) {
            duration = duration.plusHours(Long.parseLong(timeParts[0]));
            duration = duration.plusMinutes(Long.parseLong(timeParts[1]));
            duration = duration.plusMillis((long)Double.parseDouble(timeParts[2]) * 1000);
        } else if (timeParts.length == 2) {
            if (timeParts[1].indexOf('.') > -1) {
                duration = duration.plusMinutes(Long.parseLong(timeParts[0]));
                duration = duration.plusMillis((long)Double.parseDouble(timeParts[1]) * 1000);
            } else {
                duration = duration.plusHours(Long.parseLong(timeParts[0]));
                duration = duration.plusMinutes(Long.parseLong(timeParts[1]));
            }
        }
        return duration;
    }

    public static String normalizeTimeString(String timestring) {
        String result = timestring;
        int colonPos = timestring.indexOf(':');
        if( colonPos == -1 ) {
            result = "00:00:" + timestring;
        } else if( timestring.indexOf(':', colonPos + 1) == -1 ) {
            result = "00:" + timestring;
        }

        if (timestring.contains(",")) {
            result = result.replace(",", ".");
        }
        return result;
    }

    public static String toShortZoneId(ZoneId zoneId) {
        String shortId = zoneId.getId(); //GMT+02:00
        shortId = shortId.replace("0", "");
        return shortId.replace(":", "");
    }

    public static ZonedDateTime zonedDateTimeFromString(String timeString) {
        try {
            return ZonedDateTime.parse(timeString);
        } catch (DateTimeParseException e) {
            log.warn("Parse local time {} using default timezone {}", timeString, TimeTools.GMT);
            return ZonedDateTime.of(LocalDateTime.parse(timeString), TimeTools.GMT);
        }
    }

    public static Duration getFromIracingDuration(Object iRacingDuration) {
        if (iRacingDuration == null) {
            return Duration.ZERO;
        }
        double durationDouble = (Double)iRacingDuration;
        if(durationDouble < 1.5) {
            // duration delivered as fraction of 86400 (seconds of day)
            durationDouble *= 86400;
        }
        // iRacing lap, repair and tow times are seconds as float
        return Duration.ofNanos((long)(durationDouble * 1000000000));
    }

    public static Duration getFromIracingSessionDuration(String sessionDurationWithSec) {
        // 43200.0000 sec
        String strippedPostfix = sessionDurationWithSec.replaceFirst(" sec", "");
        double durationSeconds = Double.parseDouble(strippedPostfix);
        return Duration.ofSeconds((long)durationSeconds);
    }
}
