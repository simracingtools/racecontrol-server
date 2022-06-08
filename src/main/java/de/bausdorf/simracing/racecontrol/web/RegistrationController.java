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

import de.bausdorf.simracing.irdataapi.model.TeamMemberDto;
import de.bausdorf.simracing.racecontrol.iracing.LeagueDataCache;
import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Controller
@Slf4j
public class RegistrationController extends ControllerBase {
    public static final String REGISTER_TEAM_VIEW = "team-registration";
    public static final String EVENT_VIEW_MODEL_KEY = "eventView";
    public static final String INDEX_VIEW = "index";
    public static final String REGISTRATION_WORKFLOW = "TeamRegistration";
    public static final String TEAM_ID_PARAM = "teamId";
    public static final String EVENT_ID_PARAM = "eventId";

    private final EventSeriesRepository eventRepository;
    private final BalancedCarRepository balancedCarRepository;
    private final PersonRepository personRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final LeagueDataCache leagueDataCache;
    private final EventOrganizer eventOrganizer;

    public RegistrationController(@Autowired EventSeriesRepository eventRepository,
                                  @Autowired BalancedCarRepository balancedCarRepository,
                                  @Autowired PersonRepository personRepository,
                                  @Autowired WorkflowStateRepository workflowStateRepository,
                                  @Autowired WorkflowActionRepository workflowActionRepository,
                                  @Autowired LeagueDataCache leagueDataCache,
                                  @Autowired EventOrganizer eventOrganizer) {
        this.eventRepository = eventRepository;
        this.balancedCarRepository = balancedCarRepository;
        this.personRepository = personRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.leagueDataCache = leagueDataCache;
        this.eventOrganizer = eventOrganizer;
    }

