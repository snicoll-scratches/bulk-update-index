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