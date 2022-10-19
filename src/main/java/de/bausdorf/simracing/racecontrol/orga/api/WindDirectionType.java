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

public enum WindDirectionType {
    NORTH("N"),
    NORTH_EAST("NE"),
    EAST("E"),
    SOUTH_EAST("SE"),
    SOUTH("S"),
    SOUTH_WEST("SW"),
    WEST("W"),
    NORTH_WEST("NW");

    @Getter
    private final String code;

    WindDirectionType(String direction) {
        this.code = direction;
    }

    public static WindDirectionType ofCode(@NonNull String code) {
        switch(code.toUpperCase()) {
            case "N" : return NORTH;
            case "NE": return NORTH_EAST;
            case "E" : return EAST;
            case "SE": return SOUTH_EAST;
            case "S" : return SOUTH;
            case "SW": return SOUTH_WEST;
            case "W" : return WEST;
            case "NW": return NORTH_WEST;
            default: throw new IllegalArgumentException("'" + code + "' is no valid wind direction code.");
        }
    }

    public static WindDirectionType ofOrdonalNumber(int no) {
        return WindDirectionType.values()[no];
    }
}

