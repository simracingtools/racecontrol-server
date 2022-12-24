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

import de.bausdorf.simracing.racecontrol.live.model.Session;
import de.bausdorf.simracing.racecontrol.live.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.util.ResultManager;
import de.bausdorf.simracing.racecontrol.web.model.orga.EventInfoView;
import de.bausdorf.simracing.racecontrol.web.model.live.SessionOptionView;
import de.bausdorf.simracing.racecontrol.web.model.live.SessionSelectView;
import de.bausdorf.simracing.racecontrol.web.model.orga.RaceSessionResultView;
import de.bausdorf.simracing.racecontrol.web.model.orga.TeamRegistrationSelectView;
import de.bausdorf.simracing.racecontrol.web.model.orga.TeamRegistrationView;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class IndexController extends ControllerBase {
    public static final String INDEX_VIEW = "index";
    public static final String USER_ID_PARAM = "userId";
    public static final String SESSION_ID_PARAM = "sessionId";
    public static final String EVENT_ID_PARAM = "eventId";
    public static final String RACE_RESULT_VIEW = "race-result";

    private final SessionRepository sessionRepository;
    private final EventSeriesRepository eventRepository;
    private final TrackSessionRepository trackSessionRepository;
    private final EventOrganizer eventOrganizer;
    private final ResultManager resultManager;

    public IndexController(@Autowired SessionRepository sessionRepository,
                           @Autowired EventSeriesRepository eventRepository,
                           @Autowired TrackSessionRepository trackSessionRepository,
                           @Autowired EventOrganizer eventOrganizer,
                           @Autowired ResultManager resultManager) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.trackSessionRepository = trackSessionRepository;
        this.eventOrganizer = eventOrganizer;
        this.resultManager = resultManager;
    }

    @GetMapping({"/", "/index", "index.html"})
    public String index(@RequestParam Optional<String> error,
                        @RequestParam Optional<String> userId,
                        @RequestParam Optional<String> messages, Model model) {
        messages.ifPresent(m -> decodeMessagesToModel(m, model));
        error.ifPresent(s -> addError(s, model));

        List<SessionOptionView> sessionOptions = new ArrayList<>();
        SessionSelectView selectView = SessionSelectView.builder()
                .userId(userId.orElse(currentUser().getOauthId()))
                .selectedSessionId("")
                .sessions(sessionOptions)
                .build();
        for(Session session :  sessionRepository.findAllByCreatedBeforeOrderByCreatedDesc(ZonedDateTime.now())) {
            sessionOptions.add(SessionOptionView.builder()
                    .sessionId(session.getSessionId())
                    .displayName(session.getCreated().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm "))
                            + session.getTrackName() + " "
                            + session.getSessionDuration().toHours() + "h "
                            + session.getSessionType())
                    .build());
        }
        model.addAttribute("selectView", selectView);

        // Fetch active events
        List<EventInfoView> activeEvents = EventInfoView.fromEntityList(
              eventRepository.findAllByEndDateAfterAndActiveOrderByStartDateAsc(LocalDate.now(), true));
        activeEvents.forEach(e -> {
            e.setAvailableSlots(eventOrganizer.getAvailableGridSlots(e.getEventId()));
            e.setUserRegistrations(eventOrganizer.myRegistrations(e.getEventId(), currentUser()).stream()
                            .filter(IndexController.distinctByKey(TeamRegistration::getTeamName))
                            .map(TeamRegistrationView::fromEntity)
                            .collect(Collectors.toList()));
        });

        // Fetch finished events
        AtomicReference<List<Long>> sessionResults = new AtomicReference<>(new ArrayList<>());
        List<EventInfoView> finishedEvents = EventInfoView.fromEntityList(
                eventRepository.findAllByEndDateBeforeAndActiveOrderByStartDateAsc(LocalDate.now(), true)
        );
        finishedEvents.forEach(e -> {
            e.setAvailableSlots(eventOrganizer.getFilledGridSlots(e.getEventId()));
            sessionResults.set(trackSessionRepository.findAllByEventId(e.getEventId()).stream()
                    .filter(session -> !session.isPermitSession())
                    .map(TrackSession::getIrSessionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        });

        model.addAttribute("eventViews", activeEvents);
        model.addAttribute("eventViewsFinished", finishedEvents);
        model.addAttribute("raceResultIds", sessionResults.get());
        model.addAttribute("teamRegistrationSelectView", new TeamRegistrationSelectView());
        return INDEX_VIEW;
    }

    @GetMapping("/race-result")
    public String getRaceResult(@RequestParam Long eventId, @RequestParam Long subsessionId, Model model) {
        RaceSessionResultView resultView = resultManager.getRaceSessionResultView(eventId, subsessionId);
        if (resultView != null) {
            model.addAttribute("resultsView", resultView);
            return RACE_RESULT_VIEW;
        }

        return redirectBuilder(INDEX_VIEW).build(model);
    }

    @GetMapping("/login-redirect")
    @Secured(value={"ROLE_USER"})
    public String determineLocationAfterLogin(Model model) {
        RcUser currentUser = currentUser();
        if(currentUser.getTimezone() == null) {
            return redirectBuilder("profile").build(model);
        }
        List<EventSeries> events = eventOrganizer.myActiveEvents(currentUser);
        EventSeries closestEvent = eventOrganizer.closestEventByNow(currentUser, events);
        if(closestEvent != null) {
            Person person = eventOrganizer.getPersonInEvent(closestEvent.getId(), currentUser.getIRacingId());
            if(person != null) {
                return redirectBuilder("event-detail")
                        .withParameter(EVENT_ID_PARAM, Long.toString(closestEvent.getId()))
                        .withParameter("activeTab", (person.getRole().isParticipant() ? "teams" : "tasks"))
                        .build(model);
            }
        }
        return redirectBuilder(INDEX_VIEW).build(model);
    }

    @PostMapping("/register-car-for-team")
    public String forwardTeamRegistration(@ModelAttribute TeamRegistrationSelectView teamRegistrationSelect, Model model) {
        return redirectBuilder("team-registration")
                .withParameter(EVENT_ID_PARAM, teamRegistrationSelect.getEventId())
                .withParameter("teamId", teamRegistrationSelect.getTeamId())
                .build(model);
    }
    @GetMapping("/logout")
    public String logout(HttpServletRequest servletRequest) throws ServletException {
        RefreshableKeycloakSecurityContext c =
                (RefreshableKeycloakSecurityContext) servletRequest.getAttribute(KeycloakSecurityContext.class.getName());
        KeycloakDeployment d = c.getDeployment();
        c.logout(d);
        servletRequest.logout();
        return "redirect:/";
    }

    @PostMapping({"/session"})
    public String session(SessionSelectView selectView) {
        return redirectBuilder("session")
                .withParameter(USER_ID_PARAM, selectView.getUserId())
                .withParameter(SESSION_ID_PARAM, URLEncoder.encode(selectView.getSelectedSessionId(), StandardCharsets.UTF_8))
                .build(null);
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
