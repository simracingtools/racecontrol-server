package de.bausdorf.simracing.racecontrol.web.model;

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

import de.bausdorf.simracing.irdataapi.model.TrackInfoDto;
import lombok.*;

import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackConfigurationView {
    @Setter
    private static TrackInfoDto[] trackInfos;

    private long trackId;

    private TrackView track;
    private String configName;

    public static TrackConfigurationView fromId(long trackConfigId) {
        if(trackConfigId == 0L) {
            return buildEmpty();
        }
        TrackInfoDto track = Arrays.stream(trackInfos).filter(t -> t.getTrackId() == trackConfigId).findFirst().orElse(null);
        if(track != null) {
            return TrackConfigurationView.builder()
                    .trackId(trackConfigId)
                    .configName(track.getConfigName())
                    .track(TrackView.builder()
                            .name(track.getTrackName())
                            .pkgId(track.getPackageId())
                            .build())
                    .build();
        }
        return buildEmpty();
    }

    public static TrackConfigurationView buildEmpty() {
        return TrackConfigurationView.builder()
                .trackId(0L)
                .configName("")
                .track(TrackView.builder()
                        .name("")
                        .pkgId(0L)
                        .build())
                .build();
    }
}
