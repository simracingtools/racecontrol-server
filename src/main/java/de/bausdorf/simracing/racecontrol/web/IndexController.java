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
import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeriesRepository;
import de.bausdorf.simracing.racecontrol.orga.model.Person;
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistration;
import de.bausdorf.simracing.racecontrol.web.model.orga.EventInfoView;
import de.bausdorf.simracing.racecontrol.web.model.live.SessionOptionView;
import de.bausdorf.simracing.racecontrol.web.model.live.SessionSelectView;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class IndexController extends ControllerBase {
    public static final String INDEX_VIEW = "index";
    public static final String USER_ID_PARAM = "userId=";
    public static final String SESSION_ID_PARAM = "sessionId=";
    public static final String EVENT_ID_PARAM = "?eventId=";

    private final SessionRepository sessionRepository;
    private final EventSeriesRepository eventRepository;
    private final EventOrganizer eventOrganizer;

    public IndexController(@Autowired SessionRepository sessionRepository,
                           @Autowired EventSeriesRepository eventRepository,
                           @Autowired EventOrganizer eventOrganizer) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.eventOrganizer = eventOrganizer;
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

        List<EventInfoView> eventInfoViews = EventInfoView.fromEntityList(
              eventRepository.findAllByEndDateAfterAndActiveOrderByStartDateAsc(LocalDate.now(), true));
        eventInfoViews.forEach(e -> {
            e.setAvailableSlots(eventOrganizer.getAvailableGridSlots(e.getEventId()));
            e.setUserRegistrations(eventOrganizer.myRegistrations(e.getEventId(), currentUser()).stream()
                            .filter(IndexController.distinctByKey(TeamRegistration::getTeamName))
                            .map(TeamRegistrationView::fromEntity)
                            .collect(Collectors.toList()));
        });
        model.addAttribute("eventViews", eventInfoViews);
        model.addAttribute("teamRegistrationSelectView", new TeamRegistrationSelectView());
        return INDEX_VIEW;
    }

    @GetMapping("/login-redirect")
    @Secured(value={"ROLE_USER"})
    public String determineLocationAfterLogin(Model model) {
        RcUser currentUser = currentUser();
        if(currentUser.getTimezone() == null) {
            return super.redirectView("profile");
        }
        List<EventSeries> events = eventOrganizer.myActiveEvents(currentUser);
        EventSeries closestEvent = eventOrganizer.closestEventByNow(currentUser, events);
        if(closestEvent != null) {
            Person person = eventOrganizer.getPersonInEvent(closestEvent.getId(), currentUser.getIRacingId());
            if(person != null) {
                return redirectView("event-detail")
                        + EVENT_ID_PARAM + closestEvent.getId()
                        + "&activeTab=" + (person.getRole().isParticipant() ? "teams" : "tasks");
            }
        }
        return redirectView(INDEX_VIEW);
    }

    @PostMapping("/register-car-for-team")
    public String forwardTeamRegistration(@ModelAttribute TeamRegistrationSelectView teamRegistrationSelect) {
        return super.redirectView("team-registration")
                + EVENT_ID_PARAM + teamRegistrationSelect.getEventId()
                + "&teamId=" + teamRegistrationSelect.getTeamId();
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
        String redirectUri = "redirect:session?";
        if(!selectView.getUserId().isEmpty()) {
            redirectUri += USER_ID_PARAM + selectView.getUserId() + "&";
        }
        return redirectUri + SESSION_ID_PARAM + URLEncoder.encode(selectView.getSelectedSessionId(), StandardCharsets.UTF_8);
    }

    protected String redirectView(String viewName, long eventId, String encodedMessages) {
        return super.redirectView(viewName)
                + (eventId != 0 ? EVENT_ID_PARAM + eventId : "")
                + (StringUtils.isEmpty(activeNav) ? "" : "&activeTab=" + activeNav)
                + (encodedMessages != null ? "&messages=" + encodedMessages : "");
    }
    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
