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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class BalancedCar {
	@Id
	@GeneratedValue
	private long id;

	private long carId;
	private long carClassId;
	private String carName;
	private double maxFuel;
	private double fuelPercent;
	private double weightPenalty;
	private double enginePowerPercent;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		BalancedCar that = (BalancedCar) o;
		return id != that.id;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
