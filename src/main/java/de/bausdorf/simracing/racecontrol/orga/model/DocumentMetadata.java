package de.bausdorf.simracing.racecontrol.orga.model;

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

import de.bausdorf.simracing.racecontrol.util.FileTypeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class DocumentMetadata {
    @Id
    @GeneratedValue
    long id;

    @Column(nullable = false)
    private long eventId;
    @Column(nullable = false)
    private String originalFileName;
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    @Convert(converter = OffsetDateTimeConverter.class)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    @Convert(converter = OffsetDateTimeConverter.class)
    private OffsetDateTime lastChanged;
    @ManyToOne
    private Person createdBy;
    @Column(nullable = false)
    private FileTypeEnum documentType;
    @Column(nullable = false)
    private long refItemId;
    private String fileUrl;
    private long version;
}
