package de.bausdorf.simracing.racecontrol.web.model;

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

	public String getDiscordText() {
		String discordText =  "R" + bulletinNo + " " + sessionTime + " - car #" + carNo + " - ";
		discordText += getBulletinIssue();
		if(!message.isEmpty()) {
			discordText += " - " + message;
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
			chatText += "car #" + carNo + ": " + message;
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
		String penaltyDescription = "";
		if(violation != null) {
			penaltyDescription = violation.getCategory().getCategoryCode() + " " + violation.getDescription();
		}
		return RcIssuedBulletinView.builder()
				.sessionId(bulletin.getSessionId())
				.bulletinNo(bulletin.getBulletinNo())
				.carNo(bulletin.getCarNo())
				.violationDescription(penaltyDescription)
				.sessionTime(bulletin.getSessionTime())
				.selectedPenalty(penalty != null ? penalty.getIRacingPenalty() : null)
				.penaltyDescription(penalty != null ? penalty.getName() : null)
				.penaltySeconds(bulletin.getPenaltySeconds().intValue())
				.message(bulletin.getMessage())
				.build();
	}
}
