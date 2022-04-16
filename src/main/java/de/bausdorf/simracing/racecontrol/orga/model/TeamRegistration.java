package de.bausdorf.simracing.racecontrol.orga.model;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2021 bausdorf engineering
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

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class TeamRegistration implements WorkflowItem {
	@Id
	@GeneratedValue
	private long id;

	private long eventId;
	private String teamName;
	private String likedCarNumbers;
	private String assignedCarNumber;
	private Long iracingId;
	private String logoUrl;
	@ManyToOne
	private Person createdBy;
	@Convert(converter = OffsetDateTimeConverter.class)
	OffsetDateTime created;
	@ManyToOne
	BalancedCar car;
	private boolean wildcard;
	@ManyToOne
	WorkflowState workflowState;
	@OneToMany
	private List<Person> teamMembers = new ArrayList<>();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		TeamRegistration that = (TeamRegistration) o;
		return id != that.id;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
