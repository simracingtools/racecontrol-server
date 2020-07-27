package de.bausdorf.simracing.racecontrol.api;

import org.springframework.web.bind.annotation.RequestBody;

public interface RacecontrolDataService {

	public String receiveClientData(@RequestBody String clientString);

}
