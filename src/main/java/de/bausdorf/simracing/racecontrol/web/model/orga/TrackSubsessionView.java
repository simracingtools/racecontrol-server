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

import de.bausdorf.simracing.racecontrol.orga.api.SessionType;
import de.bausdorf.simracing.racecontrol.orga.model.TrackSubsession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class TrackSubsessionView {
    private long id;

    private long trackSessionId;
    private long eventId;
    private Long irSubsessionId;
    private String sessionType;
    private String sessionTypeName;
    private Duration duration;
    private String durationString;
    private long minutes;
    private long hours;

    public TrackSubsession toEntity(TrackSubsession e) {
        if(e == null) {
            e = new TrackSubsession();
        }
        e.setId(id != 0L ? id : e.getId());
        e.setTrackSessionId(trackSessionId != 0L ? trackSessionId : e.getTrackSessionId());
        e.setIrSubsessionId(irSubsessionId != null ? irSubsessionId : e.getIrSubsessionId());
        e.setSessionType(sessionType != null ? SessionType.valueOf(sessionType) : e.getSessionType());
        e.setDuration(Duration.ZERO.plus(hours, ChronoUnit.HOURS).plus(minutes, ChronoUnit.MINUTES));

        return e;
    }

    public static TrackSubsessionView fromEntity(TrackSubsession entity) {
        return TrackSubsessionView.builder()
                .id(entity.getId())
                .trackSessionId(entity.getTrackSessionId())
                .irSubsessionId(entity.getIrSubsessionId())
                .sessionType(entity.getSessionType().toString())
                .sessionTypeName(entity.getSessionType().getDisplayName())
                .durationString(String.format("%02d:%02d", entity.getDuration().toHours(), entity.getDuration().toMinutesPart()))
                .duration(entity.getDuration())
                .hours(entity.getDuration().toHours())
                .minutes(entity.getDuration().toMinutesPart())
                .build();
    }

    public static List<TrackSubsessionView> fromEntityList(List<TrackSubsession> entityList) {
        return entityList.stream()
                .map(TrackSubsessionView::fromEntity)
                .collect(Collectors.toList());
    }


}
