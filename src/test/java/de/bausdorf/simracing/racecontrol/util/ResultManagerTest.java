package de.bausdorf.simracing.racecontrol.util;

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

import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class ResultManagerTest {
    @Autowired
    ResultManager resultManager;
    @Autowired
    TeamRegistrationRepository registrationRepository;
    @Autowired
    DriverPermissionRepository driverPermissionRepository;
    @Autowired
    RcUserRepository userRepository;

    @Test
    void testFetchPermitSessionResult() {
//        List<PermitSessionResult> permitSessionResults = resultManager.fetchPermitSessionResult(2L, 51632638L, null);
        List<PermitSessionResult> permitSessionResults = resultManager.fetchPermitSessionResult(1L, 51810178L, new TrackSession());
//        List<PermitSessionResult> permitSessionResults = resultManager.fetchPermitSessionResult(2L, 43352007L);

        permitSessionResults.forEach(data -> log.info("#{} {}({}) - {}({}): {} laps, Fast: {}, Avg: {}, Slow: {}", data.getId(),
                data.getName(), data.getIracingId(), data.getCarName(), data.getCarId(), data.getLapCount(),
                data.getFastestLapTime(), data.getAverageLapTime(), data.getSlowestLapTime()));
    }

    @Test
    void testUpdatePermissions() {
//        List<DriverPermission> permissionList = resultManager.updatePermissions(2L, 51632638L);
        List<DriverPermission> permissionList = resultManager.updatePermissions(1L, 51810178L);
        permissionList.forEach(p -> log.info(p.toString()));
    }

    @Test
    void testGetTeamPermissionTime() {
        List<DriverPermission> driverPermissions = driverPermissionRepository.findAllByEventIdAndCarIdAndIracingIdInOrderByPermissionTimeAsc(1L, 143L, List.of(372473L, 315956L, 229120L));
        Duration teamPermissionTime = resultManager.getTeamPermissionTime(driverPermissions);

        log.info("Team Permit Time: {}", TimeTools.lapDisplayTimeFromDuration(teamPermissionTime));
    }

    @Test
    void testGetTeamPermissionTimeOneDriverWithoutPermission() {
        List<DriverPermission> driverPermissions = driverPermissionRepository.findAllByEventIdAndCarIdAndIracingIdInOrderByPermissionTimeAsc(1L, 143L, List.of(372473L, 315956L, 229120L, 0L));

        Duration teamPermissionTime = resultManager.getTeamPermissionTime(driverPermissions);

        log.info("Team Permit Time: {}", TimeTools.lapDisplayTimeFromDuration(teamPermissionTime));
    }

    @Test
    void testGetTeamsWithoutPermisstion() {
        Map<RcUser, List<TeamRegistration>> withoutPermit = resultManager.missingTeamPermitContacts(9001L);

        withoutPermit.forEach((user, list )-> log.info("{}({}): {}", user.getName(), user.getEmail(), list.stream().map(TeamRegistration::getName).collect(Collectors.toList())));
    }

    @Test
    void testGetTeamPermissionTimeEmptyList() {
        Duration teamPermissionTime = resultManager.getTeamPermissionTime(List.of());

        log.info("Team Permit Time: {}", TimeTools.lapDisplayTimeFromDuration(teamPermissionTime));
    }

    @Test
    void testGetDriverPermissionRatio() {
        String permissionRatio = resultManager.getDriverPermissionRatio(9001L);
        log.info("Driver permission ratio for event {}: {}", 9001L, permissionRatio);
    }
}
