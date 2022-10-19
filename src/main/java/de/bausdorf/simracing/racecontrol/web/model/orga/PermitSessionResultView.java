package de.bausdorf.simracing.racecontrol.web.model.orga;

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
public class PermitSessionResultView {
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

    private List<DriverPermitResultView> results;

    public static PermitSessionResultView fromEntitiy(TrackSession trackSession) {
        return PermitSessionResultView.builder()
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
