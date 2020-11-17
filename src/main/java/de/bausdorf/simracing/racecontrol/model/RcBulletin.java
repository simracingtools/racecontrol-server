package de.bausdorf.simracing.racecontrol.model;

import java.time.Duration;

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

	private Duration sessionTime;
	private String carNo;
	private String message;
	private String penaltyCategory;
	private String penaltyIdentifier;
}
