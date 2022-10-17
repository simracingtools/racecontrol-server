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

import de.bausdorf.simracing.irdataapi.model.*;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.orga.model.DriverPermission;
import de.bausdorf.simracing.racecontrol.orga.model.DriverPermissionRepository;
import de.bausdorf.simracing.racecontrol.orga.model.PermitSessionResult;
import de.bausdorf.simracing.racecontrol.orga.model.PermitSessionResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ResultManager {
    private final PermitSessionResultRepository permitSessionResultRepository;
    private final DriverPermissionRepository driverPermissionRepository;
    private final IRacingClient dataClient;
    private final RacecontrolServerProperties config;

    public ResultManager(@Autowired PermitSessionResultRepository permitSessionResultRepository,
                         @Autowired DriverPermissionRepository driverPermissionRepository,
                         @Autowired IRacingClient dataClient,
                         @Autowired RacecontrolServerProperties racecontrolServerProperties) {
        this.permitSessionResultRepository = permitSessionResultRepository;
        this.driverPermissionRepository = driverPermissionRepository;
        this.dataClient = dataClient;
        this.config = racecontrolServerProperties;
    }

    @Transactional
    public List<PermitSessionResult> fetchPermitSessionResult(long eventId, long subsessionId) {
        SubsessionResultDto subsessionResult = dataClient.getSubsessionResult(subsessionId).orElse(null);
        if (subsessionResult == null) {
            return List.of();
        }

        SessionResultDto[] sessionResults = subsessionResult.getSessionResults();
        Optional<SessionResultDto> qualifyResult = Arrays.stream(sessionResults)
                .filter(sessionResult -> sessionResult.getSimsessionName().equals("QUALIFY"))
                .findFirst();
        if (qualifyResult.isPresent()) {
            List<PermitSessionResult> resultList = new ArrayList<>();

            Arrays.stream(qualifyResult.get().getResults()).forEach(memberResult -> {
                MemberSessionResultDto memberResultToUse = memberResult;
                if (memberResult.getCustId() == null && memberResult.getTeamId() != null) {
                    log.info("Member result list length for {}: {}", memberResult.getDisplayName(), memberResult.getDriverResults().length);
                    memberResultToUse = memberResult.getDriverResults()[0];
                }
                resultList.add(fetchPermitSessionResult(eventId, subsessionId, memberResultToUse));
            });

            findSlowestLap(subsessionId, qualifyResult.get().getSimsessionNumber(), resultList);

            List<PermitSessionResult> existingSessionResult = permitSessionResultRepository.findByEventIdAndSubsessionId(eventId, subsessionId);
            if (!existingSessionResult.isEmpty()) {
                permitSessionResultRepository.deleteAllByEventIdAndSubsessionId(eventId, subsessionId);
            }
            permitSessionResultRepository.saveAll(resultList);
            return resultList.stream().sorted(Comparator.comparingLong(r -> r.getAverageLapTime().toMillis()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public List<DriverPermission> updatePermissions(long eventId, long subsessionId) {
        List<PermitSessionResult> sessionResults = permitSessionResultRepository.findByEventIdAndSubsessionId(eventId, subsessionId);
        List<DriverPermission> resultList = new ArrayList<>();
        sessionResults.forEach(result -> {
            if (isDriverPermitted(result)) {
                Optional<DriverPermission> driverPermission = driverPermissionRepository
                        .findByEventIdAndIracingIdAndCarId(eventId, result.getIracingId(), result.getCarId());
                driverPermission.ifPresentOrElse(
                        permit -> {
                            if (permit.getPermissionTime() >= result.getAverageLapTime().toMillis()) {
                                permit.setEventId(eventId);
                                permit.setSubsessionId(subsessionId);
                                updatePermission(permit, result);
                                resultList.add(driverPermissionRepository.save(permit));
                            }
                        },
                        () -> {
                            DriverPermission permit = new DriverPermission();
                            permit.setEventId(eventId);
                            permit.setSubsessionId(subsessionId);
                            permit.setIracingId(result.getIracingId());
                            permit.setDriverName(result.getName());
                            permit.setCarId(result.getCarId());
                            updatePermission(permit, result);
                            resultList.add(driverPermissionRepository.save(permit));
                        }
                );
            }
        });
        return resultList;
    }

    public boolean isDriverPermitted(PermitSessionResult result) {
        boolean permitted = true;
        if (result.getLapCount() < config.getRequiredCleanPermitLapNum()) {
            permitted = false;
        }
        if (result.getSlowestLapTime().toSeconds() - result.getFastestLapTime().toSeconds() > config.getMaxPermitLapTimeDiffSeconds()) {
            permitted = false;
        }
        return permitted;
    }

    public Duration getTeamPermissionTime(long eventId, List<Long> iracingIds) {
        List<DriverPermission> driverPermissions = driverPermissionRepository.findAllByEventIdAndIracingIdInOrderByPermissionTimeAsc(eventId, iracingIds);
        if (log.isDebugEnabled()) {
            driverPermissions.forEach(p -> log.debug("{}({}) {}", p.getDriverName(), p.getIracingId(), p.getDisplayTime()));
        }
        OptionalDouble teamPermitTime = driverPermissions.stream()
                .mapToLong(DriverPermission::getPermissionTime)
                .average();
        long millis = (long)teamPermitTime.orElse(0.0D);
        return Duration.ofMillis(millis);
    }

    private void findSlowestLap(long subsessionId, long simsessionNumber, List<PermitSessionResult> resultList) {
        List<LapChartEntryDto> lapData = dataClient.getLapChartData(subsessionId, simsessionNumber);
        lapData.forEach(data -> {
            PermitSessionResult permitSessionResult = resultList.stream().filter(r -> r.getIracingId() == data.getCustId()).findFirst().orElse(null);
            if (permitSessionResult == null) {
                throw new IllegalStateException("Driver id " + data.getCustId() + " has no lap data");
            }
            if (isValidLap(data)) {
                permitSessionResult.setLapCount(permitSessionResult.getLapCount() + 1);
            } else {
                annotateInvalidLap(data, permitSessionResult);
            }

            Duration lapDuration = Duration.ofMillis(data.getLapTime() / 10);
            if (permitSessionResult.getSlowestLapTime() == null || permitSessionResult.getSlowestLapTime().toMillis() < lapDuration.toMillis()) {
                permitSessionResult.setSlowestLapTime(lapDuration);
            }
        });
    }

    private static boolean isValidLap(final LapChartEntryDto lapData) {
        List<String> lapEvents = List.of(lapData.getLapEvents());
        return lapData.getLapTime() != -1 && !lapEvents.contains("invalid");
    }

    private static void updatePermission(DriverPermission permit, PermitSessionResult result) {
        permit.setDriverName(result.getName());
        permit.setCarName(result.getCarName());
        permit.setPermitDateTime(result.getBestLapAt());
        permit.setPermissionTime(result.getAverageLapTime().toMillis());
        permit.setDisplayTime(TimeTools.lapDisplayTimeFromDuration(result.getAverageLapTime()));
    }
    private static PermitSessionResult fetchPermitSessionResult(long eventId, long subsessionId, MemberSessionResultDto memberResult) {
        PermitSessionResult permitSessionResult = new PermitSessionResult();
        permitSessionResult.setSubsessionId(subsessionId);
        permitSessionResult.setEventId(eventId);
        permitSessionResult.setIracingId(memberResult.getCustId());
        permitSessionResult.setName(memberResult.getDisplayName());
        permitSessionResult.setCarId(memberResult.getCarId());
        permitSessionResult.setCarName(memberResult.getCarName());
        permitSessionResult.setAverageLapTime(Duration.ofMillis(memberResult.getAverageLap() / 10));
        permitSessionResult.setFastestLapTime(Duration.ofMillis(memberResult.getBestLapTime() / 10));
        permitSessionResult.setBestLapAt(memberResult.getBestQualLapAt().toOffsetDateTime());
        return permitSessionResult;
    }

    private static void annotateInvalidLap(LapChartEntryDto data, PermitSessionResult permitSessionResult) {
        if (!"0".equals(data.getLapNumber())) {
            if (permitSessionResult.getEvents() == null) {
                permitSessionResult.setEvents("Lap " + data.getLapNumber() + ": " + Arrays.toString(data.getLapEvents()));
            } else {
                permitSessionResult.setEvents(permitSessionResult.getEvents()
                        + ", Lap " + data.getLapNumber() + ": " + Arrays.toString(data.getLapEvents()));
            }
        }
    }
}
