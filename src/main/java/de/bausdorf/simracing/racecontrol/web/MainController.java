package de.bausdorf.simracing.racecontrol.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.bausdorf.simracing.racecontrol.model.Session;
import de.bausdorf.simracing.racecontrol.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.model.TeamRepository;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.model.CssClassType;
import de.bausdorf.simracing.racecontrol.web.model.SessionOptionView;
import de.bausdorf.simracing.racecontrol.web.model.SessionSelectView;
import de.bausdorf.simracing.racecontrol.web.model.SessionView;
import de.bausdorf.simracing.racecontrol.web.model.TableCellView;
import de.bausdorf.simracing.racecontrol.web.model.TeamView;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MainController {
	public static final String INDEX_VIEW = "index";
	public static final String SESSION_VIEW = "timetable";

	final SessionRepository sessionRepository;
	final TeamRepository teamRepository;

	public MainController(@Autowired SessionRepository sessionRepository,
		@Autowired TeamRepository teamRepository) {
		this.sessionRepository = sessionRepository;
		this.teamRepository = teamRepository;
	}

	@GetMapping({"/", "/index", "index.html"})
	public String index(Model model) {
		List<SessionOptionView> sessionOptions = new ArrayList<>();
		SessionSelectView selectView = SessionSelectView.builder()
				.selectedSessionId("")
				.sessions(sessionOptions)
				.build();
		for(Session session :  sessionRepository.findAll()) {
			sessionOptions.add(SessionOptionView.builder()
					.sessionId(session.getSessionId())
					.displayName(session.getTrackName() + " "
							+ session.getSessionDuration().toHours() + "h "
							+ session.getSessionType())
					.build());
		}
		model.addAttribute("selectView", selectView);
		return INDEX_VIEW;
	}

	@PostMapping({"/session"})
	public String session(SessionSelectView selectView, Model model) {

		try {
			return "redirect:session?sessionId=" + URLEncoder.encode(selectView.getSelectedSessionId(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return INDEX_VIEW;
	}

	@GetMapping({"/session"})
	public String session(@RequestParam String sessionId, Model model) {

		List<Team> teamsInSession = teamRepository.findBySessionIdOrderByNameAsc(sessionId);
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		if(selectedSession.isPresent()) {
			SessionView sessionView = SessionView.builder()
					.sessionId(selectedSession.get().getSessionId())
					.sessionDuration(TableCellView.builder()
							.value(TimeTools.shortDurationString(selectedSession.get().getSessionDuration()))
							.displayType(CssClassType.DEFAULT)
							.build())
					.sessionType(TableCellView.builder()
							.value(selectedSession.get().getSessionType())
							.displayType(CssClassType.DANGER)
							.build())
					.build();

			sessionView.setTeams(teamsInSession.stream()
					.map(s -> TeamView.buildFromTeamList(s))
					.collect(Collectors.toList()));

			model.addAttribute("sessionView", sessionView);

		}
		return SESSION_VIEW;
	}
}
