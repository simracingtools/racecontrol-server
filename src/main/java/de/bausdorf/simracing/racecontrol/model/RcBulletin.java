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

import java.time.ZonedDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(BulletinPk.class)
public class RcBulletin {
	@Id
	String sessionId;
	@Id
	private long bulletinNo;

	private ZonedDateTime created;
	private ZonedDateTime sent;
	private String sessionTime;
	private String carNo;
	private String message;
	private String violationCategory;
	private String violationIdentifier;
	private String violationDescription;
	private String selectedPenaltyCode;
	private String penaltyDescription;
	private Long penaltySeconds;
	private boolean valid;

	public String getDiscordTitle() {
		return "R" + bulletinNo + " " + sessionTime;
	}

	public String getViolationText() {
		return (violationCategory != null ? violationCategory + " ": "")
				+ (violationDescription != null ? violationDescription : "");
	}

	public String getPenaltyText() {
		return (selectedPenaltyCode != null ? selectedPenaltyCode + " " : "")
				+ (penaltyDescription != null ? penaltyDescription + " " : "")
				+ (penaltySeconds != null ? penaltySeconds + " sec" : "");
	}
}
