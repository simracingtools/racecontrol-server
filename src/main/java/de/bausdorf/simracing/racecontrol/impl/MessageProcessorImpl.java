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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
import de.bausdorf.simracing.racecontrol.model.Driver;
import de.bausdorf.simracing.racecontrol.model.DriverChange;
import de.bausdorf.simracing.racecontrol.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.model.DriverRepository;
import de.bausdorf.simracing.racecontrol.model.Session;
import de.bausdorf.simracing.racecontrol.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MessageProcessorImpl implements MessageProcessor {

	private final DriverRepository driverRepository;
	private final DriverChangeRepository changeRepository;
	private final SessionRepository sessionRepository;

	public MessageProcessorImpl(@Autowired DriverRepository driverRepository,
			@Autowired DriverChangeRepository driverChangeRepository,
			@Autowired SessionRepository sessionRepository) {
		this.driverRepository = driverRepository;
		this.changeRepository = driverChangeRepository;
		this.sessionRepository = sessionRepository;
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
		if(message.getType() == ClientMessageType.EVENT) {
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
					.build();
			driver.setStints(new ArrayList<>(Collections.singletonList(stint)));
		}
		switch(eventType) {
			case ON_TRACK:
				if(stint.getStartTime() == null) {
					log.debug("{} starting stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
					stint.setStartTime(sessionTime);
				}
				break;
			case ENTER_PITLANE:
				if(stint.getEndTime() == null && stint.getStartTime() != null) {
					log.debug("{} ending stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
					stint.setEndTime(sessionTime);
				}
				break;
			case EXIT_PITLANE:
				log.debug("{} stating stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
				if(stint.getStartTime() == null) {
					stint.setStartTime(sessionTime);
				} else {
					driver.getStints().add(Stint.builder()
							.sessionId(driver.getSessionId())
							.driver(driver)
							.startTime(sessionTime)
							.build());
				}
				break;
			default:
		}
	}

	public void createSession(ClientMessage message) {
		sessionRepository.save(Session.builder()
				.sessionDuration(Duration.ZERO)
				.sessionId(message.getSessionId())
				.lastUpdate(Timestamp.valueOf(LocalDateTime.now()))
				.build());
	}

	public void createSession(SessionMessage message, String sessionId) {
		sessionRepository.save(Session.builder()
				.sessionDuration(message.getSessionDuration())
				.trackName(message.getTrackName())
				.sessionType(message.getSessionType())
				.sessionId(sessionId)
				.lastUpdate(Timestamp.valueOf(LocalDateTime.now()))
				.build());
	}

	public void updateSession(Session session, SessionMessage message) {
		session.setLastUpdate(Timestamp.valueOf(LocalDateTime.now()));
		if(message != null) {
			session.setSessionDuration(message.getSessionDuration());
			session.setTrackName(message.getTrackName());
			session.setSessionType(message.getSessionType());
		}
	}

	public void processEventMessage(String sessionId, long driverId, long teamId, EventMessage message) {
		Optional<Driver> existingDriver = driverRepository.findBySessionIdAndIracingId(sessionId, driverId);
		if (!existingDriver.isPresent()) {
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
				.build())));
		return driver;
	}
}
