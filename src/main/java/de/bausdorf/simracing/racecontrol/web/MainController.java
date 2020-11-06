package de.bausdorf.simracing.racecontrol.web;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.bausdorf.simracing.racecontrol.live.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.live.model.EventRepository;
import de.bausdorf.simracing.racecontrol.live.model.Session;
import de.bausdorf.simracing.racecontrol.live.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.live.model.Team;
import de.bausdorf.simracing.racecontrol.live.model.TeamRepository;
import de.bausdorf.simracing.racecontrol.web.model.SessionOptionView;
import de.bausdorf.simracing.racecontrol.web.model.SessionSelectView;
import de.bausdorf.simracing.racecontrol.web.model.TeamDetailView;
import de.bausdorf.simracing.racecontrol.web.model.UserProfileView;
import de.bausdorf.simracing.racecontrol.web.security.GoogleUserService;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserType;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MainController extends ControllerBase {
	public static final String INDEX_VIEW = "index";
	public static final String SESSION_VIEW = "timetable";
	public static final String TEAM_VIEW = "teamtable";
	public static final String EVENT_VIEW = "eventtable";
	public static final String SELECTED_EVENTS = "selectedEvents";
	public static final String VIEW_MODE = "viewMode";
	public static final String SESSION_VIEW_ATTRIBUTE = "sessionView";

	final SessionRepository sessionRepository;
	final TeamRepository teamRepository;
	final DriverChangeRepository changeRepository;
	final EventRepository eventRepository;
	final ViewBuilder viewBuilder;

	public MainController(@Autowired SessionRepository sessionRepository,
			@Autowired TeamRepository teamRepository,
			@Autowired DriverChangeRepository changeRepository,
			@Autowired EventRepository eventRepository,
			@Autowired ViewBuilder viewBuilder) {
		this.sessionRepository = sessionRepository;
		this.teamRepository = teamRepository;
		this.changeRepository = changeRepository;
		this.eventRepository = eventRepository;
		this.viewBuilder = viewBuilder;
		this.activeNav = "sessionSelect";
	}

	@GetMapping({"/", "/index", "index.html"})
	public String index(@RequestParam Optional<String> error, @RequestParam Optional<String> userId, Model model) {
		error.ifPresent(s -> addError(s, model));
		currentUserProfile(userId, model);
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
		return INDEX_VIEW;
	}

	@PostMapping({"/session"})
	public String session(SessionSelectView selectView) {
		try {
			String redirectUri = "redirect:session?";
			if(!selectView.getUserId().isEmpty()) {
				redirectUri += "userId=" + selectView.getUserId() + "&";
			}
			return redirectUri + "sessionId=" + URLEncoder.encode(selectView.getSelectedSessionId(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return INDEX_VIEW;
	}

	@GetMapping({"/session"})
	public String session(@RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		currentUserProfile(userId, model);
		List<Team> teamsInSession = teamRepository.findBySessionIdOrderByNameAsc(sessionId);
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		if(selectedSession.isPresent()) {
			model.addAttribute(VIEW_MODE, "times");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), teamsInSession));
		} else {
			return "redirect:index" + userId.map(s -> "?userId=" + s).orElse("");
		}
		return SESSION_VIEW;
	}

	@GetMapping("/team")
	public String team(@RequestParam long teamId, @RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		RcUser user = currentUserProfile(userId, model);
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		Optional<Team> team = teamRepository.findBySessionIdAndIracingId(sessionId, teamId);
		if(selectedSession.isPresent() && team.isPresent()) {
			TeamDetailView teamView = viewBuilder.buildFromTeamView(
					viewBuilder.buildFromTeam(team.get()), selectedSession.get().getSessionId(), user);
			model.addAttribute(VIEW_MODE, "team");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), null));
			model.addAttribute("selectedTeam", teamView);
		} else {
			try {
				String redirectUri = "redirect:session?";
				if(userId.isPresent()) {
					redirectUri += "userId=" + userId.get() + "&";
				}
				return redirectUri + "sessionId=" + URLEncoder.encode(sessionId, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		return TEAM_VIEW;
	}

	@GetMapping("/events")
	public String getEvents(@RequestParam String teamId, @RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		RcUser user = currentUserProfile(userId, model);
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		List<Team> teamsInSession = teamRepository.findBySessionIdOrderByNameAsc(sessionId);
		if(selectedSession.isPresent()) {
			model.addAttribute(VIEW_MODE, "events");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), teamsInSession));
			model.addAttribute("selectedTeamId", teamId);
			if("All".equalsIgnoreCase(teamId)) {
				model.addAttribute(SELECTED_EVENTS, viewBuilder.buildFromEventList(eventRepository
						.findTop100BySessionIdOrderBySessionTimeDesc(sessionId), user));
			} else {
				model.addAttribute(SELECTED_EVENTS, viewBuilder.buildFromEventList(eventRepository
						.findBySessionIdAndTeamIdOrderBySessionTimeDesc(sessionId, Long.parseLong(teamId)), user));
			}
		} else {
			return "redirect:index" + userId.map(s -> "?userId=" + s).orElse("");
		}
		return EVENT_VIEW;
	}

	private RcUser currentUserProfile(Optional<String> userId, Model model) {
		if(userId.isPresent()) {
			Optional<RcUser> currentUser = userRepository.findById(userId.get());
			RcUser user = currentUser.orElse(RcUser.builder()
					.name("Unknown")
					.created(ZonedDateTime.now())
					.eventFilter(GoogleUserService.defaultEventFilter())
					.userType(RcUserType.NEW)
					.build());
			model.addAttribute("user", new UserProfileView(user));
			return user;
		}
		return RcUser.builder()
				.name("Unknown")
				.eventFilter(GoogleUserService.defaultEventFilter())
				.created(ZonedDateTime.now())
				.userType(RcUserType.NEW)
				.build();
	}
}
