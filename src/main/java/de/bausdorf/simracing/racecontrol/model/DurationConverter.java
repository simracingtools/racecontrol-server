package de.bausdorf.simracing.racecontrol.model;

import java.time.Duration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import lombok.extern.slf4j.Slf4j;

@Converter(autoApply = true)
@Slf4j
public class DurationConverter implements AttributeConverter<Duration, String> {

	@Override
	public String convertToDatabaseColumn(Duration attribute) {
		log.info("Convert to Long");
		return attribute.toString();
	}

	@Override
	public Duration convertToEntityAttribute(String duration) {
		log.info("Convert to Duration");
		return Duration.parse(duration);
	}
}