package de.bausdorf.simracing.racecontrol.orga;

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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.bausdorf.simracing.racecontrol.api.IRacingPenalty;
import de.bausdorf.simracing.racecontrol.orga.model.Penalty;
import de.bausdorf.simracing.racecontrol.orga.model.PenaltyRepository;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategory;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategoryRepository;
import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class RuleViolationTest {

	@Autowired
	PenaltyRepository penaltyRepository;

	@Autowired
	RuleViolationRepository violationRepository;

	@Autowired
	RuleViolationCategoryRepository categoryRepository;

	@BeforeEach
	public void setup() {
		Penalty p01 = Penalty.builder()
				.code("P01")
				.name("Warning")
				.iRacingPenalty(IRacingPenalty.MESSAGE)
				.build();
		penaltyRepository.save(p01);
		Penalty p04 = Penalty.builder()
				.code("P03")
				.name("Drive Through")
				.iRacingPenalty(IRacingPenalty.DRIVE_THROUGH)
				.build();
		penaltyRepository.save(p04);
		Penalty p05 = Penalty.builder()
				.code("P05")
				.name("Stop & Go")
				.iRacingPenalty(IRacingPenalty.STOP_AND_HOLD)
				.build();
		penaltyRepository.save(p05);
		Penalty p06 = Penalty.builder()
				.code("P06")
				.name("Stop & Hold")
				.iRacingPenalty(IRacingPenalty.STOP_AND_HOLD)
				.build();
		penaltyRepository.save(p06);
		Penalty p07 = Penalty.builder()
				.code("P07")
				.name("Disqualification")
				.iRacingPenalty(IRacingPenalty.DISQUALIFICATION)
				.build();
		penaltyRepository.save(p07);
		Penalty p08 = Penalty.builder()
				.code("P08")
				.name("Exclusion")
				.iRacingPenalty(IRacingPenalty.EXCLUSION)
				.build();
		penaltyRepository.save(p08);

		RuleViolationCategory cat1 = RuleViolationCategory.builder()
				.categoryCode("CAT1")
				.categoryName("ForcingContact")
				.build();
		categoryRepository.save(cat1);
	}

	@Test
	void createRuleViolation() {

		RuleViolationCategory cat1 = categoryRepository.findById("CAT1").orElse(null);

		RuleViolation ruleViolation = RuleViolation.builder()
				.category(cat1)
				.identifier("A")
				.violationReason("Avoidable contact")
				.possiblePenaltyCodes(new HashSet<>(Arrays.asList("P01", "P04", "P05")))
				.build();
		violationRepository.save(ruleViolation);

		Optional<RuleViolation> violation = violationRepository.findRuleViolationByCategoryAndIdentifier(cat1, "A");
		log.info(violation.toString());

		assertThat(violation).isPresent();
	}

	@Test
	void findPenaltiesFromRuleViolation() {
		RuleViolationCategory cat1 = categoryRepository.findById("CAT1").orElse(null);
		RuleViolation ruleViolation = RuleViolation.builder()
				.category(cat1)
				.identifier("B")
				.violationReason("Causing a collision")
				.possiblePenaltyCodes(new HashSet<>(Arrays.asList("P01", "P04", "P05")))
				.build();
		violationRepository.save(ruleViolation);

		Set<Penalty> penalties = penaltyRepository.findPenaltiesByCodeIn(ruleViolation.getPossiblePenaltyCodes());

		assertThat(penalties).isNotEmpty();
	}

	@Test
	void issueMessagePenaltyCommand() {
		Penalty penalty = penaltyRepository.findById("P01").orElse(null);

		assertThat(penalty).isNotNull();

		String command = penalty.getIRacingPenalty().issue(1);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("/all #1");

		command = penalty.getIRacingPenalty().issue(1, 10);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("/all #1");

		command = penalty.getIRacingPenalty().issue(1, "Warning message");
		log.info("command: {}", command);
		assertThat(command).isEqualTo("/all #1 Warning message");
	}

	@Test
	void issueDriveThroughPenaltyCommand() {
		Penalty penalty = penaltyRepository.findById("P03").orElse(null);

		assertThat(penalty).isNotNull();

		String command = penalty.getIRacingPenalty().issue(1);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!black #1 D");

		command = penalty.getIRacingPenalty().issue(1, 10);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!black #1 D");

		command = penalty.getIRacingPenalty().issue(1, "Warning message");
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!black #1 D");
	}

	@Test
	void issueStopAndGoOrHoldPenaltyCommand() {
		Penalty penalty = penaltyRepository.findById("P05").orElse(null);

		assertThat(penalty).isNotNull();

		String command = penalty.getIRacingPenalty().issue(1);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!black #1");

		command = penalty.getIRacingPenalty().issue(1, 10);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!black #1 10");

		command = penalty.getIRacingPenalty().issue(1, "Warning message");
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!black #1");
	}

	@Test
	void issueDisqualifyPenaltyCommand() {
		Penalty penalty = penaltyRepository.findById("P07").orElse(null);

		assertThat(penalty).isNotNull();

		String command = penalty.getIRacingPenalty().issue(1);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!dq #1");

		command = penalty.getIRacingPenalty().issue(1, 10);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!dq #1");

		command = penalty.getIRacingPenalty().issue(1, "Disqualification message");
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!dq #1 Disqualification message");
	}

	@Test
	void issueExclusionPenaltyCommand() {
		Penalty penalty = penaltyRepository.findById("P08").orElse(null);

		assertThat(penalty).isNotNull();

		String command = penalty.getIRacingPenalty().issue(1);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!remove #1");

		command = penalty.getIRacingPenalty().issue(1, 10);
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!remove #1");

		command = penalty.getIRacingPenalty().issue(1, "Disqualification message");
		log.info("command: {}", command);
		assertThat(command).isEqualTo("!remove #1 Disqualification message");
	}
}
