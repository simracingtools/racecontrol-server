package de.bausdorf.simracing.racecontrol.orga.api;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2022 bausdorf engineering
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

import lombok.Getter;
import org.springframework.lang.NonNull;

public enum SkyConditionType {
    OVERCAST("Overcast"),
    PARTLY_CLOUDY("Partly cloudy"),
    MOSTLY_CLOUDY("Mostly cloudy"),
    CLEAR("Clear");

    @Getter
    private final String name;

    SkyConditionType(String name) {
        this.name = name;
    }

    public static SkyConditionType ofName(@NonNull String name) {
        switch(name) {
            case "Overcast"     : return OVERCAST;
            case "Partly cloudy": return PARTLY_CLOUDY;
            case "Mostly cloudy": return MOSTLY_CLOUDY;
            case "Clear"        : return CLEAR;
            default: throw new IllegalArgumentException("'" + name + "' is an unknown sky type name");
        }
    }

    public static SkyConditionType ofOrdonalNumber(int no) {
        return SkyConditionType.values()[no];
    }
}
