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

import com.example.bulkupdateindex.support.VersionReference;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Denormalize stat entries to individual documents.
 *
 * @author Stephane Nicoll
 */
class StatHandler {

	private static final Logger logger = LoggerFactory.getLogger(StatHandler.class);

	JsonObject handle(JsonObject source, JsonObject stat) {
		String version = stat.get("version").getAsString();
		VersionReference versionReference = VersionReference.parse(version);
		if (versionReference.getMajor() == null && versionReference.getMinor() == null) {
			String moduleId = source.get("groupId").getAsString() + " - "
					+ source.get("artifactId").getAsString();
			logger.warn(String.format(
					"%s will have no generation information[count=%s, version='%s']",
					moduleId, stat.get("count").getAsLong(), versionReference.getId()));
		}
		return createDocument(source, stat, versionReference);
	}

	private JsonObject createDocument(JsonObject source, JsonObject stat,
			VersionReference versionReference) {
		JsonObject object = new JsonObject();
		object.addProperty("from", source.get("from").getAsLong());
		object.addProperty("to", source.get("to").getAsLong());
		object.addProperty("projectId", source.get("projectId").getAsString());
		object.addProperty("groupId", source.get("groupId").getAsString());
		object.addProperty("artifactId", source.get("artifactId").getAsString());
		object.addProperty("source", stat.get("source").getAsString());
		object.addProperty("count", stat.get("count").getAsLong());
		JsonObject versionObject = new JsonObject();
		versionObject.addProperty("id", versionReference.getId());
		if (versionReference.getMajor() != null) {
			versionObject.addProperty("major", versionReference.getMajor());
		}
		if (versionReference.getMinor() != null) {
			versionObject.addProperty("minor", versionReference.getMinor());
		}
		object.add("version", versionObject);
		return object;
	}

}
