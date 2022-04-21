package de.bausdorf.simracing.racecontrol.web;

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

import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.action.WorkflowAction;
import de.bausdorf.simracing.racecontrol.web.action.ActionException;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Optional;

@Controller
@Slf4j
public class EventDetailController extends ControllerBase {
    public static final String EVENT_DETAIL_VIEW = "event-detail";
    public static final String EVENT_VIEW_MODEL_KEY = "eventView";
    public static final String INDEX_VIEW = "index";
    public static final String TEAM_REGISTRATION_WORKFLOW = "TeamRegistration";

    private final EventSeriesRepository eventRepository;
    private final PersonRepository personRepository;
    private final EventOrganizer eventOrganizer;
    private final ApplicationContext appContext;

    public EventDetailController(@Autowired EventSeriesRepository eventRepository,
                                 @Autowired PersonRepository personRepository,
                                 @Autowired EventOrganizer eventOrganizer,
                                 @Autowired ApplicationContext appContext) {
        this.eventRepository = eventRepository;
        this.personRepository = personRepository;
        this.eventOrganizer = eventOrganizer;
        this.appContext = appContext;
    }

    @GetMapping("/event-detail")
    public String eventDetail(@RequestParam Long eventId, @RequestParam Optional<String> messages, Model model) {
        messages.ifPresent(e -> decodeMessagesToModel(e, model));

        Person currentPerson = currentPerson(eventId);

        Optional<EventSeries> eventSeries = eventRepository.findById(eventId);
        if(eventSeries.isPresent()) {

//            leagueMember = Arrays.stream(leagueDataCache.getLeagueInfo(eventSeries.get().getIRLeagueID()).getRoster())
//                            .anyMatch(member -> member.getCustId() == currentUser().getIRacingId());
            EventInfoView infoView = EventInfoView.fromEntity(eventSeries.get());
            infoView.setAvailableSlots(eventOrganizer.getAvailableGridSlots(eventId));
            model.addAttribute(EVENT_VIEW_MODEL_KEY, infoView);

            model.addAttribute("currentPerson", PersonView.fromEntity(currentPerson));
            model.addAttribute("teamRegistrations", eventOrganizer.getTeamRegistrationsCarClassList(eventId));
            model.addAttribute("carsInClasses", CarClassView.fromEntityList(eventSeries.get().getCarClassPreset()));
            model.addAttribute("editAction", WorkflowActionEditView.builder()
                            .eventId(eventId)
                            .build()
            );
            if  (currentPerson != null) {
                model.addAttribute("actions", eventOrganizer.getActiveWorkflowActionListForRole(
                        eventId, TEAM_REGISTRATION_WORKFLOW, currentPerson));
            } else {
                model.addAttribute("actions", new ArrayList<>());
            }
        } else {
            addError("Event with id " + eventId + " not found", model);
            model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.createEmpty());
        }

        return EVENT_DETAIL_VIEW;
    }

    @PostMapping("/team-registration-action")
    public String assignCarNoOrDecline(@ModelAttribute WorkflowActionEditView editAction, Model model) {
        if(editAction.getTargetStateKey() == null) {
            addError("No target action selected.", model);
        } else {
            try {
                Person currentPerson = currentPerson(editAction.getEventId());
                String actionKey = editAction.getTargetStateKey();

                // Remove comma in action message because there may be duplicate ID's in forms.
                String actionMessage = editAction.getMessage().replace(",", "");
                editAction.setMessage(actionMessage);

                WorkflowAction action = appContext.getBean(actionKey, WorkflowAction.class);
                action.performAction(editAction, currentPerson);
            } catch (ActionException | NoSuchBeanDefinitionException e) {
                log.warn(e.getMessage());
                addError(e.getMessage(), model);
            }
        }
        return redirectView(EVENT_DETAIL_VIEW, editAction.getEventId(), messagesEncoded(model));
    }

    private Person currentPerson(long eventId) {
        return personRepository.findByEventIdAndIracingId(eventId, currentUser().getIRacingId()).orElse(null);
    }

    private String redirectView(String viewName, long eventId, String encodedMessages) {
        return "redirect:/" + viewName
                + (eventId != 0 ? "?eventId=" + eventId : "")
                + (encodedMessages != null ? "&messages=" + encodedMessages : "");
    }
}
