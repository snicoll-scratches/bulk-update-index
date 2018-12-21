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

import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.bulkupdateindex.support.ReleaseTrain;
import com.example.bulkupdateindex.support.Version;
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
		String version = cleanVersion(stat.get("version").getAsString());

		Version standardVersion = Version.safeParse(version);
		if (standardVersion != null && standardVersion.getMajor() != null) {
			handleVersion(standardVersion, count);
			return true;
		}

		ReleaseTrain releaseTrain = ReleaseTrain.safeParse(version);
		if (releaseTrain != null) {
			handle(null, releaseTrain.getName(), count);
			return true;
		}

		Version nonStandardVersion = safeNonStandardVersionParse(version);
		if (nonStandardVersion != null) {
			handleVersion(nonStandardVersion, count);
			return true;
		}

		String moduleId = source.get("groupId").getAsString() + " - "
				+ source.get("artifactId").getAsString();
		logger.warn(String.format("Skipping %s [count=%s, version='%s']", moduleId, count,
				version));
		return false;
	}

	private String cleanVersion(String version) {
		try {
			String cleanVersion = URLDecoder.decode(version, "UTF-8");
			int i = cleanVersion.lastIndexOf("?");
			return (i != -1) ? cleanVersion.substring(i + 1) : cleanVersion;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return version;
		}
	}

	private void handleVersion(Version standardVersion, long count) {
		String major = String.format("%s", standardVersion.getMajor());
		String minor = (standardVersion.getMinor() != null) ? String.format("%s.%s",
				standardVersion.getMajor(), standardVersion.getMinor()) : null;
		handle(major, minor, count);
	}

	private void handle(String major, String minor, Long count) {
		if (major != null) {
			this.majorGenerations.compute(major,
					(key, v) -> (v != null) ? v + count : count);
		}
		if (minor != null) {
			this.minorGenerations.compute(minor,
					(key, v) -> (v != null) ? v + count : count);
		}
	}

	private static final Pattern NON_STANDARD_VERSION_REGEX = Pattern
			.compile("^(\\d+)\\.(\\d+|x)(?:[.|-]([^0-9]+)(\\d+)?)?$");

	private Version safeNonStandardVersionParse(String text) {
		Matcher matcher = NON_STANDARD_VERSION_REGEX.matcher(text.trim());
		if (!matcher.matches()) {
			return null;
		}
		Integer major = Integer.valueOf(matcher.group(1));
		Integer minor = Integer.valueOf(matcher.group(2));
		String qualifier = matcher.group(3);
		return new Version(major, minor, null, new Version.Qualifier(qualifier));

	}

}
