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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.bausdorf.simracing.racecontrol.live.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.live.model.DriverRepository;
import de.bausdorf.simracing.racecontrol.live.model.EventRepository;
import de.bausdorf.simracing.racecontrol.live.model.Session;
import de.bausdorf.simracing.racecontrol.live.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.model.SessionAdminView;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class SessionAdminController extends ControllerBase {

	public static final String SESSION_ADMIN_VIEW = "sessionadmin";

	private final SessionRepository sessionRepository;
	private final DriverRepository driverRepository;
	private final DriverChangeRepository changeRepository;
	private final EventRepository eventRepository;

	public SessionAdminController(@Autowired SessionRepository sessionRepository,
			@Autowired DriverRepository driverRepository,
			@Autowired DriverChangeRepository changeRepository,
			@Autowired EventRepository eventRepository) {
		this.sessionRepository = sessionRepository;
		this.driverRepository = driverRepository;
		this.changeRepository = changeRepository;
		this.eventRepository = eventRepository;
	}

	@GetMapping("/sessionadmin")
	public String getSessionAdministration(@RequestParam Optional<String> error, Model model) {
		this.activeNav = "sessionAdmin";
		error.ifPresent(s -> addError(s, model));
		List<SessionAdminView> sessionViews = new ArrayList<>();
		for(Session session : sessionRepository.findAll()) {
			sessionViews.add(SessionAdminView.builder()
					.created(session.getCreated().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
					.trackName(session.getTrackName())
					.duration(TimeTools.shortDurationString(session.getSessionDuration()))
					.type(session.getSessionType())
					.sessionId(session.getSessionId())
					.build());
		}
		model.addAttribute("sessions", sessionViews);

		return SESSION_ADMIN_VIEW;
	}

	@GetMapping("/deletesession")
	@Transactional
	public String deleteSession(@RequestParam String sessionId) {

		try {
			changeRepository.deleteAllBySessionId(sessionId);
			driverRepository.deleteAllBySessionId(sessionId);
			eventRepository.deleteAllBySessionId(sessionId);
			sessionRepository.deleteById(sessionId);
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			return "redirect:/sessionadmin?error=" + e.getMessage();
		}
		return "redirect:/sessionadmin";
	}
}
