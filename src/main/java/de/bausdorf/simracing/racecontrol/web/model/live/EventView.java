package de.bausdorf.simracing.racecontrol.web.model.live;

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

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class EventView {
	private long teamId;
	private long driverId;
	private long sessionMillis;
	private long userId;
	private Duration sessionTime;
	private TableCellView eventTime;
	private TableCellView eventType;
	private TableCellView teamName;
	private TableCellView driverName;
	private TableCellView carNo;
	private TableCellView carName;
	private TableCellView lap;
}
