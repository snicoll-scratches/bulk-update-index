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

import com.example.bulkupdateindex.support.Version;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Index the {@code initializr} document to add more information about versions and more
 * structure for selected dependencies.
 *
 * @author Stephane Nicoll
 */
public class ProjectIndexer {

	protected boolean migrate(JsonObject source) {
		if (source.has("version")) {
			return false;
		}
		indexVersion(source);
		return true;
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

}
