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

package com.example.bulkupdateindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchScroll;
import io.searchbox.core.Update;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * Bulk update elements based on a search.
 *
 * @author Stephane Nicoll
 */
@Component
public class BulkUpdateIndex {

	private static final Logger logger = LoggerFactory.getLogger(BulkUpdateIndex.class);

	private final JestClient jestClient;

	public BulkUpdateIndex(JestClient jestClient) {
		this.jestClient = jestClient;
	}

	/**
	 * Update the elements defined by the specific search.
	 * @param searchBuilder a search query builder targeting the elements to update
	 * @param pageSize the size of a page
	 * @param updateFunction the {@link Update} function for a given hit
	 * @throws IOException if an index operation fails
	 */
	public void update(Search.Builder searchBuilder, int pageSize,
			Function<JsonObject, Update> updateFunction) throws IOException {
		Search search = searchBuilder.addSort(new Sort("_doc"))
				.setParameter(Parameters.SIZE, pageSize)
				.setParameter(Parameters.SCROLL, "5m").build();
		JestResult result = this.jestClient.execute(search);
		if (!result.isSucceeded()) {
			throw new IllegalStateException("Query failed " + result.getErrorMessage());
		}
		boolean moreResults = processPage(result, updateFunction);
		String scrollId = result.getJsonObject().get("_scroll_id").getAsString();
		int page = 1;
		while (moreResults) {
			logger.info("Indexing page " + page + "[" + ((page - 1) * pageSize) + " to "
					+ (page * pageSize) + "]");
			SearchScroll scroll = new SearchScroll.Builder(scrollId, "5m").build();
			result = this.jestClient.execute(scroll);
			moreResults = processPage(result, updateFunction);
			page++;
		}
	}

	private boolean processPage(JestResult result,
			Function<JsonObject, Update> updateFunction) throws IOException {
		JsonArray hits = result.getJsonObject().getAsJsonObject("hits")
				.getAsJsonArray("hits");
		if (hits.size() == 0) {
			logger.info("No more elements");
			return false;
		}
		List<Update> updates = new ArrayList<>();
		for (JsonElement hit : hits) {
			Update action = updateFunction.apply(hit.getAsJsonObject());
			if (action != null) {
				updates.add(action);
			}
		}
		if (!updates.isEmpty()) {
			Bulk.Builder bulkUpdate = new Bulk.Builder();
			updates.forEach(bulkUpdate::addAction);
			logger.info(String.format("Updating %s elements", updates.size()));
			BulkResult updateResult = this.jestClient.execute(bulkUpdate.build());
			if (!ObjectUtils.isEmpty(updateResult.getFailedItems())) {
				logger.error(
						"Failed to update elements " + updateResult.getFailedItems());
			}
		}
		else {
			logger.info("No element to update");
		}
		return true;
	}

}
