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
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import de.bausdorf.simracing.racecontrol.web.model.orga.WorkflowActionEditView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component("DECLINED")
public class DeclineRegistrationAction extends WorkflowAction {

    public DeclineRegistrationAction(@Autowired EventOrganizer eventOrganizer) {
        super(eventOrganizer);
    }

    @Override
    @Transactional
    public void performAction(WorkflowActionEditView editView, Person actor) throws ActionException {
        de.bausdorf.simracing.racecontrol.orga.model.WorkflowAction currentAction = getEventOrganizer().getWorkflowAction(editView.getId());
        if(currentAction != null) {
            getEventOrganizer().saveRegistration(updateCurrentAction(editView, currentAction, actor));
        } else {
            throw new ActionException("Current action not found");
        }
    }
}
