package de.bausdorf.simracing.racecontrol.web.model.live;

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

import org.springframework.lang.Nullable;

import de.bausdorf.simracing.racecontrol.model.RcBulletin;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RcBulletinView {
	private String sessionId;
	private long bulletinNo;
	private String sessionTime;
	private String carNo;
	private String message;
	private long violationId;
	private String violationDescription;
	private String selectedPenaltyCode;
	private int penaltySeconds;

	public static RcBulletinView fromEntity(RcBulletin bulletin, @Nullable RuleViolation violation) {
		String penaltyDescription = "None";
		if(violation != null) {
			penaltyDescription = violation.getDescription();
		}
		return RcBulletinView.builder()
				.sessionId(bulletin.getSessionId())
				.bulletinNo(bulletin.getBulletinNo())
				.carNo(bulletin.getCarNo())
				.violationId(violation != null ? violation.getId() : 0)
				.violationDescription(penaltyDescription)
				.sessionTime(bulletin.getSessionTime())
				.selectedPenaltyCode("")
				.penaltySeconds(0)
				.message(bulletin.getMessage())
				.build();
	}

	public static RcBulletinView buildNew(String sessionId, long bulletinNo) {
		return RcBulletinView.builder()
				.sessionId(sessionId)
				.bulletinNo(bulletinNo)
				.carNo("None")
				.violationId(0)
				.violationDescription("None")
				.message("")
				.selectedPenaltyCode("")
				.penaltySeconds(0)
				.sessionTime("00:00:00")
				.build();
	}
}
