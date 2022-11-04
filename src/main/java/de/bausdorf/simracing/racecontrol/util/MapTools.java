package de.bausdorf.simracing.racecontrol.util;

/*-
 * #%L
 * tt-cloud-server
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapTools {

	public static final String EXCEPTION_LOG_PATTERN = "{} - {}: {}";

	private MapTools() {
		super();
	}

	public static String stringFromMap(String key, Map<String, Object> data) {
		try {
			return (String)data.get(key);
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return "";
	}

	public static String stringFromMapWithDefault(String key, Map<String, Object> data, String defaultString) {
		try {
			return data.get(key) != null ? (String)data.get(key) : "NONE";
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return defaultString;
	}

	public static double doubleFromMap(String key, Map<String, Object> data) {
		try {
			return (Double)data.get(key);
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return 0.0D;
	}

	public static int intFromMap(String key, Map<String, Object> data) {
		try {
			return ((Long) data.get(key)).intValue();
		} catch( Exception eLong ) {
			log.debug(EXCEPTION_LOG_PATTERN, key, eLong.getMessage(), data.get(key));
			try {
				return (Integer) data.get(key);
			} catch( Exception e ) {
				log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
			}
		}
		return 0;
	}

	public static boolean booleanFromMap(String key, Map<String, Object> data, boolean defaultValue) {
		try {
			return (Boolean) data.get(key);
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return defaultValue;
	}

	public static Duration durationFromMap(String key, Map<String, Object> data) {
		try {
			return Duration.parse((String)data.get(key));
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return Duration.ZERO;
	}

	public static LocalTime timeFromMap(String key, Map<String, Object> data) {
		try {
			return LocalTime.parse((String)data.get(key));
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return LocalTime.MIN;
	}

	public static LocalDateTime dateTimeFromMap(String key, Map<String, Object> data) {
		try {
			return LocalDateTime.parse((String)data.get(key));
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return LocalDateTime.MIN;
	}

	public static ZonedDateTime zonedDateTimeFromMap(String key, Map<String, Object> data) {
		try {
			return TimeTools.zonedDateTimeFromString((String) data.get(key));
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("GMT"));
	}

	@SuppressWarnings("unchecked")
	public static List<String> stringListFromMap(String key, Map<String, Object> data) {
		try {
			return (List<String>)data.get(key);
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> mapListFromMap(String key, Map<String, Object> data) {
		try {
			return (List<Map<String, Object>>)data.get(key);
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> mapFromMap(String key, Map<String, Object> data) {
		try {
			return (Map<String, Object>)data.get(key);
		} catch( Exception e ) {
			log.warn(EXCEPTION_LOG_PATTERN, key, e.getMessage(), data.get(key));
		}
		return new HashMap<>();
	}

	public static String hexStringFromLong(String key, Map<String, Object> data) {
		return Integer.toHexString(intFromMap(key, data));
	}

	public static <K> K mapValue(K thisValue, K oldValue, Class<K> valueClass) {
		if (valueClass.equals(Long.class)) {
			return thisValue == null || (Long)thisValue == 0L ? oldValue : thisValue;
		}
		return thisValue == null ? oldValue : thisValue;
	}
}
