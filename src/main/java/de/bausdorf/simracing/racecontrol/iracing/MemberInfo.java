package de.bausdorf.simracing.racecontrol.iracing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MemberInfo {

	private String name;
	private long lastLogin;
	private long lastSeen;
	private int custid;
}
