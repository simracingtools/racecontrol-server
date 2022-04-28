package de.bausdorf.simracing.racecontrol.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.Locale;

@Data
@AllArgsConstructor
@Builder
public class TimezoneView {
    String id;
    String fullName;
    String shortName;
    ZoneOffset utcOffset;
    String utcOffsetString;

    public static TimezoneView fromZoneId(@NonNull ZoneId zoneId) {
        ZoneOffset zoneOffset = zoneId.getRules().getOffset(LocalDateTime.now());
        return TimezoneView.builder()
                .id(zoneId.getId())
                .fullName(zoneId.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .shortName(zoneId.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .utcOffset(zoneOffset)
                .utcOffsetString(zoneOffset.getId().replace(":", ""))
                .build();
    }
}
