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

package com.example.bulkupdateindex.support;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An object that gathers a version and its major and minor references.
 *
 * @author Stephane Nicoll
 */
public final class VersionReference {

	private static final Pattern NON_STANDARD_VERSION_REGEX = Pattern
			.compile("^(\\d+)\\.(\\d+|x)(?:[.|-]([^0-9]+)(\\d+)?)?$");

	private final String id;

	private final String major;

	private final String minor;

	private VersionReference(String id, String major, String minor) {
		this.id = id;
		this.major = major;
		this.minor = minor;
	}

	public String getId() {
		return this.id;
	}

	public String getMajor() {
		return this.major;
	}

	public String getMinor() {
		return this.minor;
	}

	public static VersionReference of(String id, String major, String minor) {
		return new VersionReference(id, major, minor);
	}

	public static VersionReference parse(String text) {
		String versionText = cleanVersion(text);

		Version standardVersion = Version.safeParse(versionText);
		if (standardVersion != null && standardVersion.getMajor() != null) {
			return fromVersion(versionText, standardVersion);
		}

		ReleaseTrain releaseTrain = ReleaseTrain.safeParse(text);
		if (releaseTrain != null) {
			return of(versionText, null, releaseTrain.getName());
		}

		Version nonStandardVersion = safeNonStandardVersionParse(versionText);
		if (nonStandardVersion != null) {
			return fromVersion(versionText, nonStandardVersion);
		}
		return of(versionText, null, null);
	}

	private static VersionReference fromVersion(String id, Version version) {
		String major = String.format("%s", version.getMajor());
		String minor = (version.getMinor() != null)
				? String.format("%s.%s", version.getMajor(), version.getMinor()) : null;
		return of(id, major, minor);
	}

	private static String cleanVersion(String version) {
		try {
			String cleanVersion = URLDecoder.decode(version, "UTF-8");
			int i = cleanVersion.lastIndexOf("?");
			return (i != -1) ? cleanVersion.substring(i + 1) : cleanVersion;
		}
		catch (Exception ex) {
			return version;
		}
	}

	private static Version safeNonStandardVersionParse(String text) {
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
