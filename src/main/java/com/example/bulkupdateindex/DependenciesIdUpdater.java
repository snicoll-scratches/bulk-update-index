package com.example.bulkupdateindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Update the "dependenciesId" attribute based on the list of dependencies.
 *
 * @author Stephane Nicoll
 */
@Component
public class DependenciesIdUpdater {

	private static final Logger logger = LoggerFactory.getLogger(DependenciesIdUpdater.class);

	private final BulkUpdateIndex bulkUpdateIndex;

	public DependenciesIdUpdater(JestClient jestClient) {
		this.bulkUpdateIndex = new BulkUpdateIndex(jestClient);
	}

	public void indexDependencies(String index) throws IOException {
		logger.info("Reindexing dependencies for " + index);
		Search.Builder searchBuilder = new Search.Builder("")
				.addIndex(index)
				.addType("request");
		bulkUpdateIndex.update(searchBuilder, 2000, this::updateDependenciesId);
	}

	private Update updateDependenciesId(JsonObject hit) {
		String id = hit.get("_id").getAsString();
		String index = hit.get("_index").getAsString();
		String type = hit.get("_type").getAsString();
		JsonObject source = hit.getAsJsonObject("_source");
		if (source.has("dependenciesId")) {
			return null;
		}
		else {
			String dependenciesId = computeDependenciesId(getRawDependencies(source));
			String updatedJson = "{ \"doc\" : { \"dependenciesId\" : \"" + dependenciesId + "\" } }";
			return new Update.Builder(updatedJson).index(index).id(id).type(type).build();
		}
	}

	private String computeDependenciesId(List<String> dependencies) {
		if (ObjectUtils.isEmpty(dependencies)) {
			return "_none";
		}
		Collections.sort(dependencies);
		return StringUtils.collectionToDelimitedString(dependencies, " ");
	}

	private List<String> getRawDependencies(JsonObject source) {
		JsonArray array = source.getAsJsonArray("dependencies");
		if (array == null) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (JsonElement jsonElement : array) {
			result.add(jsonElement.getAsString());
		}
		return result;
	}

}
