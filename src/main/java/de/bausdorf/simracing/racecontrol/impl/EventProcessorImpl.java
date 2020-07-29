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

import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.api.EventProcessor;
import de.bausdorf.simracing.racecontrol.api.EventType;
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
public class EventProcessorImpl implements EventProcessor {

	private final DriverRepository driverRepository;
	private final DriverChangeRepository changeRepository;
	private final SessionRepository sessionRepository;

	public EventProcessorImpl(@Autowired DriverRepository driverRepository,
			@Autowired DriverChangeRepository driverChangeRepository,
			@Autowired SessionRepository sessionRepository) {
		this.driverRepository = driverRepository;
		this.changeRepository = driverChangeRepository;
		this.sessionRepository = sessionRepository;
	}

	@Transactional
	public void processEvent(ClientMessage message) {
		Optional<Session> session = sessionRepository.findBySessionId(message.getSessionId());
		if(!session.isPresent()) {
			sessionRepository.save(Session.builder()
					.sessionId(message.getSessionId())
					.lastUpdate(Timestamp.valueOf(LocalDateTime.now()))
					.build());
		} else {
			session.get().setLastUpdate(Timestamp.valueOf(LocalDateTime.now()));
		}
		Optional<Driver> existingDriver = driverRepository.findBySessionIdAndIracingId(message.getSessionId(), message.getDriverId());
		if (!existingDriver.isPresent()) {
			Driver driver = createNewDriver(message);
			updateDriverStint(message.getSessionId(), driver, message.getEventType(), message.getSessionTime());
			driverRepository.save(driver);
		} else {
			updateDriverStint(message.getSessionId(), existingDriver.get(), message.getEventType(), message.getSessionTime());
			if(message.getEventType() == EventType.DRIVER_CHANGE) {
				DriverChange change = DriverChange.builder()
						.sessionId(message.getSessionId())
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

	private void updateDriverStint(String sessionId, Driver driver, EventType eventType, Duration sessionTime) {
		Stint stint = driver.getLastStint();
		switch(eventType) {
			case ON_TRACK:
				if(stint.getStartTime() == null) {
					log.debug("{} stating stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
					stint.setStartTime(sessionTime);
				}
				break;
			case ENTER_PITLANE:
				if(stint.getEndTime() == null) {
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
							.sessionId(sessionId)
							.startTime(sessionTime)
							.build());
				}
				break;
			default:
		}

	}

	private Driver createNewDriver(ClientMessage message) {
		Driver driver = Driver.builder()
				.sessionId(message.getSessionId())
				.driverId(message.getDriverId())
				.name(message.getDriverName())
				.iRating(message.getIRating())
				.stints(new ArrayList<>(Collections.singletonList(Stint.builder().build())))
				.build();
		driver.setTeam(Team.builder()
				.sessionId(message.getSessionId())
				.teamId(message.getTeamId())
				.name(message.getTeamName())
				.carNo(message.getCarNo())
				.currentDriver(driver)
				.build());
		return driver;
	}
}
