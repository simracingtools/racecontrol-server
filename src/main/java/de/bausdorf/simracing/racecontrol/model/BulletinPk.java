package de.bausdorf.simracing.racecontrol.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BulletinPk implements Serializable {
	private String sessionId;
	private long bulletinNo;
}
