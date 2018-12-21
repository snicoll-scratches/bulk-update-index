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
	public void simpleMigration() {
		IndexActionContainer container = migrate("module/simple-input.json");
		assertDownload(container, 0, "0.8.0.RELEASE", "0", "0.8", 10);
		assertDownload(container, 1, "1.0.0.RELEASE", "1", "1.0", 20);
		assertDownload(container, 2, "1.0.1.RELEASE", "1", "1.0", 40);
		assertDownload(container, 3, "1.2.0.RELEASE", "1", "1.2", 100);
		assertThat(container.getActions()).hasSize(4);
	}

	@Test
	public void releaseTrainMigration() {
		IndexActionContainer container = migrate("module/release-train-input.json");
		assertDownload(container, 0, "Codd-SR2", null, "Codd", 50);
		assertDownload(container, 1, "Dijkstra-RELEASE", null, "Dijkstra", 200);
		assertDownload(container, 2, "Dijkstra-SR1", null, "Dijkstra", 100);
		assertDownload(container, 3, "Dijkstra-SR3", null, "Dijkstra", 200);
		assertDownload(container, 4, "Gosling-SR1", null, "Gosling", 100);
		assertDownload(container, 5, "Gosling-SR2A", null, "Gosling", 400);
		assertDownload(container, 6, "Gosling-SR4", null, "Gosling", 150);
		assertDownload(container, 7, "1.3.0.RELEASE", "1", "1.3", 50);
		assertDownload(container, 8, "1.4.6.RELEASE", "1", "1.4", 150);
		assertThat(container.getActions()).hasSize(9);
	}

	@Test
	public void nonStandardMigration() {
		IndexActionContainer container = migrate("module/non-standard-input.json");
		assertDownload(container, 0, "4.1.6.RELEASE", "4", "4.1", 20);
		assertDownload(container, 1, "4.1.0.RELEASE", "4", "4.1", 10);
		assertDownload(container, 2, "2.5.6", "2", "2.5", 5);
		assertDownload(container, 3, "2.0-m1", "2", "2.0", 50);
		assertDownload(container, 4, "${spring.version}", null, null, 5);
		assertDownload(container, 5, "", null, null, 5);
		assertDownload(container, 6, "3..0.RELEASE", null, null, 5);
		assertThat(container.getActions()).hasSize(7);
	}

	@Test
	public void simpleIndexReturnActions() {
		JsonObject source = read("module/simple-input.json");
		assertThat(this.indexer.index(source)).hasSize(4);
	}

	private void assertDownload(IndexActionContainer container, int index, String version,
			String major, String minor, long count) {
		JsonObject source = container.getSource();
		BulkableAction<?> action = container.getActions().get(index);
		JsonObject json = assertIndexAction(action);
		assertThat(source.get("from").getAsLong())
				.isEqualTo(json.get("from").getAsLong());
		assertThat(source.get("to").getAsLong()).isEqualTo(json.get("to").getAsLong());
		assertThat(source.get("projectId").getAsString())
				.isEqualTo(json.get("projectId").getAsString());
		assertThat(source.get("groupId").getAsString())
				.isEqualTo(json.get("groupId").getAsString());
		assertThat(source.get("artifactId").getAsString())
				.isEqualTo(json.get("artifactId").getAsString());
		assertThat(json.getAsJsonPrimitive("count").getAsLong()).isEqualTo(count);
		assertThat(json.has("version")).isTrue();
		JsonObject versionRef = json.getAsJsonObject("version");
		assertThat(versionRef.get("id").getAsString()).isEqualTo(version);
		if (major != null) {
			assertThat(versionRef.get("major").getAsString()).isEqualTo(major);
		}
		else {
			assertThat(versionRef.has("major")).isFalse();
		}
		if (minor != null) {
			assertThat(versionRef.get("minor").getAsString()).isEqualTo(minor);
		}
		else {
			assertThat(versionRef.has("minor")).isFalse();
		}
	}

	private JsonObject assertIndexAction(BulkableAction<?> action) {
		assertThat(action).isInstanceOf(Index.class);
		assertThat(action.getIndex()).isEqualTo("downloads");
		assertThat(action.getRestMethodName()).isEqualTo("POST");
		assertThat(action.getType()).isEqualTo("download");
		return (JsonObject) new DirectFieldAccessor(action).getPropertyValue("payload");
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
