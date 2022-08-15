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

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.bausdorf.simracing.racecontrol.util.FileTypeEnum;
import de.bausdorf.simracing.racecontrol.util.LocaleTools;
import de.bausdorf.simracing.racecontrol.util.LocaleView;
import de.bausdorf.simracing.racecontrol.util.UploadFileManager;
import de.bausdorf.simracing.racecontrol.web.model.TimezoneView;
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
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
public class UserAdminController extends ControllerBase {

	public static final String USER_LIST = "userList";
	public static final String SEARCH_VIEW = "searchView";
	public static final String PROFILE_VIEW = "profile";
	public static final String ADMIN_VIEW = "useradmin";

	private final IRacingClient iRacingClient;
	private final UploadFileManager uploadFileManager;

	public UserAdminController(@Autowired IRacingClient iRacingClient,
							   @Autowired UploadFileManager uploadFileManager) {
		this.iRacingClient = iRacingClient;
		this.uploadFileManager = uploadFileManager;
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
	public String getUserProfile(@RequestParam Optional<String> messages, Model model) {
		messages.ifPresent(s -> decodeMessagesToModel(s, model));
		this.activeNav = "userProfile";
		RcUser user = currentUser();
		if(user.getIRacingId() == 0) {
			addWarning("Please provide your iRacing Id !", model);
		} else {
			Optional<MemberInfo> idSearch = iRacingClient.getMemberInfo(user.getIRacingId());
			if(idSearch.isEmpty()) {
				addWarning("iRacing ID " + user.getIRacingId() + " is unknown in iRacing", model);
			}
		}
		if(user.getTimezone() == null) {
			addWarning("Timezone was chosen from your browsers location. You can change it to your preferred timezone and click 'Save'!", model);
		}
		model.addAttribute("profileView", new UserProfileView(user));
		return PROFILE_VIEW;
	}

	@PostMapping("/profile")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD", "ROLE_STAFF", "ROLE_REGISTERED_USER", "ROLE_NEW"})
	@Transactional
	public String saveUserProfile(@ModelAttribute UserProfileView profileView, Model model) {
		RcUser currentUser = currentUser();
		if(currentUser.getIRacingId() == 0 || profileView.getIRacingId() != currentUser.getIRacingId()
				|| currentUser.getName() == null || currentUser.getName().isEmpty()) {
			Optional<MemberInfo> idSearch = iRacingClient.getMemberInfo(profileView.getIRacingId());
			if(idSearch.isEmpty()) {
				log.warn("{}: No iRacing user with id {} found", currentUser.getName(), profileView.getIRacingId());
				profileView.setIRacingId(currentUser.getIRacingId());
				addError("iRacing ID " + profileView.getIRacingId() + " not present in iRacing service.", model);
			} else {
				Optional<RcUser> userForChangedId = userRepository.findByiRacingId(profileView.getIRacingId());
				if(userForChangedId.isPresent() && !userForChangedId.get().getOauthId().equals(currentUser.getOauthId())) {
					addError("A user for iRacingID " + profileView.getIRacingId() + " is already registered", model);
					profileView.setIRacingId(currentUser.getIRacingId());
				} else {
					profileView.setName(idSearch.get().getName());
					if (currentUser.getUserType() == RcUserType.NEW) {
						profileView.setUserType(RcUserType.REGISTERED_USER.toString());
					}
				}
			}
		}
		RcUser userToSave = profileView.apply(currentUser);
		userRepository.save(userToSave);
		return redirectBuilder(PROFILE_VIEW).build(model);
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

	@PostMapping("/profile-image-upload")
	public String uploadProfileImage(@RequestParam("file") MultipartFile multipartFile, Model model) {
		RcUser user = currentUser();
		try {
			String logoUrl = uploadFileManager.uploadUserFile(multipartFile, Long.toString(user.getIRacingId()), FileTypeEnum.LOGO);
			user.setImageUrl(logoUrl);
			userRepository.save(user);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			addError(e.getMessage(), model);
		}
		return redirectBuilder(PROFILE_VIEW).build(model);
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

	@ModelAttribute("timezones")
	List<TimezoneView> availableZoneIds() {
		return ZoneId.getAvailableZoneIds().stream()
				.filter(s -> s.chars().noneMatch(Character::isLowerCase))
				.map(s -> TimezoneView.fromZoneId(ZoneId.of(s)))
				.sorted(Comparator.comparing(TimezoneView::getUtcOffset))
				.collect(Collectors.toList());
	}

	@ModelAttribute("countries")
	List<LocaleView> availableCountries() {
		return LocaleTools.getLocaleViews();
	}
}
