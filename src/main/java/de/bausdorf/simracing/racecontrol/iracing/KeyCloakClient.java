package de.bausdorf.simracing.racecontrol.iracing;

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

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class KeyCloakClient {

    private final RacecontrolServerProperties config;

    public KeyCloakClient(@Autowired RacecontrolServerProperties config) {
        this.config = config;
    }

    public void syncUser(RcUser rcUser) {
        try {
            log.debug("Try to sync user {} to keycloak", rcUser.getName());

            UsersResource kcUsers = createClient().realm(config.getKeycloakRealm()).users();
            log.debug("Fetched users resource for realm {}", config.getKeycloakRealm());

            UserResource userResource = kcUsers.get(rcUser.getOauthId());
            UserRepresentation kcUser = userResource.toRepresentation();
            log.debug("Fetched user representation for {}({})", rcUser.getOauthId(), rcUser.getName());

            Optional<String> kcIracingId = kcUser.getAttributes().get("iRacingId").stream().findFirst();
            log.debug("User's keycloak iRacing id is {}", kcIracingId.orElse("null"));
            if (kcIracingId.isPresent() && Long.parseLong(kcIracingId.get()) != rcUser.getIRacingId()) {
                log.info("Changing iRacing id for {} in Keycloak to {}", rcUser.getName(), rcUser.getIRacingId());
                kcUser.singleAttribute("iRacingId", Long.toString(rcUser.getIRacingId()));
            }
            String[] nameParts = rcUser.getName().split(" ");
            log.debug("User name parts: {}", (Object) nameParts);
            log.debug("User name in Keycloak: {}, {}", kcUser.getFirstName(), kcUser.getLastName());
            kcUser.setFirstName(nameParts[0]);
            kcUser.setLastName(nameParts[nameParts.length - 1]);
            userResource.update(kcUser);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private Keycloak createClient() {
        Keycloak keycloak = KeycloakBuilder.builder()
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .serverUrl(config.getKeycloakServer())
                .realm(config.getKeycloakRealm())
                .clientId("admin-cli")
                .clientSecret(config.getKeycloakPassword())
                .build();
        log.info(keycloak.tokenManager().getAccessToken().getError());
        return keycloak;
    }
}
