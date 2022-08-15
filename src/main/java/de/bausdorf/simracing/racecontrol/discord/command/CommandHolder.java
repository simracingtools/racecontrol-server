package de.bausdorf.simracing.racecontrol.discord.command;

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

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class CommandHolder {
    private final Map<String, AbstractCommand> commandList = new HashMap<>();

    public void addCommand(AbstractCommand command) {
        commandList.put(command.getName(), command);
    }

    public Optional<AbstractCommand> getCommand(String name) {
        return Optional.ofNullable(commandList.get(name));
    }

    public Stream<AbstractCommand> stream() {
        return commandList.values().stream();
    }
}
