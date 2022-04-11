package de.bausdorf.simracing.racecontrol.web;

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

import de.bausdorf.simracing.irdataapi.model.LeagueInfoDto;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.web.model.LeagueInfoView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/rest")
public class RestDataController {
    private final IRacingClient dataClient;

    public RestDataController(@Autowired IRacingClient iRacingClient) {
        this.dataClient = iRacingClient;
    }

    @GetMapping("/leagueInfo/{leagueId}")
    public LeagueInfoView checkLeagueInfo(@PathVariable Long leagueId) {
        LeagueInfoDto infoDto = dataClient.getLeagueInfo(leagueId);
        if (infoDto != null) {
            return LeagueInfoView.builder()
                    .leagueId(infoDto.getLeagueId())
                    .leagueName(infoDto.getLeagueName())
                    .build();
        }
        return LeagueInfoView.builder()
                .leagueId(leagueId)
                .leagueName("League not found")
                .build();
    }
}
