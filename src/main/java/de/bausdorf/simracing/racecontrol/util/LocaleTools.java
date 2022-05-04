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

import de.bausdorf.simracing.racecontrol.web.IndexController;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LocaleTools {
    private static final String FLAG_IMAGE_EXT = "png";

    private static final List<LocaleView> LOCALE_VIEWS = new ArrayList<>();

    private final RacecontrolServerProperties config;

    public LocaleTools(@Autowired RacecontrolServerProperties config) {
        this.config = config;
        initMapCache();
    }

    private void initMapCache() {
        Map<String, String> codeToPathMap = new HashMap<>();
        try(ScanResult scanResult = new ClassGraph().acceptPaths(config.getFlagImageResourcePath()).scan()) {
            scanResult.getResourcesWithExtension(FLAG_IMAGE_EXT).getPaths().forEach(pathString -> {
                Path path = Paths.get(pathString);
                String[] fileNameParts = path.getFileName().toString().split("\\.");
                codeToPathMap.put(fileNameParts[0].toUpperCase(), pathString);
            });
        }

        Arrays.stream(Locale.getAvailableLocales())
                .filter(IndexController.distinctByKey(Locale::getCountry))
                .forEach(locale -> {
            if(codeToPathMap.containsKey(locale.getCountry())) {
                LOCALE_VIEWS.add(LocaleView.builder()
                        .countryName(locale.getDisplayCountry(Locale.ENGLISH))
                        .languageName(locale.getDisplayLanguage(Locale.ENGLISH))
                        .localeTag(locale.toLanguageTag())
                        .localeCode(locale.getCountry())
                        .flagUrl(config.getServerBaseUrl() + codeToPathMap.get(locale.getCountry()))
                        .build());
            }
        });
    }

    public static Optional<LocaleView> getLocaleViewByCode(String countryCode) {
        return LOCALE_VIEWS.stream()
                .filter(view -> view.getLocaleCode().equalsIgnoreCase(countryCode))
                .findFirst();
    }

    public static Optional<LocaleView> getLocaleViewByTag(String localeTag) {
        return LOCALE_VIEWS.stream()
                .filter(view -> view.getLocaleTag().equalsIgnoreCase(localeTag))
                .findFirst();
    }

    public static List<LocaleView> getLocaleViews() {
        return LOCALE_VIEWS.stream()
                .sorted(Comparator.comparing(LocaleView::getCountryName))
                .collect(Collectors.toList());
    }
}
