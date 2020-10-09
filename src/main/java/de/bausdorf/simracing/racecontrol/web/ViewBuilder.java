package de.bausdorf.simracing.racecontrol.web;

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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bausdorf.simracing.racecontrol.impl.RuleComplianceCheck;
import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.model.CssClassType;
import de.bausdorf.simracing.racecontrol.web.model.StintView;
import de.bausdorf.simracing.racecontrol.web.model.TableCellView;

@Component
public class ViewBuilder {

	final RuleComplianceCheck complianceCheck;

	public ViewBuilder(@Autowired RuleComplianceCheck complianceCheck) {
		this.complianceCheck = complianceCheck;
	}

	public List<StintView> buildStintViews(List<Stint> stints) {
		List<StintView> stintViews = new ArrayList<>();

		Stint lastStint = null;
		for(Stint stint : stints) {
			StintView view = StintView.builder()
					.stopTime(TableCellView.builder()
							.value(TimeTools.shortDurationString(stint.getEndTime()))
							.build())
					.duration(TableCellView.builder()
							.value(TimeTools.shortDurationString(stint.getStintDuration()))
							.displayType(complianceCheck.isStintDurationCompliant(stint) ? CssClassType.SUCCESS : CssClassType.DANGER)
							.build())
					.build();
			if(lastStint != null) {
				view.setStartTime(TableCellView.builder()
						.displayType(complianceCheck.isRestTimeCompliant(lastStint, stint) ? CssClassType.SUCCESS : CssClassType.DANGER)
						.value(TimeTools.shortDurationString(stint.getStartTime()))
						.build());
			} else {
				view.setStartTime(TableCellView.builder()
						.displayType(CssClassType.DEFAULT)
						.value(TimeTools.shortDurationString(stint.getStartTime()))
						.build());
			}
			stintViews.add(view);
			lastStint = stint;
		}

		return stintViews;
	}

}
