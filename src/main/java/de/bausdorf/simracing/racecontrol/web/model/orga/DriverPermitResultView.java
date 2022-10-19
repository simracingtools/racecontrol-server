package de.bausdorf.simracing.racecontrol.web.model.orga;

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
