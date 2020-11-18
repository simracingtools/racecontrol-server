package de.bausdorf.simracing.racecontrol.model;

import java.time.ZonedDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import de.bausdorf.simracing.racecontrol.api.IRacingPenalty;
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
	private IRacingPenalty penalty;
	private Long penaltySeconds;
}
