package de.bausdorf.simracing.racecontrol.orga.model;

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

import org.springframework.data.repository.CrudRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface WorkflowActionRepository extends CrudRepository<WorkflowAction, Long> {
    List<WorkflowAction> findAllByEventIdAndWorkflowNameOrderByCreatedDesc(long eventId, String workflowName);
    List<WorkflowAction> findAllByEventIdAndTargetStateIn(long eventId, Collection<WorkflowState> targetState);
    List<WorkflowAction> findAllByWorkflowItemIdAndDoneAt(long itemId, OffsetDateTime doneAt);
}
