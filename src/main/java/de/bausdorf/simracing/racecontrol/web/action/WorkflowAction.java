package de.bausdorf.simracing.racecontrol.web.action;

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

import de.bausdorf.simracing.racecontrol.orga.model.Person;
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistration;
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import de.bausdorf.simracing.racecontrol.web.model.orga.WorkflowActionEditView;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class WorkflowAction {
    @Getter
    private final EventOrganizer eventOrganizer;

    public abstract void performAction(WorkflowActionEditView editView, Person actor) throws ActionException;

    protected TeamRegistration updateCurrentAction(WorkflowActionEditView editView,
                                                   de.bausdorf.simracing.racecontrol.orga.model.WorkflowAction currentAction,
                                                   Person actor) throws ActionException {
        TeamRegistration registration = getEventOrganizer().getTeamRegistration(editView.getWorkflowItemId());
        if(registration == null) {
            throw new ActionException("Registration not found for action");
        }

        try {
            getEventOrganizer().updateCurrentAction(currentAction, actor, editView);
        } catch(IllegalStateException e) {
            throw new ActionException(e.getMessage());
        }

        registration.setWorkflowState(currentAction.getTargetState());
        return registration;
    }

}
