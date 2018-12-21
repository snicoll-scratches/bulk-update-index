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

package com.example.bulkupdateindex.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.bulkupdateindex.IndexActionContainer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.action.BulkableAction;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModuleIndexer}.
 *
 * @author Stephane Nicoll
 */
public class ModuleIndexerTests {

	private static final Gson GSON = new Gson();

	private final ModuleIndexer indexer = new ModuleIndexer();

	@Test
	public void simpleMigrationComputeTotalDownloads() {
		IndexActionContainer container = migrate("module/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(170);
	}

	@Test
	public void simpleMigrationComputeMajorVersions() {
		IndexActionContainer container = migrate("module/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("majorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("majorGenerations");
		assertThat(generations).hasSize(2);
		assertGeneration(generations.get(0), "0", 10);
		assertGeneration(generations.get(1), "1", 160);
	}

	@Test
	public void simpleMigrationComputeMinorVersions() {
		IndexActionContainer container = migrate("module/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("minorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("minorGenerations");
		assertThat(generations).hasSize(3);
		assertGeneration(generations.get(0), "0.8", 10);
		assertGeneration(generations.get(1), "1.0", 60);
		assertGeneration(generations.get(2), "1.2", 100);
	}

	@Test
	public void releaseTrainMigrationComputeTotalDownloads() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(1400);
	}

	@Test
	public void releaseTrainMigrationDoesNotComputeMajorGenerations() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("majorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("majorGenerations");
		assertThat(generations).hasSize(1);
		assertGeneration(generations.get(0), "1", 200);
	}

	@Test
	public void releaseTrainMigrationComputeMinorVersions() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("minorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("minorGenerations");
		assertThat(generations).hasSize(5);
		assertGeneration(generations.get(0), "1.3", 50);
		assertGeneration(generations.get(1), "1.4", 150);
		assertGeneration(generations.get(2), "Codd", 50);
		assertGeneration(generations.get(3), "Dijkstra", 500);
		assertGeneration(generations.get(4), "Gosling", 650);
	}

	@Test
	public void nonStandardMigrationComputeTotalDownloads() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(100);
	}

	@Test
	public void nonStandardMigrationComputeMajorVersions() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("majorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("majorGenerations");
		assertThat(generations).hasSize(2);
		assertGeneration(generations.get(0), "2", 55);
		assertGeneration(generations.get(1), "4", 30);
	}

	@Test
	public void nonStandardMigrationComputeMinorVersions() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("minorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("minorGenerations");
		assertThat(generations).hasSize(3);
		assertGeneration(generations.get(0), "2.0", 50);
		assertGeneration(generations.get(1), "2.5", 5);
		assertGeneration(generations.get(2), "4.1", 30);
	}

	@Test
	public void simpleIndexReturnUpdateDocument() {
		JsonObject source = read("module/simple-input.json");
		List<BulkableAction<?>> actions = this.indexer.index(source);
		assertThat(actions).hasSize(1);
	}

	@Test
	public void simpleIndexMissingTotalCountReturnUpdateDocument() {
		JsonObject source = read("module/simple-migrated-missing-total-count.json");
		List<BulkableAction<?>> actions = this.indexer.index(source);
		assertThat(actions).hasSize(1);
	}

	@Test
	public void simpleIndexAlreadyMigratedReturnNull() {
		JsonObject source = read("module/simple-migrated.json");
		assertThat(this.indexer.index(source)).isEmpty();
	}

	private void assertGeneration(JsonElement item, String name, long count) {
		JsonObject json = item.getAsJsonObject();
		assertThat(json.get("name").getAsString()).isEqualTo(name);
		assertThat(json.getAsJsonPrimitive("count").getAsLong()).isEqualTo(count);
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
