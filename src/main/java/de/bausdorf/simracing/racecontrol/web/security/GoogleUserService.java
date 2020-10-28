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
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleUserService extends OidcUserService implements UserDetailsService {

    private final RcUserRepository userRepository;

    public GoogleUserService(@Autowired RcUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();
        String userId = (String) attributes.get("sub");
        Optional<RcUser> user = userRepository.findById(userId);
        if(!user.isPresent()) {
            userRepository.save(RcUser.builder()
                    .email((String) attributes.get("email"))
                    .oauthId(userId)
                    .imageUrl((String) attributes.get("picture"))
                    .name((String) attributes.get("name"))
                    .userType(userRepository.count() == 0 ? RcUserType.SYSADMIN : RcUserType.NEW)
                    .created(ZonedDateTime.now())
                    .subscriptionType(SubscriptionType.NONE)
                    .lastSubscription(ZonedDateTime.now())
                    .lastAccess(ZonedDateTime.now())
                    .locked(false)
                    .expired(false)
                    .enabled(true)
                    .build()
            );
        }
        return oidcUser;
    }

    @Override
    public UserDetails loadUserByUsername(String s) {
        Optional<RcUser> user = userRepository.findByEmail(s);
        return user.orElse(null);
    }

}
