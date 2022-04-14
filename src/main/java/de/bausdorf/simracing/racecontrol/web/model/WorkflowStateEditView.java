package de.bausdorf.simracing.racecontrol.web.model;

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
import de.bausdorf.simracing.racecontrol.orga.model.WorkflowStateRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class WorkflowStateEditView {
    private long id;

    private String workflowName;
    private String stateKey;
    private String description;
    private String color;
    private String textColor;
    private Boolean initialState;
    private List<Long> dutyRoleIndices;
    private List<Long> followUpIds;

    public WorkflowState toEntity(WorkflowState state, WorkflowStateRepository stateRepository) {
        if(state == null) {
            state = new WorkflowState();
        }
        state.setId(id == 0 ? state.getId() : id);
        state.setStateKey(stateKey == null ? state.getStateKey() : stateKey);
        state.setWorkflowName(workflowName == null ? state.getWorkflowName() : workflowName);
        state.setDescription(description == null ? state.getDescription() : description);
        state.setColor(color == null ? state.getColor() : color);
        state.setTextColor(textColor == null ? state.getTextColor() : textColor);
        state.setInitialState(initialState == null ? state.isInitialState() : initialState);
        state.setDutyRoles(dutyRoleIndices == null ? state.getDutyRoles()
                : dutyRoleIndices.stream().map(code -> OrgaRoleType.ofCode(code.intValue())).collect(Collectors.toList()));

        if(followUpIds != null) {
            fetchFollowUpStates(state, stateRepository);
        }

        return state;
    }

    public static WorkflowStateEditView fromEntity(WorkflowState state) {
        return WorkflowStateEditView.builder()
                .id(state.getId())
                .stateKey(state.getStateKey())
                .workflowName(state.getWorkflowName())
                .description(state.getDescription())
                .color(state.getColor())
                .textColor(state.getTextColor())
                .initialState(state.isInitialState())
                .dutyRoleIndices(state.getDutyRoles().stream().map(role -> Long.valueOf(role.code())).collect(Collectors.toList()))
                .followUpIds(state.getFollowUps().stream().map(WorkflowState::getId).collect(Collectors.toList()))
                .build();
    }

    private void fetchFollowUpStates(WorkflowState state, WorkflowStateRepository stateRepository) {
        for(Long followUpId: followUpIds) {
            Optional<WorkflowState> workflowState = stateRepository.findById(followUpId);
            if(workflowState.isPresent() && !state.getFollowUps().contains(workflowState.get())) {
                state.getFollowUps().add(workflowState.get());
            }
        }
    }
}
