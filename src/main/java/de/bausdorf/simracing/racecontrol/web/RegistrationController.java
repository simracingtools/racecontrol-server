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

import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.LeagueDataCache;
import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.*;

@Controller
@Slf4j
public class RegistrationController extends ControllerBase {
    public static final String REGISTER_TEAM_VIEW = "team-registration";
    public static final String EVENT_VIEW_MODEL_KEY = "eventView";
    public static final String INDEX_VIEW = "index";
    public static final String REGISTRATION_WORKFLOW = "TeamRegistration";

    private final EventSeriesRepository eventRepository;
    private final BalancedCarRepository balancedCarRepository;
    private final PersonRepository personRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final TeamRegistrationRepository registrationRepository;
    private final IRacingClient iRacingClient;
    private final LeagueDataCache leagueDataCache;
    private final EventOrganizer eventOrganizer;

    public RegistrationController(@Autowired EventSeriesRepository eventRepository,
                                  @Autowired BalancedCarRepository balancedCarRepository,
                                  @Autowired PersonRepository personRepository,
                                  @Autowired TeamRegistrationRepository registrationRepository,
                                  @Autowired WorkflowStateRepository workflowStateRepository,
                                  @Autowired WorkflowActionRepository workflowActionRepository,
                                  @Autowired IRacingClient iRacingClient,
                                  @Autowired LeagueDataCache leagueDataCache,
                                  @Autowired EventOrganizer eventOrganizer) {
        this.eventRepository = eventRepository;
        this.balancedCarRepository = balancedCarRepository;
        this.personRepository = personRepository;
        this.registrationRepository = registrationRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.iRacingClient = iRacingClient;
        this.leagueDataCache = leagueDataCache;
        this.eventOrganizer = eventOrganizer;
    }

    @GetMapping("/team-registration")
    public String createRegistration(@RequestParam Long eventId, @RequestParam Optional<String> messages, Model model) {
        messages.ifPresent(e -> decodeMessagesToModel(e, model));

        Person currentPerson = personRepository.findByEventIdAndIracingId(eventId, currentUser().getIRacingId()).orElse(null);
        if (currentPerson != null && currentPerson.getRole().isRacecontrol()) {
            addError("Organizing staff is not allowed to register teams!", model);
            return redirectView(INDEX_VIEW, eventId, messagesEncoded(model));
        }

        boolean leagueMember = false;
        Optional<EventSeries> eventSeries = eventRepository.findById(eventId);
        if(eventSeries.isPresent()) {
            if(OffsetDateTime.now().isBefore(eventSeries.get().getRegistrationOpens())
                    || OffsetDateTime.now().isAfter(eventSeries.get().getRegistrationCloses())) {
                addWarning("Registration for " + eventSeries.get().getTitle() + " is currently closed.", model);
                return redirectView(INDEX_VIEW, eventId, messagesEncoded(model));
            }

            leagueMember = Arrays.stream(leagueDataCache.getLeagueInfo(eventSeries.get().getIRLeagueID()).getRoster())
                            .anyMatch(member -> member.getCustId() == currentUser().getIRacingId());
            EventInfoView infoView = EventInfoView.fromEntity(eventSeries.get());
            infoView.setAvailableSlots(eventOrganizer.getAvailableGridSlots(eventId));
            model.addAttribute(EVENT_VIEW_MODEL_KEY, infoView);
            model.addAttribute("carsInClasses", CarClassView.fromEntityList(eventSeries.get().getCarClassPreset()));
        } else {
            addError("Event with id " + eventId + " not found", model);
            model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.createEmpty());
        }

        model.addAttribute("createRegistrationView" , CreateRegistrationView.builder()
                .eventId(eventId)
                .leagueMember(leagueMember)
                .build());

        return REGISTER_TEAM_VIEW;
    }

    @PostMapping("/save-registration")
    @Transactional
    public String checkAndSaveRegistration(@ModelAttribute CreateRegistrationView createRegistrationView, Model model) {
        TeamRegistration registration = new TeamRegistration();
        registration.setEventId(createRegistrationView.getEventId());
        registration.setCreated(OffsetDateTime.now());
        registration.setLikedCarNumbers(createRegistrationView.getLikedNumbers());
        registration.setTeamName(createRegistrationView.getTeamName());
        registration.setLogoUrl(createRegistrationView.getLogoUrl());
        Optional<BalancedCar> car = balancedCarRepository.findById(createRegistrationView.getCarId());
        if(car.isPresent()) {
            registration.setCar(car.get());
        } else {
            addError("Car ID " + createRegistrationView.getCarId() + " does not exist.", model);
        }

        Person creator = buildCreator(createRegistrationView);
        registration.setCreatedBy(creator);

        WorkflowState initialWorkflowState = workflowStateRepository
                .findByWorkflowNameAndInitialState(REGISTRATION_WORKFLOW, true).orElse(null);
        if(initialWorkflowState == null) {
            addError("No initial state for workflow " + REGISTRATION_WORKFLOW + " found", model);
        } else {
            registration.setWorkflowState(initialWorkflowState);
            registration.getTeamMembers().add(creator);
            registration = registrationRepository.save(registration);
            createWorkFlowAction(registration);
            addInfo("Your application for registration was processed successfully.", model);
        }

        return redirectView(INDEX_VIEW, createRegistrationView.getEventId(), messagesEncoded(model));
    }

    private Person buildCreator(CreateRegistrationView createRegistrationView) {
        Person creator = personRepository.findByEventIdAndIracingId(
                createRegistrationView.getEventId(), currentUser().getIRacingId()).orElse(null);
        if(creator == null) {
            creator = new Person();
            creator.setEventId(createRegistrationView.getEventId());
            creator.setName(currentUser().getName());
            creator.setIracingId(currentUser().getIRacingId());
            creator.setLeagueMember(createRegistrationView.isLeagueMember());
            creator.setRegistered(true);
            if(createRegistrationView.isDriver()) {
                creator.setRole(OrgaRoleType.PARTICIPANT);
            } else {
                creator.setRole(OrgaRoleType.SUPPORT);
            }
            creator = personRepository.save(creator);
        }
        return creator;
    }

    private void createWorkFlowAction(TeamRegistration registration) {
        workflowActionRepository.save(WorkflowAction.builder()
                .created(OffsetDateTime.now())
                .createdBy(registration.getCreatedBy())
                .workflowName(REGISTRATION_WORKFLOW)
                .eventId(registration.getEventId())
                .workflowItemId(registration.getId())
                .sourceState(registration.getWorkflowState())
                .build()
        );
    }

    private String redirectView(String viewName, long eventId, String encodedMessages) {
        return "redirect:/" + viewName
                + (eventId != 0 ? "?eventId=" + eventId : "")
                + (encodedMessages != null ? "&messages=" + encodedMessages : "");
    }
}
