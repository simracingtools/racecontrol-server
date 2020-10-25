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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bausdorf.simracing.racecontrol.api.EventMessage;
import de.bausdorf.simracing.racecontrol.model.Driver;
import de.bausdorf.simracing.racecontrol.model.Event;
import de.bausdorf.simracing.racecontrol.model.EventRepository;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventLogger {
	private final EventRepository eventRepository;

	public EventLogger(@Autowired EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public boolean log(EventMessage eventMessage, Driver driver) {
		Optional<Event> existingEvent = eventRepository.findBySessionIdAndSessionTimeAndDriverIdAndEventType(
						driver.getSessionId(),
						eventMessage.getSessionTime(),
						driver.getDriverId(),
						eventMessage.getEventType().name());
		if(existingEvent.isPresent()) {
			log.warn("Event {} for session {} already exisits", driver.getSessionId(), eventMessage.getEventNo());
			return false;
		}
		eventRepository.save(Event.builder()
				.sessionId(driver.getSessionId())
				.eventNo(eventMessage.getEventNo())
				.carNo(eventMessage.getCarNo())
				.carName(eventMessage.getCarName())
				.carClass(eventMessage.getCarClass())
				.carClassColor(eventMessage.getCarClassColor())
				.lap(eventMessage.getLap())
				.lapPct(eventMessage.getLapPct())
				.driverName(eventMessage.getDriverName())
				.teamName(eventMessage.getTeamName())
				.sessionTime(eventMessage.getSessionTime())
				.eventTime(TimeTools.longDurationString(eventMessage.getSessionTime()))
				.eventType(eventMessage.getEventType().name())
				.driverId(driver.getDriverId())
				.teamId(driver.getTeam().getTeamId())
				.build()
		);
		return true;
	}
}
