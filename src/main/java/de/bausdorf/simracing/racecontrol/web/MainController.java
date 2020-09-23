package de.bausdorf.simracing.racecontrol.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import de.bausdorf.simracing.racecontrol.model.TeamRepository;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MainController {
	public static final String INDEX_VIEW = "index";
	public static final String SESSION_VIEW = "session";

	final TeamRepository teamRepository;

	public MainController(@Autowired TeamRepository teamRepository) {
		this.teamRepository = teamRepository;
	}

	@GetMapping({"/", "/index", "index.html"})
	public String index(Model model) {
		return INDEX_VIEW;
	}

	@GetMapping({"/session/{sessionId}"})
	public String session(@PathVariable String sessionId, Model model) {
		return SESSION_VIEW;
	}
}
