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
import de.bausdorf.simracing.racecontrol.orga.model.EventSeriesRepository;
import de.bausdorf.simracing.racecontrol.web.model.orga.EventInfoView;
import de.bausdorf.simracing.racecontrol.web.model.live.SessionOptionView;
import de.bausdorf.simracing.racecontrol.web.model.live.SessionSelectView;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class IndexController extends ControllerBase {
    public static final String INDEX_VIEW = "index";
    public static final String USER_ID_PARAM = "userId=";
    public static final String SESSION_ID_PARAM = "sessionId=";

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
              eventRepository.findAllByRegistrationOpensBeforeAndEndDateAfterAndActiveOrderByStartDateAsc(
                      OffsetDateTime.now(), LocalDate.now(), true));
        eventInfoViews.forEach(e -> e.setAvailableSlots(eventOrganizer.getAvailableGridSlots(e.getEventId())));
        model.addAttribute("eventViews", eventInfoViews);
        return INDEX_VIEW;
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
}
