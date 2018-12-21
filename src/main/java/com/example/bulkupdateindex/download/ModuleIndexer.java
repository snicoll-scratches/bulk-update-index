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
 * Index {@code download} documents to a new index with a flat structure.
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

	protected void migrate(IndexActionContainer container) {
		StatHandler statHandler = new StatHandler();
		JsonObject source = container.getSource();
		JsonArray stats = source.getAsJsonArray("stats");

		for (JsonElement stat : stats) {
			JsonObject downloadDocument = statHandler.handle(source,
					stat.getAsJsonObject());
			container.addAction(new Index.Builder(downloadDocument).index("downloads")
					.type("download").build());
		}
	}

}
