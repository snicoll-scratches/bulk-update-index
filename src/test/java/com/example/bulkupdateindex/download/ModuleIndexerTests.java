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

import com.example.bulkupdateindex.IndexActionContainer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.searchbox.action.BulkableAction;
import io.searchbox.core.Index;
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
		assertThat(container.getActions()).hasSize(6); // 2 major + 3 minor
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(170);
	}

	@Test
	public void simpleMigrationComputeMajorVersions() {
		IndexActionContainer container = migrate("module/simple-input.json");
		assertGeneration(container.getSource(), container.getActions().get(1),
				ModuleIndexer.PROJECTS_MAJOR_INDEX, "0", 10);
		assertGeneration(container.getSource(), container.getActions().get(2),
				ModuleIndexer.PROJECTS_MAJOR_INDEX, "1", 160);
	}

	@Test
	public void simpleMigrationComputeMinorVersions() {
		IndexActionContainer container = migrate("module/simple-input.json");
		assertGeneration(container.getSource(), container.getActions().get(3),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "0.8", 10);
		assertGeneration(container.getSource(), container.getActions().get(4),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "1.0", 60);
		assertGeneration(container.getSource(), container.getActions().get(5),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "1.2", 100);
	}

	@Test
	public void releaseTrainMigrationComputeTotalDownloads() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertThat(container.getActions()).hasSize(7); // 1 major + 5 minor
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(1400);
	}

	@Test
	public void releaseTrainMigrationDoesNotComputeMajorGenerations() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertGeneration(container.getSource(), container.getActions().get(1),
				ModuleIndexer.PROJECTS_MAJOR_INDEX, "1", 200);
	}

	@Test
	public void releaseTrainMigrationComputeMinorVersions() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertGeneration(container.getSource(), container.getActions().get(2),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "1.3", 50);
		assertGeneration(container.getSource(), container.getActions().get(3),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "1.4", 150);
		assertGeneration(container.getSource(), container.getActions().get(4),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "Codd", 50);
		assertGeneration(container.getSource(), container.getActions().get(5),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "Dijkstra", 500);
		assertGeneration(container.getSource(), container.getActions().get(6),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "Gosling", 650);
	}

	@Test
	public void nonStandardMigrationComputeTotalDownloads() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertThat(container.getActions()).hasSize(6); // 2 major + 3 minor
		JsonObject source = getUpdatedSource(container.getActions().get(0));
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(100);
	}

	@Test
	public void nonStandardMigrationComputeMajorVersions() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertGeneration(container.getSource(), container.getActions().get(1),
				ModuleIndexer.PROJECTS_MAJOR_INDEX, "2", 55);
		assertGeneration(container.getSource(), container.getActions().get(2),
				ModuleIndexer.PROJECTS_MAJOR_INDEX, "4", 30);
	}

	@Test
	public void nonStandardMigrationComputeMinorVersions() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertGeneration(container.getSource(), container.getActions().get(3),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "2.0", 50);
		assertGeneration(container.getSource(), container.getActions().get(4),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "2.5", 5);
		assertGeneration(container.getSource(), container.getActions().get(5),
				ModuleIndexer.PROJECTS_MINOR_INDEX, "4.1", 30);
	}

	@Test
	public void simpleIndexReturnUpdateDocument() {
		JsonObject source = read("module/simple-input.json");
		assertThat(this.indexer.index(source)).hasSize(6);
	}

	@Test
	public void simpleIndexAlreadyMigratedOnlyContainsGenerationsIndex() {
		JsonObject source = read("module/simple-migrated.json");
		assertThat(this.indexer.index(source)).hasSize(5);
	}

	private void assertGeneration(JsonObject source, BulkableAction action,
			String indexName, String version, long count) {
		JsonObject json = assertIndexAction(action, indexName);
		assertThat(source.get("from").getAsLong())
				.isEqualTo(json.get("from").getAsLong());
		assertThat(source.get("to").getAsLong()).isEqualTo(json.get("to").getAsLong());
		assertThat(source.get("projectId").getAsString())
				.isEqualTo(json.get("projectId").getAsString());
		assertThat(source.get("groupId").getAsString())
				.isEqualTo(json.get("groupId").getAsString());
		assertThat(source.get("artifactId").getAsString())
				.isEqualTo(json.get("artifactId").getAsString());
		assertThat(json.get("version").getAsString()).isEqualTo(version);
		assertThat(json.getAsJsonPrimitive("count").getAsLong()).isEqualTo(count);
		assertThat(json.size()).isEqualTo(7);
	}

	private JsonObject assertIndexAction(BulkableAction<?> action, String indexName) {
		assertThat(action).isInstanceOf(Index.class);
		assertThat(action.getIndex()).isEqualTo(indexName);
		assertThat(action.getRestMethodName()).isEqualTo("POST");
		assertThat(action.getType()).isEqualTo("download");
		return (JsonObject) new DirectFieldAccessor(action).getPropertyValue("payload");
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
