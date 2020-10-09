package de.bausdorf.simracing.racecontrol.web.model;

import lombok.Getter;

public enum CssClassType {
	DEFAULT("", ""),
	SUCCESS("bg-success", "text-dark"),
	WARNING("bg-warning", "text-dark"),
	DANGER("bg-danger", "text-light");

	@Getter
	String backgroundClass;

	@Getter
	String textClass;

	CssClassType(String backgroundClass, String textClass) {
		this.backgroundClass = backgroundClass;
		this.textClass = textClass;
	}

	public String getClassString() {
		return backgroundClass + " " + textClass;
	}
}
