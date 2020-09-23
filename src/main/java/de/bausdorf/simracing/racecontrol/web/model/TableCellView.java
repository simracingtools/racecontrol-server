package de.bausdorf.simracing.racecontrol.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TableCellView {
	private String value;
	private CssClassType displayType;
}
