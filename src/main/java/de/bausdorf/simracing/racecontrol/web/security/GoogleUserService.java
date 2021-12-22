package de.bausdorf.simracing.racecontrol.web.security;

/*-
 * #%L
 * tt-cloud-server
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bausdorf.simracing.racecontrol.api.EventType;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleUserService extends OidcUserService implements UserDetailsService {

    private final RcUserRepository userRepository;
    private final IRacingClient iRacingClient;

    public GoogleUserService(@Autowired RcUserRepository userRepository, @Autowired IRacingClient iRacingClient) {
        this.userRepository = userRepository;
        this.iRacingClient = iRacingClient;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();
        String userId = (String) attributes.get("sub");
        Optional<RcUser> user = userRepository.findById(userId);
        if(!user.isPresent()) {
            final String userName = (String) attributes.get("name");
            List<MemberInfo> nameSearch = iRacingClient.searchMembers(userName);
            MemberInfo identifiedMember = null;
            if(nameSearch.size() > 1) {
                nameSearch.stream().forEach(s -> log.info(s.toString()));
            } else if(nameSearch.size() == 1) {
                identifiedMember = nameSearch.get(0);
            } else {
                log.info("No iRacing user named {} found", userName);
            }
            userRepository.save(RcUser.builder()
                    .email((String) attributes.get("email"))
                    .oauthId(userId)
                    .imageUrl((String) attributes.get("picture"))
                    .name(userName)
                    .userType(userRepository.count() == 0 ? RcUserType.SYSADMIN : RcUserType.NEW)
                    .created(ZonedDateTime.now())
                    .subscriptionType(SubscriptionType.NONE)
                    .eventFilter(defaultEventFilter())
                    .lastSubscription(ZonedDateTime.now())
                    .lastAccess(ZonedDateTime.now())
                    .locked(false)
                    .expired(false)
                    .enabled(true)
                    .iRacingId(identifiedMember != null ? identifiedMember.getCustid() : 0L)
                    .iRacingName(identifiedMember != null ? identifiedMember.getName() : null)
                    .build()
            );
        } else {
            if(user.get().getEventFilter().isEmpty()) {
                user.get().setEventFilter(defaultEventFilter());
                userRepository.save(user.get());
            }
        }
        return oidcUser;
    }

    @Override
    public UserDetails loadUserByUsername(String s) {
        Optional<RcUser> user = userRepository.findByEmail(s);
        return user.orElse(null);
    }

    public static Set<EventType> defaultEventFilter() {
        Set<EventType> defaultSet = new HashSet<>();
        defaultSet.add(EventType.ON_TRACK);
        defaultSet.add(EventType.OFF_TRACK);
        defaultSet.add(EventType.APPROACHING_PITS);
        defaultSet.add(EventType.ENTER_PITLANE);
        defaultSet.add(EventType.EXIT_PITLANE);
        defaultSet.add(EventType.IN_PIT_STALL);
        defaultSet.add(EventType.DRIVER_CHANGE);
        return defaultSet;
    }
}
