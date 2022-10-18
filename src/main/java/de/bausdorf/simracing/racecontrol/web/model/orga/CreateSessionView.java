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

import de.bausdorf.simracing.racecontrol.orga.api.SkyConditionType;
import de.bausdorf.simracing.racecontrol.orga.api.WindDirectionType;
import de.bausdorf.simracing.racecontrol.orga.model.TrackSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class CreateSessionView {
    private long id;

    private long eventId;
    private String title;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime datetime;
    private String zoneOffset;

    private long trackConfigId;
    private Long irSessionId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime simulatedTimeOfDay;
    private long trackUsagePercent;
    private Boolean trackStateCarryOver;

    private Boolean generatedWeather;
    private Long temperature;
    private Long humidity;
    private Long windSpeed;
    private String windDirection;

    private Boolean generatedSky;
    private String sky;
    private Long skyVarInitial;
    private Long skyVarContinued;

    private List<TrackSubsessionView> sessionParts;

    public String getOverallDuration() {
        final Duration[] overallDuration = {Duration.ZERO};
        sessionParts.forEach(d -> overallDuration[0] = overallDuration[0].plus(d.getDuration()));
        return String.format("%02d:%02d", overallDuration[0].toHours(), overallDuration[0].toMinutesPart());
    }

    public TrackSession toEntity(TrackSession s) {
        if(s == null) {
            s = new TrackSession();
        }
        s.setId(id == 0L ? s.getId() : id);
        s.setEventId(eventId == 0L ? s.getEventId() : eventId);
        s.setTitle(title == null ? s.getTitle() : title);
        s.setDateTime(datetime == null ? s.getDateTime() : OffsetDateTime.of(datetime, ZoneOffset.of(zoneOffset)));
        s.setTrackConfigId(trackConfigId == 0L ? s.getTrackConfigId() : trackConfigId);
        s.setIrSessionId(irSessionId == null ? s.getIrSessionId() : irSessionId);
        s.setSimulatedTimeOfDay(simulatedTimeOfDay == null ? s.getSimulatedTimeOfDay() : simulatedTimeOfDay);
        s.setTrackUsagePercent(trackUsagePercent == 0L ? s.getTrackUsagePercent() : trackUsagePercent);
        s.setTrackStateCarryOver(trackStateCarryOver == null ? s.isTrackStateCarryOver() : trackStateCarryOver);
        s.setGeneratedWeather(generatedWeather == null ? s.isGeneratedWeather() : generatedWeather);
        s.setTemperature(temperature == null ? s.getTemperature() : temperature);
        s.setHumidity(humidity == null ? s.getHumidity() : humidity);
        s.setWindSpeed(windSpeed == null ? s.getWindSpeed() : windSpeed);
        s.setWindDirection(windDirection == null || windDirection.isEmpty() ? s.getWindDirection() : WindDirectionType.ofCode(windDirection));
        s.setGeneratedSky(generatedSky == null ? s.isGeneratedSky() : generatedSky);
        s.setSky(sky == null || sky.isEmpty() ? s.getSky() : SkyConditionType.ofName(sky));
        s.setSkyVarInitial(skyVarInitial == null ? s.getSkyVarInitial() : skyVarInitial);
        s.setSkyVarContinued(skyVarContinued == null ? s.getSkyVarContinued() : skyVarContinued);

        return s;
    }

    public static CreateSessionView fromEntity(TrackSession session) {
        return CreateSessionView.builder()
                .id(session.getId())
                .eventId(session.getEventId())
                .title(session.getTitle())
                .datetime(session.getDateTime().toLocalDateTime())
                .zoneOffset(session.getDateTime().getOffset().getId())
                .trackConfigId(session.getTrackConfigId())
                .irSessionId(session.getIrSessionId())
                .simulatedTimeOfDay(session.getSimulatedTimeOfDay())
                .trackUsagePercent(session.getTrackUsagePercent())
                .trackStateCarryOver(session.isTrackStateCarryOver())
                .generatedWeather(session.isGeneratedWeather())
                .temperature(session.getTemperature())
                .humidity(session.getHumidity())
                .windSpeed(session.getWindSpeed())
                .windDirection(session.getWindDirection() != null ? session.getWindDirection().getCode() : null)
                .generatedSky(session.isGeneratedSky())
                .sky(session.getSky() != null ? session.getSky().getName() : null)
                .sessionParts(TrackSubsessionView.fromEntityList(session.getSessionParts()))
                .skyVarInitial(session.getSkyVarInitial())
                .skyVarContinued(session.getSkyVarContinued())
                .build();
    }

    public static List<CreateSessionView> fromEntityList(List<TrackSession> trackSessions) {
        return trackSessions.stream().map(CreateSessionView::fromEntity).collect(Collectors.toList());
    }
}
