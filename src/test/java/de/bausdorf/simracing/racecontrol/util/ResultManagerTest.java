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

import de.bausdorf.simracing.racecontrol.orga.model.DriverPermission;
import de.bausdorf.simracing.racecontrol.orga.model.PermitSessionResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class ResultManagerTest {
    @Autowired
    ResultManager resultManager;

    @Test
    void testFetchPermitSessionResult() {
        List<PermitSessionResult> permitSessionResults = resultManager.fetchPermitSessionResult(2L, 51632638L);
//        List<PermitSessionResult> permitSessionResults = resultManager.fetchPermitSessionResult(1L, 50380624L);
//        List<PermitSessionResult> permitSessionResults = resultManager.fetchPermitSessionResult(2L, 43352007L);

        permitSessionResults.forEach(data -> {
            log.info("#{} {}({}) - {}({}): {} laps, Fast: {}, Avg: {}, Slow: {}", data.getId(),
                    data.getName(), data.getIracingId(), data.getCarName(), data.getCarId(), data.getLapCount(),
                    data.getFastestLapTime(), data.getAverageLapTime(), data.getSlowestLapTime());
        });
    }

    @Test
    void testUpdatePermissions() {
        List<DriverPermission> permissionList = resultManager.updatePermissions(2L, 51632638L);
//        List<DriverPermission> permissionList = resultManager.updatePermissions(1L, 50380624L);
        permissionList.forEach(p -> log.info(p.toString()));
    }

    @Test
    void testGetTeamPermissionTime() {
        Duration teamPermissionTime = resultManager.getTeamPermissionTime(1L, List.of(372473L, 315956L, 229120L));

        log.info("Team Permit Time: {}", TimeTools.lapDisplayTimeFromDuration(teamPermissionTime));
    }

    @Test
    void testGetTeamPermissionTimeOneDriverWithoutPermission() {
        Duration teamPermissionTime = resultManager.getTeamPermissionTime(1L, List.of(372473L, 315956L, 229120L, 0L));

        log.info("Team Permit Time: {}", TimeTools.lapDisplayTimeFromDuration(teamPermissionTime));
    }

    @Test
    void testGetTeamPermissionTimeEmptyList() {
        Duration teamPermissionTime = resultManager.getTeamPermissionTime(1L, List.of());

        log.info("Team Permit Time: {}", TimeTools.lapDisplayTimeFromDuration(teamPermissionTime));
        Assertions.assertEquals(Duration.ZERO, teamPermissionTime);
    }}