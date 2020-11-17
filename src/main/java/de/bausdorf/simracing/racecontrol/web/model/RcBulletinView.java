package de.bausdorf.simracing.racecontrol.web.model;

import java.time.Duration;

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

	private Duration sessionTime;
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
				.sessionTime(Duration.ZERO)
				.build();
	}
}
