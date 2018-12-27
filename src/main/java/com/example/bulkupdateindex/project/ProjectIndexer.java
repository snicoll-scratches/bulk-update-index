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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.bulkupdateindex.AbstractIndexer;
import com.example.bulkupdateindex.BulkUpdateIndex;
import com.example.bulkupdateindex.IndexActionContainer;
import com.example.bulkupdateindex.support.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Index the {@code initializr} requests to add more information about versions and more
 * structure for selected dependencies.
 *
 * @author Stephane Nicoll
 */
public class ProjectIndexer extends AbstractIndexer {

	private static final Logger logger = LoggerFactory.getLogger(ProjectIndexer.class);

	public void indexRequests(BulkUpdateIndex bulkUpdateIndex, String indexName)
			throws IOException {
		logger.info("Reindexing generated project requests");
		Search.Builder searchBuilder = new Search.Builder("").addIndex(indexName)
				.addType("request");
		bulkUpdateIndex.update(searchBuilder, 2000, this::index);
	}

	protected void migrate(IndexActionContainer container) {
		JsonObject source = container.getSource();
		JsonObject target = initializeDocument(source);
		indexVersion(source, target);
		indexDependencies(source, target);
		indexBuildSystem(source, target);
		container.addAction(new Index.Builder(target)
				.index(container.getDocument().get("_index").getAsString() + "-new")
				.type("request").build());
	}

	private JsonObject initializeDocument(JsonObject source) {
		JsonObject object = new JsonObject();
		object.addProperty("generationTimestamp",
				source.get("generationTimestamp").getAsLong());
		object.addProperty("type", source.get("type").getAsString());
		object.addProperty("groupId", source.get("groupId").getAsString());
		object.addProperty("artifactId", source.get("artifactId").getAsString());
		object.addProperty("javaVersion", source.get("javaVersion").getAsString());
		object.addProperty("language", source.get("language").getAsString());
		object.addProperty("packageName", source.get("packageName").getAsString());
		object.addProperty("packaging", source.get("packaging").getAsString());

		return object;
	}

	private void indexVersion(JsonObject source, JsonObject target) {
		Version version = determineSpringBootVersion(source);
		if (version != null) {
			JsonObject versionObject = new JsonObject();
			versionObject.addProperty("id", version.toString());
			versionObject.addProperty("major", String.format("%s", version.getMajor()));
			if (version.getMinor() != null) {
				versionObject.addProperty("minor",
						String.format("%s.%s", version.getMajor(), version.getMinor()));
			}
			target.add("version", versionObject);
		}
	}

	private void indexDependencies(JsonObject source, JsonObject target) {
		List<String> dependencies = determineRawDependencies(source);
		if (dependencies != null) {
			JsonObject dependenciesObject = new JsonObject();
			JsonArray values = new JsonArray();
			dependencies.forEach(values::add);
			dependenciesObject.add("values", values);
			String dependenciesId = computeDependenciesId(dependencies);
			dependenciesObject.addProperty("id", dependenciesId);
			dependenciesObject.addProperty("count", dependencies.size());
			target.add("dependencies", dependenciesObject);
		}
	}

	private void indexBuildSystem(JsonObject source, JsonObject target) {
		String type = source.get("type").getAsString();
		String[] elements = type.split("-");
		if (elements.length == 2) {
			target.addProperty("buildSystem", elements[0]);
		}
	}

	private Version determineSpringBootVersion(JsonObject source) {
		JsonElement versionEl = source.get("bootVersion");
		if (versionEl != null) {
			String bootVersion = versionEl.getAsString();
			Version version = Version.safeParse(bootVersion);
			if (version != null && version.getMajor() != null) {
				return version;
			}
		}
		return null;
	}

	private List<String> determineRawDependencies(JsonObject source) {
		if (source.has("invalidDependencies")
				&& source.get("invalidDependencies").getAsJsonArray().size() > 0) {
			return null;
		}
		JsonArray array = source.getAsJsonArray("dependencies");
		if (array == null) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (JsonElement jsonElement : array) {
			result.add(jsonElement.getAsString());
		}
		return result;
	}

	private String computeDependenciesId(List<String> dependencies) {
		if (ObjectUtils.isEmpty(dependencies)) {
			return "_none";
		}
		Collections.sort(dependencies);
		return StringUtils.collectionToDelimitedString(dependencies, " ");
	}

}
