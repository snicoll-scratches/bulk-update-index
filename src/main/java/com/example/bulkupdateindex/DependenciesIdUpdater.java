package com.example.bulkupdateindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.springframework.util.StringUtils;

/**
 * Update the "dependenciesId" attribute based on the list of dependencies.
 *
 * @author Stephane Nicoll
 */
@Component
public class DependenciesIdUpdater {

	private static final Logger logger = LoggerFactory.getLogger(DependenciesIdUpdater.class);

	private final JestClient jestClient;

	public DependenciesIdUpdater(JestClient jestClient) {
		this.jestClient = jestClient;
	}

	public void indexDependencies(String index) throws IOException {
		logger.info("Reindexing dependencies for " + index);
		Search search = new Search.Builder("")
				.addIndex(index)
				.addType("request")
				.addSort(new Sort("_doc"))
				.setParameter(Parameters.SIZE, 2000)
				.setParameter(Parameters.SCROLL, "5m")
				.build();
		JestResult result = jestClient.execute(search);
		if (!result.isSucceeded()) {
			throw new IllegalStateException("Query failed " + result.getErrorMessage());
		}
		boolean moreResults = processPage(result);
		String scrollId = result.getJsonObject().get("_scroll_id").getAsString();
		int page = 1;
		while (moreResults) {
			logger.info("Indexing page " + page + "[" + ((page - 1) * 2000) + " to " + (page * 2000) + "]");
			SearchScroll scroll = new SearchScroll.Builder(scrollId, "5m").build();
			result = jestClient.execute(scroll);
			moreResults = processPage(result);
			page++;
		}
	}

	private boolean processPage(JestResult result) throws IOException {
		JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
		if (hits.size() == 0) {
			logger.info("No more elements");
			return false;
		}
		List<Update> updates = new ArrayList<>();
		for (JsonElement hit : hits) {
			JsonObject request = hit.getAsJsonObject();
			Update action = updateDependenciesId(request);
			if (action != null) {
				updates.add(action);
			}
		}
		if (!updates.isEmpty()) {
			Bulk.Builder bulkUpdate = new Bulk.Builder();
			updates.forEach(bulkUpdate::addAction);
			logger.info(String.format("Updating %s elements", updates.size()));
			BulkResult updateResult = jestClient.execute(bulkUpdate.build());
			if (!ObjectUtils.isEmpty(updateResult.getFailedItems())) {
				logger.error("Failed to update elements " + updateResult.getFailedItems());
			}
		}
		return true;
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
