package de.bausdorf.simracing.racecontrol.api;

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
