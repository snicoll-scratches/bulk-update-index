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
import com.example.bulkupdateindex.IndexActionContainer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * Index the {@code download} document to add more information about versions. Assumes
 * that the major and minor indexes are empty or do not contain any information about the
 * {@code download} document to index.
 *
 * @author Stephane Nicoll
 */
@Component
public class ModuleIndexer extends AbstractIndexer {

	static final String PROJECTS_MAJOR_INDEX = "projects-major";

	static final String PROJECTS_MINOR_INDEX = "projects-minor";

	private static final Logger logger = LoggerFactory.getLogger(ModuleIndexer.class);

	public void indexModules(BulkUpdateIndex bulkUpdateIndex) throws IOException {
		logger.info("Reindexing versions");
		Search.Builder searchBuilder = new Search.Builder("").addIndex("projects")
				.addType("download");
		bulkUpdateIndex.update(searchBuilder, 2000, this::index);
	}

	protected void migrate(IndexActionContainer container) {
		JsonObject source = container.getSource();
		DownloadCountAggregation aggregation = computeAggregation(source);
		if (!source.has("totalCount")) {
			source.addProperty("totalCount", aggregation.getTotalCount());
			container.addUpdateAction(source);
		}
		indexVersions(container, PROJECTS_MAJOR_INDEX, aggregation.getMajorGenerations());
		indexVersions(container, PROJECTS_MINOR_INDEX, aggregation.getMinorGenerations());
	}

	private DownloadCountAggregation computeAggregation(JsonObject source) {
		JsonArray stats = source.getAsJsonArray("stats");
		DownloadCountAggregation aggregation = new DownloadCountAggregation();
		for (JsonElement stat : stats) {
			aggregation.handle(source, stat.getAsJsonObject());
		}
		return aggregation;
	}

	private void indexVersions(IndexActionContainer container, String indexName,
			Map<String, Long> versions) {
		versions.forEach((version, count) -> {
			JsonObject versionSource = createVersionSource(container.getSource(), version,
					count);
			container.addAction(new Index.Builder(versionSource).index(indexName)
					.type("download").build());
		});
	}

	private JsonObject createVersionSource(JsonObject source, String version,
			long count) {
		JsonObject object = new JsonObject();
		object.addProperty("from", source.get("from").getAsLong());
		object.addProperty("to", source.get("to").getAsLong());
		object.addProperty("projectId", source.get("projectId").getAsString());
		object.addProperty("groupId", source.get("groupId").getAsString());
		object.addProperty("artifactId", source.get("artifactId").getAsString());
		object.addProperty("version", version);
		object.addProperty("count", count);
		return object;
	}

}
