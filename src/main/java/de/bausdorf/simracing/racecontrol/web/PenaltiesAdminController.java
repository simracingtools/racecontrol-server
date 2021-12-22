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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.bausdorf.simracing.racecontrol.api.IRacingPenalty;
import de.bausdorf.simracing.racecontrol.orga.model.Penalty;
import de.bausdorf.simracing.racecontrol.orga.model.PenaltyRepository;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategory;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategoryRepository;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationRepository;
import de.bausdorf.simracing.racecontrol.web.model.PenaltyView;
import de.bausdorf.simracing.racecontrol.web.model.RuleViolationView;
import de.bausdorf.simracing.racecontrol.web.model.ViolationCategoryView;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class PenaltiesAdminController extends ControllerBase {
	private static final String PENALTIES_VIEW = "penaltiesadmin";

	private final PenaltyRepository penaltyRepository;
	private final RuleViolationCategoryRepository violationCategoryRepository;
	private final RuleViolationRepository violationRepository;

	public PenaltiesAdminController(@Autowired PenaltyRepository penaltyRepository,
			@Autowired RuleViolationCategoryRepository categoryRepository,
			@Autowired RuleViolationRepository violationRepository) {
		this.penaltyRepository = penaltyRepository;
		this.violationCategoryRepository = categoryRepository;
		this.violationRepository = violationRepository;
	}

	@GetMapping("/penalties")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String getPenaltiesView(@RequestParam Optional<String> activeTab,
			@RequestParam Optional<String> selectedPenaltyCode,
			@RequestParam Optional<String> selectedCategoryCode,
			@RequestParam Optional<Long> selectedViolationId,
			@RequestParam Optional<String> error,
			@RequestParam Optional<String> warn,
			@RequestParam Optional<String> info,
			Model model ) {

		prepareMessageModel(error, warn, info, model);

		if(activeTab.isPresent()) {
			model.addAttribute("activeTab", activeTab.get());
		} else {
			model.addAttribute("activeTab", "violation");
		}

		preparePenaltyTabModel(model, selectedPenaltyCode);
		prepareCategoryTabModel(model, selectedCategoryCode);
		prepareViolationTabModel(model, selectedViolationId);

		return PENALTIES_VIEW;
	}

	@PostMapping("/savepenalty")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String savePenalty(@ModelAttribute PenaltyView selectedPenalty) {
		Optional<Penalty> penaltyToSave = penaltyRepository.findById(selectedPenalty.getCode());
		if(penaltyToSave.isPresent()) {
			selectedPenalty.updateEntity(penaltyToSave.get());
		} else {
			Penalty newPenalty = Penalty.builder()
					.code(selectedPenalty.getCode())
					.name(selectedPenalty.getName())
					.iRacingPenalty(IRacingPenalty.valueOf(selectedPenalty.getIRacingPenalty()))
					.build();
			penaltyRepository.save(newPenalty);
		}
		return "redirect:/penalties?activeTab=penalty";
	}

	@PostMapping("/savecategory")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String savePenalty(@ModelAttribute ViolationCategoryView selectedCategory) {
		Optional<RuleViolationCategory> category = violationCategoryRepository.findById(selectedCategory.getCode());
		if(category.isPresent()) {
			selectedCategory.updateEntity(category.get());
		} else {
			RuleViolationCategory newCategory = RuleViolationCategory.builder()
					.categoryCode(selectedCategory.getCode())
					.categoryName(selectedCategory.getDescription())
					.build();
			violationCategoryRepository.save(newCategory);
		}
		return "redirect:/penalties?activeTab=category";
	}

	@PostMapping("/saveviolation")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String saveViolation(@ModelAttribute RuleViolationView selectedViolation, Model model) {
		Optional<RuleViolation> violation = violationRepository.findById(selectedViolation.getId());
		RuleViolationCategory category = violationCategoryRepository.findById(selectedViolation.getCategoryCode()).orElse(null);
		if(violation.isPresent()) {
			if( category != null) {
				violation.get().setCategory(category);
				violation.get().setIdentifier(selectedViolation.getIdentifier());
				violation.get().setViolationReason(selectedViolation.getViolationReason());
				violation.get().setPossiblePenaltyCodes(selectedViolation.getPossiblePenaltyCodes());
				addInfo("Rule violation changed", model);
			} else {
				log.warn("category code {} not found", selectedViolation.getCategoryCode());
				addWarning("Category " + selectedViolation.getCategoryCode() + " not found", model);
			}
		} else {
			if(category != null) {
				RuleViolation newViolation = RuleViolation.builder()
						.category(category)
						.identifier(selectedViolation.getIdentifier())
						.violationReason(selectedViolation.getViolationReason())
						.possiblePenaltyCodes(selectedViolation.getPossiblePenaltyCodes())
						.build();
				violationRepository.save(newViolation);
				addInfo("New rule violation saved", model);
			} else {
				log.warn("category code {} not found", selectedViolation.getCategoryCode());
				addWarning("Category " + selectedViolation.getCategoryCode() + " not found", model);
			}
		}

		return "redirect:/penalties?activeTab=violation";
	}

	private List<PenaltyView> getPenaltyList() {
		return penaltyRepository.findAllByCodeContainingOrderByCodeAsc("")
				.map(PenaltyView::buildFromEntity)
				.collect(Collectors.toList());
	}

	private List<ViolationCategoryView> getCategoryList() {
		return violationCategoryRepository.findAllByCategoryCodeContainingOrderByCategoryCodeAsc("")
				.map(ViolationCategoryView::buildFromEntity)
				.collect(Collectors.toList());
	}

	private List<RuleViolationView> getViolationList() {
		List<RuleViolationView> ruleViolationViews = new ArrayList<>();
		for(RuleViolation violation : violationRepository.findAll()) {
			ruleViolationViews.add(RuleViolationView.buildFromEntity(violation));
		}
		return ruleViolationViews;
	}

	private void preparePenaltyTabModel(Model model, Optional<String> selectedPenaltyCode) {
		model.addAttribute("penaltyViews", getPenaltyList());
		if(selectedPenaltyCode.isPresent()) {
			model.addAttribute("selectedPenalty",
					PenaltyView.buildFromEntity(penaltyRepository.findById(selectedPenaltyCode.get()).orElse(null)));
		} else {
			model.addAttribute("selectedPenalty",
					PenaltyView.buildEmpty());
		}
		model.addAttribute("iRacingPenalties", Arrays.stream(IRacingPenalty.values())
				.map(IRacingPenalty::name)
				.collect(Collectors.toList()));
	}

	private void prepareCategoryTabModel(Model model, Optional<String> selectedCategoryCode) {
		model.addAttribute("categoryViews", getCategoryList());
		if(selectedCategoryCode.isPresent()) {
			model.addAttribute("selectedCategory",
					ViolationCategoryView.buildFromEntity(
							violationCategoryRepository.findById(selectedCategoryCode.get()).orElse(null)
					)
			);
		} else {
			model.addAttribute("selectedCategory", ViolationCategoryView.buildEmpty());
		}
	}

	private void prepareViolationTabModel(Model model, Optional<Long> selectedViolationCode) {
		model.addAttribute("violationViews", getViolationList());
		if(selectedViolationCode.isPresent()) {
			model.addAttribute("selectedViolation",
					RuleViolationView.buildFromEntity(
							violationRepository.findById(selectedViolationCode.get()).orElse(null)
					)
			);
		} else {
			model.addAttribute("selectedViolation", RuleViolationView.buildEmpty());
		}
	}
}
