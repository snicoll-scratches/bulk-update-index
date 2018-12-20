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

import java.io.IOException;
import java.util.Map;

import com.example.bulkupdateindex.AbstractIndexer;
import com.example.bulkupdateindex.BulkUpdateIndex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * Index the {@code download} document to add more information about versions.
 *
 * @author Stephane Nicoll
 */
@Component
public class ModuleIndexer extends AbstractIndexer {

	private static final Logger logger = LoggerFactory.getLogger(ModuleIndexer.class);

	public void indexModules(BulkUpdateIndex bulkUpdateIndex) throws IOException {
		logger.info("Reindexing versions");
		Search.Builder searchBuilder = new Search.Builder("").addIndex("projects")
				.addType("download");
		bulkUpdateIndex.update(searchBuilder, 2000, this::index);
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
