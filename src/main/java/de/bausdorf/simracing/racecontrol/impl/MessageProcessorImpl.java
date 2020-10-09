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
import org.springframework.stereotype.Component;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MessageProcessorImpl implements MessageProcessor {

	public static final String VALID_TRANSITION_FOR = "Valid transition for {}: {} -> {}";
	public static final String INVALID_STATE_TRANSITION_FOR = "Invalid state transition for {}: {} -> {}";
	private final DriverRepository driverRepository;
	private final DriverChangeRepository changeRepository;
	private final SessionRepository sessionRepository;
	private final TeamRepository teamRepository;


	public MessageProcessorImpl(@Autowired DriverRepository driverRepository,
			@Autowired TeamRepository teamRepository,
			@Autowired DriverChangeRepository driverChangeRepository,
			@Autowired SessionRepository sessionRepository) {
		this.driverRepository = driverRepository;
		this.changeRepository = driverChangeRepository;
		this.sessionRepository = sessionRepository;
		this.teamRepository = teamRepository;
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
		} else {
			updateSession(session.get(), message.getType() == ClientMessageType.SESSION ? (SessionMessage)clientData : null);
		}
		session = sessionRepository.findBySessionId(message.getSessionId());
		if(message.getType() == ClientMessageType.EVENT && session.isPresent()
				&& session.get().getSessionState().getTypeCode() < SessionStateType.CHECKERED.getTypeCode()) {
			processEventMessage(message.getSessionId(),
					Long.parseLong(message.getDriverId()),
					Long.parseLong(message.getTeamId()),
					(EventMessage)clientData);
		}
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
					stint.setState(StintStateType.IN_PIT);
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

	public void updateSession(Session session, SessionMessage message) {
		if(message != null) {
			session.setLastUpdate(message.getSessionTime());
			session.setSessionDuration(message.getSessionDuration());
			session.setTrackName(message.getTrackName());
			session.setSessionType(message.getSessionType());
			session.setSessionState(SessionStateType.ofTypeCode(message.getSessionState()));
			sessionRepository.save(session);

			if(session.getSessionState() == SessionStateType.RACING) {
				processGreenFlag(session);
			} else if(session.getSessionState() == SessionStateType.CHECKERED) {
				processCheckeredFlag(session);
			}
		}
	}

	public void processEventMessage(String sessionId, long driverId, long teamId, EventMessage message) {
		Optional<Driver> existingDriver = driverRepository.findBySessionIdAndIracingId(sessionId, driverId);
		if (!existingDriver.isPresent()) {
			 if(driverId < 1) {
			 	log.warn("Invalid driver id {} for driver {}", driverId, message.getDriverName());
			 }
			Driver driver = createNewDriver(sessionId, driverId, teamId, message);
			updateDriverStint(driver, message.getEventType(), message.getSessionTime());
			driverRepository.save(driver);
		} else {
			updateDriverStint(existingDriver.get(), message.getEventType(), message.getSessionTime());
			driverRepository.save(existingDriver.get());
			if(message.getEventType() == EventType.DRIVER_CHANGE) {
				DriverChange change = DriverChange.builder()
						.sessionId(sessionId)
						.changeFrom(existingDriver.get().getTeam().getCurrentDriver())
						.changeTo(existingDriver.get())
						.changeTime(message.getSessionTime())
						.build();
				log.info("Driver change for team {}; {}", existingDriver.get().getTeam().getName(), change);
				changeRepository.save(change);
				existingDriver.get().getTeam().setCurrentDriver(existingDriver.get());
			}
		}
	}

	private Driver createNewDriver(String sessionId, long driverId, long teamId, EventMessage message) {
		Driver driver = Driver.builder()
				.sessionId(sessionId)
				.driverId(driverId)
				.name(message.getDriverName())
				.iRating(message.getIRating())
				.build();
		driver.setTeam(Team.builder()
				.sessionId(sessionId)
				.teamId(teamId)
				.name(message.getTeamName())
				.carNo(message.getCarNo())
				.currentDriver(driver)
				.build());
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
			log.info("{} starting stint (green flag) at {}", team.getCurrentDriver().getName(),
					TimeTools.longDurationString(session.getLastUpdate()));
			Stint stint = team.getCurrentDriver().getLastStint();
			if(stint == null) {
				stint = Stint.builder()
						.sessionId(team.getSessionId())
						.driver(team.getCurrentDriver())
						.state(StintStateType.RUNNING)
						.startTime(session.getLastUpdate())
						.build();
				team.getCurrentDriver().setStints(new ArrayList<>(Collections.singletonList(stint)));
			} else {
				stint.setStartTime(session.getLastUpdate());
				stint.setState(StintStateType.RUNNING);
			}
		}
	}

	private void processCheckeredFlag(Session session) {
		List<Team> teams = teamRepository.findBySessionIdOrderByNameAsc(session.getSessionId());
		for(Team team : teams) {
			log.info("{} end stint (checkered flag) at {}", team.getCurrentDriver().getName(),
					TimeTools.longDurationString(session.getLastUpdate()));
			Stint stint = team.getCurrentDriver().getLastStint();
			if(stint == null) {
				log.error("No last stint for {} at race end", team.getCurrentDriver());
			} else {
				stint.setEndTime(session.getLastUpdate());
				stint.setState(StintStateType.IN_PIT);
			}
		}
	}
}
