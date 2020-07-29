package de.bausdorf.simracing.racecontrol.model;

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
		final Object this$sessionId = this.getSessionId();
		final Object other$sessionId = other.getSessionId();
		if (!Objects.equals(this$sessionId, other$sessionId)) {
			return false;
		}
		return this.getIracingId() == other.getIracingId();
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $sessionId = this.getSessionId();
		result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
		final long $iRacingId = this.getIracingId();
		result = result * PRIME + (int) ($iRacingId >>> 32 ^ $iRacingId);
		return result;
	}
}
