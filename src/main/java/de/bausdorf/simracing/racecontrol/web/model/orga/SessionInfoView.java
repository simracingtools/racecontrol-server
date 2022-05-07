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

import de.bausdorf.simracing.racecontrol.orga.model.TrackSession;
import de.bausdorf.simracing.racecontrol.web.model.TrackConfigurationView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class SessionInfoView {
    private long id;

    private long eventId;
    private String title;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm ZZ")
    private OffsetDateTime datetime;
    private String zoneOffset;

    private TrackConfigurationView trackConfigView;
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

    public String getTrackConditionInfo() {
        return (Boolean.TRUE.equals(generatedWeather) ? "Dynamic weather" : getFixedWeatherInfo())
                + (Boolean.TRUE.equals(generatedSky) ? ", Dynamic sky" : (", " + getSkyString()))
                + ", Usage: " + trackUsagePercent + "%, "
                + (Boolean.TRUE.equals(trackStateCarryOver) ? "Keep marbles" : "Clear marbles");
    }

    private String getFixedWeatherInfo() {
        return "T " + temperature + "°C, H " + humidity + "%, W: " + windSpeed + "kph " + windDirection;
    }

    private String getSkyString() {
        return "Sky i:" + skyVarInitial + "% c:" + skyVarContinued + "%";
    }

    public static SessionInfoView fromEntity(TrackSession session) {
        return SessionInfoView.builder()
                .id(session.getId())
                .eventId(session.getEventId())
                .title(session.getTitle())
                .datetime(session.getDateTime())
                .zoneOffset(session.getDateTime().getOffset().getId())
                .irSessionId(session.getIrSessionId())
                .simulatedTimeOfDay(session.getSimulatedTimeOfDay())
                .trackUsagePercent(session.getTrackUsagePercent())
                .trackStateCarryOver(session.isTrackStateCarryOver())
                .generatedWeather(session.isGeneratedWeather())
                .temperature(session.getTemperature())
                .humidity(session.getHumidity())
                .windSpeed(session.getWindSpeed())
                .windDirection(session.getWindDirection().getCode())
                .generatedSky(session.isGeneratedSky())
                .sky(session.getSky() != null ? session.getSky().getName() : null)
                .sessionParts(TrackSubsessionView.fromEntityList(session.getSessionParts()))
                .skyVarInitial(session.getSkyVarInitial())
                .skyVarContinued(session.getSkyVarContinued())
                .trackConfigView(TrackConfigurationView.fromId(session.getTrackConfigId()))
                .build();
    }

    public static List<SessionInfoView> fromEntityList(List<TrackSession> trackSessions) {
        return trackSessions.stream()
                .sorted(Comparator.comparing(TrackSession::getDateTime))
                .map(SessionInfoView::fromEntity)
                .collect(Collectors.toList());
    }
}
