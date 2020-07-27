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

import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CachedRepository<T> extends SimpleRepository<T> {

	protected Map<String, CacheEntry<T>> cache;

	public CachedRepository(FirestoreDB db) {
		super(db);
		cache = new HashMap<>();
	}

	@Override
	public void save(String collectionName, String name, T object) {
		super.save(collectionName, name, object);
		putToCache(name, object);
	}

	@Override
	public void delete(String collectionName, String name) {
		super.delete(collectionName, name);
		removeFromCache(name);
	}
	
	protected void putToCache(String key, T object) {
		cache.put(key, new CacheEntry<>(object));
	}

	protected Optional<T> getFromCache(String key) {
		if( cache.containsKey(key) ) {
			return Optional.ofNullable(cache.get(key).getContent());
		}
		return Optional.empty();
	}

	protected void removeFromCache(String key) {
		if( cache.containsKey(key) ) {
			cache.remove(key);
		}
	}

	public void flushCache() {
		cache.clear();
	}

	@Override
	protected Optional<T> findByName(String collectionName, String key) {
		if( cache.containsKey(key) ) {
			return getFromCache(key);
		}
		Optional<T> fromDb = load(collectionName, key);
		if( fromDb.isPresent() ) {
			putToCache(key, fromDb.get());
		}
		return fromDb;
	}
}
