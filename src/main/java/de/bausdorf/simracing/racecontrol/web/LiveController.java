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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.bausdorf.simracing.racecontrol.discord.DiscordNotifier;
import de.bausdorf.simracing.racecontrol.live.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.live.model.EventRepository;
import de.bausdorf.simracing.racecontrol.model.RcBulletin;
import de.bausdorf.simracing.racecontrol.model.RcBulletinRepository;
import de.bausdorf.simracing.racecontrol.live.model.Session;
import de.bausdorf.simracing.racecontrol.live.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.live.model.Team;
import de.bausdorf.simracing.racecontrol.live.model.TeamRepository;
import de.bausdorf.simracing.racecontrol.orga.model.Penalty;
import de.bausdorf.simracing.racecontrol.orga.model.PenaltyRepository;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategory;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategoryRepository;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationRepository;
import de.bausdorf.simracing.racecontrol.web.model.PenaltySelectView;
import de.bausdorf.simracing.racecontrol.web.model.live.RcBulletinView;
import de.bausdorf.simracing.racecontrol.web.model.live.RcIssuedBulletinView;
import de.bausdorf.simracing.racecontrol.web.model.RuleViolationCategorySelectView;
import de.bausdorf.simracing.racecontrol.web.model.RuleViolationView;
import de.bausdorf.simracing.racecontrol.web.model.live.TeamDetailView;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserType;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class LiveController extends ControllerBase {
	public static final String SESSION_VIEW = "timetable";
	public static final String TEAM_VIEW = "teamtable";
	public static final String EVENT_VIEW = "eventtable";
	public static final String SELECTED_EVENTS = "selectedEvents";
	public static final String VIEW_MODE = "viewMode";
	public static final String SESSION_VIEW_ATTRIBUTE = "sessionView";
	public static final String SELECTED_TEAM_ID = "selectedTeamId";
	public static final String NEXT_BULLETIN_VIEW = "nextBulletinView";
	public static final String REDIRECT_INDEX = "redirect:index";
	public static final String PARAM_USER_ID = "?userId=";
	public static final String USER_ID_PARAM = "userId=";
	public static final String SESSION_ID_PARAM = "sessionId=";
	public static final String UTF_8 = "utf-8";

	final SessionRepository sessionRepository;
	final TeamRepository teamRepository;
	final DriverChangeRepository changeRepository;
	final EventRepository eventRepository;
	final RcBulletinRepository bulletinRepository;
	final RuleViolationRepository violationRepository;
	final RuleViolationCategoryRepository categoryRepository;
	final PenaltyRepository penaltyRepository;
	final ViewBuilder viewBuilder;
	final DiscordNotifier discordNotifier;

	public LiveController(@Autowired SessionRepository sessionRepository,
						  @Autowired TeamRepository teamRepository,
						  @Autowired DriverChangeRepository changeRepository,
						  @Autowired EventRepository eventRepository,
						  @Autowired RcBulletinRepository bulletinRepository,
						  @Autowired RuleViolationRepository violationRepository,
						  @Autowired RuleViolationCategoryRepository categoryRepository,
						  @Autowired PenaltyRepository penaltyRepository,
						  @Autowired ViewBuilder viewBuilder,
						  @Autowired DiscordNotifier discordNotifier) {
		this.sessionRepository = sessionRepository;
		this.teamRepository = teamRepository;
		this.changeRepository = changeRepository;
		this.eventRepository = eventRepository;
		this.bulletinRepository = bulletinRepository;
		this.violationRepository = violationRepository;
		this.categoryRepository = categoryRepository;
		this.penaltyRepository = penaltyRepository;
		this.viewBuilder = viewBuilder;
		this.discordNotifier = discordNotifier;
		this.activeNav = "sessionSelect";
	}

	@GetMapping({"/session"})
	public String session(@RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		List<Team> teamsInSession = teamRepository.findBySessionIdOrderByNameAsc(sessionId);
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		if(selectedSession.isPresent()) {
			model.addAttribute(VIEW_MODE, "times");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), teamsInSession));
			model.addAttribute(NEXT_BULLETIN_VIEW, prepareNextBulletin(sessionId));
		} else {
			return REDIRECT_INDEX + userId.map(s -> PARAM_USER_ID + s).orElse("");
		}
		return SESSION_VIEW;
	}

	@GetMapping("/team")
	public String team(@RequestParam long teamId, @RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		Optional<Team> team = teamRepository.findBySessionIdAndIracingId(sessionId, teamId);
		if(selectedSession.isPresent() && team.isPresent()) {
			TeamDetailView teamView = viewBuilder.buildFromTeamView(
					viewBuilder.buildFromTeam(team.get(), selectedSession.get().getEventId()), selectedSession.get().getSessionId(), currentUser());
			model.addAttribute(VIEW_MODE, "team");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), null));
			model.addAttribute(NEXT_BULLETIN_VIEW, prepareNextBulletin(sessionId));
			model.addAttribute("selectedTeam", teamView);
		} else {
			String redirectUri = "redirect:session?";
			if(userId.isPresent()) {
				redirectUri += USER_ID_PARAM + userId.get() + "&";
			}
			return redirectUri + SESSION_ID_PARAM + URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
		}
		return TEAM_VIEW;
	}

	@GetMapping("/events")
	public String getEvents(@RequestParam String teamId, @RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		RcUser user = currentUser();
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		List<Team> teamsInSession = teamRepository.findBySessionIdOrderByNameAsc(sessionId);
		if(selectedSession.isPresent()) {
			model.addAttribute(VIEW_MODE, "events");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), teamsInSession));
			model.addAttribute(NEXT_BULLETIN_VIEW, prepareNextBulletin(sessionId));
			model.addAttribute(SELECTED_TEAM_ID, teamId);
			if("All".equalsIgnoreCase(teamId)) {
				model.addAttribute(SELECTED_EVENTS, viewBuilder.buildFromEventList(eventRepository
						.findTop100BySessionIdOrderBySessionTimeDesc(sessionId), user));
			} else {
				model.addAttribute(SELECTED_EVENTS, viewBuilder.buildFromEventList(eventRepository
						.findBySessionIdAndTeamIdOrderBySessionTimeDesc(sessionId, Long.parseLong(teamId)), user));
			}
		} else {
			return REDIRECT_INDEX + userId.map(s -> PARAM_USER_ID + s).orElse("");
		}
		return EVENT_VIEW;
	}

	@GetMapping("/bulletins")
	@Transactional
	public String getBulletins(@RequestParam String sessionId, @RequestParam Optional<String> userId, Model model) {
		List<Team> teamsInSession = teamRepository.findBySessionIdOrderByNameAsc(sessionId);
		Optional<Session> selectedSession = sessionRepository.findBySessionId(sessionId);
		if(selectedSession.isPresent()) {
			model.addAttribute(VIEW_MODE, "bulletins");
			model.addAttribute(SESSION_VIEW_ATTRIBUTE, viewBuilder.buildSessionView(selectedSession.get(), teamsInSession));
			model.addAttribute("issuedBulletins", getIssuedBulletinViews(sessionId));
		} else {
			return REDIRECT_INDEX + userId.map(s -> PARAM_USER_ID + s).orElse("");
		}

		return "bulletintable";
	}

	@PostMapping("/issueBulletin")
	@Transactional
	public String issueBulletin(@ModelAttribute RcBulletinView nextBulletinView,
			@RequestParam String redirectTo,
			@RequestParam Optional<String> userId, Model model) {
		Optional<RcBulletin> bulletin = bulletinRepository.findBySessionIdAndBulletinNo(
				nextBulletinView.getSessionId(), nextBulletinView.getBulletinNo());
		if(bulletin.isPresent()) {
			addError("Bulletin " + bulletin.get().getBulletinNo() + " exists in this session", model);
			return "redirect:index.html?messages=" + messagesEncoded(model);
		} else {
			RuleViolation violation = violationRepository.findById(nextBulletinView.getViolationId()).orElse(null);
			Penalty penalty = penaltyRepository.findById(nextBulletinView.getSelectedPenaltyCode()).orElse(null);

			bulletinRepository.save(buildFromView(nextBulletinView, violation, penalty));
		}
		if(redirectTo.isEmpty()) {
			redirectTo = "session";
		}
		String paramString = "?teamId=All" + userId.map(s -> "&userId=" + s).orElse("");
		paramString += "&sessionId=" + URLEncoder.encode(nextBulletinView.getSessionId(), StandardCharsets.UTF_8);
		return "redirect:" + redirectTo + paramString;
	}

	@GetMapping("/sendBulletin")
	@Transactional
	public String sendBulletin(@RequestParam String sessionId, @RequestParam long bulletinNo, @RequestParam Optional<String> userId, Model model) {
		RcUser user = currentUser();
		RcBulletin bulletin = bulletinRepository.findBySessionIdAndBulletinNo(sessionId, bulletinNo).orElse(null);
		if(bulletin != null) {
			if(isUserRaceControl(user)) {
				discordNotifier.sendRcBulletin(bulletin);
				bulletin.setSent(ZonedDateTime.now());
				log.info("Bulletin no {} sent to Discord.", bulletinNo);
			} else {
				log.warn("User " + user.getName() + " is not allowed to send RC bulletin");
			}
		}
		String paramString = userId.map(s -> USER_ID_PARAM + s + "&").orElse("");
		paramString += SESSION_ID_PARAM + URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
		return "redirect:bulletins?" + paramString;
	}

	@GetMapping("/voidBulletin")
	@Transactional
	public String voidBulletin(@RequestParam String sessionId, @RequestParam long bulletinNo, @RequestParam Optional<String> userId, Model model) {
		RcUser user = currentUser();
		RcBulletin bulletin = bulletinRepository.findBySessionIdAndBulletinNo(sessionId, bulletinNo).orElse(null);
		if(bulletin != null) {
			if(isUserRaceControl(user)) {
				bulletin.setValid(false);
				log.info("Bulletin {} voided", bulletinNo);
			} else {
				log.warn("User " + user.getName() + " is not allowed to send RC bulletin");
			}
		}
		String paramString = userId.map(s -> USER_ID_PARAM + s + "&").orElse("");
		paramString += SESSION_ID_PARAM + URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
		return "redirect:bulletins?" + paramString;
	}

	@ModelAttribute("ruleViolations")
	public List<RuleViolationCategorySelectView> getRuleViolations() {
		List<RuleViolationCategorySelectView> views = new ArrayList<>();
		RuleViolation lastViolation = null;
		RuleViolationCategorySelectView lastCategory = null;
		for(RuleViolation violation : violationRepository.findAll()) {
			if(lastViolation == null ||
					!lastViolation.getCategory().getCategoryCode().equals(violation.getCategory().getCategoryCode())) {
				List<RuleViolationView> violationSelectViews = new ArrayList<>();
				violationSelectViews.add(RuleViolationView.buildFromEntity(violation));

				lastCategory = RuleViolationCategorySelectView.builder()
						.code(violation.getCategory().getCategoryCode())
						.description(violation.getCategoryDescription())
						.violations(violationSelectViews)
						.build();
				views.add(lastCategory);
			} else {
				lastCategory.getViolations().add(RuleViolationView.buildFromEntity(violation));
			}
			lastViolation = violation;
		}
		return views;
	}

	@ModelAttribute("penaltyViews")
	@Transactional
	public List<PenaltySelectView> getPenalties() {
		return penaltyRepository.findAllByCodeContainingOrderByCodeAsc("")
				.map(PenaltySelectView::buildFromEntity)
				.collect(Collectors.toList());
	}

	private RcBulletinView prepareNextBulletin(String sessionId) {
		long nextBulletinNo = bulletinRepository.countAllBySessionId(sessionId) + 1;
		return RcBulletinView.buildNew(sessionId, nextBulletinNo);
	}

	private List<RcIssuedBulletinView> getIssuedBulletinViews(String sessionId) {
		List<RcIssuedBulletinView> bulletinViews = new ArrayList<>();
		for( RcBulletin bulletin : bulletinRepository.findAllBySessionIdOrderByBulletinNoDesc(sessionId)) {
			RuleViolation violation = null;
			Penalty penalty = null;
			if(bulletin.getViolationCategory() != null) {
				RuleViolationCategory category = categoryRepository.findById(bulletin.getViolationCategory()).orElse(null);
				if (category != null) {
					try {
						violation = violationRepository.findRuleViolationByCategoryAndIdentifier(category,
								bulletin.getViolationIdentifier()).orElse(null);
					} catch(IncorrectResultSizeDataAccessException e) {
						log.error("LiveController.getIssuedBulletinsView: {} for category {}, identifier {}",
								e.getMessage(), category.getCategoryCode(), bulletin.getViolationIdentifier());
						continue;
					}
				}
			}
			if(bulletin.getSelectedPenaltyCode() != null) {
				penalty = penaltyRepository.findById(bulletin.getSelectedPenaltyCode()).orElse(null);
			}
			bulletinViews.add(RcIssuedBulletinView.fromEntity(bulletin, violation, penalty));
		}

		return bulletinViews;
	}

	private boolean isUserRaceControl(@NonNull RcUser user) {
		return user.getUserType() == RcUserType.SYSADMIN
				|| user.getUserType() == RcUserType.RACE_DIRECTOR
				|| user.getUserType() == RcUserType.STEWARD;

	}

	private RcBulletin buildFromView(RcBulletinView bulletinView, RuleViolation violation, Penalty penalty) {
		return RcBulletin.builder()
				.created(ZonedDateTime.now())
				.bulletinNo(bulletinView.getBulletinNo())
				.sessionId(bulletinView.getSessionId())
				.carNo(bulletinView.getCarNo())
				.violationIdentifier(violation != null ? violation.getIdentifier() : null)
				.violationCategory(violation != null ? violation.getCategory().getCategoryCode() : null)
				.violationDescription(violation != null ? violation.getDescription() : null)
				.selectedPenaltyCode((violation != null && penalty != null) ? penalty.getCode() : null)
				.penaltyDescription((violation != null && penalty != null) ? penalty.getName() : null)
				.penaltySeconds((violation != null && penalty != null && penalty.getIRacingPenalty().isTimeParamNeeded())
						? (long) bulletinView.getPenaltySeconds() : null)
				.message(bulletinView.getMessage())
				.sessionTime(bulletinView.getSessionTime())
				.valid(true)
				.build();
	}

}
