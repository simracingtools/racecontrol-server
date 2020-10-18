package de.bausdorf.simracing.racecontrol.model;

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

public class EventPk implements Serializable {
	private String sessionId;
	private long eventNo;

	public EventPk(String sessionId, long eventNo) {
		this.sessionId = sessionId;
		this.eventNo = eventNo;
	}

	public EventPk() {
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public long getEventNo() {
		return this.eventNo;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setEventNo(long eventNo) {
		this.eventNo = eventNo;
	}

	public String toString() {
		return "EventPk(sessionId=" + this.getSessionId() + ", eventNo=" + this.getEventNo() + ")";
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof EventPk)) {
			return false;
		}
		final EventPk other = (EventPk) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$sessionId = this.getSessionId();
		final Object other$sessionId = other.getSessionId();
		if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) {
			return false;
		}
		return this.getEventNo() == other.getEventNo();
	}

	protected boolean canEqual(final Object other) {
		return other instanceof EventPk;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $sessionId = this.getSessionId();
		result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
		final long $eventNo = this.getEventNo();
		result = result * PRIME + (int) ($eventNo >>> 32 ^ $eventNo);
		return result;
	}
}
