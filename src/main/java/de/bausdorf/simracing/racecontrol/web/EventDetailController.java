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

import de.bausdorf.simracing.racecontrol.discord.JdaClient;
import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.action.WorkflowAction;
import de.bausdorf.simracing.racecontrol.web.action.ActionException;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class EventDetailController extends ControllerBase {
    public static final String EVENT_DETAIL_VIEW = "event-detail";
    public static final String EVENT_VIEW_MODEL_KEY = "event";
    public static final String INDEX_VIEW = "index";
    public static final String TEAM_REGISTRATION_WORKFLOW = "TeamRegistration";
    public static final String TEAMS_TAB = "teams";
    public static final String TASKS_TAB = "tasks";

    private final EventSeriesRepository eventRepository;
    private final PersonRepository personRepository;
    private final BalancedCarRepository carRepository;
    private final EventOrganizer eventOrganizer;
    private final JdaClient jdaClient;
    private final ApplicationContext appContext;

    public EventDetailController(@Autowired EventSeriesRepository eventRepository,
                                 @Autowired PersonRepository personRepository,
                                 @Autowired BalancedCarRepository carRepository,
                                 @Autowired EventOrganizer eventOrganizer,
                                 @Autowired JdaClient jdaClient,
                                 @Autowired ApplicationContext appContext) {
        this.eventRepository = eventRepository;
        this.personRepository = personRepository;
        this.carRepository = carRepository;
        this.eventOrganizer = eventOrganizer;
        this.jdaClient = jdaClient;
        this.appContext = appContext;
    }

    @GetMapping("/event-detail")
    public String eventDetail(@RequestParam Long eventId,
                              @RequestParam Optional<String> messages,
                              @RequestParam Optional<String> activeTab,
                              Model model) {
        Person currentPerson = currentPerson(eventId);
        messages.ifPresent(e -> decodeMessagesToModel(e, model));
        activeTab.ifPresentOrElse(
                s -> setActiveNav(s, model),
                () -> setActiveNav(currentPerson.getRole().isParticipant() ? TEAMS_TAB : TASKS_TAB, model)
        );

        Optional<EventSeries> eventSeries = eventRepository.findById(eventId);
        if(eventSeries.isPresent()) {
            EventInfoView infoView = EventInfoView.fromEntity(eventSeries.get());
            infoView.setAvailableSlots(eventOrganizer.getAvailableGridSlots(eventId));
            infoView.setUserRegistrations(eventOrganizer.myRegistrations(infoView.getEventId(), currentUser()).stream()
                    .filter(IndexController.distinctByKey(team -> team.getTeamName() + team.getCarQualifier()))
                    .map(r -> {
                        TeamRegistrationView registrationView = TeamRegistrationView.fromEntity(r);
                        registrationView.setCarClass(eventOrganizer.getCarClassView(r.getCar().getCarClassId()));
                        return registrationView;
                    })
                    .collect(Collectors.toList()));
            model.addAttribute(EVENT_VIEW_MODEL_KEY, infoView);

            model.addAttribute("currentPerson", PersonView.fromEntity(currentPerson));
            model.addAttribute("teamRegistrations", eventOrganizer.getTeamRegistrationsCarClassList(eventId));
            model.addAttribute("carsInClasses", CarClassView.fromEntityList(eventSeries.get().getCarClassPreset()));
            model.addAttribute("editAction", WorkflowActionEditView.builder()
                            .eventId(eventId)
                            .build()
            );
            model.addAttribute("editStaffView", PersonView.builder()
                    .eventId(eventSeries.get().getId())
                    .build());

            if  (currentPerson != null) {
                model.addAttribute("actions", eventOrganizer.getActiveWorkflowActionListForRole(
                        eventId, TEAM_REGISTRATION_WORKFLOW, currentPerson));
            } else {
                model.addAttribute("actions", new ArrayList<>());
            }
        } else {
            addError("Event with id " + eventId + " not found", model);
            model.addAttribute(EVENT_VIEW_MODEL_KEY, EventInfoView.createEmpty());
            model.addAttribute("editStaffView", PersonView.builder()
                    .eventId(0L)
                    .build());
        }
        model.addAttribute("teamRegistrationSelectView", new TeamRegistrationSelectView());
        model.addAttribute("registeredCarEditView", RegisteredCarEditView.builder()
                .eventId(eventId)
                .build());
        return EVENT_DETAIL_VIEW;
    }

    @PostMapping("/team-registration-action")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER"})
    public String performWorkflowAction(@ModelAttribute WorkflowActionEditView editAction, Model model) {
        if(editAction.getTargetStateKey() == null) {
            addError("No target action selected.", model);
        } else {
            try {
                Person currentPerson = currentPerson(editAction.getEventId());
                String actionKey = editAction.getTargetStateKey();
                if(!StringUtils.isEmpty(actionKey)) {
                    WorkflowAction action = appContext.getBean(actionKey, WorkflowAction.class);
                    action.performAction(editAction, currentPerson);
                }
            } catch (ActionException | NoSuchBeanDefinitionException e) {
                log.warn(e.getMessage());
                addError(e.getMessage(), model);
            }
        }
        return redirectView(EVENT_DETAIL_VIEW, editAction.getEventId(), messagesEncoded(model));
    }

    @PostMapping("/team-save-member")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER"})
    @Transactional
    public String saveStaffPerson(@ModelAttribute PersonView personView, Model model) {
        EventSeries event = eventRepository.findById(personView.getEventId()).orElse(null);
        if(event == null) {
            addError("No event found for id " + personView.getEventId(), model);
            return redirectView(INDEX_VIEW, 0L, messagesEncoded(model));
        }

        TeamRegistration registration = eventOrganizer.getTeamRegistration(personView.getTeamId());
        boolean isLeagueMember = eventOrganizer.checkLeagueMembership(personView.getIracingId(), event.getIRLeagueID());
        boolean isRegistered = userRepository.findByiRacingId(personView.getIracingId()).isPresent();
        Person staff = personRepository.findByEventIdAndIracingId(personView.getEventId(), personView.getIracingId()).orElse(null);
        if(staff != null && staff.getRole().isRacecontrol()) {
            addError(staff.getName() + " is member of race control staff and can't be added to a team!", model);
        } else {
            if (eventOrganizer.denyDriverRole(staff, registration, OrgaRoleType.valueOf(personView.getRole()))) {
                addError((staff != null ? staff.getName() : "Person") + " is assigned as driver in another team !", model);
            } else {
                checkMemberOnIRacingService(personView, model);

                if (Boolean.TRUE.equals(personView.getIracingChecked())) {
                    staff = personView.toEntity(staff);
                    staff.setLeagueMember(isLeagueMember);
                    staff.setRegistered(isRegistered);
                    staff = personRepository.save(staff);

                    addPersonToTeam(registration, staff, personView.getTeamId(), model);
                }
            }
        }
        activeNav = TEAMS_TAB;
        return redirectView(EVENT_DETAIL_VIEW, personView.getEventId(), messagesEncoded(model));
    }

    @GetMapping("/team-remove-member")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER"})
    @Transactional
    public String removeStaffPerson(@RequestParam long personId, @RequestParam long registrationId, Model model) {
        TeamRegistration registration = eventOrganizer.getTeamRegistration(registrationId);
        Optional<Person> person = personRepository.findById(personId);
        if(registration != null) {
            person.ifPresentOrElse(
                    p -> {
                        if(registration.getTeamMembers().stream().anyMatch(m -> m.getIracingId() == p.getIracingId())) {
                            removePersonFromTeam(registration, p);
                        } else {
                            addWarning(p.getName() + " is not a member of " + registration.getTeamName() + " " + registration.getCarQualifier(), model);
                        }
                    },
                    () -> addError("No person found for id " + personId, model)
            );
            activeNav= TEAMS_TAB;
            return redirectView(EVENT_DETAIL_VIEW, registration.getEventId(), messagesEncoded(model));
        } else {
            addError("No team registration found for id " + registrationId, model);
        }
        return redirectView(INDEX_VIEW, 0L, messagesEncoded(model));
    }

    @PostMapping("/edit-registered-car")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF"})
    public String editRegisteredCar(@ModelAttribute RegisteredCarEditView registeredCarEditView, Model model) {
        TeamRegistration registration = eventOrganizer.getTeamRegistration(registeredCarEditView.getTeamId());
        if(registration != null) {
            registration.setWildcard(registeredCarEditView.isUseWildcard());
            carRepository.findById(registeredCarEditView.getCarId()).ifPresent(car -> {
                registration.setCar(car);
                eventOrganizer.saveRegistration(registration);
            });
        } else {
            addError("No registration found for id " + registeredCarEditView.getTeamId(), model);
        }
        return redirectView(EVENT_DETAIL_VIEW, registeredCarEditView.getEventId(), messagesEncoded(model));
    }

    @ModelAttribute(name="staffRoles")
    public List<OrgaRoleType> staffRoles() {
        return OrgaRoleType.participantValues();
    }

    private Person currentPerson(long eventId) {
        return personRepository.findByEventIdAndIracingId(eventId, currentUser().getIRacingId()).orElse(null);
    }

    private void addPersonToTeam(TeamRegistration registration, Person staff, long teamId, Model model) {
        if (registration != null) {
            AtomicReference<Person> toFind = new AtomicReference<>(staff);
            if (registration.getTeamMembers().stream().noneMatch(p -> p.getIracingId() == toFind.get().getIracingId())) {
                registration.getTeamMembers().add(staff);
                eventOrganizer.saveRegistration(registration);
                Optional<Member> discordMember = jdaClient.getMember(staff.getEventId(), staff.getName());
                discordMember.ifPresent(member -> {
                    CarClassView carClass = eventOrganizer.getCarClassView(registration.getCar().getCarClassId());
                    if(carClass != null) {
                        jdaClient.addRoleToMember(registration.getEventId(), member, carClass.getName());
                    }
                    jdaClient.addRoleToMember(registration.getEventId(), member, registration.getTeamName());
                });
            }
        } else {
            addError("No team registration found for id " + teamId, model);
        }
    }

    private void removePersonFromTeam(TeamRegistration registration, Person toRemove) {
        int i = 0;
        for(Person member : registration.getTeamMembers()) {
            if(toRemove.getIracingId() == member.getIracingId()) {
                break;
            }
            i++;
        }
        registration.getTeamMembers().remove(i);
        eventOrganizer.saveRegistration(registration);
        Optional<Member> discordMember = jdaClient.getMember(toRemove.getEventId(), toRemove.getName());
        discordMember.ifPresent(member -> {
            jdaClient.removeRoleFromMember(toRemove.getEventId(), member, registration.getTeamName());
            List<Long> carClassIds = eventOrganizer.myRegistrations(toRemove).stream()
                    .map(r -> r.getCar().getCarClassId())
                    .collect(Collectors.toList());
            if(!carClassIds.contains(registration.getCar().getCarClassId())) {
                CarClassView ccView = eventOrganizer.getCarClassView(registration.getCar().getCarClassId());
                jdaClient.removeRoleFromMember(toRemove.getEventId(), member, ccView.getName());
            }
        });
    }

    private void checkMemberOnIRacingService(PersonView personView, Model model) {
        if(Boolean.FALSE.equals(personView.getIracingChecked())) {
            String iRacingName = eventOrganizer.getIRacingMemberName(personView.getIracingId());
            if(StringUtils.isEmpty(iRacingName)) {
                addError("ID " + personView.getIracingId() + " not found in iRacing service.", model);
            } else if(!iRacingName.equalsIgnoreCase(personView.getName())) {
                addError("Person name " + personView.getName() + " does not match iRacing name " + iRacingName, model);
            } else {
                personView.setIracingChecked(true);
            }
        }
    }

    private String redirectView(String viewName, long eventId, String encodedMessages) {
        return "redirect:/" + viewName
                + (eventId != 0 ? "?eventId=" + eventId : "")
                + (StringUtils.isEmpty(activeNav) ? "" : "&activeTab=" + activeNav)
                + (encodedMessages != null ? "&messages=" + encodedMessages : "");
    }
}
