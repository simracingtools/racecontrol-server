package de.bausdorf.simracing.racecontrol.web.model.orga;

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

import de.bausdorf.simracing.racecontrol.orga.model.PermitSessionResult;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import lombok.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DriverPermitResultView {
    private long id;

    private long eventId;
    private long iracingId;
    private String driverName;
    private String carName;
    private long lapCount;
    private boolean lapCountOk;
    private OffsetDateTime bestLapAt;
    private String permitTime;
    private String slowestLapTime;
    private String fastestLapTime;
    private String lapTimeVariance;
    private boolean varianceOk;
    private String driverPermitTime;
    private String events;

    public boolean isPermitted() {
        return lapCountOk && varianceOk;
    }

    public static DriverPermitResultView fromEntity(PermitSessionResult result) {
        Duration variance = result.getSlowestLapTime().minus(result.getFastestLapTime());

        return DriverPermitResultView.builder()
                .id(result.getId())
                .eventId(result.getEventId())
                .driverName(result.getName())
                .carName(result.getCarName())
                .lapCount(result.getLapCount())
                .bestLapAt(result.getBestLapAt())
                .permitTime(TimeTools.lapDisplayTimeFromDuration(result.getAverageLapTime()))
                .slowestLapTime(TimeTools.lapDisplayTimeFromDuration(result.getSlowestLapTime()))
                .fastestLapTime(TimeTools.lapDisplayTimeFromDuration(result.getFastestLapTime()))
                .lapTimeVariance(TimeTools.lapDisplayTimeFromDuration(variance))
                .driverPermitTime(TimeTools.lapDisplayTimeFromDuration(result.getAverageLapTime()))
                .events(result.getEvents())
                .build();
    }

    public static List<DriverPermitResultView> fromEntityList(List<PermitSessionResult> results) {
        return results.stream()
                .map(DriverPermitResultView::fromEntity)
                .collect(Collectors.toList());
    }
}
