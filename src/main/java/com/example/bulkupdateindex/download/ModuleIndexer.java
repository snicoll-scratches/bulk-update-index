package com.example.bulkupdateindex.download;

import java.io.IOException;
import java.util.Map;

import com.example.bulkupdateindex.BulkUpdateIndex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.Search;
import io.searchbox.core.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * @author Stephane Nicoll
 */
@Component
public class ModuleIndexer {

	private static final Logger logger = LoggerFactory.getLogger(ModuleIndexer.class);

	public void indexModules(BulkUpdateIndex bulkUpdateIndex) throws IOException {
		logger.info("Reindexing versions");
		Search.Builder searchBuilder = new Search.Builder("").addIndex("projects")
				.addType("download");
		bulkUpdateIndex.update(searchBuilder, 2000, this::index);
	}

	protected Update index(JsonObject input) {
		String id = input.get("_id").getAsString();
		String index = input.get("_index").getAsString();
		String type = input.get("_type").getAsString();
		JsonObject source = input.getAsJsonObject("_source");
		boolean modified = migrate(source);
		if (modified) {
			JsonObject object = new JsonObject();
			object.add("doc", source);
			return new Update.Builder(object).index(index).id(id).type(type).build();
		}
		return null;
	}

	protected boolean migrate(JsonObject source) {
		if (source.has("totalCount")
				&& (source.has("majorGenerations") && (source.has("minorGenerations")))) {
			return false;
		}
		DownloadCountAggregation aggregation = computeAggregation(source);
		source.addProperty("totalCount", aggregation.getTotalCount());
		if (!aggregation.getMajorGenerations().isEmpty()) {
			source.add("majorGenerations",
					toJsonArray(aggregation.getMajorGenerations()));
		}
		if (!aggregation.getMinorGenerations().isEmpty()) {
			source.add("minorGenerations",
					toJsonArray(aggregation.getMinorGenerations()));
		}
		return true;
	}

	private DownloadCountAggregation computeAggregation(JsonObject source) {
		JsonArray stats = source.getAsJsonArray("stats");
		DownloadCountAggregation aggregation = new DownloadCountAggregation();
		for (JsonElement stat : stats) {
			aggregation.handle(source, stat.getAsJsonObject());
		}
		return aggregation;
	}

	private JsonArray toJsonArray(Map<String, Long> content) {
		JsonArray elements = new JsonArray();
		content.keySet().stream().sorted().forEach((name) -> {
			JsonObject element = new JsonObject();
			element.addProperty("name", name);
			element.addProperty("count", content.get(name));
			elements.add(element);
		});
		return elements;
	}

}
