package de.bausdorf.simracing.racecontrol.web;

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
