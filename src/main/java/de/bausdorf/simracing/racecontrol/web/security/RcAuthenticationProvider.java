package de.bausdorf.simracing.racecontrol.web.security;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2022 bausdorf engineering
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

import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import de.bausdorf.simracing.racecontrol.live.api.EventType;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.stereotype.Component;
import org.keycloak.representations.AccessToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Component
@Slf4j
public class RcAuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider {

    private final RcUserRepository userRepository;
    private final IRacingClient iRacingClient;
    private final GrantedAuthoritiesMapper authoritiesMapper;

    public RcAuthenticationProvider(@Autowired RcUserRepository userRepository, @Autowired IRacingClient iRacingClient) {
        this.userRepository = userRepository;
        this.iRacingClient = iRacingClient;
        this.authoritiesMapper = new SimpleAuthorityMapper();
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken)authentication;

        AccessToken userDetails = token.getAccount().getKeycloakSecurityContext().getToken();
        final String userId = userDetails.getSubject();
        final long iRacingId = Long.parseLong((String)userDetails.getOtherClaims().get("iRacingID"));
        Optional<RcUser> user = userRepository.findById(userId);
        if(user.isEmpty()) {

            Optional<MemberInfo> identifiedMember = iRacingClient.getMemberInfo(iRacingId);
            RcUserType newUserType = RcUserType.REGISTERED_USER;
            if(identifiedMember.isEmpty()) {
                log.warn("No iRacing user with id {} found", iRacingId);
                newUserType = RcUserType.NEW;
            } else {
                Optional<RcUser> existingUserForId = userRepository.findByiRacingId(iRacingId);
                if(existingUserForId.isPresent()) {
                    log.warn("iRacing ID {} already registered for user with email {}", iRacingId, existingUserForId.get().getEmail());
                    newUserType = RcUserType.NEW;
                    identifiedMember = Optional.empty();
                }
            }

            user = Optional.of(userRepository.save(RcUser.builder()
                    .email(userDetails.getEmail())
                    .oauthId(userId)
                    .imageUrl(userDetails.getPicture())
                    .name(usernameFromIRacingName(identifiedMember.map(MemberInfo::getName).orElse(null)))
                    .userType(userRepository.count() == 0 ? RcUserType.SYSADMIN : newUserType)
                    .created(LocalDateTime.now())
                    .subscriptionType(SubscriptionType.NONE)
                    .eventFilter(defaultEventFilter())
                    .lastSubscription(LocalDateTime.now())
                    .lastAccess(LocalDateTime.now())
                    .locked(false)
                    .expired(false)
                    .enabled(true)
                    .iRacingId(identifiedMember.map(MemberInfo::getCustid).orElse(0))
                    .build())
            );
        } else {
            user.get().setLastAccess(LocalDateTime.now());
            if(user.get().getEventFilter().isEmpty()) {
                user.get().setEventFilter(defaultEventFilter());
            }
            userRepository.save(user.get());
        }

        return generateAuthenticationToken(authentication, user);
    }

    private String usernameFromIRacingName(String iRacingName) {
        if(iRacingName != null) {
            String[] nameParts = iRacingName.split(" ");
            return nameParts[0] + " " + nameParts[nameParts.length - 1];
        }
        return null;
    }

    private Authentication generateAuthenticationToken(Authentication authentication, Optional<RcUser>  user) {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken)authentication;
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        for (String role : token.getAccount().getRoles()) {
            grantedAuthorities.add(new KeycloakRole(role));
        }
        user.ifPresent(u -> grantedAuthorities.add(new KeycloakRole(u.getUserType().toString())));
        return new KeycloakAuthenticationToken(token.getAccount(), token.isInteractive(), authoritiesMapper.mapAuthorities(grantedAuthorities));
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return KeycloakAuthenticationToken.class.isAssignableFrom(aClass);
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
