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
import de.bausdorf.simracing.racecontrol.util.ResultManager;
import de.bausdorf.simracing.racecontrol.web.action.WorkflowAction;
import de.bausdorf.simracing.racecontrol.web.action.ActionException;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
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
import java.util.concurrent.atomic.AtomicLong;
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
    public static final String BOP_TAB = "bop";
    public static final String ACTIVE_CAR_CLASS_KEY = "activeCarClass";

    private final EventSeriesRepository eventRepository;
    private final PersonRepository personRepository;
    private final BalancedCarRepository carRepository;
    private final EventOrganizer eventOrganizer;
    private final ResultManager resultManager;
    private final JdaClient jdaClient;
    private final ApplicationContext appContext;

    public EventDetailController(@Autowired EventSeriesRepository eventRepository,
                                 @Autowired PersonRepository personRepository,
                                 @Autowired BalancedCarRepository carRepository,
                                 @Autowired EventOrganizer eventOrganizer,
                                 @Autowired ResultManager resultManager,
                                 @Autowired JdaClient jdaClient,
                                 @Autowired ApplicationContext appContext) {
        this.eventRepository = eventRepository;
        this.personRepository = personRepository;
        this.carRepository = carRepository;
        this.eventOrganizer = eventOrganizer;
        this.resultManager = resultManager;
        this.jdaClient = jdaClient;
        this.appContext = appContext;
    }

    @GetMapping("/event-detail")
    public String eventDetail(@RequestParam Long eventId,
                              @RequestParam Optional<String> messages,
                              @RequestParam Optional<String> activeTab,
                              @RequestParam Optional<String> activeCarClass,
                              Model model) {
        Person currentPerson = currentPerson(eventId);
        log.debug("/event-detail current person: {}", currentPerson);
        if(currentPerson == null) {
            return redirectView(INDEX_VIEW, 0L, model);
        }
        messages.ifPresent(e -> decodeMessagesToModel(e, model));
        activeTab.ifPresentOrElse(
                s -> setActiveNav(s, model),
                () -> setActiveNav(currentPerson.getRole().isParticipant() ? TEAMS_TAB : TASKS_TAB, model)
        );
        activeCarClass.ifPresentOrElse(
                s -> model.addAttribute(ACTIVE_CAR_CLASS_KEY, s),
                () -> model.addAttribute(ACTIVE_CAR_CLASS_KEY, ""));

        Optional<EventSeries> eventSeries = eventRepository.findById(eventId);
        if(eventSeries.isPresent()) {
            EventInfoView infoView = EventInfoView.fromEntity(eventSeries.get());
            infoView.setDriverPermitRatio(resultManager.getDriverPermissionRatio(eventId));
            infoView.setAvailableSlots(eventOrganizer.getAvailableGridSlots(eventId));
            infoView.setUserRegistrations(eventOrganizer.myRegistrations(infoView.getEventId(), currentUser()).stream()
                    .filter(IndexController.distinctByKey(team -> team.getTeamName() + team.getCarQualifier()))
                    .map(r -> {
                        TeamRegistrationView registrationView = TeamRegistrationView.fromEntity(r);
                        registrationView.setCarClass(eventOrganizer.getCarClassView(r.getCar().getCarClassId()));
                        resultManager.fillPermitTimes(registrationView);
                        return registrationView;
                    })
                    .collect(Collectors.toList()));
            model.addAttribute(EVENT_VIEW_MODEL_KEY, infoView);
            setActiveCarClass(infoView, model);

            List<CarClassRegistrationsView> activeRegistrations = eventOrganizer.getTeamRegistrationsCarClassList(eventId);
            AtomicLong activeRegistrationsCount = new AtomicLong(0L);
            AtomicLong teamPermissionCount = new AtomicLong(0L);
            activeRegistrations.forEach(carClassView -> {
                activeRegistrationsCount.addAndGet(carClassView.getRegistrations().size());
                carClassView.getRegistrations()
                        .stream().filter(r -> r.getTeamPermitTime() != null && !r.getTeamPermitTime().equalsIgnoreCase("NONE"))
                        .forEach(registration -> teamPermissionCount.incrementAndGet());
            });
            infoView.setActiveRegistrations(activeRegistrationsCount.get());
            infoView.setTeamPermissionCount(teamPermissionCount.get());

            model.addAttribute("currentPerson", PersonView.fromEntity(currentPerson));
            model.addAttribute("teamRegistrations", activeRegistrations);
            model.addAttribute("bopViews", eventOrganizer.getBopViews(eventId));
            model.addAttribute("bopEditView", BalancedCarView.builder().eventId(eventId).build());
            model.addAttribute("carsInClasses", CarClassView.fromEntityList(eventSeries.get().getCarClassPreset()));
            model.addAttribute("editAction", WorkflowActionEditView.builder()
                            .eventId(eventId)
                            .build()
            );
            model.addAttribute("editStaffView", PersonView.builder()
                    .eventId(eventSeries.get().getId())
                    .build());
            model.addAttribute("requestPaintsView", RequestPaintsView.builder()
                    .eventId(eventId)
                    .confirmedTeams(eventOrganizer.getConfirmedRegistrationsWithoutPaintRequest(eventId))
                    .build());

            model.addAttribute("actions", eventOrganizer.getActiveWorkflowActionListForRole(
                    eventId, currentPerson));
        } else {
            addError("Event with id " + eventId + " not found", model);
            model.addAttribute(EVENT_VIEW_MODEL_KEY, EventInfoView.createEmpty());
            model.addAttribute("editStaffView", PersonView.builder()
                    .eventId(0L)
                    .build());
            model.addAttribute("requestPaintsView", RequestPaintsView.builder()
                    .eventId(eventId)
                    .confirmedTeams(new ArrayList<>())
                    .build());
        }
        model.addAttribute("teamRegistrationSelectView", new TeamRegistrationSelectView());
        model.addAttribute("registeredCarEditView", RegisteredCarEditView.builder()
                .eventId(eventId)
                .build());
        model.addAttribute("changeOwnerView", ChangeOwnerView.builder().eventId(eventId).build());
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
        return redirectView(EVENT_DETAIL_VIEW, editAction.getEventId(), model);
    }

    @PostMapping("/team-save-member")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER"})
    @Transactional
    public String saveStaffPerson(@ModelAttribute PersonView personView, Model model) {
        EventSeries event = eventRepository.findById(personView.getEventId()).orElse(null);
        if(event == null) {
            addError("No event found for id " + personView.getEventId(), model);
            return redirectView(INDEX_VIEW, 0L, model);
        }

        TeamRegistration registration = eventOrganizer.getTeamRegistration(personView.getTeamId());
        boolean isLeagueMember = eventOrganizer.checkLeagueMembership(personView.getIracingId(), event.getIRLeagueID());
        boolean isRegistered = userRepository.findByiRacingId(personView.getIracingId()).isPresent();
        Person staff = personRepository.findByEventIdAndIracingId(personView.getEventId(), personView.getIracingId()).orElse(null);
        if(staff != null && staff.getRole().isRacecontrol()) {
            addError(staff.getName() + " is member of race control staff and can't be added to a team!", model);
        } else {
            staff = personView.toEntity(staff);

            staff.setIracingTeamChecked(eventOrganizer.checkTeamMembership(staff.getIracingId(), registration.getIracingId()));
            if(staff.getRole() == OrgaRoleType.DRIVER && !staff.isIracingTeamChecked()) {
                addWarning(staff.getName() + " is not a member of this team on iRacing platform!", model);
            }

            if (eventOrganizer.denyDriverRole(staff, registration, OrgaRoleType.valueOf(personView.getRole()))) {
                addError(staff.getName() + " is assigned as driver in another team !", model);
            } else {
                checkMemberOnIRacingService(personView, model);

                if (Boolean.TRUE.equals(personView.getIracingChecked())) {
                    staff.setLeagueMember(isLeagueMember);
                    staff.setRegistered(isRegistered);
                    staff = personRepository.save(staff);

                    addPersonToTeam(registration, staff, personView.getTeamId(), model);
                }
            }
        }
        activeNav = TEAMS_TAB;
        return redirectView(EVENT_DETAIL_VIEW, personView.getEventId(), model);
    }

    @GetMapping("/team-check-members")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER"})
    @Transactional
    public String checkTeamMemberStatus(@RequestParam long teamId, Model model) {
        TeamRegistration registration = eventOrganizer.getTeamRegistration(teamId);
        EventSeries event = eventOrganizer.getEventSeries(registration.getEventId());
        registration.getTeamMembers().forEach(p -> {
            p.setLeagueMember(eventOrganizer.checkLeagueMembership(p.getIracingId(), event.getIRLeagueID()));
            p.setIracingTeamChecked(eventOrganizer.checkTeamMembership(p.getIracingId(), registration.getIracingId()));
            p.setRegistered(userRepository.findByiRacingId(p.getIracingId()).isPresent());
            ensureMemberDiscordRoles(p, registration);
            personRepository.save(p);
        });
        return redirectView(EVENT_DETAIL_VIEW, registration.getEventId(), model);
    }

    @PostMapping("/change-team-owner")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER"})
    @Transactional
    public String changeTeamOwner(@ModelAttribute ChangeOwnerView changeOwnerView, Model model) {
        TeamRegistration registration = eventOrganizer.getTeamRegistration(changeOwnerView.getTeamId());
        Optional<Person> newOwner = personRepository.findById(changeOwnerView.getNewOwnerId());
        if(newOwner.isEmpty()) {
            addError("Person id " + changeOwnerView.getNewOwnerId() + " not found!", model);
        }
        if(registration == null) {
            addError("Team id " + changeOwnerView.getTeamId() + " not found!", model);
        }
        if(newOwner.isPresent() && registration != null) {
            registration.setCreatedBy(newOwner.get());
            eventOrganizer.saveRegistration(registration);
        }
        return redirectView(EVENT_DETAIL_VIEW, changeOwnerView.getEventId(), model);
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
            return redirectView(EVENT_DETAIL_VIEW, registration.getEventId(), model);
        } else {
            addError("No team registration found for id " + registrationId, model);
        }
        return redirectView(INDEX_VIEW, 0L, model);
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
        return redirectView(EVENT_DETAIL_VIEW, registeredCarEditView.getEventId(), model);
    }

    @PostMapping("/save-bop")
    public String saveBop(@ModelAttribute BalancedCarView bopEditView, Model model) {
        Optional<BalancedCar> optionalCar = carRepository.findById(bopEditView.getId());
        activeNav = BOP_TAB;
        optionalCar.ifPresent(car -> {
            BalancedCar carToSave = bopEditView.toEntity(car);
            carRepository.save(carToSave);
        });
        return redirectView(EVENT_DETAIL_VIEW, bopEditView.getEventId(), model);
    }

    @PostMapping("/request-paints")
    public String requestPaints(@ModelAttribute RequestPaintsView requestPaintsView, Model model) {
        Person current = currentPerson(requestPaintsView.getEventId());
        requestPaintsView.getSelectedTeamIds().forEach(l -> eventOrganizer.createPaintRequestAction(requestPaintsView.getEventId(), l, current));
        return redirectView(EVENT_DETAIL_VIEW, requestPaintsView.getEventId(), model);
    }

    @ModelAttribute(name="staffRoles")
    public List<OrgaRoleType> staffRoles() {
        return OrgaRoleType.participantValues();
    }

    private void setActiveCarClass(EventInfoView eventInfoView, Model model) {
        if(!eventInfoView.getUserRegistrations().isEmpty()) {
            model.addAttribute(ACTIVE_CAR_CLASS_KEY, eventInfoView.getUserRegistrations().get(0).getCarClass().getName());
        } else if(!eventInfoView.getCarClassPreset().isEmpty()){
            model.addAttribute(ACTIVE_CAR_CLASS_KEY, eventInfoView.getCarClassPreset().get(0).getName());
        } else {
            model.addAttribute(ACTIVE_CAR_CLASS_KEY, "");
        }
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
                ensureMemberDiscordRoles(staff, registration);
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

    private void ensureMemberDiscordRoles(Person person, TeamRegistration registration) {
        Optional<Member> discordMember = jdaClient.getMemberUncached(person.getEventId(), person.getName());
        discordMember.ifPresent(member -> {
            CarClassView carClass = eventOrganizer.getCarClassView(registration.getCar().getCarClassId());
            if(carClass != null) {
                jdaClient.addRoleToMember(registration.getEventId(), member, carClass.getName());
            }
            jdaClient.addRoleToMember(registration.getEventId(), member, registration.getTeamName());
        });
    }

    private String redirectView(String viewName, long eventId, Model model) {
        return redirectBuilder(viewName)
                .withParameter("eventId", eventId)
                .withParameter("activeTab", activeNav)
                .build(model);
    }
}
