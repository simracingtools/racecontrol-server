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

import de.bausdorf.simracing.racecontrol.orga.model.Track;
import de.bausdorf.simracing.racecontrol.orga.model.TrackConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackConfigurationView {
    private long trackId;

    private TrackView track;
    private String configName;

    public Long getTrackPkgId() {
        return track != null ? track.getPkgId() : 0L;
    }

    public static TrackConfigurationView buildFromEntity(@Nullable TrackConfiguration entity) {
        if(entity == null) {
            return buildEmpty();
        }
        return TrackConfigurationView.builder()
                .trackId(entity.getTrackId())
                .configName(entity.getConfigName())
                .track(TrackView.buildFromEntity(entity.getTrack()))
                .build();
    }

    public static TrackConfigurationView buildFromEntity(@Nullable TrackConfiguration entity, TrackView trackView) {
        if(entity == null) {
            return buildEmpty();
        }
        return TrackConfigurationView.builder()
                .trackId(entity.getTrackId())
                .configName(entity.getConfigName())
                .track(trackView)
                .build();
    }

    public static TrackConfigurationView buildEmpty() {
        return TrackConfigurationView.builder()
                .trackId(0L)
                .configName("")
                .track(null)
                .build();
    }
}
