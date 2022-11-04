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
import de.bausdorf.simracing.racecontrol.util.MapTools;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateEventView {
    private long eventId;
    private String title;
    private String logoUrl;
    private String discordInvite;
    private long discordGuildId;
    private long discordPresetChannelId;
    private long discordSpacerCategoryId;
    private String description;
    private long irLeagueID;
    private String irLeagueName;
    private long irSeasonId;
    private String irSeasonName;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationOpens;
    private String registrationOpensTZ;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationCloses;
    private String registrationClosesTZ;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate endDate;
    private Boolean active;
    private long maxTeamDrivers;
    private long minTeamDrivers;
    private List<CarClassView> carClassPreset = new ArrayList<>();
    private List<PersonView> organizingStaff = new ArrayList<>();

    public EventSeries toEntity(@Nullable EventSeries eventSeries) {
        if (eventSeries == null) {
            eventSeries = new EventSeries();
        }
        eventSeries.setId(eventId);
        eventSeries.setTitle(MapTools.mapValue(title, eventSeries.getTitle(), String.class));
        eventSeries.setLogoUrl(MapTools.mapValue(logoUrl, eventSeries.getLogoUrl(), String.class));
        eventSeries.setDiscordInvite(MapTools.mapValue(discordInvite, eventSeries.getDiscordInvite(), String.class));
        eventSeries.setDiscordGuildId(MapTools.mapValue(discordGuildId, eventSeries.getDiscordGuildId(), Long.class));
        eventSeries.setDiscordPresetChannelId(MapTools.mapValue(discordPresetChannelId, eventSeries.getDiscordPresetChannelId(), Long.class));
        eventSeries.setDiscordSpacerCategoryId(MapTools.mapValue(discordSpacerCategoryId, eventSeries.getDiscordSpacerCategoryId(), Long.class));
        eventSeries.setIRLeagueName(MapTools.mapValue(irLeagueName, eventSeries.getIRLeagueName(), String.class));
        eventSeries.setIRLeagueID(MapTools.mapValue(irLeagueID, eventSeries.getIRLeagueID(), Long.class));
        eventSeries.setIrSeasonId(MapTools.mapValue(irSeasonId, eventSeries.getIrSeasonId(), Long.class));
        eventSeries.setIrSeasonName(MapTools.mapValue(irSeasonName, eventSeries.getIrSeasonName(), String.class));
        eventSeries.setDescription(MapTools.mapValue(description, eventSeries.getDescription(), String.class));
        eventSeries.setRegistrationOpens(registrationOpens == null ? eventSeries.getRegistrationOpens()
                : OffsetDateTime.of(registrationOpens, ZoneOffset.of(registrationOpensTZ)));
        eventSeries.setRegistrationCloses(registrationCloses == null ? eventSeries.getRegistrationCloses()
                : OffsetDateTime.of(registrationCloses, ZoneOffset.of(registrationClosesTZ)));
        eventSeries.setStartDate(MapTools.mapValue(startDate, eventSeries.getStartDate(), LocalDate.class));
        eventSeries.setEndDate(MapTools.mapValue(endDate, eventSeries.getEndDate(), LocalDate.class));
        eventSeries.setActive(MapTools.mapValue(active, eventSeries.isActive(), Boolean.class));
        eventSeries.setMaxTeamDrivers(MapTools.mapValue(maxTeamDrivers, eventSeries.getMaxTeamDrivers(), Long.class));
        eventSeries.setMinTeamDrivers(MapTools.mapValue(minTeamDrivers, eventSeries.getMinTeamDrivers(), Long.class));
        return eventSeries;
    }

    public static CreateEventView fromEntity(EventSeries eventSeries) {
        return CreateEventView.builder()
                .eventId(eventSeries.getId())
                .title(eventSeries.getTitle())
                .logoUrl(eventSeries.getLogoUrl())
                .discordInvite(eventSeries.getDiscordInvite())
                .discordGuildId(eventSeries.getDiscordGuildId())
                .discordPresetChannelId(eventSeries.getDiscordPresetChannelId())
                .discordSpacerCategoryId(eventSeries.getDiscordSpacerCategoryId())
                .irLeagueID(eventSeries.getIRLeagueID())
                .irLeagueName(eventSeries.getIRLeagueName())
                .irSeasonId(eventSeries.getIrSeasonId())
                .irSeasonName(eventSeries.getIrSeasonName())
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
                .organizingStaff(PersonView.fromEntityList(
                        eventSeries.getStaff().stream()
                                .filter(p -> p.getRole().isRacecontrol())
                                .collect(Collectors.toList()))
                )
                .build();
    }

    public static CreateEventView createEmpty() {
        return CreateEventView.builder()
                .registrationOpens(LocalDateTime.now())
                .registrationOpensTZ(ZonedDateTime.now().getOffset().toString())
                .registrationClosesTZ(ZonedDateTime.now().getOffset().toString())
                .startDate(LocalDate.now())
                .active(true)
                .build();
    }
}
