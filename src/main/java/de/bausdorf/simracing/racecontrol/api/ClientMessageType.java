package de.bausdorf.simracing.racecontrol.api;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ClientMessageType {
	PING(MessageConstants.MessageType.PING_NAME),
	EVENT(MessageConstants.MessageType.EVENTDATA_NAME);

	private final String jsonKey;

	ClientMessageType(String name) {
		this.jsonKey = name;
	}

	@JsonValue
	public String getJsonKey() {
		return jsonKey;
	}

	public static ClientMessageType fromJsonKey(String key) {
		if( key == null ) {
			throw new IllegalArgumentException("Invalid message type null");
		}
		switch(key) {
			case MessageConstants.MessageType.EVENTDATA_NAME: return EVENT;
			case MessageConstants.MessageType.PING_NAME: return PING;
			default:
				throw new IllegalArgumentException("Invalid message type \"" + key + "\"");
		}
	}
}
