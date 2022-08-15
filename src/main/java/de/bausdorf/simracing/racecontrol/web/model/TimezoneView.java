package de.bausdorf.simracing.racecontrol.web.model;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2022 bausdorf engineering
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
