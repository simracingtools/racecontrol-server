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

import de.bausdorf.simracing.irdataapi.model.CarAssetDto;
import de.bausdorf.simracing.irdataapi.model.CarInfoDto;
import de.bausdorf.simracing.irdataapi.model.LeagueInfoDto;
import de.bausdorf.simracing.irdataapi.model.web.TeamMemberDto;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class EventOrganizer {
    public static final String ASSET_BASE_URL = "https://images-static.iracing.com";
    private final CarClassRepository carClassRepository;
    private final TeamRegistrationRepository registrationRepository;
    private final WorkflowActionRepository actionRepository;
    private final WorkflowStateRepository stateRepository;
    private final PersonRepository personRepository;
    private final EventSeriesRepository seriesRepository;
    private final IRacingClient dataClient;
    public EventOrganizer(@Autowired CarClassRepository carClassRepository,
                          @Autowired TeamRegistrationRepository registrationRepository,
                          @Autowired WorkflowActionRepository actionRepository,
                          @Autowired WorkflowStateRepository stateRepository,
                          @Autowired PersonRepository personRepository,
                          @Autowired EventSeriesRepository seriesRepository,
                          @Autowired IRacingClient dataClient) {
        this.carClassRepository = carClassRepository;
        this.registrationRepository = registrationRepository;
        this.stateRepository = stateRepository;
        this.personRepository = personRepository;
        this.seriesRepository = seriesRepository;
        this.dataClient = dataClient;
        this.actionRepository = actionRepository;
    }

    public List<CarClassRegistrationsView> getTeamRegistrationsCarClassList(long eventId) {
        List<CarClassRegistrationsView> resultList = new ArrayList<>();
        List<TeamRegistration> eventRegistrations = registrationRepository.findAllByEventId(eventId);
        List<CarClass> eventClasses = carClassRepository.findAllByEventIdOrderByClassOrderAsc(eventId);
        eventClasses.forEach(carClass -> {
            AtomicInteger availableSlotsCount = new AtomicInteger(carClass.getMaxSlots() - carClass.getWildcards());
            AtomicLong wildcards = new AtomicLong(carClass.getWildcards());
            AtomicLong onWaitingList = new AtomicLong(0L);
            AtomicReference<TeamRegistrationView[]> regArray = new AtomicReference<>(new TeamRegistrationView[carClass.getMaxSlots()]);
            AtomicReference<List<TeamRegistrationView>> waitingList = new AtomicReference<>(new ArrayList<>());
            AtomicReference<CarClassRegistrationsView> classView = new AtomicReference<>(new CarClassRegistrationsView());
            AtomicLong regCount = new AtomicLong(0L);
            eventRegistrations.stream()
                    .filter(r -> (r.getCar().getCarClassId() == carClass.getId()
                            && !r.getWorkflowState().isInActive()))
                    .forEach(r -> {
                        if (r.isWildcard() && wildcards.get() > 0) {
                            wildcards.decrementAndGet();
                        } else if (availableSlotsCount.get() > 0) {
                            availableSlotsCount.decrementAndGet();
                        } else  {
                            onWaitingList.incrementAndGet();
                        }
                        CarAssetDto assets = getCarAsset(r.getCar().getCarId());
                        TeamRegistrationView view = TeamRegistrationView.fromEntity(r);
                        view.setCarClass(CarClassView.fromEntity(carClass));
                        if(assets != null) {
                            view.getCar().setCarLogoUrl(ASSET_BASE_URL + assets.getLogo());
                        }
                        fillRegisteredSlots(view, carClass, regArray, regCount, waitingList);
                        regCount.incrementAndGet();
                    });
            fillEmptySlots(carClass, regArray, waitingList);

            classView.get().setRegistrations(waitingList.get());
            classView.get().setMaxSlots(carClass.getMaxSlots());
            classView.get().setCarClassId(carClass.getId());
            classView.get().setName(carClass.getName());
            classView.get().setAvailableSlots(availableSlotsCount.get());
            classView.get().setWildcards(wildcards.get());
            classView.get().setOnWaitingList(onWaitingList.get());
            resultList.add(classView.get());
        });
        return resultList;
    }

    public List<AvailableSlotsView> getAvailableGridSlots(long eventId) {
        List<AvailableSlotsView> carClassMap = new ArrayList<>();
        List<CarClass> eventClasses = carClassRepository.findAllByEventId(eventId);
        List<TeamRegistration> eventRegistrations = registrationRepository.findAllByEventId(eventId);
        eventClasses.forEach(carClass -> {
            AtomicInteger availableSlotsCount = new AtomicInteger(carClass.getMaxSlots() - carClass.getWildcards());
            AtomicLong wildcards = new AtomicLong(carClass.getWildcards());
            AtomicLong onWaitingList = new AtomicLong(0L);

            eventRegistrations.stream()
                    .filter(r -> (r.getCar().getCarClassId() == carClass.getId()
                            && !r.getWorkflowState().isInActive()))
                    .forEach(r -> {
                        if (r.isWildcard() && wildcards.get() > 0) {
                            wildcards.decrementAndGet();
                        } else if (availableSlotsCount.get() > 0) {
                            availableSlotsCount.decrementAndGet();
                        } else  {
                            onWaitingList.incrementAndGet();
                        }
                    });

            AvailableSlotsView availableSlots = AvailableSlotsView.builder()
                    .carClassId(carClass.getId())
                    .name(carClass.getName())
                    .availableSlots(availableSlotsCount.get())
                    .wildcards(wildcards.get())
                    .onWaitingList(onWaitingList.get())
                    .build();
            carClassMap.add(availableSlots);
        });
        return carClassMap;
    }

    public List<WorkflowActionInfoView> getActiveWorkflowActionListForRole(long eventId, String workflowName, @NonNull Person currentPerson) {
        List<WorkflowAction> actionList = actionRepository.findAllByEventIdAndWorkflowNameOrderByCreatedDesc(eventId, workflowName);
        List<WorkflowActionInfoView> resultList = actionList.stream()
                .filter(a -> filterMyOpenTasks(a, currentPerson))
                .map(action -> mapWorkflowAction(workflowName, action, currentPerson))
                .collect(Collectors.toList());
        resultList.addAll(actionList.stream()
                .filter(a -> filterMyClosedTasks(a, currentPerson))
                .map(action -> mapWorkflowAction(workflowName, action, currentPerson))
                .collect(Collectors.toList()));
        return resultList;
    }

    public List<BalancedCarBopView> getBopViews(long eventId) {
        List<BalancedCar> carsInEvent = new ArrayList<>();
        carClassRepository.findAllByEventIdOrderByClassOrderAsc(eventId)
                .forEach(carClass -> carsInEvent.addAll(carClass.getCars()));

        return carsInEvent.stream()
                .map(car -> {
                    Optional<CarInfoDto> irCar = Arrays.stream(dataClient.getDataCache().getCars())
                            .filter(c -> (c.getCarId() == car.getCarId()))
                            .findFirst();

                    return BalancedCarBopView.fromEntityAndCar(eventId, car, irCar);
                })
                .collect(Collectors.toList());
    }

    public boolean checkUniqueCarNumber(long eventId, @NonNull String carNo) {
        return registrationRepository.findAllByEventId(eventId).stream()
                .noneMatch(r -> !r.getWorkflowState().isInActive() && carNo.equalsIgnoreCase(r.getAssignedCarNumber()));
    }

    public boolean checkLeagueMembership(long iracingId, long irLeagueID) {
        LeagueInfoDto leagueInfo = dataClient.getLeagueInfo(irLeagueID);
        return Arrays.stream(leagueInfo.getRoster()).anyMatch(member -> member.getCustId() == iracingId);
    }

    public String getIRacingMemberName(long iracingId) {
        MemberInfo memberInfo = dataClient.getMemberInfo(iracingId).orElse(null);
        return memberInfo != null ? memberNameWithoutMiddleInitial(memberInfo.getName()) :  null;
    }

    public List<TeamMemberDto> getTeamMembers(long iracingTeamId) {
        return dataClient.getTeamMembers(iracingTeamId);
    }

    public String getTeamName(long iracingTeamId) {
        List<TeamMemberDto> members = getTeamMembers(iracingTeamId);
        if(members.isEmpty()) {
            return null;
        }
        return members.get(0).getTeamName();
    }

    public List<TeamRegistration> checkUniqueTeamDriver(@Nullable Person person) {
        if(person == null) {
            return new ArrayList<>();
        }
        return registrationRepository.findAllByEventId(person.getEventId()).stream()
                .filter(r -> !r.getWorkflowState().isInActive())
                .filter(r -> r.getTeamMembers().stream().anyMatch(p -> p.getIracingId() == person.getIracingId()))
                .collect(Collectors.toList());
    }

    public boolean isQualifierUnique(long eventId, String teamName, String qualifier) {
        List<TeamRegistration> registrations = registrationRepository.findAllByEventIdAndTeamName(eventId, teamName);
        return registrations.stream().noneMatch(r -> r.getCarQualifier().equalsIgnoreCase(qualifier));
    }

    public boolean denyDriverRole(Person supporter, TeamRegistration team, OrgaRoleType targetRole) {
        if(supporter == null || team == null) {
            return false;
        }
        boolean deny = false;
        List<TeamRegistration> memberDrivingIn = checkUniqueTeamDriver(supporter);
        if(memberDrivingIn.size() == 1) {
            if(memberDrivingIn.get(0).getId() != team.getId()) {
                deny = true;
            }
        } else if(memberDrivingIn.size() > 1
                && (targetRole == OrgaRoleType.DRIVER || supporter.getRole() == OrgaRoleType.DRIVER)) {
            deny = true;
        }
        return deny;
    }

    public List<TeamRegistration> myRegistrations(long eventId, @NonNull RcUser currentUser) {
        Person currentPerson = personRepository.findByEventIdAndIracingId(eventId, currentUser.getIRacingId()).orElse(null);
        if(currentPerson != null) {
            return myRegistrations(currentPerson);
        }
        return List.of();
    }

    public List<EventSeries> myActiveEvents(@NonNull RcUser currentUser) {
        return personRepository.findAllByIracingId(currentUser.getIRacingId()).stream()
                .map(p -> getEventSeries(p.getEventId()))
                .collect(Collectors.toList());
    }

    public Person getPersonInEvent(long eventId, long iracingId) {
        return personRepository.findByEventIdAndIracingId(eventId, iracingId).orElse(null);
    }

    public EventSeries closestEventByNow(@NonNull RcUser currentUser, List<EventSeries> events) {
        OffsetDateTime userLocalTime = getUserLocalTime(currentUser);
        AtomicReference<EventSeries> closestEvent = closeToRegistration(events, userLocalTime);
        if(closestEvent.get() == null) {
            events.forEach(event -> {
                if(event.getStartDate().isAfter(userLocalTime.toLocalDate())) {
                    if(closestEvent.get() == null) {
                        closestEvent.set(event);
                    } else {
                        if(ChronoUnit.MINUTES.between(closestEvent.get().getStartDate(), userLocalTime) >
                                ChronoUnit.MINUTES.between(event.getStartDate(), userLocalTime)) {
                            closestEvent.set(event);
                        }
                    }
                }
            });
        }
        return closestEvent.get();
    }

    public OffsetDateTime getUserLocalTime(@NonNull RcUser currentUser) {
        return OffsetDateTime.now().withOffsetSameInstant(currentUser.getTimezone().getRules().getOffset(Instant.now()));
    }

    public List<TeamRegistration> myRegistrations(Person person) {
        return  registrationRepository.findAllByEventId(person.getEventId()).stream()
                .filter(r -> r.getCreatedBy().getId() == person.getId()
                        || r.getTeamMembers().stream().anyMatch(p -> p.getId() == person.getId()))
                .collect(Collectors.toList());
    }

    public List<Long> myRegistrationIds(Person person) {
        return myRegistrations(person).stream()
                .map(TeamRegistration::getId)
                .collect(Collectors.toList());
    }

    public TeamRegistration getTeamRegistration(long registrationId) {
        return registrationRepository.findById(registrationId).orElse(null);
    }

    public TeamRegistration saveRegistration(TeamRegistration registration) {
        return registrationRepository.save(registration);
    }

    public CarClassView getCarClassView(long carClassId) {
        CarClass carClass = carClassRepository.findById(carClassId).orElse(null);
        if(carClass != null) {
            return CarClassView.fromEntity(carClass);
        }
        return CarClassView.builder()
                .name("Unknown")
                .build();
    }

    public WorkflowAction getWorkflowAction(long actionId) {
        return actionRepository.findById(actionId).orElse(null);
    }

    public List<WorkflowAction> getActiveWorkflowActionForItem(long registrationId) {
        return actionRepository.findAllByWorkflowItemIdAndDoneAt(registrationId, null);
    }

    public WorkflowAction updateCurrentAction(@NonNull WorkflowAction currentAction, @NonNull Person actor, @NonNull WorkflowActionEditView editAction) {
        currentAction.setDoneAt(OffsetDateTime.now());
        currentAction.setDoneBy(actor);
        currentAction.setMessage(editAction.getMessage());
        WorkflowState targetState = stateRepository.findWorkflowStateByWorkflowNameAndStateKey(
                currentAction.getWorkflowName(), editAction.getTargetStateKey()).orElse(null);
        if(targetState == null) {
            throw new IllegalStateException("Target state " + editAction.getTargetStateKey() + " not found.");
        }
        currentAction.setTargetState(targetState);
        return actionRepository.save(currentAction);
    }

    public WorkflowAction createFollowUpAction(WorkflowAction currentAction, Person actor, LocalDateTime dueDate) {
        WorkflowAction followUp = WorkflowAction.builder()
                .eventId(currentAction.getEventId())
                .workflowName(currentAction.getTargetState().getWorkflowName())
                .workflowItemId(currentAction.getWorkflowItemId())
                .created(OffsetDateTime.now())
                .createdBy(actor)
                .sourceState(currentAction.getTargetState())
                .dueDate(dueDate != null ? OffsetDateTime.of(dueDate, ZoneOffset.UTC) : null)
                .build();
        return actionRepository.save(followUp);
    }

    public EventSeries getEventSeries(long eventId) {
        return seriesRepository.findById(eventId).orElse(null);
    }

    public static String memberNameWithoutMiddleInitial(@NonNull String iRacingName) {
        String[] nameParts = iRacingName.split(" ");
        return nameParts[0] + " " + nameParts[nameParts.length - 1];
    }

    private AtomicReference<EventSeries> closeToRegistration(List<EventSeries> events, OffsetDateTime userLocalTime) {
        AtomicReference<EventSeries> closestEvent = new AtomicReference<>();
        events.forEach(event -> {
            if(event.getRegistrationCloses().isAfter(userLocalTime)) {
                if(closestEvent.get() == null) {
                    closestEvent.set(event);
                } else {
                    if(ChronoUnit.MINUTES.between(closestEvent.get().getRegistrationCloses(), userLocalTime) >
                            ChronoUnit.MINUTES.between(event.getRegistrationCloses(), userLocalTime)) {
                        closestEvent.set(event);
                    }
                }
            }
        });
        return closestEvent;
    }

    private void setWildcardSlots(TeamRegistrationView view, CarClass carClass, AtomicReference<TeamRegistrationView[]> regArray) {
        for(int i = 0; i < carClass.getWildcards(); i++) {
            if(regArray.get()[i] == null) {
                regArray.get()[i] = view;
                break;
            }
        }
    }

    private void setRegularSlots(TeamRegistrationView view, CarClass carClass, AtomicReference<TeamRegistrationView[]> regArray) {
        for(int i = carClass.getWildcards(); i < carClass.getMaxSlots(); i++) {
            if (regArray.get()[i] == null) {
                regArray.get()[i] = view;
                break;
            }
        }
    }

    private void fillRegisteredSlots(TeamRegistrationView view,
                                     CarClass carClass,
                                     AtomicReference<TeamRegistrationView[]> regArray,
                                     AtomicLong regCount,
                                     AtomicReference<List<TeamRegistrationView>> waitingList) {
        if(view.isWildcard()) {
            setWildcardSlots(view, carClass, regArray);
        } else if(regCount.get() < carClass.getMaxSlots() - carClass.getWildcards()){
            setRegularSlots(view, carClass, regArray);
        } else {
            waitingList.get().add(view);
        }
    }
    private void fillEmptySlots(CarClass carClass, AtomicReference<TeamRegistrationView[]> regArray, AtomicReference<List<TeamRegistrationView>> waitingList) {
        for (int i = 0; i < carClass.getMaxSlots(); i++) {
            if (regArray.get()[i] == null) {
                regArray.get()[i] = TeamRegistrationView.builder()
                        .teamName(i < carClass.getWildcards() ? "Free wildcard" : "Free slot")
                        .workflowState(WorkflowStateInfoView.builder()
                                .build())
                        .carClass(CarClassView.fromEntity(carClass))
                        .car(BalancedCarView.builder()
                                .carLogoUrl("")
                                .carName("")
                                .carClassId(0)
                                .build())
                        .build();
            }
            waitingList.get().add(i, regArray.get()[i]);
        }
    }

    private boolean filterMyOpenTasks(WorkflowAction action, Person currentPerson) {
        boolean isOpenTask =  action.getTargetState() == null;

        boolean maySeeTask;
        if(currentPerson.getRole().isParticipant()) {
            maySeeTask = myRegistrationIds(currentPerson).contains(action.getWorkflowItemId());
        } else {
            maySeeTask = action.getSourceState().getFollowUps().stream()
                    .anyMatch(s -> s.getDutyRoles().contains(currentPerson.getRole()));

        }

        return isOpenTask && maySeeTask;
    }

    private boolean filterMyClosedTasks(WorkflowAction action, Person currentPerson) {
        boolean isClosedTask = action.getTargetState() != null;

        boolean maySeeTask;
        if(currentPerson.getRole().isParticipant()) {
            maySeeTask = myRegistrationIds(currentPerson).contains(action.getWorkflowItemId());
        } else {
            maySeeTask = action.getSourceState().getFollowUps().stream()
                .anyMatch(s -> s.getDutyRoles().contains(currentPerson.getRole()));

        }

        return maySeeTask && isClosedTask;
    }

    private CarAssetDto getCarAsset(long carId) {
        return dataClient.getDataCache().getCarAssets().values().stream()
                .filter(asset -> asset.getCarId() == carId).findFirst().orElse(null);
    }

    private WorkflowActionInfoView mapWorkflowAction(String workflowName, WorkflowAction action, @NonNull Person currentPerson){
        WorkflowActionInfoView infoView = WorkflowActionInfoView.fromEntity(action);
        if(workflowName.equalsIgnoreCase("TeamRegistration")) {
            Optional<TeamRegistration> registration = registrationRepository.findById(action.getWorkflowItemId());
            if(action.getSourceState().getStateKey().equalsIgnoreCase("TEAM_REGISTRATION")) {
                registration.ifPresent(r -> infoView.setEditActionMessage(r.getLikedCarNumbers()));
            }
            registration.ifPresent(r -> {
                        String carQualifier = StringUtils.isEmpty(r.getCarQualifier()) ? "" : (" " + r.getCarQualifier());
                        infoView.setTeamName(
                                StringUtils.isEmpty(r.getAssignedCarNumber())
                                        ? r.getTeamName()+ carQualifier
                                        : "#" + r.getAssignedCarNumber() + " " + r.getTeamName() + carQualifier);
                        infoView.setExecutableByUser(registration.get().getCreatedBy().getId() == currentPerson.getId());
                    }
            );
        }
        List<WorkflowStateInfoView> statesForPerson = infoView.getTargetStates().stream()
                .filter(state -> state.getDutyRoles().contains(currentPerson.getRole().toString()))
                .collect(Collectors.toList());
        infoView.setTargetStates(statesForPerson);
        return infoView;
    }
}
