package de.bausdorf.simracing.racecontrol.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.api.ClientMessageType;
import de.bausdorf.simracing.racecontrol.api.EventProcessor;
import de.bausdorf.simracing.racecontrol.api.EventType;
import de.bausdorf.simracing.racecontrol.api.InvalidClientMessageException;
import de.bausdorf.simracing.racecontrol.api.MessageConstants;
import de.bausdorf.simracing.racecontrol.api.MessageConstants.EventData;
import de.bausdorf.simracing.racecontrol.api.MessageConstants.Message;
import de.bausdorf.simracing.racecontrol.api.RacecontrolDataService;
import de.bausdorf.simracing.racecontrol.api.UnsupportedClientException;
import de.bausdorf.simracing.racecontrol.util.MapTools;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class RacecontrolDataServiceImpl implements RacecontrolDataService {

	private final ObjectMapper objectMapper;
	private final EventProcessor eventProcessor;

	public RacecontrolDataServiceImpl(@Autowired EventProcessor eventProcessor) {
		this.objectMapper = new ObjectMapper();
		this.eventProcessor = eventProcessor;
	}

	@Override
	@PostMapping(value = "/clientmessage")
	public String receiveClientData(@RequestBody String clientString) {

		// Maybe Json is escaped - so remove escape characters
		Map<String, Object> clientMessage = readClientMessage(clientString.replace("\\", ""));

		try {
			ClientMessage msg = this.validateClientMessage(clientMessage);
			if (msg.getType() != ClientMessageType.PING) {
				eventProcessor.processEvent(msg);
			}
			return msg.getType().name();
		} catch (InvalidClientMessageException e) {
			log.warn(e.getMessage());
			return "VALIDATION_ERROR";
		} catch (UnsupportedClientException e) {
			log.warn(e.getMessage());
			return "UNSUPPORTED_CLIENT";
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
		Integer clientId = (Integer) clientMessage.get(MessageConstants.Message.CLIENT_ID);

		String messageType = (String) clientMessage.get(Message.MESSAGE_TYPE);
		String clientVersion = (String) clientMessage.get(MessageConstants.Message.VERSION);
		String sessionId = (String) clientMessage.get(MessageConstants.Message.SESSION_ID);
		Integer teamId = (Integer) clientMessage.get(MessageConstants.Message.TEAM_ID);

		if (messageType == null) {
			throw new InvalidClientMessageException("No message type in message");
		}
		if (clientVersion == null || clientVersion.isEmpty()) {
			throw new InvalidClientMessageException("No message version");
		}

		if (clientId == null) {
			throw new InvalidClientMessageException("Message without client id");
		}
		if (sessionId == null) {
			throw new InvalidClientMessageException("Message without session id");
		}

		try {
			return ClientMessage.builder()
					.type(ClientMessageType.fromJsonKey(messageType))
					.version(clientVersion)
					.sessionId(sessionId)
					.teamId(teamId)
					.driverId(clientId)
					.eventType(EventType.ofType(MapTools.stringFromMap(EventData.EVENT_TYPE, clientMessage)))
					.carNo(MapTools.intFromMap(EventData.CAR_NUMBER, clientMessage))
					.driverName(MapTools.stringFromMap(EventData.DRIVER_NAME, clientMessage))
					.iRating(MapTools.intFromMap(EventData.I_RATING, clientMessage))
					.lap(MapTools.intFromMap(EventData.LAP, clientMessage))
					.teamName(MapTools.stringFromMap(EventData.TEAM_NAME, clientMessage))
					.sessionTime(TimeTools.getFromIracingDuration(MapTools.doubleFromMap(Message.SESSION_TIME, clientMessage)))
					.eventNo(MapTools.intFromMap(EventData.EVENT_NO, clientMessage))
					.build();
		} catch (Exception e) {
			throw new InvalidClientMessageException(e.getMessage());
		}
	}
}
