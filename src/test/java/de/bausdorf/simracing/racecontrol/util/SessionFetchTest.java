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

import de.bausdorf.simracing.irdataapi.model.LeagueSeasonSessionsDto;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeriesRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class SessionFetchTest {
    @Autowired
    IRacingClient dataClient;
    @Autowired
    SessionManager sessionManager;
    @Autowired
    EventSeriesRepository eventRepository;

    @Test
    @Disabled("manual only")
    void testFetchFutureSessions() {
        sessionManager.fetchFutureTrackSessions(8001L);
        log.info("sessions fetched");
    }

    @Test
    @Disabled("manual only")
    void testFetchCompletedSessions() {
        Optional<EventSeries> event = eventRepository.findById(8001L);
        event.ifPresent(evt -> {
            Optional<LeagueSeasonSessionsDto> seasonSessions = dataClient.getLeaguePastSessions(evt.getIRLeagueID(), evt.getIrSeasonId());
            seasonSessions.ifPresent(sessions -> {
                Arrays.stream(sessions.getSessions()).forEach(session -> {
                    log.info("{}: {} ({})", session.getPrivateSessionId(), session.getSessionDescription(), session.getSubsessionId());
                });
            });
        });
    }
}
