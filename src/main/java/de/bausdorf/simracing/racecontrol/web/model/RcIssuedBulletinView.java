package de.bausdorf.simracing.racecontrol.web.model;

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

import de.bausdorf.simracing.racecontrol.api.IRacingPenalty;
import de.bausdorf.simracing.racecontrol.model.RcBulletin;
import de.bausdorf.simracing.racecontrol.orga.model.Penalty;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RcIssuedBulletinView {
	private String sessionId;
	private long bulletinNo;
	private String sessionTime;
	private String carNo;
	private String violationDescription;
	private String message;
	private IRacingPenalty selectedPenalty;
	private String penaltyDescription;
	private int penaltySeconds;
	private boolean sent;

	public int getCarNoAsInt() {
		return Integer.parseInt(carNo);
	}

	public String getDiscordText() {
		String discordText =  "R" + bulletinNo + " " + sessionTime + " #" + carNo + " - ";
		discordText += getBulletinIssue();
		if(!message.isEmpty()) {
			discordText += (!discordText.endsWith(" - ") ? " - " : "") + message;
		}
		return discordText;
	}

	public String getSimulationChatText() {
		String chatText = "/all ";
		if(selectedPenalty != null) {
			if(selectedPenalty.isTimeParamNeeded()) {
				chatText = selectedPenalty.issue(Integer.parseInt(carNo), penaltySeconds);
			} else {
				if(message.isEmpty()) {
					message = getBulletinIssue();
				}
				chatText = selectedPenalty.issue(Integer.parseInt(carNo), message);
			}
		} else {
			chatText += "#" + carNo + " - " + message;
		}
		return chatText;
	}

	public String getBulletinIssue() {
		String issueText = "";
		if(!violationDescription.isEmpty()) {
			issueText += violationDescription + " - ";
		}
		if(selectedPenalty != null) {
			issueText += penaltyDescription;
			if(selectedPenalty.isTimeParamNeeded()) {
				issueText += " " + penaltySeconds + " sec";
			}
		}
		return issueText;
	}

	public static RcIssuedBulletinView fromEntity(RcBulletin bulletin, @Nullable RuleViolation violation, @Nullable Penalty penalty) {
		String violationDescription = "";
		if(violation != null) {
			violationDescription = violation.getCategory().getCategoryCode() + " " + violation.getDescription();
		}
		return RcIssuedBulletinView.builder()
				.sessionId(bulletin.getSessionId())
				.bulletinNo(bulletin.getBulletinNo())
				.carNo(bulletin.getCarNo())
				.violationDescription(violationDescription)
				.sessionTime(bulletin.getSessionTime())
				.selectedPenalty(penalty != null ? penalty.getIRacingPenalty() : null)
				.penaltyDescription(penalty != null ? penalty.getCode() + " " + penalty.getName() : null)
				.penaltySeconds(bulletin.getPenaltySeconds() != null ? bulletin.getPenaltySeconds().intValue() : 0)
				.message(bulletin.getMessage())
				.sent(bulletin.getSent() != null)
				.build();
	}
}
