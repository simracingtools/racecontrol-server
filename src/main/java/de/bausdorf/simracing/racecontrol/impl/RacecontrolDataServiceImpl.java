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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.api.ClientMessageType;
import de.bausdorf.simracing.racecontrol.api.MessageConstants.MessageType;
import de.bausdorf.simracing.racecontrol.api.MessageProcessor;
import de.bausdorf.simracing.racecontrol.api.InvalidClientMessageException;
import de.bausdorf.simracing.racecontrol.api.MessageConstants.Message;
import de.bausdorf.simracing.racecontrol.api.RacecontrolDataService;
import de.bausdorf.simracing.racecontrol.api.UnsupportedClientException;
import de.bausdorf.simracing.racecontrol.util.MapTools;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class RacecontrolDataServiceImpl implements RacecontrolDataService {

	private final ObjectMapper objectMapper;
	private final MessageProcessor messageProcessor;

	public RacecontrolDataServiceImpl(@Autowired MessageProcessor messageProcessor) {
		this.objectMapper = new ObjectMapper();
		this.messageProcessor = messageProcessor;
	}

	@Override
	@PostMapping(value = "/clientmessage", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String receiveClientData(@RequestBody String clientString) {

		// Maybe Json is escaped - so remove escape characters
		Map<String, Object> clientMessage = readClientMessage(clientString.replace("\\", ""));

		ClientMessage msg;
		try {
			msg = this.validateClientMessage(clientMessage);
			if (msg.getType() != ClientMessageType.PING) {
				messageProcessor.processMessage(msg);
			}
			return msg.getType().name();
		} catch (InvalidClientMessageException e) {
			log.warn(e.getMessage());
			return "VALIDATION_ERROR";
		} catch (UnsupportedClientException e) {
			log.warn(e.getMessage());
			return "UNSUPPORTED_CLIENT";
		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	private Map<String, Object> readClientMessage(String clientString) {
		try {
			// Maybe json string is quoted - so remove quotes if present
			if (clientString.startsWith("\"") && clientString.endsWith("\"")) {
				clientString = clientString.substring(1, clientString.length() - 1);
			}
			TypeReference<HashMap<String, Object>> typeRef
					= new TypeReference<HashMap<String, Object>>() {
			};
			return objectMapper.readValue(clientString, typeRef);
		} catch (JsonProcessingException e) {
			throw new InvalidClientMessageException(e.getMessage());
		}
	}

	private ClientMessage validateClientMessage(Map<String, Object> clientMessage) {
		String clientId = clientMessage.get(Message.CLIENT_ID).toString();

		String messageType = (String) clientMessage.get(Message.MESSAGE_TYPE);
		String clientVersion = (String) clientMessage.get(Message.VERSION);
		String sessionId = (String) clientMessage.get(Message.SESSION_ID);
		String teamId = clientMessage.get(Message.TEAM_ID).toString();

		if(messageType == null) {
			throw new InvalidClientMessageException("No message type in message");
		}
		if(clientVersion == null || clientVersion.isEmpty()) {
			throw new InvalidClientMessageException("No message version");
		}
		if(clientId == null) {
			throw new InvalidClientMessageException("Message without client id");
		}
		if(MessageType.EVENTDATA_NAME.equalsIgnoreCase(messageType)
				&& ("-1".equalsIgnoreCase(clientId) || "0".equalsIgnoreCase(teamId))) {
			throw new InvalidClientMessageException("Event message with invalid client or team id");
		}
		if(sessionId == null) {
			throw new InvalidClientMessageException("Message without session id");
		}

		try {
			return ClientMessage.builder()
					.type(ClientMessageType.fromJsonKey(messageType))
					.version(clientVersion)
					.sessionId(sessionId)
					.teamId(teamId)
					.driverId(clientId)
					.lap(MapTools.intFromMap(Message.SESSION_LAP, clientMessage))
					.type(ClientMessageType.fromJsonKey(MapTools.stringFromMap(Message.MESSAGE_TYPE, clientMessage)))
					.payload((Map<String, Object>) clientMessage.get(Message.PAYLOAD))
					.build();
		} catch (Exception e) {
			throw new InvalidClientMessageException(e.getMessage());
		}
	}
}
