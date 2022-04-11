package de.bausdorf.simracing.racecontrol.util;

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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class UploadFileManager {
    private final RacecontrolServerProperties config;

    public UploadFileManager(@Autowired RacecontrolServerProperties config) {
        this.config = config;
    }

    public String uploadEventFile(MultipartFile multipartFile, String eventId, FileTypeEnum type) throws IOException {
        Path fileDestinationDir = Paths.get(config.getFileUploadBasePath()
                + "/event-" + eventId + '/' + type.getDestination());
        log.debug("Try to upload to {}", fileDestinationDir);
        checkAndCreateDirectories(fileDestinationDir);
        Path destinationFile = Paths.get(fileDestinationDir.toFile().getAbsolutePath(), multipartFile.getOriginalFilename());
        try (OutputStream os = Files.newOutputStream(destinationFile)) {
            os.write(multipartFile.getBytes());
            log.info("Uploaded {}", destinationFile);
        }
        return getFileUri(eventId, type, multipartFile.getOriginalFilename());
    }

    private void checkAndCreateDirectories(Path fileDestinationDir) throws IOException {
        if (!fileDestinationDir.toFile().exists()) {
            Files.createDirectories(fileDestinationDir);
        } else if (!fileDestinationDir.toFile().isDirectory()) {
            throw new IOException(fileDestinationDir + " exists as regular file");
        }
    }

    private String getFileUri(String eventId, FileTypeEnum fileType, String fileName) {
        return config.getUploadBaseUri() + "/event-" + eventId + '/' + fileType.getDestination() + fileName;
    }

}
