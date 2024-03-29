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
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
@Slf4j
public class TeamRegistrationView {
    private long id;

    private long eventId;
    private String teamName;
    private String carQualifier;
    private String likedCarNumbers;
    private String assignedCarNumber;
    private Long iracingId;
    private String logoUrl;
    private PersonView createdBy;
    OffsetDateTime created;
    BalancedCarView car;
    private CarClassView carClass;
    private boolean wildcard;
    WorkflowStateInfoView workflowState;
    private List<PersonView> teamMembers;
    private String teamPermitTime;

    public String getColorStyleString() {
        if(workflowState != null) {
            return workflowState.getColorStyleString();
        }
        return "";
    }

    public String getFullTeamName() {
        return teamName + (StringUtils.isEmpty(carQualifier) ? "" : (" " + carQualifier));
    }

    public String getFullTeamNameWithNumber() {
        if(StringUtils.isEmpty(assignedCarNumber)) {
            return getFullTeamName();
        }
        return "#" + assignedCarNumber + " " + getFullTeamName();
    }

    public long getDriverCount() {
        return teamMembers.stream()
                .filter(member -> OrgaRoleType.valueOf(member.getRole()) == OrgaRoleType.DRIVER)
                .count();
    }

    public long getDriversNotInLeagueCount() {
        return teamMembers.stream()
                .filter(member -> OrgaRoleType.valueOf(member.getRole()) == OrgaRoleType.DRIVER)
                .filter(member -> !member.getLeagueMember())
                .count();
    }

    public long getDriversNotRegisteredCount() {
        return teamMembers.stream()
                .filter(member -> OrgaRoleType.valueOf(member.getRole()) == OrgaRoleType.DRIVER)
                .filter(member -> !member.getRegistered())
                .count();
    }

    public List<PersonView> getPotentialTeamOwners() {
        return teamMembers.stream()
                .filter(p -> p.getRegistered() && p.getIracingTeamChecked())
                .collect(Collectors.toList());
    }

    public List<CarView> getCarsOfClass() {
        if(carClass == null) {
            log.error("Null car class on registration id {} {}", id, teamName);
            return List.of();
        }
        return carClass.getCars();
    }

    public static TeamRegistrationView fromEntity(TeamRegistration registration) {
        return TeamRegistrationView.builder()
                .id(registration.getId())
                .eventId(registration.getEventId())
                .teamName(registration.getTeamName())
                .carQualifier(registration.getCarQualifier())
                .likedCarNumbers(registration.getLikedCarNumbers())
                .assignedCarNumber(registration.getAssignedCarNumber())
                .iracingId(registration.getIracingId())
                .logoUrl(registration.getLogoUrl())
                .createdBy(PersonView.fromEntity(registration.getCreatedBy()))
                .created(registration.getCreated())
                .car(BalancedCarView.fromEntity(registration.getEventId(), registration.getCar()))
                .wildcard(registration.isWildcard())
                .teamMembers(PersonView.fromEntityList(registration.getTeamMembers()))
                .workflowState(WorkflowStateInfoView.fromEntity(registration.getWorkflowState()))
                .build();
    }
}
