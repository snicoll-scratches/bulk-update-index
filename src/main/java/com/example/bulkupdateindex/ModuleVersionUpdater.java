package com.example.bulkupdateindex;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.bulkupdateindex.support.ReleaseTrain;
import com.example.bulkupdateindex.support.Version;
import com.example.bulkupdateindex.support.VersionParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 *
 * @author Stephane Nicoll
 */
@Component
public class ModuleVersionUpdater {

	private static final Logger logger = LoggerFactory.getLogger(DependenciesIdUpdater.class);

	private final BulkUpdateIndex bulkUpdateIndex;

	public ModuleVersionUpdater(JestClient jestClient) {
		this.bulkUpdateIndex = new BulkUpdateIndex(jestClient);
	}

	public void indexVersions() throws IOException {
		logger.info("Reindexing versions");
		Search.Builder searchBuilder = new Search.Builder("")
				.addIndex("projects")
				.addType("download");
		bulkUpdateIndex.update(searchBuilder, 2000, this::updateVersions);
	}

	private Update updateVersions(JsonObject hit) {
		String id = hit.get("_id").getAsString();
		String index = hit.get("_index").getAsString();
		String type = hit.get("_type").getAsString();
		JsonObject source = hit.getAsJsonObject("_source");
		JsonArray stats = source.getAsJsonArray("stats");
		boolean modified = false;
		for (JsonElement statEl : stats) {
			JsonObject stat = statEl.getAsJsonObject();
			boolean handled = handleStat(source, stat);
			if (handled && !modified) {
				modified = true;
			}
		}
		if (modified) {
			JsonObject object = new JsonObject();
			object.add("doc", source);
			return new Update.Builder(object).index(index).id(id).type(type).build();
		}
		return null;
	}

	private boolean handleStat(JsonObject source, JsonObject stat) {
		if (stat.has("versionMajor") || (stat.has("versionMinor") || (stat.has("releaseTrain")))) {
			return false;
		}
		String version = stat.get("version").getAsString();
		Version standardVersion = new VersionParser(Collections.emptyList()).safeParse(version);
		if (standardVersion != null && standardVersion.getMajor() != null) {
			handleVersion(stat, standardVersion);
			return true;
		}
		ReleaseTrain releaseTrain = ReleaseTrain.safeParse(version);
		if (releaseTrain != null) {
			JsonPrimitive name = new JsonPrimitive(releaseTrain.getName());
			stat.add("releaseTrain", name);
			logger.debug(String.format("Set %s to release train (%s)", version, name));
			return true;
		}
		Version legacyVersion = safeLegacyVersionParse(version);
		if (legacyVersion != null) {
			handleVersion(stat, legacyVersion);
			return true;
		}
		String moduleId = source.get("groupId").getAsString() + " - "
				+ source.get("artifactId").getAsString();
		logger.warn("Could not handle " + moduleId + " --> " + version);
		return false;
	}

	private void handleVersion(JsonObject stat, Version version) {
		JsonPrimitive major = new JsonPrimitive(
				String.valueOf(version.getMajor()));
		stat.add("versionMajor", major);
		JsonPrimitive minor = new JsonPrimitive(
				version.getMajor() + "." + version.getMinor());
		if (version.getMinor() != null) {
			stat.add("versionMinor", minor);
		}
		logger.debug(String.format("Set %s to major (%s) and minor (%s)", version, major, minor));
	}

	private static final Pattern LEGACY_VERSION_REGEX =
			Pattern.compile("^(\\d+)\\.(\\d+|x)(?:[.|-]([^0-9]+)(\\d+)?)?$");


	private Version safeLegacyVersionParse(String text) {
		Matcher matcher = LEGACY_VERSION_REGEX.matcher(text.trim());
		if (!matcher.matches()) {
			return null;
		}
		Integer major = Integer.valueOf(matcher.group(1));
		Integer minor = Integer.valueOf(matcher.group(2));
		String qualifier = matcher.group(3);
		return new Version(major, minor, null, new Version.Qualifier(qualifier));

	}

}
