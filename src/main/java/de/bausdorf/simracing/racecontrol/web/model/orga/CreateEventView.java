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

import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
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
        eventSeries.setTitle(title == null ? eventSeries.getTitle() : title);
        eventSeries.setLogoUrl(logoUrl == null ? eventSeries.getLogoUrl() : logoUrl);
        eventSeries.setDiscordInvite(discordInvite == null ? eventSeries.getDiscordInvite() : discordInvite);
        eventSeries.setDiscordGuildId(discordGuildId == 0 ? eventSeries.getDiscordGuildId() : discordGuildId);
        eventSeries.setDiscordPresetChannelId(discordPresetChannelId == 0 ? eventSeries.getDiscordPresetChannelId() : discordPresetChannelId);
        eventSeries.setDiscordSpacerCategoryId(discordSpacerCategoryId == 0 ? eventSeries.getDiscordSpacerCategoryId() : discordSpacerCategoryId);
        eventSeries.setIRLeagueName(irLeagueName == null ? eventSeries.getIRLeagueName() : irLeagueName);
        eventSeries.setIRLeagueID(irLeagueID == 0 ? eventSeries.getIRLeagueID() : irLeagueID);
        eventSeries.setDescription(description == null ? eventSeries.getDescription() : description);
        eventSeries.setRegistrationOpens(registrationOpens == null ? eventSeries.getRegistrationOpens()
                : OffsetDateTime.of(registrationOpens, ZoneOffset.of(registrationOpensTZ)));
        eventSeries.setRegistrationCloses(registrationCloses == null ? eventSeries.getRegistrationCloses()
                : OffsetDateTime.of(registrationCloses, ZoneOffset.of(registrationClosesTZ)));
        eventSeries.setStartDate(startDate == null ? eventSeries.getStartDate() : startDate);
        eventSeries.setEndDate(endDate == null ? eventSeries.getEndDate() : endDate);
        eventSeries.setActive(active == null ? eventSeries.isActive() : active);
        eventSeries.setMaxTeamDrivers(maxTeamDrivers == 0 ? eventSeries.getMaxTeamDrivers() : maxTeamDrivers);
        eventSeries.setMinTeamDrivers(minTeamDrivers == 0 ? eventSeries.getMinTeamDrivers() : minTeamDrivers);
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
