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

import java.time.ZonedDateTime;
import java.util.Optional;

import de.bausdorf.simracing.racecontrol.web.security.*;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import de.bausdorf.simracing.racecontrol.web.model.UserProfileView;

public class ControllerBase {

	public static final String MESSAGES = "messages";
	@Autowired
	RcUserRepository userRepository;

	@Autowired
	RacecontrolServerProperties config;

	String activeNav = "";

	@ModelAttribute("navigation")
	String activeNav() {
		return activeNav;
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
				.created(ZonedDateTime.now())
				.build());
	}

	@ModelAttribute(MESSAGES)
	Messages messages() {
		return new Messages();
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
}
