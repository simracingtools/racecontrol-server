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

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RaceTeamResultView {
    private long eventId;
    private long iracingId;
    private Long position;
    private Long classPosition;
    private Long startPosition;
    private String carLogoUrl;
    private String carName;
    private String carClass;
    private String carNumber;
    private String teamName;
    private String teamRating;
    private String ratingCssClass;
    private long leadLaps;
    private String intervall;
    private String averageLapTime;
    private String fastestLapTime;
    private Long bestLap;
    private Long lapCompleted;
    private String state;
    private boolean rated;
    private String notRatedReason;
    private String classColor;
    private List<RaceDriverResultView> driverResults;
}
