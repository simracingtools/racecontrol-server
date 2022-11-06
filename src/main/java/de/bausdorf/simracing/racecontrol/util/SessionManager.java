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
import de.bausdorf.simracing.racecontrol.orga.api.SessionType;
import de.bausdorf.simracing.racecontrol.orga.api.SkyConditionType;
import de.bausdorf.simracing.racecontrol.orga.api.WindDirectionType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class SessionManager {
    private final TrackSessionRepository trackSessionRepository;
    private final EventSeriesRepository eventSeriesRepository;
    private final TrackSubsessionRepository subsessionRepository;
    private final ResultManager resultManager;
    private final IRacingClient dataClient;

    public SessionManager(@Autowired TrackSessionRepository trackSessionRepository,
                          @Autowired EventSeriesRepository eventSeriesRepository,
                          @Autowired TrackSubsessionRepository subsessionRepository,
                          @Autowired ResultManager resultManager,
                          @Autowired IRacingClient dataClient) {
        this.trackSessionRepository = trackSessionRepository;
        this.eventSeriesRepository = eventSeriesRepository;
        this.subsessionRepository = subsessionRepository;
        this.resultManager = resultManager;
        this.dataClient = dataClient;
    }

    @Transactional
    public void fetchFutureTrackSessions(long eventId) {
        Optional<EventSeries> event = eventSeriesRepository.findById(eventId);
        event.ifPresent(evt -> {
            List<TrackSession> eventTrackSessions = trackSessionRepository.findAllByEventId(eventId);
            Optional<CustLeagueSessionsDto> leagueSessions = dataClient.getLeagueFutureSessions();
            leagueSessions.ifPresent(sessions -> Arrays.stream(sessions.getSessions())
                    .filter(s -> Objects.equals(s.getLeagueId(), evt.getIRLeagueID()) && Objects.equals(s.getLeagueSeasonId(), evt.getIrSeasonId()))
                    .forEach(irSession -> {
                        TrackSession trackSession = eventTrackSessions.stream()
                                .filter(s -> Objects.equals(s.getIrPrivateSessionId(), irSession.getPrivateSessionId()))
                                .findFirst().orElse(null);
                        TrackSession newSession = trackSessionFromIrSession(eventId, irSession, trackSession);
                        trackSessionRepository.save(newSession);
                        updateTrackSubSessions(irSession, newSession);
                    }));
        });
    }

    @Scheduled(cron = "${racecontrol.fetchSessionsCron}")
    @Transactional
    public void fetchSessionResults() {
        List<EventSeries> activeEvents = eventSeriesRepository.findAllByActive(true);
        log.debug("try to fetch sessionResults for {} active events", activeEvents.size());
        activeEvents.forEach(event -> {
            if( event.getIrSeasonId() != 0 || event.getIRLeagueID() != 0) {
                List<TrackSession> eventSessions = trackSessionRepository.findAllByEventId(event.getId());
                Optional<LeagueSeasonSessionsDto> seasonSessions = dataClient.getLeaguePastSessions(event.getIRLeagueID(), event.getIrSeasonId());
                seasonSessions.ifPresent(sessions ->  Arrays.stream(sessions.getSessions()).forEach(session -> {
                        Optional<TrackSession> matchingTrackSession = eventSessions.stream()
                                .filter(trackSession -> Objects.equals(session.getPrivateSessionId(), trackSession.getIrPrivateSessionId())
                                        && (trackSession.getIrSessionId() == null || trackSession.getIrSessionId() == 0L)
                                        && session.getSubsessionId() != null)
                                .findFirst();
                        if (matchingTrackSession.isEmpty()) {
                            log.debug("{}: No updatable TrackSession for existing subsession ({}) result at {}, privateSessionId {}",
                                    event.getTitle(), session.getSubsessionId(), session.getLaunchAt(), session.getPrivateSessionId());
                        }
                        matchingTrackSession.ifPresent(trackSession -> fetchEventSessionResults(event.getId(), session, trackSession));
                    })
                );
            } else {
                log.debug("Event {} has no configured league or season", event.getTitle());
            }
        });
    }

    private void fetchEventSessionResults(long eventId, LeagueSessionInfoDto session, @NonNull TrackSession trackSession) {
        if (trackSession.isPermitSession()) {
            resultManager.fetchPermitSessionResult(eventId, session.getSubsessionId(), trackSession);
            trackSessionRepository.save(trackSession);
            log.info("Fetched new result on private session {}: {} ({})", session.getPrivateSessionId(), session.getSessionDescription(), session.getSubsessionId());
            resultManager.updatePermissions(eventId, trackSession.getIrSessionId());
        } else {
            log.debug("TrackSession {} is not a Permit Session", trackSession.getTitle());
        }
    }

    private TrackSession trackSessionFromIrSession(long eventId, CustomSessionInfoDto irSession, @Nullable TrackSession trackSession) {
        if (trackSession == null) {
            trackSession = new TrackSession();
        }
        trackSession.setEventId(eventId);
        trackSession.setIrPrivateSessionId(irSession.getPrivateSessionId());
        trackSession.setIrSessionId(irSession.getSubsessionId());
        trackSession.setTitle(irSession.getSessionDescription());
        trackSession.setTrackConfigId(irSession.getTrack().getTrackId());
        trackSession.setTrackUsagePercent(irSession.getTrackState().getPracticeRubber());
        trackSession.setTrackStateCarryOver(irSession.getTrackState().getLeaveMarbles());
        trackSession.setDateTime(irSession.getLaunchAt().toOffsetDateTime());
        trackSession.setSimulatedTimeOfDay(irSession.getWeather().getSimulatedStartTime());

        if ((irSession.getWeather().getType() & 1L) == 0) {
            trackSession.setGeneratedSky(false);
            trackSession.setSkyVarInitial(irSession.getWeather().getWeatherVarInitial());
            trackSession.setSkyVarContinued(irSession.getWeather().getWeatherVarOngoing());
        } else {
            trackSession.setGeneratedSky(true);
            trackSession.setSkyVarInitial(null);
            trackSession.setSkyVarContinued(null);
        }
        if ((irSession.getWeather().getType() & 2L) == 0) {
            trackSession.setGeneratedWeather(true);
            trackSession.setHumidity(null);
            trackSession.setWindSpeed(null);
            trackSession.setWindDirection(null);
            trackSession.setTemperature(null);
            trackSession.setSky(null);
        } else {
            trackSession.setGeneratedWeather(false);
            trackSession.setHumidity(irSession.getWeather().getRelHumidity());
            trackSession.setWindSpeed(irSession.getWeather().getWindValue());
            trackSession.setWindDirection(WindDirectionType.ofOrdonalNumber(irSession.getWeather().getWindDir().intValue()));
            trackSession.setTemperature(irSession.getWeather().getTempValue());
            trackSession.setSky(SkyConditionType.ofOrdonalNumber(irSession.getWeather().getSkies().intValue()));
        }

        if (irSession.getRaceLength() == 0 && Boolean.TRUE.equals(irSession.getLoneQualify())) {
            trackSession.setPermitSession(true);
        }
        return trackSession;
    }

    private void updateTrackSubSessions(CustomSessionInfoDto irSession, TrackSession trackSession) {
        if (trackSession.getId() != 0L) {
            List<TrackSubsession> subSessions = trackSession.getSessionParts();
            if (subSessions == null) {
                subSessions = new ArrayList<>();
            }

            updateOpenPractice(irSession, trackSession, subSessions);
            updateQualify(irSession, trackSession, subSessions);
            updateWarmup(irSession, trackSession, subSessions);
            updateRace(irSession, trackSession, subSessions);

        }
    }

    private void updateOpenPractice(CustomSessionInfoDto irSession, TrackSession trackSession, List<TrackSubsession> subSessions) {
        if (irSession.getPracticeLength() > 0) {
            Optional<TrackSubsession> practiceOption = subSessions.stream().filter(s -> s.getSessionType() == SessionType.OPEN_PRACTICE).findFirst();
            practiceOption.ifPresentOrElse(
                    practice -> {
                        practice.setDuration(Duration.ofMinutes(irSession.getPracticeLength()));
                        subsessionRepository.save(practice);
                    },
                    () -> {
                        TrackSubsession practice = new TrackSubsession();
                        practice.setTrackSessionId(trackSession.getId());
                        practice.setSessionType(SessionType.OPEN_PRACTICE);
                        practice.setDuration(Duration.ofMinutes(irSession.getPracticeLength()));
                        subsessionRepository.save(practice);
                    });
        }
    }

    private void updateQualify(CustomSessionInfoDto irSession, TrackSession trackSession, List<TrackSubsession> subSessions) {
        if (irSession.getQualifyLength() > 0) {
            Optional<TrackSubsession> qualiOption = subSessions.stream()
                    .filter(s -> s.getSessionType() == SessionType.OPEN_QUALIFY || s.getSessionType() == SessionType.LONE_QUALIFY).findFirst();
            qualiOption.ifPresentOrElse(
                    quali -> {
                        quali.setDuration(Duration.ofMinutes(irSession.getQualifyLength()));
                        if (Boolean.TRUE.equals(irSession.getLoneQualify())) {
                            quali.setSessionType(SessionType.LONE_QUALIFY);
                        } else {
                            quali.setSessionType(SessionType.OPEN_QUALIFY);
                        }
                        subsessionRepository.save(quali);
                    },
                    () -> {
                        TrackSubsession quali = new TrackSubsession();
                        quali.setTrackSessionId(trackSession.getId());
                        quali.setDuration(Duration.ofMinutes(irSession.getQualifyLength()));
                        if (Boolean.TRUE.equals(irSession.getLoneQualify())) {
                            quali.setSessionType(SessionType.LONE_QUALIFY);
                        } else {
                            quali.setSessionType(SessionType.OPEN_QUALIFY);
                        }
                        subsessionRepository.save(quali);
                    });
        }
    }

    private void updateWarmup(CustomSessionInfoDto irSession, TrackSession trackSession, List<TrackSubsession> subSessions) {
        if (irSession.getWarmupLength() > 0) {
            Optional<TrackSubsession> warmupOption = subSessions.stream().filter(s -> s.getSessionType() == SessionType.WARMUP).findFirst();
            warmupOption.ifPresentOrElse(
                    warmup -> {
                        warmup.setDuration(Duration.ofMinutes(irSession.getWarmupLength()));
                        subsessionRepository.save(warmup);
                    },
                    () -> {
                        TrackSubsession warmup = new TrackSubsession();
                        warmup.setTrackSessionId(trackSession.getId());
                        warmup.setSessionType(SessionType.WARMUP);
                        warmup.setDuration(Duration.ofMinutes(irSession.getWarmupLength()));
                        subsessionRepository.save(warmup);
                    });
        }
    }

    private void updateRace(CustomSessionInfoDto irSession, TrackSession trackSession, List<TrackSubsession> subSessions) {
        if (irSession.getRaceLength() > 0) {
            Optional<TrackSubsession> raceOption = subSessions.stream().filter(s -> s.getSessionType() == SessionType.RACE).findFirst();
            raceOption.ifPresentOrElse(
                    race -> {
                        race.setDuration(Duration.ofMinutes(irSession.getRaceLength()));
                        subsessionRepository.save(race);
                    },
                    () -> {
                        TrackSubsession race = new TrackSubsession();
                        race.setTrackSessionId(trackSession.getId());
                        race.setSessionType(SessionType.RACE);
                        race.setDuration(Duration.ofMinutes(irSession.getRaceLength()));
                        subsessionRepository.save(race);
                    });
        }
    }
}
