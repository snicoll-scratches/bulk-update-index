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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.bulkupdateindex.support.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.Update;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Index the {@code initializr} document to add more information about versions and more
 * structure for selected dependencies.
 *
 * @author Stephane Nicoll
 */
public class ProjectIndexer {

	protected boolean migrate(JsonObject source) {
		if (source.has("version") && source.has("dependenciesId")
				&& source.has("dependenciesCount")) {
			return false;
		}
		indexVersion(source);
		indexDependencies(source);
		return true;
	}

	protected Update index(JsonObject input) {
		String id = input.get("_id").getAsString();
		String index = input.get("_index").getAsString();
		String type = input.get("_type").getAsString();
		JsonObject source = input.getAsJsonObject("_source");
		boolean modified = migrate(source);
		if (modified) {
			JsonObject object = new JsonObject();
			object.add("doc", source);
			return new Update.Builder(object).index(index).id(id).type(type).build();
		}
		return null;
	}

	private void indexVersion(JsonObject source) {
		Version version = determineSpringBootVersion(source);
		if (version != null) {
			JsonObject versionObject = new JsonObject();
			versionObject.addProperty("id", version.toString());
			versionObject.addProperty("major", String.format("%s", version.getMajor()));
			if (version.getMinor() != null) {
				versionObject.addProperty("minor",
						String.format("%s.%s", version.getMajor(), version.getMinor()));
			}
			source.add("version", versionObject);
		}
	}

	private void indexDependencies(JsonObject source) {
		List<String> dependencies = determineRawDependencies(source);
		if (dependencies != null) {
			String dependenciesId = computeDependenciesId(dependencies);
			source.addProperty("dependenciesId", dependenciesId);
			source.addProperty("dependenciesCount", dependencies.size());
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
