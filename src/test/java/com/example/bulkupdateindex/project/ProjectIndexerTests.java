/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bulkupdateindex.project;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.bulkupdateindex.IndexActionContainer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.searchbox.action.BulkableAction;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectIndexer}.
 *
 * @author Stephane Nicoll
 */
public class ProjectIndexerTests {

	private static final Gson GSON = new Gson();

	private final ProjectIndexer indexer = new ProjectIndexer();

	@Test
	public void indexVersion() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("version")).isTrue();
		JsonObject version = source.get("version").getAsJsonObject();
		assertThat(version.get("id").getAsString()).isEqualTo("2.1.1.RELEASE");
		assertThat(version.get("major").getAsString()).isEqualTo("2");
		assertThat(version.get("minor").getAsString()).isEqualTo("2.1");
	}

	@Test
	public void indexDependenciesId() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("dependenciesId")).isTrue();
		assertThat(source.get("dependenciesId").getAsString()).isEqualTo("security web");
	}

	@Test
	public void indexDependenciesCount() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("dependenciesCount")).isTrue();
		assertThat(source.get("dependenciesCount").getAsInt()).isEqualTo(2);
	}

	@Test
	public void indexMissingVersionReturnUpdateDocument() {
		JsonObject source = read("project/simple-migrated-missing-version.json");
		List<BulkableAction<?>> actions = this.indexer.index(source);
		assertThat(actions).hasSize(1);
	}

	@Test
	public void indexMissingDependenciesIdReturnUpdateDocument() {
		JsonObject source = read("project/simple-migrated-missing-dependencies-id.json");
		List<BulkableAction<?>> actions = this.indexer.index(source);
		assertThat(actions).hasSize(1);
	}

	@Test
	public void indexMissingDependenciesCountReturnUpdateDocument() {
		JsonObject source = read(
				"project/simple-migrated-missing-dependencies-count.json");
		List<BulkableAction<?>> actions = this.indexer.index(source);
		assertThat(actions).hasSize(1);
	}

	@Test
	public void indexWithUpToDateDocumentReturnsNull() {
		JsonObject source = read("project/simple-migrated.json");
		assertThat(this.indexer.index(source)).isEmpty();
	}

	private JsonObject getUpdatedSource(BulkableAction<?> action) {
		JsonObject document = (JsonObject) new DirectFieldAccessor(action)
				.getPropertyValue("payload");
		return document.getAsJsonObject("doc");
	}

	private IndexActionContainer migrate(String location) {
		IndexActionContainer container = new IndexActionContainer(read(location));
		this.indexer.migrate(container);
		return container;
	}

	private JsonObject read(String location) {
		try {
			try (InputStream in = new ClassPathResource(location).getInputStream()) {
				String json = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
				return GSON.fromJson(json, JsonObject.class);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Fail to read json from " + location, ex);
		}
	}

}
