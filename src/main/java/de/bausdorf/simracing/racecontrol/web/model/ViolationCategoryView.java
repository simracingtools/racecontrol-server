package de.bausdorf.simracing.racecontrol.web.model;

import org.springframework.lang.Nullable;

import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ViolationCategoryView {

	private String code;
	private String description;

	public void updateEntity(RuleViolationCategory category) {
		category.setCategoryCode(code);
		category.setCategoryName(description);
	}

	public static ViolationCategoryView buildFromEntity(@Nullable RuleViolationCategory category) {
		if(category == null) {
			return buildEmpty();
		}
		return ViolationCategoryView.builder()
				.code(category.getCategoryCode())
				.description(category.getCategoryName())
				.build();
	}

	public static ViolationCategoryView buildEmpty() {
		return ViolationCategoryView.builder()
				.code("")
				.description("")
				.build();
	}
}