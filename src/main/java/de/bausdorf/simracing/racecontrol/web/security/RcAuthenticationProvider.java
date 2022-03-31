package de.bausdorf.simracing.racecontrol.web.security;

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
        final String userName = userDetails.getName();
        final long iRacingId = Long.parseLong((String)userDetails.getOtherClaims().get("iRacingID"));
        Optional<RcUser> user = userRepository.findById(userId);
        if(user.isEmpty()) {

            Optional<MemberInfo> identifiedMember = iRacingClient.getMemberInfo(iRacingId);
            if(identifiedMember.isEmpty()) {
                log.info("No iRacing user with id {} found", iRacingId);
            }
            userRepository.save(RcUser.builder()
                    .email(userDetails.getEmail())
                    .oauthId(userId)
                    .imageUrl(userDetails.getPicture())
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
                    .iRacingId(identifiedMember.map(MemberInfo::getCustid).orElse(0))
                    .iRacingName(identifiedMember.map(MemberInfo::getName).orElse(null))
                    .build()
            );
        } else {
            if(user.get().getEventFilter().isEmpty()) {
                user.get().setEventFilter(defaultEventFilter());
                userRepository.save(user.get());
            }
        }

        return generateAuthenticationToken(authentication, user);
    }

    private Authentication generateAuthenticationToken(Authentication authentication, Optional<RcUser>  user) {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken)authentication;
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        Iterator<?> var4 = token.getAccount().getRoles().iterator();

        while(var4.hasNext()) {
            String role = (String)var4.next();
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
