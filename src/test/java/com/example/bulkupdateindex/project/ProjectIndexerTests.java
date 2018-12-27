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
import com.google.gson.JsonPrimitive;
import io.searchbox.action.BulkableAction;
import io.searchbox.core.Index;
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
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("version")).isTrue();
		JsonObject version = source.get("version").getAsJsonObject();
		assertThat(version.get("id").getAsString()).isEqualTo("2.1.1.RELEASE");
		assertThat(version.get("major").getAsString()).isEqualTo("2");
		assertThat(version.get("minor").getAsString()).isEqualTo("2.1");
	}

	@Test
	public void indexDependencies() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("dependencies")).isTrue();
		JsonObject dependencies = source.get("dependencies").getAsJsonObject();
		assertThat(dependencies.getAsJsonArray("values"))
				.containsExactly(new JsonPrimitive("web"), new JsonPrimitive("security"));
		assertThat(dependencies.get("id").getAsString()).isEqualTo("security web");
		assertThat(dependencies.get("count").getAsInt()).isEqualTo(2);
	}

	@Test
	public void indexBuildSystem() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("buildSystem")).isTrue();
		assertThat(source.get("buildSystem").getAsString()).isEqualTo("maven");
	}

	@Test
	public void indexClient() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("client")).isTrue();
		JsonObject client = source.get("client").getAsJsonObject();
		assertThat(client.get("id").getAsString()).isEqualTo("my-ide");
		assertThat(client.get("version").getAsString()).isEqualTo("1.2.3");
		assertThat(client.get("ip").getAsString()).isEqualTo("127.0.0.1");
		assertThat(client.get("country").getAsString()).isEqualTo("ID");
	}

	@Test
	public void indexValidRequest() {
		IndexActionContainer container = migrate("project/simple-input.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isFalse();
	}

	@Test
	public void indexWrongJavaVersionRequest() {
		IndexActionContainer container = migrate(
				"project/simple-invalid-wrong-java-version.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isTrue();
		JsonObject errorState = source.get("errorState").getAsJsonObject();
		assertThat(errorState.get("invalid").getAsBoolean()).isTrue();
		assertThat(errorState.get("javaVersion").getAsBoolean()).isTrue();
		assertThat(errorState.size()).isEqualTo(2);
		assertThat(source.get("javaVersion").getAsString()).isEqualTo("abc");
	}

	@Test
	public void indexWrongLanguageRequest() {
		IndexActionContainer container = migrate(
				"project/simple-invalid-wrong-language.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isTrue();
		JsonObject errorState = source.get("errorState").getAsJsonObject();
		assertThat(errorState.get("invalid").getAsBoolean()).isTrue();
		assertThat(errorState.get("language").getAsBoolean()).isTrue();
		assertThat(errorState.size()).isEqualTo(2);
		assertThat(source.get("language").getAsString()).isEqualTo("c");
	}

	@Test
	public void indexWrongPackagingRequest() {
		IndexActionContainer container = migrate(
				"project/simple-invalid-wrong-packaging.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isTrue();
		JsonObject errorState = source.get("errorState").getAsJsonObject();
		assertThat(errorState.get("invalid").getAsBoolean()).isTrue();
		assertThat(errorState.get("packaging").getAsBoolean()).isTrue();
		assertThat(errorState.size()).isEqualTo(2);
		assertThat(source.get("packaging").getAsString()).isEqualTo("pom");
	}

	@Test
	public void indexWrongTypeRequest() {
		IndexActionContainer container = migrate(
				"project/simple-invalid-wrong-type.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isTrue();
		JsonObject errorState = source.get("errorState").getAsJsonObject();
		assertThat(errorState.get("invalid").getAsBoolean()).isTrue();
		assertThat(errorState.get("type").getAsBoolean()).isTrue();
		assertThat(errorState.size()).isEqualTo(2);
		assertThat(source.get("type").getAsString()).isEqualTo("build");
		assertThat(source.has("buildSystem")).isFalse();
	}

	@Test
	public void indexInvalidDependenciesRequest() {
		IndexActionContainer container = migrate(
				"project/simple-invalid-dependencies.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isTrue();
		JsonObject errorState = source.get("errorState").getAsJsonObject();
		assertThat(errorState.get("invalid").getAsBoolean()).isTrue();
		assertThat(errorState.get("dependencies").getAsJsonArray())
				.containsExactly(new JsonPrimitive("h2"), new JsonPrimitive("h3"));
		assertThat(errorState.size()).isEqualTo(2);
		assertThat(source.getAsJsonObject("dependencies").getAsJsonArray("values"))
				.containsExactly(new JsonPrimitive("web"), new JsonPrimitive("security"));
	}

	@Test
	public void indexInvalidRequestWithErrorMessage() {
		IndexActionContainer container = migrate(
				"project/simple-invalid-error-message.json");
		assertThat(container.getActions()).hasSize(1);
		JsonObject source = assertIndexAction(container.getActions().get(0));
		assertThat(source.has("errorState")).isTrue();
		JsonObject errorState = source.get("errorState").getAsJsonObject();
		assertThat(errorState.get("invalid").getAsBoolean()).isTrue();
		assertThat(errorState.get("message").getAsString())
				.isEqualTo("Something went wrong");
		assertThat(errorState.size()).isEqualTo(2);
	}

	@Test
	public void indexReturnAction() {
		JsonObject source = read("project/simple-input.json");
		List<BulkableAction<?>> actions = this.indexer.index(source);
		assertThat(actions).hasSize(1);
	}

	private JsonObject assertIndexAction(BulkableAction<?> action) {
		assertThat(action).isInstanceOf(Index.class);
		assertThat(action.getIndex()).isEqualTo("initializr-2015-new");
		assertThat(action.getRestMethodName()).isEqualTo("POST");
		assertThat(action.getType()).isEqualTo("request");
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
