package de.bausdorf.simracing.racecontrol.orga.model;

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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@Setter
@ToString
@Entity
public class TrackConfiguration {
    @Id
    private long trackId;

    @ManyToOne
    @JoinColumn(name = "track_pkg_id")
    private Track track;
    private String configName;

    @Builder
    public TrackConfiguration(long trackId, Track track, String configName) {
        this.trackId = trackId;
        this.track = track;
        this.configName = configName;
    }

    public TrackConfiguration() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TrackConfiguration other = (TrackConfiguration) o;
        return trackId != other.trackId;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
