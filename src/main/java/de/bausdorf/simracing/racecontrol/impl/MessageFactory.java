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

import java.util.Map;

import de.bausdorf.simracing.racecontrol.api.ClientData;
import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.api.EventMessage;
import de.bausdorf.simracing.racecontrol.api.EventType;
import de.bausdorf.simracing.racecontrol.api.InvalidClientMessageException;
import de.bausdorf.simracing.racecontrol.api.MessageConstants.EventData;
import de.bausdorf.simracing.racecontrol.api.MessageConstants.SessionData;
import de.bausdorf.simracing.racecontrol.api.SessionMessage;
import de.bausdorf.simracing.racecontrol.util.MapTools;
import de.bausdorf.simracing.racecontrol.util.TimeTools;

public class MessageFactory {

	public static ClientData validateAndConvert(ClientMessage message) {
		switch(message.getType()) {
			case EVENT: return fromEventMessage(message.getPayload());
			case SESSION: return fromSessionMessage(message.getPayload());
			default: throw new InvalidClientMessageException("Unknown ClientMessage type");
		}
	}

	private static ClientData fromEventMessage(Map<String, Object> data) {
		return EventMessage.builder()
				.carNo(MapTools.stringFromMap(EventData.CAR_NUMBER, data))
				.driverName(MapTools.stringFromMap(EventData.DRIVER_NAME, data))
				.eventNo(MapTools.intFromMap(EventData.EVENT_NO, data))
				.eventType(EventType.ofType(MapTools.stringFromMap(EventData.EVENT_TYPE, data)))
				.iRating(MapTools.intFromMap(EventData.I_RATING, data))
				.lap(MapTools.intFromMap(EventData.LAP, data))
				.sessionTime(TimeTools.getFromIracingDuration(MapTools.doubleFromMap(EventData.SESSION_TIME, data)))
				.teamName(MapTools.stringFromMap(EventData.TEAM_NAME, data))
				.carName(MapTools.stringFromMap(EventData.CAR_NAME, data))
				.carClass(MapTools.stringFromMap(EventData.CAR_CLASS, data))
				.carClassId(MapTools.intFromMap(EventData.CAR_CLASS_ID, data))
				.carClassColor(MapTools.hexStringFromLong(EventData.CAR_CLASS_COLOR, data))
				.lapPct(MapTools.doubleFromMap(EventData.LAP_PCT, data))
				.build();
	}

	private static ClientData fromSessionMessage(Map<String, Object> data) {
		return SessionMessage.builder()
				.sessionDuration(TimeTools.getFromIracingSessionDuration(MapTools.stringFromMap(SessionData.SESSION_DURATION, data)))
				.sessionType(MapTools.stringFromMap(SessionData.SESSION_TYPE, data))
				.trackName(MapTools.stringFromMap(SessionData.TRACK_NAME, data))
				.sessionState(MapTools.intFromMap(SessionData.SESSION_STATE, data))
				.sessionTime(TimeTools.getFromIracingDuration(MapTools.doubleFromMap(SessionData.SESSION_TIME, data)))
				.build();
	}

	private MessageFactory() {}
}
