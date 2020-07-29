package de.bausdorf.simracing.racecontrol.model;

import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@IdClass(IRacingPk.class)
public class BaseEntity {
	@Id
	String sessionId;
	@Id
	long iracingId;
}
