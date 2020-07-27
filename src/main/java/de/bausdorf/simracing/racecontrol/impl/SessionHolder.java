package de.bausdorf.simracing.racecontrol.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.api.EventProcessor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SessionHolder implements EventProcessor {

	private Map<String, SessionController> sessionMap;

	public SessionHolder() {
		this.sessionMap = new HashMap<>();
	}

	public SessionController getSessionController(String sessionId) {
		return sessionMap.get(sessionId);
	}

	public void addController(ClientMessage message) {
		log.info("adding session {}", message.getSessionId());
		this.sessionMap.put(message.getSessionId(), new SessionController(message));
	}

	@Override
	public void processEvent(ClientMessage message) {
		SessionController controller = getSessionController(message.getSessionId());
		if(controller == null) {
			addController(message);
		} else {
			controller.processEventMessage(message);
		}
	}
}
