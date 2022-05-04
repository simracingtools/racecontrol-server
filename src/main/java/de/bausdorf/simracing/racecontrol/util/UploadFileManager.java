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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class UploadFileManager {
    public static final String EVENT_SUBDIR = "/event-";
    public static final String USER_SUBDIR = "/user-";
    private final RacecontrolServerProperties config;

    public UploadFileManager(@Autowired RacecontrolServerProperties config) {
        this.config = config;
    }

    public String uploadEventFile(@NonNull MultipartFile multipartFile, @NonNull String eventId, @NonNull FileTypeEnum type) throws IOException {
        Path fileDestinationDir = Paths.get(config.getFileUploadBasePath()
                + EVENT_SUBDIR + eventId + '/' + type.getDestination());
        saveFile(fileDestinationDir, multipartFile, multipartFile.getOriginalFilename());
        return config.getUploadBaseUri() + EVENT_SUBDIR + eventId + '/' + type.getDestination() + multipartFile.getOriginalFilename();
    }

    public String uploadUserFile(@NonNull MultipartFile multipartFile, @NonNull String userId, @NonNull FileTypeEnum type) throws IOException {
        Path fileDestinationDir = Paths.get(config.getFileUploadBasePath()
                + USER_SUBDIR + userId + '/' + type.getDestination());
        saveFile(fileDestinationDir, multipartFile, multipartFile.getOriginalFilename());
        return config.getUploadBaseUri() + USER_SUBDIR + userId + '/' + type.getDestination() + multipartFile.getOriginalFilename();
    }

    public String uploadTeamLogo(@NonNull MultipartFile multipartFile, @NonNull String eventId, @NonNull String teamIRacingId, @NonNull FileTypeEnum type) throws IOException {
        Path fileDestinationDir = Paths.get(config.getFileUploadBasePath()
                + EVENT_SUBDIR + eventId + '/' + type.getDestination());
        String fileExtension = Objects.requireNonNull(multipartFile.getOriginalFilename()).split("\\.")[1];
        saveFile(fileDestinationDir, multipartFile, "teamlogo-" + teamIRacingId + "." + fileExtension);
        return config.getUploadBaseUri() + EVENT_SUBDIR + eventId + '/' + type.getDestination() + multipartFile.getOriginalFilename();
    }

    private void saveFile(Path fileDestinationDir, MultipartFile multipartFile, String destinationFileName) throws IOException {
        log.debug("Try to upload to {}", fileDestinationDir);
        checkAndCreateDirectories(fileDestinationDir);
        Path destinationFile = Paths.get(fileDestinationDir.toFile().getAbsolutePath(), destinationFileName);
        try (OutputStream os = Files.newOutputStream(destinationFile)) {
            os.write(multipartFile.getBytes());
            log.info("Uploaded {}", destinationFile);
        }
    }

    private void checkAndCreateDirectories(Path fileDestinationDir) throws IOException {
        if (!fileDestinationDir.toFile().exists()) {
            Files.createDirectories(fileDestinationDir);
        } else if (!fileDestinationDir.toFile().isDirectory()) {
            throw new IOException(fileDestinationDir + " exists as regular file");
        }
    }
}
