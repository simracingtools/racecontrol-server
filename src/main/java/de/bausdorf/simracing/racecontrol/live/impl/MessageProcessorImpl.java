package de.bausdorf.simracing.racecontrol.live.impl;

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

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import de.bausdorf.simracing.racecontrol.live.api.ClientData;
import de.bausdorf.simracing.racecontrol.live.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.live.api.ClientMessageType;
import de.bausdorf.simracing.racecontrol.live.api.EventFilterClientMessage;
import de.bausdorf.simracing.racecontrol.live.api.EventMessage;
import de.bausdorf.simracing.racecontrol.live.api.MessageProcessor;
import de.bausdorf.simracing.racecontrol.live.api.EventType;
import de.bausdorf.simracing.racecontrol.live.api.ReplayPositionClientMessage;
import de.bausdorf.simracing.racecontrol.live.api.SessionMessage;
import de.bausdorf.simracing.racecontrol.live.api.SessionStateType;
import de.bausdorf.simracing.racecontrol.live.api.StintStateType;
import de.bausdorf.simracing.racecontrol.live.model.Driver;
import de.bausdorf.simracing.racecontrol.live.model.DriverChange;
import de.bausdorf.simracing.racecontrol.live.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.live.model.DriverRepository;
import de.bausdorf.simracing.racecontrol.live.model.Event;
import de.bausdorf.simracing.racecontrol.live.model.Session;
import de.bausdorf.simracing.racecontrol.live.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.live.model.Stint;
import de.bausdorf.simracing.racecontrol.live.model.Team;
import de.bausdorf.simracing.racecontrol.live.model.TeamRepository;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.ViewBuilder;
import de.bausdorf.simracing.racecontrol.web.model.live.ClientAck;
import de.bausdorf.simracing.racecontrol.web.model.live.ClientConnectMessage;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserRepository;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MessageProcessorImpl implements MessageProcessor {

	public static final String VALID_TRANSITION_FOR = "Ignored transition for {}: {} -> {}";
	public static final String INVALID_STATE_TRANSITION_FOR = "Invalid state transition for {}: {} -> {}";
	public static final String TIMING = "/timing/";
	private final DriverRepository driverRepository;
	private final DriverChangeRepository changeRepository;
	private final SessionRepository sessionRepository;
	private final TeamRepository teamRepository;
	private final RcUserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final ViewBuilder viewBuilder;
	private final EventLogger eventLogger;

	public MessageProcessorImpl(@Autowired DriverRepository driverRepository,
			@Autowired TeamRepository teamRepository,
			@Autowired DriverChangeRepository driverChangeRepository,
			@Autowired SessionRepository sessionRepository,
			@Autowired RcUserRepository userRepository,
			@Autowired SimpMessagingTemplate messagingTemplate,
			@Autowired ViewBuilder viewBuilder,
			@Autowired EventLogger eventLogger) {
		this.driverRepository = driverRepository;
		this.changeRepository = driverChangeRepository;
		this.sessionRepository = sessionRepository;
		this.teamRepository = teamRepository;
		this.userRepository = userRepository;
		this.messagingTemplate = messagingTemplate;
		this.viewBuilder = viewBuilder;
		this.eventLogger = eventLogger;
	}

	@Transactional
	public long processMessage(ClientMessage message) {
		ClientData clientData = MessageFactory.validateAndConvert(message);
		Optional<Session> session = sessionRepository.findBySessionId(message.getSessionId());
		if(session.isEmpty()) {
			try {
				switch (message.getType()) {
					case EVENT: createSession(message); break;
					case SESSION: createSession((SessionMessage) clientData, message.getSessionId()); break;
					default: break;
				}
			} catch (DataIntegrityViolationException e) {
				log.info("Create session: {}", e.getMessage());
				logSqlIntegrityViolation(e);
			}
		} else if(message.getType() == ClientMessageType.SESSION) {
			try {
				processSessionMessage(session.get(), message.getLap(), (SessionMessage) clientData);
			} catch (DataIntegrityViolationException e) {
				log.info("Process session event: {}", e.getMessage());
				logSqlIntegrityViolation(e);
			}
		}
		session = sessionRepository.findBySessionId(message.getSessionId());
		if(message.getType() == ClientMessageType.EVENT && session.isPresent()
				&& session.get().getSessionState().getTypeCode() < SessionStateType.COOL_DOWN.getTypeCode()) {
			try {
				processEventMessage(session.get(),
						Long.parseLong(message.getDriverId()),
						Long.parseLong(message.getTeamId()),
						(EventMessage) clientData);
			} catch (DataIntegrityViolationException e) {
				log.info("Process track event: {}", e.getMessage());
				logSqlIntegrityViolation(e);
			}
		}
		return session.map(s -> eventLogger.getLastEventNo(s.getSessionId())).orElse(-1L);
	}

//	@Transactional
	public void processSessionMessage(Session session, int lap, SessionMessage clientData) {
		if(session.getSessionLaps() < lap) {
			session.setSessionLaps(lap);
			sessionRepository.save(session);
		}
		if(!updateSession(session, clientData)) {
			log.warn("Session update for state {} for {} already exists",
					SessionStateType.ofTypeCode(clientData.getSessionState()),
					session.getSessionId());
		}
	}

//	@Transactional
	public void processEventMessage(Session session, long driverId, long teamId, EventMessage message) {
		String sessionId = session.getSessionId();
		Driver existingDriver = driverRepository.findBySessionIdAndIracingId(sessionId, driverId).orElse(null);
		if (existingDriver == null) {
			if (driverId < 1) {
				log.warn("Invalid driver id {} for driver {}", driverId, message.getDriverName());
			}
			existingDriver = createNewDriver(sessionId, driverId, teamId, message);
		}
		Event event = eventLogger.log(message, existingDriver);
		if (event == null) {
			return;
		}
		sendEventMessage(event);
		updateDriver(session, existingDriver, message);

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

	@MessageMapping("/rctimestamp")
	@SendToUser("/timing/client-ack")
	public ClientAck respondTimestampMessage(ReplayPositionClientMessage message) {
		message.setTimestamp(message.getTimestamp() - 5000);
		messagingTemplate.convertAndSend("/rc/" + message.getUserId() + "/replayposition", message);
		return new ClientAck("timestamp received");
	}

	@MessageMapping("/eventfilter")
	@SendToUser("/timing/client-ack")
	@Transactional
	public ClientAck changeEventFilter(EventFilterClientMessage message) {
		Optional<RcUser> user = userRepository.findByiRacingId(message.getUserId());
		if(user.isPresent()) {
			updateEventFilter(user.get(), message.getEventType(), message.isChecked());
			sendPageReload(message.getSessionId(), "EventFilter change");
		}
		return new ClientAck("eventfilter change for " + message.getUserId());
	}

	@MessageMapping("/rcclient")
	@SendToUser("/rc/client-ack")
	public String respondRcAck(String message) {
		log.info(message);
		return "{\"messageType\":\"ack\", \"clientId\": \"" + message + "\"}";
	}

	private void logSqlIntegrityViolation(Throwable t) {
		if (t == null) {
			return;
		}
		if (t instanceof SQLIntegrityConstraintViolationException) {
			log.warn(t.getMessage());
			return;
		}
		logSqlIntegrityViolation(t.getCause());
	}

	private void sendPageReload(String sessionId, String message) {
		log.debug("send {} to {}", message, TIMING + sessionId + "/reload");
		messagingTemplate.convertAndSend(TIMING + sessionId + "/reload", message);
	}

	private void sendDriverMessage(Driver driver, long eventId, String carName) {
		messagingTemplate.convertAndSend(TIMING + driver.getSessionId() + "/driver",
				viewBuilder.buildDriverView(driver, eventId, carName));
	}

	private void sendEventMessage(Event event) {
		messagingTemplate.convertAndSend(TIMING + event.getSessionId() + "/event",
				viewBuilder.buildEventView(event));
	}

	private void updateDriverStint(Driver driver, EventType eventType, Duration sessionTime, long sessionLap, SessionStateType sessionState) {
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
			case LAP_COMPLETE:
				if(stint.getState() == StintStateType.UNDEFINED) {
					stint.setStartTime(sessionTime);
					stint.setStartLap(sessionLap);
					stint.setState(StintStateType.RUNNING);
					log.info("{} starting first stint at {} ({})", driver.getName(), TimeTools.longDurationString(sessionTime), stint.getState());
				} else if(sessionState == SessionStateType.CHECKERED) {
					stint.setEndTime(sessionTime);
					stint.setStopLap(sessionLap);
					stint.setState(StintStateType.IN_PIT);
					log.info("{} ending last stint at {} ({})", driver.getName(), TimeTools.longDurationString(sessionTime), stint.getState());
				}
				break;
			case ON_TRACK:
				if(stint.getState() == StintStateType.IN_PIT) {
					driver.getStints().add(Stint.builder()
							.sessionId(driver.getSessionId())
							.driver(driver)
							.startTime(sessionTime)
							.startLap(sessionLap)
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
					stint.setStopLap(sessionLap);
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

//	@Transactional
	public void createSession(ClientMessage message) {
		sessionRepository.save(Session.builder()
				.sessionDuration(Duration.ZERO)
				.sessionId(message.getSessionId())
				.lastUpdate(Duration.ZERO)
				.created(ZonedDateTime.now())
				.build());
		log.debug("created session {}", message.getSessionId());
	}

//	@Transactional
	public void createSession(SessionMessage message, String sessionId) {
		sessionRepository.save(Session.builder()
				.sessionDuration(message.getSessionDuration())
				.trackName(message.getTrackName())
				.sessionType(message.getSessionType())
				.sessionId(sessionId)
				.sessionState(SessionStateType.ofTypeCode(message.getSessionState()))
				.lastUpdate(message.getSessionTime())
				.created(ZonedDateTime.now())
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
				.lastLapPosition(message.getLapPct())
				.build();
		Team team = teamRepository.findBySessionIdAndIracingId(sessionId, teamId).orElse(Team.builder()
				.sessionId(sessionId)
				.teamId(teamId)
				.name(message.getTeamName())
				.carNo(message.getCarNo())
				.carName(message.getCarName())
				.carClass(message.getCarClass())
				.carClassId(message.getCarClassId())
				.carClassColor(message.getCarClassColor())
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

	private void updateDriver(Session session, Driver existingDriver, EventMessage message) {
		if (session.getSessionState() == SessionStateType.RACING || session.getSessionState() == SessionStateType.CHECKERED) {
			updateDriverStint(existingDriver, message.getEventType(), message.getSessionTime(), message.getLap(), session.getSessionState());
		}
		if (message.getEventType() != existingDriver.getLastEventType()) {
			existingDriver.setLastEventType(message.getEventType());
			sendDriverMessage(existingDriver, session.getEventId(), existingDriver.getTeam().getCarName());
		}
		if (message.getLapPct() != existingDriver.getLastLapPosition()) {
			existingDriver.setLastLapPosition(message.getLapPct());
		}
		driverRepository.save(existingDriver);
	}

	private void updateEventFilter(RcUser user, String event, boolean checked) {
		if(checked) {
			user.getEventFilter().remove(EventType.valueOf(event));
		} else {
			user.getEventFilter().add(EventType.valueOf(event));
		}
		userRepository.save(user);
	}
}
