package de.bausdorf.simracing.racecontrol.web.security;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface RcUserRepository extends CrudRepository<RcUser, String> {

	Optional<RcUser> findByEmail(String email);
}
