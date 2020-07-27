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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SimpleRepository<T> {
	private FirestoreDB firestore;

	protected abstract T fromMap(Map<String, Object> data);
	protected abstract Map<String, Object> toMap(T object);

	public SimpleRepository(FirestoreDB db) {
		this.firestore = db;
	}

	public Optional<T> load(String collectionName, String name) {
		if(StringUtils.isEmpty(collectionName) || StringUtils.isEmpty(name) ) {
			return Optional.empty();
		}
		ApiFuture<DocumentSnapshot> carDoc = firestore.getDocumentById(collectionName, name);
		DocumentSnapshot docSnap = null;
		try {
			docSnap = carDoc.get();
			return Optional.ofNullable(fromMap(docSnap.getData()));
		} catch (InterruptedException e) {
			log.warn(e.getMessage());
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (ExecutionException e) {
			log.warn(e.getMessage());
			return Optional.empty();
		}
	}

	public List<T> loadAll(String collectionName) {
		List<QueryDocumentSnapshot> list = firestore.loadAll(collectionName);
		List<T> objectList = new ArrayList<>();
		for( QueryDocumentSnapshot docSnap : list ) {
			objectList.add(fromMap(docSnap.getData()));
		}
		return objectList;
	}

	public void save(String collectionName, String name, T object) {
		firestore.save(collectionName, name, toMap(object));
	}

	public void update(String collectionName, String documentName, Map<String, Object> updates) {
		firestore.updateDocument(collectionName, documentName, updates);
	}

	public void delete(String collectionName, String name) {
		firestore.delete(collectionName, name);
	}

	protected Optional<T> findByName(String collectionName, String key) {
		return load(collectionName, key);
	}

	public List<T> findByFieldValue(String collectionName, String fieldName, String fieldValue) {
		List<QueryDocumentSnapshot> list = firestore.findByFieldValue(collectionName, fieldName, fieldValue);
		List<T> objectList = new ArrayList<>();
		for( QueryDocumentSnapshot docSnap : list ) {
			objectList.add(fromMap(docSnap.getData()));
		}
		return objectList;
	}

	public List<T> findByArrayContains(String collectionName, String fieldName, String fieldValue) {
		List<T> objectList = new ArrayList<>();
		if (fieldName != null && fieldValue != null) {
			List<QueryDocumentSnapshot> list = firestore.findByArrayContains(collectionName, fieldName, fieldValue);
			for (QueryDocumentSnapshot docSnap : list) {
				objectList.add(fromMap(docSnap.getData()));
			}
		}
		return objectList;
	}

	public CollectionReference getCollectionReference(String collectionName) {
		return firestore.getCollectionReference(collectionName);
	}

	public List<T> findByQuery(Query query) {
		List<T> objectList = new ArrayList<>();
		if (query != null) {
			List<QueryDocumentSnapshot> list = firestore.findByQuery(query);
			for (QueryDocumentSnapshot docSnap : list) {
				objectList.add(fromMap(docSnap.getData()));
			}
		}
		return objectList;
	}
}
