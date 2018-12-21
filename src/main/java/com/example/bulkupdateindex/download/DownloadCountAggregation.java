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

import java.util.Map;
import java.util.TreeMap;

import com.example.bulkupdateindex.support.VersionReference;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregates download count.
 *
 * @author Stephane Nicoll
 */
class DownloadCountAggregation {

	private static final Logger logger = LoggerFactory
			.getLogger(DownloadCountAggregation.class);

	private long totalCount;

	private final Map<String, Long> majorGenerations = new TreeMap<>();

	private final Map<String, Long> minorGenerations = new TreeMap<>();

	public long getTotalCount() {
		return this.totalCount;
	}

	public Map<String, Long> getMajorGenerations() {
		return this.majorGenerations;
	}

	public Map<String, Long> getMinorGenerations() {
		return this.minorGenerations;
	}

	boolean handle(JsonObject source, JsonObject stat) {
		long count = stat.get("count").getAsLong();
		this.totalCount += count;
		String versionText = stat.get("version").getAsString();
		VersionReference versionReference = VersionReference.parse(versionText);
		if (versionReference != null) {
			handle(versionReference, count);
		}
		else {
			String moduleId = source.get("groupId").getAsString() + " - "
					+ source.get("artifactId").getAsString();
			logger.warn(String.format("Skipping %s [count=%s, version='%s']", moduleId,
					count, versionText));
		}
		return versionReference != null;
	}

	private void handle(VersionReference versionReference, Long count) {
		if (versionReference.getMajor() != null) {
			this.majorGenerations.compute(versionReference.getMajor(),
					(key, v) -> (v != null) ? v + count : count);
		}
		if (versionReference.getMinor() != null) {
			this.minorGenerations.compute(versionReference.getMinor(),
					(key, v) -> (v != null) ? v + count : count);
		}
	}

}
