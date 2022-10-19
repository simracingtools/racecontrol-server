package de.bausdorf.simracing.racecontrol.web;

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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bausdorf.simracing.racecontrol.web.security.*;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import de.bausdorf.simracing.racecontrol.web.model.UserProfileView;
import org.thymeleaf.util.StringUtils;

@Slf4j
public class ControllerBase {

	public static final String MESSAGES = "messages";
	@Autowired
	RcUserRepository userRepository;

	@Autowired
	RacecontrolServerProperties config;

	String activeNav = "";
	ObjectMapper mapper = new ObjectMapper();

	@ModelAttribute("navigation")
	String activeNavigationOrTab() {
		return activeNav == null ? "" : activeNav;
	}

	public void setActiveNav(String nav, Model model) {
		activeNav = nav;
		model.addAttribute("navigation", activeNav);
	}

	@ModelAttribute("user")
	public UserProfileView currentUserProfile() {
		return new UserProfileView(currentUser());
	}

	@ModelAttribute("serverVersion")
	public String serverVersion() {
		return "RaceControl server version: " + config.getVersion();
	}

	protected RcUser currentUser() {
		Optional<RcUser> details = Optional.empty();
		if(SecurityContextHolder.getContext().getAuthentication() instanceof KeycloakAuthenticationToken) {
			KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
			AccessToken auth = token.getAccount().getKeycloakSecurityContext().getToken();
			details =auth != null ? userRepository.findById(auth.getSubject()) : Optional.empty();
		}
		return details.orElseGet(() -> RcUser.builder()
				.name("Unknown")
				.oauthId("")
				.eventFilter(RcAuthenticationProvider.defaultEventFilter())
				.userType(RcUserType.NEW)
				.created(LocalDateTime.now())
				.timezone(ZoneId.systemDefault())
				.build());
	}

	protected String messagesEncoded(@NonNull Model model) {
		try {
			Messages messages = ((Messages)model.getAttribute(MESSAGES));
			if(messages == null || messages.isEmpty()) {
				return null;
			}
			String messagesJson = mapper.writeValueAsString(messages.toArray());
			return Base64.getEncoder().encodeToString(messagesJson.getBytes());
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	protected void decodeMessagesToModel(String messagesEncoded, Model model) {
		try {
			String messagesDecoded = new String(Base64.getDecoder().decode(messagesEncoded));
			Messages messages = mapper.readValue(messagesDecoded, Messages.class);
			model.addAttribute(MESSAGES, messages);
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
		}
	}

	@ExceptionHandler
	public String handleException(Exception e) {
		if (e instanceof AccessDeniedException) {
			log.debug(e.getMessage(), e);
		} else {
			log.error(e.getMessage(), e);
		}
		if(e instanceof AccessDeniedException) {
			return redirectBuilder("/index")
					.withParameter("error", "Access denied")
					.build(null);
		}
		return "error";
	}

	@ModelAttribute(MESSAGES)
	Messages messages() {
		return new Messages();
	}


	protected RedirectBuilder redirectBuilder(String viewName) {
		return new RedirectBuilder(viewName);
	}

	protected void prepareMessageModel(Optional<String> error, Optional<String> warn, Optional<String> info, Model model) {
		error.ifPresent(s -> addError(s, model));
		warn.ifPresent(s -> addWarning(s, model));
		info.ifPresent(s -> addWarning(s, model));
	}

	protected void addMessage(Message msg, Model model) {
		Messages messages = ((Messages)model.getAttribute(MESSAGES));
		if( messages == null ) {
			messages = messages();
			model.addAttribute(MESSAGES, messages);
		}
		messages.add(msg);
	}

	protected void addError(String error, Model model) {
		addMessage(Message.builder()
				.type(Message.ERROR)
				.text(error)
				.build(), model);
	}

	protected void addWarning(String warning, Model model) {
		addMessage(Message.builder()
				.type(Message.WARN)
				.text(warning)
				.build(), model);
	}

	protected void addInfo(String info, Model model) {
		addMessage(Message.builder()
				.type(Message.INFO)
				.text(info)
				.build(), model);
	}

	public class RedirectBuilder {
		private String redirectUri = "";
		private int parameterCount = 0;

		public RedirectBuilder(String viewName) {
			redirectUri += (viewName.startsWith("/") ? "redirect:" : "redirect:/") + viewName;
		}

		public RedirectBuilder withParameter(String name, String value) {
			if(!StringUtils.isEmpty(value)) {
				redirectUri += (parameterCount == 0 ? "?" : "&") + name + "=" + value;
				parameterCount++;
			}
			return this;
		}

		public RedirectBuilder withParameter(String name, long value) {
			return withParameter(name, value == 0L ? null : Long.toString(value));
		}

		public String build(@Nullable Model model) {
			if(model == null) {
				return redirectUri;
			}
			String encodedMessages = messagesEncoded(model);
			if(encodedMessages != null) {
				redirectUri += (parameterCount == 0 ? "?" : "&") + MESSAGES + "=" + encodedMessages;
			}
			return redirectUri;
		}
	}
}