    @GetMapping("/team-registration")
    public String createRegistration(@RequestParam Long eventId,
                                     @RequestParam Optional<String> messages,
                                     @RequestParam Optional<Long> teamId,
                                     Model model) {
        messages.ifPresent(e -> decodeMessagesToModel(e, model));

        Person currentPerson = personRepository.findByEventIdAndIracingId(eventId, currentUser().getIRacingId()).orElse(null);
        if (currentPerson != null && currentPerson.getRole().isRacecontrol()) {
            addError("Organizing staff is not allowed to register teams!", model);
            return redirectBuilder(INDEX_VIEW)
                    .withParameter(EVENT_ID_PARAM, eventId)
                    .build(model);
        }

        boolean leagueMember = false;
        Optional<EventSeries> eventSeries = eventRepository.findById(eventId);
        if(eventSeries.isPresent()) {
            if(OffsetDateTime.now().isBefore(eventSeries.get().getRegistrationOpens())
                    || OffsetDateTime.now().isAfter(eventSeries.get().getRegistrationCloses())) {
                addWarning("Registration for " + eventSeries.get().getTitle() + " is currently closed.", model);
                return redirectBuilder(INDEX_VIEW)
                        .withParameter(EVENT_ID_PARAM, eventId)
                        .build(model);
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

        TeamRegistration registration = null;
        if(teamId.isPresent()) {
            registration = eventOrganizer.getTeamRegistration(teamId.get());
        }
        model.addAttribute("createRegistrationView" , CreateRegistrationView.builder()
                        .eventId(eventId)
                        .teamName(registration != null ? registration.getTeamName() : null)
                        .discordChannelId(registration != null ? registration.getDiscordChannelId() : null)
                        .otherTeamId(registration != null ? registration.getId() : 0L)
                        .leagueMember(leagueMember)
                        .build());

        return REGISTER_TEAM_VIEW;
    }

    @PostMapping("/save-registration")
    @Transactional
    public String checkAndSaveRegistration(@ModelAttribute CreateRegistrationView createRegistrationView, Model model) {
        Person creator = buildCreator(createRegistrationView);

        List<TeamRegistration> myRegistrations = eventOrganizer.myRegistrations(creator);
        AtomicReference<String> error = new AtomicReference<>(null);
        if(createRegistrationView.isDriver()) {
            myRegistrations.stream()
                    .filter(r -> r.getTeamMembers().stream().anyMatch(p -> p.getIracingId() == creator.getIracingId() && p.getRole() == OrgaRoleType.DRIVER))
                    .forEach(r -> error.set("You are already a driver in team " + r.getTeamName() + " " + (r.getCarQualifier() != null ? r.getCarQualifier() : "")));
        }
        if(error.get() != null) {
            addError(error.get(), model);
            return redirectBuilder(REGISTER_TEAM_VIEW)
                    .withParameter(EVENT_ID_PARAM, createRegistrationView.getEventId())
                    .withParameter(TEAM_ID_PARAM, createRegistrationView.getOtherTeamId())
                    .build(model);
        }

        if(!eventOrganizer.isQualifierUnique(createRegistrationView.getEventId(), createRegistrationView.getTeamName(), createRegistrationView.getCarQualifier())) {
            addError("Qualifier " + createRegistrationView.getCarQualifier() + " is already used on another team of the same name", model);
            return redirectBuilder(REGISTER_TEAM_VIEW)
                    .withParameter(EVENT_ID_PARAM, createRegistrationView.getEventId())
                    .withParameter(TEAM_ID_PARAM, createRegistrationView.getOtherTeamId())
                    .build(model);
        }

        String teamError = checkTeamIdAndName(createRegistrationView.getIracingId(),
                createRegistrationView.getTeamName(), createRegistrationView.getCarQualifier(), currentUser());
        if(teamError != null) {
            addError(teamError, model);
            return redirectBuilder(REGISTER_TEAM_VIEW)
                    .withParameter(EVENT_ID_PARAM, createRegistrationView.getEventId())
                    .withParameter(TEAM_ID_PARAM, createRegistrationView.getOtherTeamId())
                    .build(model);
        }

        TeamRegistration registration = new TeamRegistration();
        registration.setEventId(createRegistrationView.getEventId());
        registration.setCreated(OffsetDateTime.now());
        registration.setLikedCarNumbers(createRegistrationView.getLikedNumbers());
        registration.setTeamName(createRegistrationView.getTeamName());
        registration.setCarQualifier(createRegistrationView.getCarQualifier());
        registration.setLogoUrl(createRegistrationView.getLogoUrl());
        registration.setIracingId(createRegistrationView.getIracingId());
        registration.setDiscordChannelId(createRegistrationView.getDiscordChannelId());
        Optional<BalancedCar> car = balancedCarRepository.findById(createRegistrationView.getCarId());
        if(car.isPresent()) {
            registration.setCar(car.get());
        } else {
            addError("Car ID " + createRegistrationView.getCarId() + " does not exist.", model);
            return redirectBuilder(REGISTER_TEAM_VIEW)
                    .withParameter(EVENT_ID_PARAM, createRegistrationView.getEventId())
                    .withParameter(TEAM_ID_PARAM, createRegistrationView.getOtherTeamId())
                    .build(model);
        }

        registration.setCreatedBy(creator);

        WorkflowState initialWorkflowState = workflowStateRepository
                .findByWorkflowNameAndInitialState(REGISTRATION_WORKFLOW, true).orElse(null);
        if(initialWorkflowState == null) {
            addError("No initial state for workflow " + REGISTRATION_WORKFLOW + " found", model);
        } else {
            registration.setWorkflowState(initialWorkflowState);
//            registration.getTeamMembers().add(creator);

            registration = eventOrganizer.saveRegistration(registration);
            createWorkFlowAction(registration);
            addInfo("Your application for registration was processed successfully.", model);
        }

        return redirectBuilder(INDEX_VIEW)
                .withParameter(EVENT_ID_PARAM, createRegistrationView.getEventId())
                .build(model);
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
                creator.setRole(OrgaRoleType.DRIVER);
            } else {
                creator.setRole(OrgaRoleType.SUPPORT);
            }
            creator = personRepository.save(creator);
        }
        return creator;
    }

    private String checkTeamIdAndName(long irTeamId, @NonNull String teamName, @Nullable String carQualifier, RcUser currentUser) {
        String irTeamName = eventOrganizer.getTeamName(irTeamId);
        if(irTeamName == null) {
            return "Team id " + irTeamId + " not found on iRacing service.";
        }

        String fullTeamName = teamName + (carQualifier != null ? " " + carQualifier : "");
        if(!irTeamName.equalsIgnoreCase(fullTeamName)) {
            return "Team name " + fullTeamName + " does not match iRacing team name " + irTeamName;
        }

        String[] nameParts = currentUser.getName().split(" ");
        Optional<TeamMemberDto> member = eventOrganizer.getTeamMembers(irTeamId).stream()
                .filter(m -> Arrays.stream(nameParts)
                            .allMatch(s -> m.getDisplayName().toLowerCase().contains(s.toLowerCase()))
                )
                .findAny();
        if(member.isEmpty()) {
            return "You are not a member of " + irTeamName + " in iRacing service";
        }
        return null;
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
}
