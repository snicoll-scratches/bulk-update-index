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
 * Tests for {@link ReleaseTrain}.
 *
 * @author Stephane Nicoll
 */
public class ReleaseTrainTests {

	@Test
	public void parseSpringDataReleaseTrain() {
		ReleaseTrain releaseTrain = ReleaseTrain.safeParse("Ingalls-SR5-1");
		assertThat(releaseTrain).isNotNull();
		assertThat(releaseTrain.getName()).isEqualTo("Ingalls");
		assertThat(releaseTrain.getQualifier()).isEqualTo("SR5-1");
	}

	@Test
	public void parseSpringCloudReleaseTrain() {
		ReleaseTrain releaseTrain = ReleaseTrain.safeParse("Dalston.SR4");
		assertThat(releaseTrain).isNotNull();
		assertThat(releaseTrain.getName()).isEqualTo("Dalston");
		assertThat(releaseTrain.getQualifier()).isEqualTo("SR4");
	}

	@Test
	public void parseReactorReleaseTrain() {
		ReleaseTrain releaseTrain = ReleaseTrain.safeParse("Bismuth-RELEASE");
		assertThat(releaseTrain).isNotNull();
		assertThat(releaseTrain.getName()).isEqualTo("Bismuth");
		assertThat(releaseTrain.getQualifier()).isEqualTo("RELEASE");
	}

	@Test
	public void shouldNotParseReleaseVersion() {
		ReleaseTrain releaseTrain = ReleaseTrain.safeParse("1.0-rc1");
		assertThat(releaseTrain).isNull();
	}

}
