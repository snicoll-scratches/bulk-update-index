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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VersionReference}.
 *
 * @author Stephane Nicoll
 */
public class VersionReferenceTests {

	@Test
	public void parseSimpleVersion() {
		testParser("1.2.3.RELEASE", "1", "1.2");
	}

	@Test
	public void parseVersionWithDashedQualifier() {
		testParser("2.0.0-M1", "2", "2.0");
	}

	@Test
	public void parseNoQualifier() {
		testParser("2.1.4", "2", "2.1");
	}

	@Test
	public void parseWithQuestionMarkPrefix() {
		testParser("?2.1.4", "2.1.4", "2", "2.1");
	}

	@Test
	public void parseWithEncodedVersion() {
		testParser("4%2E1%2E6%2ERELEASE", "4.1.6.RELEASE", "4", "4.1");
	}

	@Test
	public void parseWithMajorMinorOnly() {
		testParser("2.0", "2", "2.0");
	}

	@Test
	public void parseReleaseTrain() {
		testParser("Gosling-SR1", null, "Gosling");
	}

	@Test
	public void parseVersionProperty() {
		testParser("${spring.version}", null, null);
	}

	private void testParser(String text, String major, String minor) {
		testParser(text, text, major, minor);
	}

	private void testParser(String text, String id, String major, String minor) {
		VersionReference reference = VersionReference.parse(text);
		assertThat(reference).isNotNull();
		assertThat(reference.getId()).isEqualTo(id);
		assertThat(reference.getMajor()).isEqualTo(major);
		assertThat(reference.getMinor()).isEqualTo(minor);
	}

}
