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
import de.bausdorf.simracing.irdataapi.model.TeamInfoDto;
import de.bausdorf.simracing.irdataapi.model.TeamMemberDto;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.LeagueDataCache;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import de.bausdorf.simracing.racecontrol.web.model.orga.LeagueInfoView;
import de.bausdorf.simracing.racecontrol.web.security.RcUserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/rest")
public class RestDataController {
    private final IRacingClient dataClient;
    private final RcUserRepository userRepository;
    private final LeagueDataCache leagueDataCache;

    public RestDataController(@Autowired IRacingClient iRacingClient,
                              @Autowired RcUserRepository userRepository,
                              @Autowired LeagueDataCache leagueDataCache) {
        this.dataClient = iRacingClient;
        this.userRepository = userRepository;
        this.leagueDataCache = leagueDataCache;
    }

    @GetMapping("/data/renew")
    public String renewStockData() {
        try {
            dataClient.renewStockDataCache();
            return "OK";
        } catch(Exception e) {
            return e.getMessage();
        }
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

    @GetMapping("/memberInfo/{memberId}")
    public PersonSearchItem checkMemberId(@PathVariable Long memberId) {
        Optional<MemberInfo> memberInfo = dataClient.getMemberInfo(memberId);
        AtomicReference<PersonSearchItem> item = new AtomicReference<>(PersonSearchItem.builder()
                .iracingId("0")
                .label("")
                .value("")
                .registered(false)
                .leagueMember(false)
                .build());
        memberInfo.ifPresent(
                member -> {
                    String memberName = EventOrganizer.memberNameWithoutMiddleInitial(member.getName());
                    item.set(PersonSearchItem.builder()
                            .leagueMember(false)
                            .registered(false)
                            .iracingId(Integer.toString(member.getCustid()))
                            .value(memberName)
                            .label(memberName)
                            .build());
                }
        );
        return item.get();
    }

    @GetMapping("/teamInfo/{teamId}")
    public TeamInfo checkTeamName(@PathVariable Long teamId) {
        Optional<TeamInfoDto> teamInfo = dataClient.getTeamMembers(teamId);
        if(teamInfo.isEmpty()) {
            return TeamInfo.builder()
                    .teamName("")
                    .teamId(0L)
                    .build();
        }
        return TeamInfo.builder()
                .teamName(teamInfo.get().getTeamName())
                .teamId(teamInfo.get().getTeamId())
                .build();
    }

    @GetMapping("staff-search")
    public List<PersonSearchItem> stateItems(@RequestParam(value = "q", required = false) String query,
                                             @RequestParam(value = "league", required = false) String leagueId) {
        Map<String, PersonSearchItem> matches = userRepository.findByNameContaining(StringUtils.isEmpty(query) ? "" : query).stream()
                .limit(15)
                .map(rcUser -> PersonSearchItem.builder()
                        .label(rcUser.getName())
                        .value(rcUser.getName())
                        .iracingId(Long.toString(rcUser.getIRacingId()))
                        .registered(true)
                        .leagueMember(false)
                        .build()
                )
                .collect(Collectors.toMap(PersonSearchItem::getValue, item -> item));

        if(!StringUtils.isEmptyOrWhitespace(leagueId) && matches.size() < 15) {
            LeagueInfoDto leagueInfo = leagueDataCache.getLeagueInfo(Long.parseLong(leagueId));
            Arrays.stream(leagueInfo.getRoster())
                    .filter(member -> member.getDisplayName().contains(StringUtils.isEmpty(query) ? "" : query))
                    .forEach(member -> {
                        String memberNameWithoutMiddleInitial = EventOrganizer.memberNameWithoutMiddleInitial(member.getDisplayName());
                        PersonSearchItem item = PersonSearchItem.builder()
                                .label(memberNameWithoutMiddleInitial)
                                .value(memberNameWithoutMiddleInitial)
                                .iracingId(member.getCustId().toString())
                                .leagueMember(true)
                                .registered(matches.containsKey(memberNameWithoutMiddleInitial))
                                .build();
                        matches.put(memberNameWithoutMiddleInitial, item);
                    });

        }
        return new ArrayList<>(matches.values());
    }

    @GetMapping("/team-search/{teamId}")
    public List<PersonSearchItem> getTeamMembers(@PathVariable long teamId,
                                                 @RequestParam(value = "q", required = false) String query,
                                                 @RequestParam(value = "league", required = false) long leagueId) {
        LeagueInfoDto leagueInfo = leagueDataCache.getLeagueInfo(leagueId);
        Optional<TeamInfoDto> teamInfo = dataClient.getTeamMembers(teamId);
        if(teamInfo.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(teamInfo.get().getRoster())
                .filter(member -> member.getDisplayName().contains(StringUtils.isEmpty(query) ? "" : query))
                .map(member -> {
                    String memberNameWithoutMiddleInitial = EventOrganizer.memberNameWithoutMiddleInitial(member.getDisplayName());
                    return PersonSearchItem.builder()
                            .iracingId(member.getCustId().toString())
                            .label(memberNameWithoutMiddleInitial)
                            .value(memberNameWithoutMiddleInitial)
                            .registered(userRepository.findByiRacingId(member.getCustId()).isPresent())
                            .leagueMember(Arrays.stream(leagueInfo.getRoster())
                                    .anyMatch(m -> m.getDisplayName().equalsIgnoreCase(member.getDisplayName())))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    public static class PersonSearchItem {
        private String label;
        private String value;
        private String iracingId;
        private boolean registered;
        private boolean leagueMember;
    }

    @Data
    @Builder
    public static class TeamInfo {
        private String teamName;
        private long teamId;
    }
}
