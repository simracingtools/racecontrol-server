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
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RaceSessionResultView {
    private long eventId;
    private String title;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm Z")
    private OffsetDateTime datetime;
    private String zoneOffset;

    private String trackName;
    private Long irSessionId;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime timeOfDay;

    private Boolean dynamicWeather;
    private String airTemp;
    private String windDirectionAndSpeed;
    private String relativeHumidity;
    private String skies;

    private Boolean dynamicSkies;

    private List<RaceTeamResultView> results;

    public static RaceSessionResultView fromEntitiy(TrackSession trackSession) {
        return RaceSessionResultView.builder()
                .eventId(trackSession.getEventId())
                .title(trackSession.getTitle())
                .datetime(trackSession.getDateTime())
                .zoneOffset(trackSession.getDateTime().getOffset().toString())
                .irSessionId(trackSession.getIrSessionId())
                .timeOfDay(trackSession.getSimulatedTimeOfDay())
                .dynamicWeather(trackSession.isGeneratedWeather())
                .airTemp("Air " + trackSession.getTemperature().toString() + " °F")
                .windDirectionAndSpeed("Wind " + trackSession.getWindDirection().getCode() + " @ " + trackSession.getWindSpeed() + " MPH")
                .relativeHumidity("Atmosphere: " + trackSession.getHumidity() + " % RH")
                .skies(trackSession.getSky().getName())
                .dynamicSkies(trackSession.isGeneratedSky())
                .build();
    }
}
