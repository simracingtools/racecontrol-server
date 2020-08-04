package de.bausdorf.simracing.racecontrol.impl;

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
				.carNo(MapTools.intFromMap(EventData.CAR_NUMBER, data))
				.driverName(MapTools.stringFromMap(EventData.DRIVER_NAME, data))
				.eventNo(MapTools.intFromMap(EventData.EVENT_NO, data))
				.eventType(EventType.ofType(MapTools.stringFromMap(EventData.EVENT_TYPE, data)))
				.iRating(MapTools.intFromMap(EventData.I_RATING, data))
				.lap(MapTools.intFromMap(EventData.LAP, data))
				.sessionTime(TimeTools.getFromIracingDuration(MapTools.doubleFromMap(EventData.SESSION_TIME, data)))
				.teamName(MapTools.stringFromMap(EventData.TEAM_NAME, data))
				.build();
	}

	private static ClientData fromSessionMessage(Map<String, Object> data) {
		return SessionMessage.builder()
				.sessionDuration(TimeTools.getFromIracingDuration(MapTools.doubleFromMap(SessionData.SESSION_DURATION, data)))
				.sessionType(MapTools.stringFromMap(SessionData.SESSION_TYPE, data))
				.trackName(MapTools.stringFromMap(SessionData.TRACK_NAME, data))
				.build();
	}

	private MessageFactory() {}
}
