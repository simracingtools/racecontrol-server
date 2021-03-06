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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import de.bausdorf.simracing.racecontrol.web.model.UserProfileView;
import de.bausdorf.simracing.racecontrol.web.model.UserSearchView;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserType;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class UserAdminController extends ControllerBase {

	public static final String USER_LIST = "userList";
	public static final String SEARCH_VIEW = "searchView";
	public static final String PROFILE_VIEW = "profile";
	public static final String ADMIN_VIEW = "useradmin";

	private final IRacingClient iRacingClient;

	public UserAdminController(@Autowired IRacingClient iRacingClient) {
		this.iRacingClient = iRacingClient;
	}

	@GetMapping("/useradmin")
	@Secured("ROLE_SYSADMIN")
	public String adminView(Model model) {
		this.activeNav = "userAdmin";
		model.addAttribute(SEARCH_VIEW, new UserSearchView());
		model.addAttribute(USER_LIST, new ArrayList<>());

		return ADMIN_VIEW;
	}

	@PostMapping("/usersearch")
	@Secured("ROLE_SYSADMIN")
	public String searchUsers(@ModelAttribute UserSearchView searchView, Model model) {
		this.activeNav = "userAdmin";
		if (searchView != null) {
			List<RcUser> userList = findBySearchView(searchView);
			model.addAttribute(SEARCH_VIEW, searchView);
			model.addAttribute(USER_LIST, userList);
		} else {
			model.addAttribute(SEARCH_VIEW, new UserSearchView());
			model.addAttribute(USER_LIST, new ArrayList<RcUser>());
		}
		return ADMIN_VIEW;
	}

	@GetMapping("/savesiteuser")
	@Secured("ROLE_SYSADMIN")
	@Transactional
	public String saveUser(@RequestParam String userId,  @RequestParam String role,
			@RequestParam boolean enabled, @RequestParam boolean locked, @RequestParam boolean expired, Model model) {
		UserSearchView searchView = new UserSearchView();
		if (userId != null) {
			RcUser existingUser = saveUser(userId, role, enabled, locked, expired);
			searchView.setUserRole(role);
			searchView.setUserName(existingUser != null ? existingUser.getName() : "");
		}
		return searchUsers(searchView, model);
	}

	@GetMapping("/profile")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER", "ROLE_NEW"})
	public String getUserProfile(@RequestParam Optional<String> userId, Model model) {
		this.activeNav = "userProfile";
		RcUser user = currentUser();
		if(user.getIRacingId() == 0) {
			addWarning("Please provide your iRacing Id !", model);
		}
		model.addAttribute("profileView", new UserProfileView(user));
		return PROFILE_VIEW;
	}

	@PostMapping("/profile")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER", "ROLE_NEW"})
	@Transactional
	public String saveUserProfile(@ModelAttribute UserProfileView profileView, Model model) {
		RcUser currentUser = currentUser();
		if(currentUser.getIRacingId() == 0 || profileView.getIRacingId() != currentUser.getIRacingId()) {
			List<MemberInfo> idSearch = iRacingClient.searchMembers(Long.toString(profileView.getIRacingId()));
			if(idSearch.isEmpty()) {
				log.warn("No iRacing user with id {} found", profileView.getIRacingId());
			} else {
				MemberInfo identifiedMember = idSearch.stream()
						.filter(s -> s.getCustid() == profileView.getIRacingId()).findFirst().orElse(null);
				profileView.setIRacingName(identifiedMember != null ? identifiedMember.getName() : profileView.getIRacingName());
			}
		}
		RcUser userToSave = profileView.apply(currentUser);
		userRepository.save(userToSave);
		return "redirect:/profile";
	}

	@GetMapping("/deletesiteuser")
	public String deleteUser(@RequestParam String userId, Model model) {
		UserSearchView searchView = new UserSearchView();
		if (userId != null) {
			Optional<RcUser> existingUser = userRepository.findById(userId);
			if (existingUser.isPresent()) {
				userRepository.delete(existingUser.get());
				addInfo("User " + existingUser.get().getName() + " deleted", model);
				searchView.setUserRole(existingUser.get().getUserType().name());
			}
		}
		return searchUsers(searchView, model);
	}

	@ModelAttribute("userTypes")
	public RcUserType[] userTypes() {
		return RcUserType.values();
	}

	private List<RcUser> findBySearchView(UserSearchView searchView) {

		if(searchView.getUserRole().equalsIgnoreCase("*")) {
			return userRepository.findByNameContainingAndEmailContaining(searchView.getUserName(), searchView.getEmail());
		} else {
			return userRepository.findByNameContainingAndEmailContainingAndUserType(
					searchView.getUserName(), searchView.getEmail(), RcUserType.valueOf(searchView.getUserRole()));
		}
	}

	public RcUser saveUser(String userId, String role, boolean enabled, boolean locked, boolean expired) {
		Optional<RcUser> existingUser = userRepository.findById(userId);
		if (existingUser.isPresent()) {
			existingUser.get().setUserType(RcUserType.valueOf(role));
			existingUser.get().setEnabled(enabled);
			existingUser.get().setLocked(locked);
			existingUser.get().setExpired(expired);
			userRepository.save(existingUser.get());
		}
		return existingUser.orElse(null);
	}
}
