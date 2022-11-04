package de.bausdorf.simracing.racecontrol.live.model;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
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

import java.io.Serializable;
import java.util.Objects;

public class IRacingPk implements Serializable {
	String sessionId;
	long iracingId;

	public IRacingPk(String sessionId, long iRacingId) {
		this();
		this.sessionId = sessionId;
		this.iracingId = iRacingId;
	}

	public IRacingPk() {
		this.sessionId = null;
		this.iracingId = -1;
	}

	public long getIracingId() {
		return this.iracingId;
	}

	public void setIracingId(long iRacingId) {
		this.iracingId = iRacingId;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof IRacingPk)) {
			return false;
		}
		final IRacingPk other = (IRacingPk) o;
		final Object thisSessionId = this.getSessionId();
		final Object otherSessionId = other.getSessionId();
		if (!Objects.equals(thisSessionId, otherSessionId)) {
			return false;
		}
		return this.getIracingId() == other.getIracingId();
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object localSessionId = this.getSessionId();
		result = result * PRIME + (localSessionId == null ? 43 : localSessionId.hashCode());
		final long $iRacingId = this.getIracingId();
		result = result * PRIME + (int) ($iRacingId >>> 32 ^ $iRacingId);
		return result;
	}
}
