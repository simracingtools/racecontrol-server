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

import de.bausdorf.simracing.irdataapi.model.CarInfoDto;
import de.bausdorf.simracing.irdataapi.model.TrackInfoDto;
import de.bausdorf.simracing.irdataapi.tools.CarCategoryType;
import de.bausdorf.simracing.irdataapi.tools.StockDataTools;
import de.bausdorf.simracing.racecontrol.discord.JdaClient;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.util.FileTypeEnum;
import de.bausdorf.simracing.racecontrol.util.ResultManager;
import de.bausdorf.simracing.racecontrol.util.SessionManager;
import de.bausdorf.simracing.racecontrol.util.UploadFileManager;
import de.bausdorf.simracing.racecontrol.web.model.TimezoneView;
import de.bausdorf.simracing.racecontrol.web.model.TrackConfigurationView;
import de.bausdorf.simracing.racecontrol.web.model.TrackView;
import de.bausdorf.simracing.racecontrol.web.model.orga.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class EventAdminController extends ControllerBase {
    private static final String CREATE_EVENT_VIEW = "create-event";
    private static final String CREATE_SESSION_VIEW = "create-session";
    private static final String PERMIT_RESULT_VIEW = "permit-result";
    public static final String EVENT_DETAIL_VIEW = "event-detail";
    private static final String EVENT_VIEW_MODEL_KEY = "eventView";
    public static final String SESSION_ID_PARAM = "sessionId";
    public static final String SUBSESSION_EDIT_VIEW_KEY = "subsessionEditView";
    public static final String EVENT_ID_PARAM = "eventId";
    public static final String SESSION_EDIT_VIEW_KEY = "sessionEditView";
    public static final String DISCORD_CLEANUP_VIEW_KEY = "discordCleanupView";
    public static final String INDEX_VIEW = "index";

    private final ResultManager resultManager;
    private final SessionManager sessionManager;
    private final EventSeriesRepository eventRepository;
    private final CarClassRepository carClassRepository;
    private final BalancedCarRepository balancedCarRepository;
    private final PersonRepository personRepository;
    private final TeamRegistrationRepository registrationRepository;
    private final TrackSessionRepository sessionRepository;
    private final TrackSubsessionRepository subsessionRepository;
    private final IRacingClient iRacingClient;
    private final UploadFileManager uploadFileManager;
    private final JdaClient jdaClient;

    public EventAdminController(@Autowired ResultManager resultManager,
                                @Autowired SessionManager sessionManager,
                                @Autowired EventSeriesRepository eventRepository,
                                @Autowired CarClassRepository carClassRepository,
                                @Autowired BalancedCarRepository balancedCarRepository,
                                @Autowired PersonRepository personRepository,
                                @Autowired TeamRegistrationRepository registrationRepository,
                                @Autowired TrackSessionRepository sessionRepository,
                                @Autowired TrackSubsessionRepository subsessionRepository,
                                @Autowired UploadFileManager uploadFileManager,
                                @Autowired IRacingClient iRacingClient,
                                @Autowired JdaClient jdaClient) {
        this.resultManager = resultManager;
        this.sessionManager = sessionManager;
        this.eventRepository = eventRepository;
        this.carClassRepository = carClassRepository;
        this.balancedCarRepository = balancedCarRepository;
        this.personRepository = personRepository;
        this.registrationRepository = registrationRepository;
        this.sessionRepository = sessionRepository;
        this.subsessionRepository = subsessionRepository;
        this.uploadFileManager = uploadFileManager;
        this.iRacingClient = iRacingClient;
        this.jdaClient = jdaClient;
    }

    @GetMapping("/create-event")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    public String createEvent(@RequestParam Optional<Long> eventId, @RequestParam Optional<String> messages, Model model) {
        messages.ifPresent(e -> decodeMessagesToModel(e, model));

        if (eventId.isPresent()) {
            Optional<EventSeries> eventSeries = eventRepository.findById(eventId.get());
            if(eventSeries.isPresent()) {
                model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.fromEntity(eventSeries.get()));
                model.addAttribute(DISCORD_CLEANUP_VIEW_KEY, getDiscordTeamCategories(eventSeries.get()));
            } else {
                addWarning("Event with id " + eventId.get() + " not found", model);
                model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.createEmpty());
                model.addAttribute(DISCORD_CLEANUP_VIEW_KEY, DiscordCleanupView.buildEmpty());
            }
        } else {
            model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.createEmpty());
            model.addAttribute(DISCORD_CLEANUP_VIEW_KEY, DiscordCleanupView.buildEmpty());
        }
        model.addAttribute("editCarClassView", EditCarClassView.builder()
                        .eventId(eventId.orElse(0L))
                        .classOrder(99)
                        .build());
        model.addAttribute("editStaffView", PersonView.builder()
                        .eventId(eventId.orElse(0L))
                        .build());

        return CREATE_EVENT_VIEW;
    }

    @PostMapping("/create-event")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String createEvent(@ModelAttribute CreateEventView eventView, Model model) {
        EventSeries eventSeriesToSave = null;
        if (eventView.getEventId() != 0) {
            Optional<EventSeries> eventSeries = eventRepository.findById(eventView.getEventId());
            if (eventSeries.isEmpty()) {
                addError("Event series with id " + eventView.getEventId() + " does not exist", model);
            } else {
                eventSeriesToSave = eventSeries.get();
            }
        }
        if(eventSeriesToSave != null && checkDiscord(eventSeriesToSave, model)) {
            eventSeriesToSave = eventRepository.save(eventView.toEntity(eventSeriesToSave));
            return redirectView(eventSeriesToSave.getId(), model);
        }
        if(eventSeriesToSave == null) {
            eventSeriesToSave = eventRepository.save(eventView.toEntity(eventSeriesToSave));
            checkDiscord(eventSeriesToSave, model);
            return redirectView(eventSeriesToSave.getId(), model);
        }
        return redirectView(0L, model);
    }

    @PostMapping("/event-save-carclass")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String addCarClass(@ModelAttribute EditCarClassView carClassView, Model model) {
        if(carClassView.getId() != 0) {
            CarClass carClass = carClassRepository.findById(carClassView.getId()).orElse(null);
            if(carClass != null) {
                if(carsReferencedInEvent(carClass)) {
                    addWarning("Cars of " + carClass.getName() + " class are referenced in event, cars will not be changed", model);
                    carClassRepository.save(carClassView.toEntity(carClass, false));
                } else {
                    balancedCarRepository.deleteAllByCarClassId(carClass.getId());
                    carClassRepository.save(updateCarDataIRacingReferences(carClassView.toEntity(carClass, true)));
                }
            } else {
                addError("Car class with id " + carClassView.getId() + " not found", model);
            }
        } else {
            Optional<EventSeries> eventSeriesOptional = eventRepository.findById(carClassView.getEventId());
            eventSeriesOptional.ifPresentOrElse(eventSeries -> {
                        CarClass carClass = carClassView.toEntity(null, true);
                        CarClass finalCarClass = updateCarDataIRacingReferences(carClassRepository.save(carClass));
                        eventSeries.getCarClassPreset().add(finalCarClass);
                        eventRepository.save(eventSeries);
                    },
                    () -> addError("No event series with id " + carClassView.getEventId() + " found.", model));
        }
        return redirectView(carClassView.getEventId(), model);
    }

    @GetMapping("/remove-car-class")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String removeCarClass(@RequestParam long classId, Model model) {
        Optional<CarClass> carClass = carClassRepository.findById(classId);
        AtomicLong eventId = new AtomicLong();
        carClass.ifPresentOrElse(
                cc -> {
                    eventId.set(cc.getEventId());
                    carClassRepository.delete(cc);
                },
                () -> addError("No car class found for id " + classId, model)
        );
        return redirectView(eventId.get(), model);
    }

    @PostMapping("/event-logo-upload")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String eventLogoUpload(@RequestParam("file") MultipartFile multipartFile,
                                  @RequestParam(EVENT_ID_PARAM) String eventId, Model model) {
        Optional<EventSeries> series = eventRepository.findById(Long.parseLong(eventId));
        series.ifPresentOrElse(
                event -> {
                    try {
                        String logoUrl = uploadFileManager.uploadEventFile(multipartFile, eventId, FileTypeEnum.LOGO);
                        event.setLogoUrl(logoUrl);
                        eventRepository.save(event);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        addError("Could not save uploaded file " + multipartFile.getOriginalFilename(), model);
                    }
                },
                () -> addError("Event series not found for id " + eventId, model)
        );
        return redirectView(Long.parseLong(eventId), model);
    }

    @PostMapping("/event-save-person")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String saveStaffPerson(@ModelAttribute PersonView personView) {
        Optional<Person> staff = personRepository.findByEventIdAndIracingId(personView.getEventId(), personView.getIracingId());
        Person toSave = personView.toEntity(staff.orElse(new Person()));
        personRepository.save(toSave);
        return redirectView(personView.getEventId(), null);
    }

    @GetMapping("remove-staff")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String removeStaffPerson(@RequestParam long personId, Model model) {
        Optional<Person> person = personRepository.findById(personId);
        AtomicLong eventId = new AtomicLong(0L);
        person.ifPresentOrElse(
                p -> {
                    eventId.set(p.getEventId());
                    personRepository.delete(p);
                },
                () -> addError("No person found for id " + personId, model)
        );
        return redirectView(eventId.get(), model);
    }

    @PostMapping("/delete-from-discord")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    public String cleanupDiscord(@ModelAttribute DiscordCleanupView discordCleanupView, Model model) {
        Guild guild = jdaClient.getApi().getGuildById(discordCleanupView.getServerId());
        if(guild != null) {
            for(String id : discordCleanupView.getSelectedItems()) {
                Messages messages = ((Messages)model.getAttribute(MESSAGES));
                if(messages != null && messages.size() > 10) {
                    break;
                }
                String[] idParts = id.split(":");
                if(idParts.length == 2) {
                    // id has format [itemId]:[categoryId] -> item is a channel
                    deleteChannel(guild, idParts, model);
                } else if(idParts.length == 1) {
                    // id plain number -> item is a role
                    deleteRole(guild, idParts[0], model);
                }
            }
        }
        return redirectView(discordCleanupView.getEventId(), model);
    }

    @GetMapping("/create-session")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
    public String editSession(@RequestParam long eventId, @RequestParam Optional<Long> sessionId, @RequestParam Optional<String> messages, Model model) {
        messages.ifPresent(e -> decodeMessagesToModel(e, model));

        EventSeries series = eventRepository.findById(eventId).orElse(null);
        if(series == null) {
            addError("Event not found for id " + eventId, model);
            return redirectBuilder(INDEX_VIEW).build(model);
        }
        sessionId.ifPresentOrElse(sid -> sessionRepository.findById(sid).ifPresentOrElse(
                        session -> {
                            CreateSessionView sessionView = CreateSessionView.fromEntity(session);
                            model.addAttribute(SESSION_EDIT_VIEW_KEY, sessionView);
                            model.addAttribute(SUBSESSION_EDIT_VIEW_KEY, TrackSubsessionView.builder()
                                            .eventId(eventId)
                                            .trackSessionId(session.getId())
                                            .build());
                        },
                        () -> {
                            CreateSessionView sessionView = CreateSessionView.builder()
                                    .eventId(eventId)
                                    .datetime(LocalDateTime.of(series.getStartDate(), LocalTime.NOON))
                                    .simulatedTimeOfDay(LocalDateTime.of(series.getStartDate(), LocalTime.NOON))
                                    .sessionParts(new ArrayList<>())
                                    .build();
                            model.addAttribute(SESSION_EDIT_VIEW_KEY, sessionView);
                            model.addAttribute(SUBSESSION_EDIT_VIEW_KEY, TrackSubsessionView.builder()
                                            .eventId(eventId)
                                            .build());

                        }
                ),
                () -> {
                    CreateSessionView sessionView = CreateSessionView.builder()
                            .eventId(eventId)
                            .datetime(LocalDateTime.of(series.getStartDate(), LocalTime.NOON))
                            .zoneOffset(series.getRegistrationCloses().getOffset().getId())
                            .simulatedTimeOfDay(LocalDateTime.of(series.getStartDate(), LocalTime.NOON))
                            .sessionParts(new ArrayList<>())
                            .build();
                    model.addAttribute(SESSION_EDIT_VIEW_KEY, sessionView);
                    model.addAttribute(SUBSESSION_EDIT_VIEW_KEY, TrackSubsessionView.builder()
                                    .eventId(eventId)
                                    .build());
                });

        return CREATE_SESSION_VIEW;
    }

    @PostMapping("/save-session")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
    public String saveSession(@ModelAttribute CreateSessionView sessionEditView, Model model) {
        TrackSession existingSession = sessionRepository.findById(sessionEditView.getId()).orElse(null);
        long existingIrSessionId = (existingSession != null && existingSession.getIrSessionId() != null) ? existingSession.getIrSessionId() : 0L;
        TrackSession trackSession = sessionEditView.toEntity(existingSession);

        if (existingIrSessionId == 0L && (trackSession.getIrSessionId() != null || Long.valueOf(0L).equals(trackSession.getIrSessionId()))) {
            log.info("Tying to fetch result information on {}({})})", trackSession.getTitle(), trackSession.getIrSessionId());
            // Fetch sessionResults
            if (trackSession.isPermitSession()) {
                resultManager.fetchPermitSessionResult(trackSession.getEventId(), trackSession.getIrSessionId(), trackSession);
                resultManager.updatePermissions(trackSession.getEventId(), trackSession.getIrSessionId());
            } else {
                log.info("Session is no permit session");
            }
        }
        trackSession = sessionRepository.save(trackSession);

        return redirectBuilder(CREATE_SESSION_VIEW)
                .withParameter(EVENT_ID_PARAM, trackSession.getEventId())
                .withParameter(SESSION_ID_PARAM, trackSession.getId())
                .build(model);
    }

    @GetMapping("/refetch-session")
    public String refectchSession(@RequestParam Long eventId, @RequestParam Long irSessionid, Model model) {
        TrackSession trackSession = sessionRepository.findByEventIdAndIrSessionId(eventId, irSessionid).orElse(null);
        if (trackSession != null) {
            if (trackSession.isPermitSession()) {
                resultManager.updatePermissions(trackSession.getEventId(), trackSession.getIrSessionId());
            }
            return redirectBuilder(CREATE_SESSION_VIEW)
                    .withParameter(EVENT_ID_PARAM, trackSession.getEventId())
                    .withParameter(SESSION_ID_PARAM, trackSession.getId())
                    .build(model);
        }

        return redirectBuilder(INDEX_VIEW).build(model);
    }

    @GetMapping("/fetch-league-sessions")
    public String fetchLeagueSessions(@RequestParam Long eventId, Model model) {
        sessionManager.fetchFutureTrackSessions(eventId);

        return redirectBuilder(EVENT_DETAIL_VIEW)
                .withParameter(EVENT_ID_PARAM, eventId)
                .withParameter("activeTab", "tasks")
                .build(model);
    }

    @PostMapping("/duplicate-session")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
    public String duplicateSession(@ModelAttribute CreateSessionView sessionEditView, Model model) {
        TrackSession existingSession = sessionRepository.findById(sessionEditView.getId()).orElse(null);
        sessionEditView.setId(0);
        TrackSession trackSession = sessionEditView.toEntity(null);
        trackSession = sessionRepository.save(trackSession);

        final long tracksessionId = trackSession.getId();
        final List<TrackSubsession> clonedSubsessions = new ArrayList<>();
        if(existingSession != null) {
            List<TrackSubsession> originalSubsessions = existingSession.getSessionParts();
            originalSubsessions.forEach(subSession -> {
                TrackSubsession session = new TrackSubsession();
                session.setTrackSessionId(tracksessionId);
                session.setSessionType(subSession.getSessionType());
                session.setDuration(subSession.getDuration());
                session.setIrSubsessionId(subSession.getIrSubsessionId());
                clonedSubsessions.add(subsessionRepository.save(session));
            });

            trackSession.setSessionParts(clonedSubsessions);
            sessionRepository.save(trackSession);
        }
        return redirectBuilder(CREATE_SESSION_VIEW)
                .withParameter(EVENT_ID_PARAM, trackSession.getEventId())
                .withParameter(SESSION_ID_PARAM, trackSession.getId())
                .build(model);
    }

    @PostMapping("/save-subsession")
    public String saveSubsession(@ModelAttribute TrackSubsessionView subsessionEditView, Model model) {
        TrackSubsession trackSubsession = subsessionEditView.toEntity(subsessionRepository.findById(subsessionEditView.getId()).orElse(null));
        trackSubsession = subsessionRepository.save(trackSubsession);

        return redirectBuilder(CREATE_SESSION_VIEW)
                .withParameter(EVENT_ID_PARAM, subsessionEditView.getEventId())
                .withParameter(SESSION_ID_PARAM, trackSubsession.getTrackSessionId())
                .build(model);
    }

    @GetMapping("/remove-subsession")
    public String removeSubsession(@RequestParam long subSessionId, Model model) {
        TrackSubsession subsession = subsessionRepository.findById(subSessionId).orElse(null);
        if(subsession != null) {
            TrackSession trackSession = sessionRepository.findById(subsession.getTrackSessionId()).orElse(null);
            if(trackSession != null) {
                subsessionRepository.deleteById(subsession.getId());
                return redirectBuilder(CREATE_SESSION_VIEW)
                        .withParameter(EVENT_ID_PARAM, trackSession.getEventId())
                        .withParameter(SESSION_ID_PARAM, trackSession.getId())
                        .build(model);
            } else {
                addError("No track session for id " + subsession.getTrackSessionId(), model);
            }
        } else {
            addError("No subsession for id " + subSessionId, model);
        }
        return redirectBuilder(INDEX_VIEW).build(model);
    }

    @GetMapping("/session-result")
    public String showSessionResult(@RequestParam long eventId, @RequestParam long irSessionId, @RequestParam String activeTab, Model model) {
        Optional<TrackSession> trackSession = sessionRepository.findByEventIdAndIrSessionId(eventId, irSessionId);
        if(trackSession.isPresent() && trackSession.get().isPermitSession()) {
            PermitSessionResultView permitSessionResultView = resultManager.getPermitSessionResultView(eventId, irSessionId, trackSession.get());
            model.addAttribute("resultsView", permitSessionResultView);
            model.addAttribute("navigation", activeTab);

            return PERMIT_RESULT_VIEW;
        }
        return redirectBuilder(EVENT_DETAIL_VIEW)
                .withParameter(EVENT_ID_PARAM, eventId)
                .withParameter("activeTab", activeTab)
                .build(model);
    }


    @ModelAttribute("allTracks")
    public List<TrackView> getAllTracks() {
        Map<String, TrackView> trackConfigMap = new TreeMap<>();
        Arrays.stream(iRacingClient.getDataCache().getTracks())
                .filter(track -> Arrays.stream(track.getTrackTypes()).anyMatch(type -> "road".equalsIgnoreCase(type.getTrackType())))
                .sorted(Comparator.comparing(TrackInfoDto::getTrackName))
                .forEach(trackConfig -> {
                    TrackView trackView = trackConfigMap.computeIfAbsent(trackConfig.getTrackName(),
                            k -> TrackView.builder()
                                    .name(trackConfig.getTrackName())
                                    .pkgId(trackConfig.getPackageId())
                                    .configViewList(new ArrayList<>())
                                    .build());

                    trackView.getConfigViewList().add(TrackConfigurationView.builder()
                                    .trackId(trackConfig.getTrackId())
                                    .configName(StringUtils.isEmpty(trackConfig.getConfigName()) ? trackConfig.getTrackName() : trackConfig.getConfigName())
                                    .track(trackView)
                                    .build());
                });

        return trackConfigMap.values().stream().sorted(Comparator.comparing(TrackView::getName)).collect(Collectors.toList());
    }

    @ModelAttribute(name="allCars")
    public List<CarView> getAllCars() {
        return StockDataTools.carsByCategory(iRacingClient.getDataCache().getCars(), CarCategoryType.ROAD, false).stream()
                .filter(car -> Arrays.stream(car.getCarTypes()).anyMatch(type -> type.getCarType().equalsIgnoreCase("road")))
                .map(car -> CarView.builder()
                        .carId(car.getCarId())
                        .name(car.getCarName())
                        .build())
                .sorted(Comparator.comparing(CarView::getName))
                .collect(Collectors.toList());
    }

    @ModelAttribute("timezones")
    List<TimezoneView> availableZoneIds() {
        return ZoneId.getAvailableZoneIds().stream()
                .filter(s -> s.chars().noneMatch(Character::isLowerCase))
                .map(s -> TimezoneView.fromZoneId(ZoneId.of(s)))
                .sorted(Comparator.comparing(TimezoneView::getUtcOffset))
                .collect(Collectors.toList());
    }

    @ModelAttribute(name="staffRoles")
    public List<OrgaRoleType> staffRoles() {
        return OrgaRoleType.racecontrolValues();
    }

    private String redirectView(long eventId, Model model) {
        return redirectBuilder(EventAdminController.CREATE_EVENT_VIEW)
                .withParameter(EVENT_ID_PARAM, eventId)
                .build(model);
    }

    private boolean carsReferencedInEvent(CarClass carClass) {
        AtomicBoolean referenceExists = new AtomicBoolean(false);
        for(BalancedCar car : carClass.getCars()) {
            List<TeamRegistration> referencingTeams = registrationRepository.findAllByEventIdAndCar(carClass.getEventId(), car);
            if(!referencingTeams.isEmpty()) {
                referenceExists.set(true);
                break;
            }
        }
        return referenceExists.get();
    }

    private CarClass updateCarDataIRacingReferences(CarClass carClass) {
        carClass.getCars().forEach(bc -> {
            Optional<CarInfoDto> carInfo = Arrays.stream(iRacingClient.getDataCache().getCars())
                    .filter(c -> c.getCarId().equals(bc.getCarId()))
                    .findFirst();
            carInfo.ifPresent(ci -> {
                bc.setCarName(ci.getCarName());
                bc.setCarClassId(carClass.getId());
            });
        });
        return carClass;
    }

    private void deleteOnDiscord(Channel channel, Guild guild, String categoryId) {
        channel.delete().complete();
        Category category = guild.getCategoryById(categoryId);
        if(category != null && category.getChannels().isEmpty()) {
            category.delete().complete();
        }
    }

    private void deleteChannel(Guild guild, String[] idParts, Model model) {
        GuildChannel channel = guild.getChannelById(GuildChannel.class, idParts[0]);
        if (channel != null) {
            try {
                deleteOnDiscord(channel, guild, idParts[1]);
            } catch (IllegalStateException | InsufficientPermissionException e) {
                log.error(e.getMessage());
                addError("Cannot delete category/channel " + channel.getName() + ": " + e.getMessage(), model);
            }
        }
    }

    private void deleteRole(Guild guild, String roleId, Model model) {
        Role role = guild.getRoleById(roleId);
        if (role != null) {
            try {
                role.delete().complete();
            } catch (IllegalStateException | InsufficientPermissionException e) {
                log.error(e.getMessage());
                addError("Cannot delete role " + roleId + ": " + e.getMessage(), model);
            }
        }
    }

    private boolean checkDiscord(@NonNull EventSeries eventSeriesToSave, Model model) {
        boolean discordOk = true;
        if(eventSeriesToSave.getDiscordGuildId() != 0) {
            Guild guild = jdaClient.getApi().getGuildById(eventSeriesToSave.getDiscordGuildId());
            if (guild != null) {
                if(!checkSpacerCategory(eventSeriesToSave, guild, model) || !checkpresetChannel(eventSeriesToSave, guild, model)) {
                    discordOk = false;
                }
            } else {
                addError("Discord server id invalid", model);
            }
        }
        return discordOk;
    }

    private boolean checkSpacerCategory(EventSeries eventSeriesToSave, Guild guild, Model model) {
        boolean discordOk = true;
        if (eventSeriesToSave.getDiscordSpacerCategoryId() != 0) {
            Category cat = guild.getCategoryById(eventSeriesToSave.getDiscordSpacerCategoryId());
            if (cat == null) {
                discordOk = false;
                addError("Discord spacer category is invalid", model);
            }
        }
        return discordOk;
    }

    private boolean checkpresetChannel(EventSeries eventSeriesToSave, Guild guild, Model model) {
        boolean discordOk = true;
        if (eventSeriesToSave.getDiscordPresetChannelId() != 0) {
            MessageChannel chan = guild.getTextChannelById(eventSeriesToSave.getDiscordPresetChannelId());
            if (chan == null) {
                discordOk = false;
                addError("Discord preset channel does not exist", model);
            }
        }
        return discordOk;
    }

    private DiscordCleanupView getDiscordTeamCategories(EventSeries event) {
        List<TeamRegistration> registrations = registrationRepository.findAllByEventId(event.getId());
        Guild guild = jdaClient.getApi().getGuildById(event.getDiscordGuildId());
        List<Role> guildRoles = guild != null ? guild.getRoles() : new ArrayList<>();
        return DiscordCleanupView.builder()
                .categories(jdaClient.getTeamCategories(event.getDiscordGuildId(), event.getDiscordSpacerCategoryId()).stream()
                        .map(category -> {
                            DiscordItemView categoryView = DiscordItemView.builder()
                                    .id(category.getIdLong())
                                    .name(category.getName())
                                    .type(DiscordItemView.DiscordItemType.CATEGORY)
                                    .children(category.getChannels().stream()
                                            .map(channel -> DiscordItemView.builder()
                                                    .id(channel.getIdLong())
                                                    .name(channel.getName())
                                                    .type(channel.getType() == ChannelType.TEXT ? DiscordItemView.DiscordItemType.TEXT_CHANNEL : DiscordItemView.DiscordItemType.VOICE_CHANNEL)
                                                    .children(List.of())
                                                    .inEvent(channelInEvent(registrations, category, channel))
                                                    .categoryId(category.getIdLong())
                                                    .build())
                                            .collect(Collectors.toList())
                                    )
                                    .build();
                            categoryView.setInEvent(categoryView.getChildren().stream().anyMatch(DiscordItemView::isInEvent));
                            return categoryView;
                        })
                        .collect(Collectors.toList())
                )
                .roles(guildRoles.stream()
                        .map(role -> DiscordItemView.builder()
                                    .id(role.getIdLong())
                                    .type(DiscordItemView.DiscordItemType.ROLE)
                                    .name(role.getName())
                                    .inEvent(roleInEvent(event, registrations, role))
                                    .build())
                        .collect(Collectors.toList())
                )
                .selectedItems(new ArrayList<>())
                .eventId(event.getId())
                .serverId(event.getDiscordGuildId())
                .build();
    }

    private boolean channelInEvent(List<TeamRegistration> allRegistrations, Category category, GuildChannel channel) {
        return allRegistrations.stream()
                .filter(r -> r.getTeamName().equalsIgnoreCase(category.getName()))
                .anyMatch(r -> {
                    String voiceChannelName = "#" + r.getAssignedCarNumber() + " voice";
                    if (channel.getType() == ChannelType.VOICE && channel.getName().equals(voiceChannelName)) {
                        return true;
                    }
                    return channel.getType() == ChannelType.TEXT && channel.getName().equals("text");
                });
    }

    private boolean roleInEvent(EventSeries event, List<TeamRegistration> allRegistrations, Role role) {
        List<String> carClassNames = carClassRepository.findAllByEventId(event.getId()).stream()
                .map(CarClass::getName)
                .collect(Collectors.toList());
        Guild guild = jdaClient.getApi().getGuildById(event.getDiscordGuildId());
        Member botMember = guild != null ? guild.getMember(jdaClient.getApi().getSelfUser()) : null;
        List<Role> guildRoles = botMember != null ? botMember.getRoles() : new ArrayList<>();
        return OrgaRoleType.racecontrolValues().stream().anyMatch(t -> t.discordRoleName().equalsIgnoreCase(role.getName()))
                || allRegistrations.stream().anyMatch(r -> r.getTeamName().equalsIgnoreCase(role.getName()))
                || role.isPublicRole()
                || guildRoles.contains(role)
                || role.getName().equalsIgnoreCase("Admin")
                || role.getName().equalsIgnoreCase("WaitingList")
                || carClassNames.contains(role.getName());
    }
}
