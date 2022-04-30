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

import de.bausdorf.simracing.racecontrol.orga.model.WorkflowAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkflowActionInfoView {
    private Long id;
    private Long eventId;
    private String workflowName;
    private OffsetDateTime created;
    private Long workflowItemId;
    private String teamName;
    private PersonView createdBy;
    private OffsetDateTime dueDate;
    private WorkflowStateInfoView sourceState;
    private List<WorkflowStateInfoView> targetStates;
    private WorkflowStateInfoView targetState;
    private PersonView doneBy;
    private OffsetDateTime doneAt;
    private String editActionMessage;
    private String message;
    private boolean executableByUser;

    public static WorkflowActionInfoView fromEntity(WorkflowAction action) {
        return WorkflowActionInfoView.builder()
                .id(action.getId())
                .workflowName(action.getWorkflowName())
                .workflowItemId(action.getWorkflowItemId())
                .created(action.getCreated())
                .createdBy(PersonView.fromEntity(action.getCreatedBy()))
                .doneAt(action.getDoneAt())
                .doneBy(PersonView.fromEntity(action.getDoneBy()))
                .dueDate(action.getDueDate())
                .sourceState(WorkflowStateInfoView.fromEntity(action.getSourceState()))
                .targetStates(WorkflowStateInfoView.fromEntityList(action.getSourceState().getFollowUps()))
                .targetState(WorkflowStateInfoView.fromEntity(action.getTargetState()))
                .message(action.getMessage())
                .build();
    }
}
