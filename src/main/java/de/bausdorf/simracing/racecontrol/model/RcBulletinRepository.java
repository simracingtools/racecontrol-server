package de.bausdorf.simracing.racecontrol.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface RcBulletinRepository extends CrudRepository<RcBulletin, Integer> {

	long countAllBySessionId(String sessionId);
	List<RcBulletin> findAllBySessionIdOrderByBulletinNoDesc(String sessionId);
	Optional<RcBulletin> findBySessionIdAndBulletinNo(String sessionId, long bulletinNo);
}
