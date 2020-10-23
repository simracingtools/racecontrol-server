package de.bausdorf.simracing.racecontrol.impl;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import de.bausdorf.simracing.racecontrol.api.ClientData;
import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.api.ClientMessageType;
import de.bausdorf.simracing.racecontrol.api.EventMessage;
import de.bausdorf.simracing.racecontrol.api.MessageProcessor;
import de.bausdorf.simracing.racecontrol.api.EventType;
import de.bausdorf.simracing.racecontrol.api.SessionMessage;
import de.bausdorf.simracing.racecontrol.api.SessionStateType;
import de.bausdorf.simracing.racecontrol.api.StintStateType;
import de.bausdorf.simracing.racecontrol.model.Driver;
import de.bausdorf.simracing.racecontrol.model.DriverChange;
import de.bausdorf.simracing.racecontrol.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.model.DriverRepository;
import de.bausdorf.simracing.racecontrol.model.Session;
import de.bausdorf.simracing.racecontrol.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.model.TeamRepository;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.ViewBuilder;
import de.bausdorf.simracing.racecontrol.web.model.ClientAck;
import de.bausdorf.simracing.racecontrol.web.model.ClientConnectMessage;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MessageProcessorImpl implements MessageProcessor {

	public static final String VALID_TRANSITION_FOR = "Ignored transition for {}: {} -> {}";
	public static final String INVALID_STATE_TRANSITION_FOR = "Invalid state transition for {}: {} -> {}";
	private final DriverRepository driverRepository;
	private final DriverChangeRepository changeRepository;
	private final SessionRepository sessionRepository;
	private final TeamRepository teamRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final ViewBuilder viewBuilder;
	private final EventLogger eventLogger;

	public MessageProcessorImpl(@Autowired DriverRepository driverRepository,
			@Autowired TeamRepository teamRepository,
			@Autowired DriverChangeRepository driverChangeRepository,
			@Autowired SessionRepository sessionRepository,
			@Autowired SimpMessagingTemplate messagingTemplate,
			@Autowired ViewBuilder viewBuilder,
			@Autowired EventLogger eventLogger) {
		this.driverRepository = driverRepository;
		this.changeRepository = driverChangeRepository;
		this.sessionRepository = sessionRepository;
		this.teamRepository = teamRepository;
		this.messagingTemplate = messagingTemplate;
		this.viewBuilder = viewBuilder;
		this.eventLogger = eventLogger;
	}

	@Transactional
	public void processMessage(ClientMessage message) {
		ClientData clientData = MessageFactory.validateAndConvert(message);
		Optional<Session> session = sessionRepository.findBySessionId(message.getSessionId());
		if(!session.isPresent()) {
			switch(message.getType()) {
				case EVENT: createSession(message); break;
				case SESSION: createSession((SessionMessage)clientData, message.getSessionId()); break;
				default: break;
			}
		} else if(message.getType() == ClientMessageType.SESSION) {
			if(!updateSession(session.get(), (SessionMessage)clientData)) {
				log.warn("Session update for state {} for {} already exists",
						SessionStateType.ofTypeCode(((SessionMessage)clientData).getSessionState()),
						session.get().getSessionId());
				return;
			}
		}
		session = sessionRepository.findBySessionId(message.getSessionId());
		if(message.getType() == ClientMessageType.EVENT && session.isPresent()
				&& session.get().getSessionState().getTypeCode() < SessionStateType.COOL_DOWN.getTypeCode()) {
			processEventMessage(session.get(),
					Long.parseLong(message.getDriverId()),
					Long.parseLong(message.getTeamId()),
					(EventMessage)clientData);
		}
	}

	public void processEventMessage(Session session, long driverId, long teamId, EventMessage message) {
		String sessionId = session.getSessionId();
		Driver existingDriver = driverRepository.findBySessionIdAndIracingId(sessionId, driverId).orElse(null);
		if (existingDriver == null) {
			if(driverId < 1) {
				log.warn("Invalid driver id {} for driver {}", driverId, message.getDriverName());
			}
			existingDriver = createNewDriver(sessionId, driverId, teamId, message);
		}
		if(!eventLogger.log(message, existingDriver)) {
			return;
		}
		if(session.getSessionState() == SessionStateType.RACING) {
			updateDriverStint(existingDriver, message.getEventType(), message.getSessionTime());
		}
		if(message.getEventType() != existingDriver.getLastEventType()) {
			existingDriver.setLastEventType(message.getEventType());
			sendDriverMessage(existingDriver);
		}
		driverRepository.save(existingDriver);

		if(message.getEventType() == EventType.DRIVER_CHANGE
				&& existingDriver.getTeam().getCurrentDriverId() != existingDriver.getIracingId()) {
			DriverChange change = DriverChange.builder()
					.sessionId(sessionId)
					.changeFromId(existingDriver.getTeam().getCurrentDriverId())
					.changeToId(existingDriver.getIracingId())
					.changeTime(message.getSessionTime())
					.team(existingDriver.getTeam())
					.build();
			log.info("Driver change for team {}; {}", existingDriver.getTeam().getName(), change);
			changeRepository.save(change);
			existingDriver.getTeam().setCurrentDriverId(existingDriver.getDriverId());
			sendPageReload(existingDriver.getSessionId(), "Driver change " + existingDriver.getTeam().getName());
		}
	}

	@MessageMapping("/timingclient")
	@SendToUser("/timing/client-ack")
	public ClientAck respondAck(ClientConnectMessage message) {
		log.info(message.toString());
		return new ClientAck(message.getSessionId());
	}

	@MessageMapping("/rcclient")
	@SendToUser("/rc/client-ack")
	public String respondRcAck(String message) {
		log.info(message);
		return "rc ack for user";
	}

	private void sendPageReload(String sessionId, String message) {
		messagingTemplate.convertAndSend("/timing/" + sessionId + "/reload", message);
	}

	private void sendDriverMessage(Driver driver) {
		messagingTemplate.convertAndSend("/timing/" + driver.getSessionId() + "/driver",
				viewBuilder.buildDriverView(driver));
	}

	private void updateDriverStint(Driver driver, EventType eventType, Duration sessionTime) {
		Stint stint = driver.getLastStint();
		if(stint == null) {
			stint = Stint.builder()
					.sessionId(driver.getSessionId())
					.driver(driver)
					.state(StintStateType.UNDEFINED)
					.build();
			driver.setStints(new ArrayList<>(Collections.singletonList(stint)));
		}
		switch(eventType) {
			case ON_TRACK:
				if(stint.getState() == StintStateType.UNDEFINED) {
					stint.setStartTime(sessionTime);
					stint.setState(StintStateType.RUNNING);
					log.info("{} starting stint at {} ({})", driver.getName(), TimeTools.longDurationString(sessionTime), stint.getState());
				} else if(stint.getState() == StintStateType.IN_PIT) {
					driver.getStints().add(Stint.builder()
							.sessionId(driver.getSessionId())
							.driver(driver)
							.startTime(sessionTime)
							.state(StintStateType.RUNNING)
							.build());
					log.info("{} starting stint at {} ({})", driver.getName(), TimeTools.longDurationString(sessionTime), stint.getState());
				} else if(stint.getState() != StintStateType.RUNNING){
					log.warn(INVALID_STATE_TRANSITION_FOR, driver.getName(), stint.getState(), eventType);
				}
				break;

			case ENTER_PITLANE:
				if(stint.getState() == StintStateType.RUNNING) {
					stint.setEndTime(sessionTime);
					stint.setState(StintStateType.IN_PIT);
					log.info("{} ending stint at {} ({})", driver.getName(), TimeTools.longDurationString(sessionTime), stint.getState());
				} else if(stint.getState() == StintStateType.UNDEFINED) {
					log.debug(VALID_TRANSITION_FOR, driver.getName(), stint.getState(), eventType);
				} else if(stint.getState() != StintStateType.IN_PIT){
					log.warn(INVALID_STATE_TRANSITION_FOR, driver.getName(), stint.getState(), eventType);
				}
				break;
			default:
		}
	}

	public void createSession(ClientMessage message) {
		sessionRepository.save(Session.builder()
				.sessionDuration(Duration.ZERO)
				.sessionId(message.getSessionId())
				.lastUpdate(Duration.ZERO)
				.build());
		log.debug("created session {}", message.getSessionId());
	}

	public void createSession(SessionMessage message, String sessionId) {
		sessionRepository.save(Session.builder()
				.sessionDuration(message.getSessionDuration())
				.trackName(message.getTrackName())
				.sessionType(message.getSessionType())
				.sessionId(sessionId)
				.sessionState(SessionStateType.ofTypeCode(message.getSessionState()))
				.lastUpdate(message.getSessionTime())
				.build());
		log.debug("created session {}", sessionId);
	}

	public boolean updateSession(Session session, SessionMessage message) {
		SessionStateType messageType = SessionStateType.ofTypeCode(message.getSessionState());
		if(session.getSessionState().getTypeCode() < messageType.getTypeCode()) {
			boolean doReload = message.getSessionState() != session.getSessionState().getTypeCode();
			session.setLastUpdate(message.getSessionTime());
			session.setSessionDuration(message.getSessionDuration());
			session.setTrackName(message.getTrackName());
			session.setSessionType(message.getSessionType());
			session.setSessionState(messageType);
			sessionRepository.save(session);

			if(session.getSessionState() == SessionStateType.RACING) {
				processGreenFlag(session);
			} else if(session.getSessionState() == SessionStateType.CHECKERED) {
				processCheckeredFlag(session);
			}
			if(doReload) {
				sendPageReload(session.getSessionId(), session.getSessionState().name());
			}
			return true;
		} else {
			return false;
		}
	}

	private Driver createNewDriver(String sessionId, long driverId, long teamId, EventMessage message) {
		Driver driver = Driver.builder()
				.sessionId(sessionId)
				.driverId(driverId)
				.name(message.getDriverName())
				.iRating(message.getIRating())
				.lastEventType(message.getEventType())
				.build();
		Team team = teamRepository.findBySessionIdAndIracingId(sessionId, teamId).orElse(Team.builder()
				.sessionId(sessionId)
				.teamId(teamId)
				.name(message.getTeamName())
				.carNo(message.getCarNo())
				.currentDriverId(driverId)
				.build());
		driver.setTeam(team);
		driver.setStints(new ArrayList<>(Collections.singletonList(Stint.builder()
				.driver(driver)
				.sessionId(sessionId)
				.state(StintStateType.UNDEFINED)
				.build())));
		return driver;
	}

	private void processGreenFlag(Session session) {
		List<Team> teams = teamRepository.findBySessionIdOrderByNameAsc(session.getSessionId());
		for(Team team : teams) {
			Driver currentDriver = driverRepository.findBySessionIdAndIracingId(session.getSessionId(), team.getCurrentDriverId()).orElse(null);
			if(currentDriver == null) {
				continue;
			}
			log.info("{} starting stint (green flag) at {}", currentDriver.getName(),
					TimeTools.longDurationString(session.getLastUpdate()));
			Stint stint = currentDriver.getLastStint();
			if(stint == null) {
				stint = Stint.builder()
						.sessionId(team.getSessionId())
						.driver(currentDriver)
						.state(StintStateType.RUNNING)
						.startTime(session.getLastUpdate())
						.build();
				currentDriver.setStints(new ArrayList<>(Collections.singletonList(stint)));
			} else {
				stint.setStartTime(session.getLastUpdate());
				stint.setState(StintStateType.RUNNING);
			}
		}
	}

	private void processCheckeredFlag(Session session) {
		List<Team> teams = teamRepository.findBySessionIdOrderByNameAsc(session.getSessionId());
		for(Team team : teams) {
			Driver currentDriver = driverRepository.findBySessionIdAndIracingId(session.getSessionId(), team.getCurrentDriverId()).orElse(null);
			if(currentDriver == null) {
				continue;
			}
			Stint stint = currentDriver.getLastStint();
			if(stint == null) {
				log.error("No last stint for {} at race end", currentDriver);
			} else {
				if(stint.getEndTime() == null) {
					log.info("{} end stint (checkered flag) at {}", currentDriver.getName(),
							TimeTools.longDurationString(session.getLastUpdate()));
					stint.setEndTime(session.getLastUpdate());
					stint.setState(StintStateType.IN_PIT);
				} else {
					log.info("{} ended last stint at {}", currentDriver.getName(), TimeTools.longDurationString(stint.getEndTime()));
				}
			}
		}
	}
}
