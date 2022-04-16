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
import de.bausdorf.simracing.racecontrol.orga.model.WorkflowState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class WorkflowStateInfoView {
    private long id;

    private String workflowName;
    private String stateKey;
    private String description;
    private String color;
    private String textColor;
    private Boolean initialState;
    private Boolean inActive;
    private List<String> dutyRoles;
    private List<WorkflowState> followUps;
    private List<Long> followUpIds;
    private List<Long> dutyRoleIndices;

    public static WorkflowStateInfoView fromEntity(WorkflowState state) {
        return WorkflowStateInfoView.builder()
                .id(state.getId())
                .stateKey(state.getStateKey())
                .workflowName(state.getWorkflowName())
                .description(state.getDescription())
                .color(state.getColor())
                .textColor(state.getTextColor())
                .initialState(state.isInitialState())
                .inActive(state.isInActive())
                .dutyRoles(state.getDutyRoles().stream().map(OrgaRoleType::toString).collect(Collectors.toList()))
                .followUps(state.getFollowUps())
                .followUpIds(state.getFollowUps().stream().map(WorkflowState::getId).collect(Collectors.toList()))
                .dutyRoleIndices(state.getDutyRoles().stream().map(role -> Long.valueOf(role.code())).collect(Collectors.toList()))
                .build();
    }

    public static List<WorkflowStateInfoView> fromEntityList(List<WorkflowState> states) {
        return states.stream().map(WorkflowStateInfoView::fromEntity).collect(Collectors.toList());
    }
}
