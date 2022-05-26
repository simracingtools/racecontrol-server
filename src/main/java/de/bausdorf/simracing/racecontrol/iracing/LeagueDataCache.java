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

import de.bausdorf.simracing.irdataapi.config.ConfigProperties;
import de.bausdorf.simracing.irdataapi.model.LeagueInfoDto;
import de.bausdorf.simracing.irdataapi.tools.JsonFileCache;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class LeagueDataCache {
    public static final String JSON_FILE_PREFIX = "league-";
    private static final String JSON_FILE_EXTENSION = ".json";

    private final Map<Long, JsonFileCache<LeagueInfoDto>> leagueInfoMap = new HashMap<>();

    private final IRacingClient dataClient;
    private final ConfigProperties dataClientConfig;
    private final RacecontrolServerProperties serverConfig;

    public LeagueDataCache(@Autowired IRacingClient dataClient,
                           @Autowired ConfigProperties dataClientConfig,
                           @Autowired RacecontrolServerProperties serverConfig) {
        this.dataClient = dataClient;
        this.dataClientConfig = dataClientConfig;
        this.serverConfig = serverConfig;

        initializeFromFiles();
    }

    public LeagueInfoDto getLeagueInfo(long leagueId) {
        if(!leagueInfoMap.containsKey(leagueId)) {
            JsonFileCache<LeagueInfoDto> newCache = new JsonFileCache<>(dataClientConfig.getCacheDirectory(), leagueName(leagueId));
            newCache.setCachedData(dataClient.getLeagueInfo(leagueId));
            leagueInfoMap.put(leagueId, newCache);
            return newCache.getCachedData();
        } else {
            JsonFileCache<LeagueInfoDto> cache = leagueInfoMap.get(leagueId);
            if(isCacheOutdated(cache, serverConfig.getLeagueInfoCacheMaxAgeMillis())) {
                log.info("League cache outdated, refeshing");
                cache.setCachedData(dataClient.getLeagueInfo(leagueId));
            }
            return cache.getCachedData();
        }
    }

    private String leagueName(long leagueId) {
        return JSON_FILE_PREFIX + leagueId;
    }

    private boolean isCacheOutdated(JsonFileCache<?> cache, long maxAgeMillis) {
        return cache.cacheLastModified() < (System.currentTimeMillis() - maxAgeMillis);
    }

    private void initializeFromFiles() {
        File[] leagueCacheFiles = listLeagueCacheFiles();
        if(leagueCacheFiles != null && leagueCacheFiles.length > 0) {
            Arrays.stream(leagueCacheFiles).forEach(cacheFile -> {
                String[] nameParts = cacheFile.getName().split("[-.]");
                String leagueId = nameParts[1];
                leagueInfoMap.put(Long.parseLong(leagueId),
                        new JsonFileCache<>(dataClientConfig.getCacheDirectory(), JSON_FILE_PREFIX + leagueId));
            });
        }
    }

    private File[] listLeagueCacheFiles() {
        File cacheDir = new File(dataClientConfig.getCacheDirectory());
        return cacheDir.listFiles((dir, name) -> name.startsWith(JSON_FILE_PREFIX) && name.endsWith(JSON_FILE_EXTENSION));
    }
}
