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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectIndexer}.
 *
 * @author Stephane Nicoll
 */
public class ProjectIndexerTests {

	private final ProjectIndexer indexer = new ProjectIndexer();

	@Test
	public void indexVersion() {
		JsonObject source = readSource("project/simple-input.json");
		assertThat(this.indexer.migrate(source)).isTrue();
		assertThat(source.has("version")).isTrue();
		JsonObject version = source.get("version").getAsJsonObject();
		assertThat(version.get("id").getAsString()).isEqualTo("2.1.1.RELEASE");
		assertThat(version.get("major").getAsString()).isEqualTo("2");
		assertThat(version.get("minor").getAsString()).isEqualTo("2.1");
	}

	@Test
	public void indexDependenciesId() {
		JsonObject source = readSource("project/simple-input.json");
		assertThat(this.indexer.migrate(source)).isTrue();
		assertThat(source.has("dependenciesId")).isTrue();
		assertThat(source.get("dependenciesId").getAsString()).isEqualTo("security web");
	}

	private JsonObject readSource(String location) {
		return read(location).getAsJsonObject("_source");
	}

	private JsonObject read(String location) {
		try {
			try (InputStream in = new ClassPathResource(location).getInputStream()) {
				String json = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
				return new Gson().fromJson(json, JsonObject.class);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Fail to read json from " + location, ex);
		}
	}

}
