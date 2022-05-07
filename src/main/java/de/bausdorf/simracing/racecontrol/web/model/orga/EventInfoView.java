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

import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.web.model.TrackView;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EventInfoView {
    private long eventId;
    private String title;
    private String logoUrl;
    private String discordInvite;
    private String description;
    private long irLeagueID;
    private String irLeagueName;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationOpens;
    private String registrationOpensTZ;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationCloses;
    private boolean active;
    private long maxTeamDrivers;
    private long minTeamDrivers;
    private String registrationClosesTZ;
    private List<CarClassView> carClassPreset = new ArrayList<>();
    private List<PersonView> organizingStaff = new ArrayList<>();
    private List<AvailableSlotsView> availableSlots = new ArrayList<>();
    private List<TeamRegistrationView> userRegistrations = new ArrayList<>();
    private List<SessionInfoView> trackSessions = new ArrayList<>();

    public boolean isRegistrationOpen() {
        return LocalDateTime.now().isAfter(registrationOpens) && LocalDateTime.now().isBefore(registrationCloses);
    }

    public boolean isRegistrationClosed() {
        return LocalDateTime.now().isAfter(registrationCloses);
    }

    public static EventInfoView fromEntity(EventSeries eventSeries) {
        return EventInfoView.builder()
                .eventId(eventSeries.getId())
                .title(eventSeries.getTitle())
                .logoUrl(eventSeries.getLogoUrl())
                .discordInvite(eventSeries.getDiscordInvite())
                .irLeagueID(eventSeries.getIRLeagueID())
                .irLeagueName(eventSeries.getIRLeagueName())
                .description(eventSeries.getDescription())
                .registrationOpens(eventSeries.getRegistrationOpens().toLocalDateTime())
                .registrationOpensTZ(eventSeries.getRegistrationOpens().getOffset().toString())
                .registrationCloses(eventSeries.getRegistrationCloses().toLocalDateTime())
                .registrationClosesTZ(eventSeries.getRegistrationCloses().getOffset().toString())
                .startDate(eventSeries.getStartDate())
                .endDate(eventSeries.getEndDate())
                .active(eventSeries.isActive())
                .maxTeamDrivers(eventSeries.getMaxTeamDrivers())
                .minTeamDrivers(eventSeries.getMinTeamDrivers())
                .carClassPreset(CarClassView.fromEntityList(eventSeries.getCarClassPreset()))
                .organizingStaff(PersonView.fromEntityList(eventSeries.getStaff()))
                .trackSessions(SessionInfoView.fromEntityList(eventSeries.getTrackSessions()))
                .build();
    }

    public static List<EventInfoView> fromEntityList(List<EventSeries> eventSeries) {
        return eventSeries.stream()
                .map(EventInfoView::fromEntity)
                .collect(Collectors.toList());
    }

    public static EventInfoView createEmpty() {
        return EventInfoView.builder()
                .registrationOpens(LocalDateTime.now())
                .startDate(LocalDate.now())
                .active(true)
                .registrationOpensTZ(ZonedDateTime.now().getOffset().toString())
                .registrationClosesTZ(ZonedDateTime.now().getOffset().toString())
                .build();
    }
}
