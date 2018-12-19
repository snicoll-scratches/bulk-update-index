package com.example.bulkupdateindex.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.Update;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModuleIndexer}.
 *
 * @author Stephane Nicoll
 */
public class ModuleIndexerTests {

	private final ModuleIndexer updater = new ModuleIndexer();

	@Test
	public void simpleMigrationComputeTotalDownloads() {
		JsonObject source = readSource("module/simple-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(170);
	}

	@Test
	public void simpleMigrationComputeMajorVersions() {
		JsonObject source = readSource("module/simple-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("majorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("majorGenerations");
		assertThat(generations).hasSize(2);
		assertGeneration(generations.get(0), "0", 10);
		assertGeneration(generations.get(1), "1", 160);
	}

	@Test
	public void simpleMigrationComputeMinorVersions() {
		JsonObject source = readSource("module/simple-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("minorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("minorGenerations");
		assertThat(generations).hasSize(3);
		assertGeneration(generations.get(0), "0.8", 10);
		assertGeneration(generations.get(1), "1.0", 60);
		assertGeneration(generations.get(2), "1.2", 100);
	}

	@Test
	public void releaseTrainMigrationComputeTotalDownloads() {
		JsonObject source = readSource("module/release-train-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(1400);
	}

	@Test
	public void releaseTrainMigrationDoesNotComputeMajorGenerations() {
		JsonObject source = readSource("module/release-train-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("majorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("majorGenerations");
		assertThat(generations).hasSize(1);
		assertGeneration(generations.get(0), "1", 200);
	}

	@Test
	public void releaseTrainMigrationComputeMinorVersions() {
		JsonObject source = readSource("module/release-train-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("minorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("minorGenerations");
		assertThat(generations).hasSize(5);
		assertGeneration(generations.get(0), "1.3", 50);
		assertGeneration(generations.get(1), "1.4", 150);
		assertGeneration(generations.get(2), "Codd", 50);
		assertGeneration(generations.get(3), "Dijkstra", 500);
		assertGeneration(generations.get(4), "Gosling", 650);
	}

	@Test
	public void nonStandardMigrationComputeTotalDownloads() {
		JsonObject source = readSource("module/non-standard-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("totalCount")).isTrue();
		assertThat(source.getAsJsonPrimitive("totalCount").getAsLong()).isEqualTo(100);
	}

	@Test
	public void nonStandardMigrationComputeMajorVersions() {
		JsonObject source = readSource("module/non-standard-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("majorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("majorGenerations");
		assertThat(generations).hasSize(2);
		assertGeneration(generations.get(0), "2", 55);
		assertGeneration(generations.get(1), "4", 30);
	}

	@Test
	public void nonStandardMigrationComputeMinorVersions() {
		JsonObject source = readSource("module/non-standard-input.json");
		assertThat(updater.migrate(source)).isTrue();
		assertThat(source.has("minorGenerations")).isTrue();
		JsonArray generations = source.getAsJsonArray("minorGenerations");
		assertThat(generations).hasSize(3);
		assertGeneration(generations.get(0), "2.0", 50);
		assertGeneration(generations.get(1), "2.5", 5);
		assertGeneration(generations.get(2), "4.1", 30);
	}

	@Test
	public void simpleIndexReturnUpdateDocument() {
		JsonObject source = read("module/simple-input.json");
		Update update = updater.index(source);
		assertThat(update).isNotNull();
	}

	@Test
	public void simpleIndexMissingTotalCountReturnUpdateDocument() {
		JsonObject source = read("module/simple-migrated-missing-total-count.json");
		Update update = updater.index(source);
		assertThat(update).isNotNull();
	}

	@Test
	public void simpleIndexAlreadyMigratedReturnNull() {
		JsonObject source = read("module/simple-migrated.json");
		assertThat(updater.index(source)).isNull();
	}

	private void assertGeneration(JsonElement item, String name, long count) {
		JsonObject json = item.getAsJsonObject();
		assertThat(json.get("name").getAsString()).isEqualTo(name);
		assertThat(json.getAsJsonPrimitive("count").getAsLong()).isEqualTo(count);
	}

	private JsonObject readSource(String location) {
		return read(location).getAsJsonObject("_source");
	}

	private JsonObject read(String location) {
		try {
			try (InputStream in = new ClassPathResource(location).getInputStream()) {
				String json = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
				return new Gson().fromJson(json, JsonObject.class);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Fail to read json from " + location, ex);
		}
	}

}
