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

import com.google.gson.JsonObject;
import io.searchbox.core.Update;

/**
 * Based indexer.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractIndexer {

	/**
	 * Analyze the input document and return an {@link Update} or {@code null} if the
	 * document does not need to be updated.
	 * @param input the input document to reindex
	 * @return an {@link Update} or {@code null}
	 */
	public Update index(JsonObject input) {
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

	protected abstract boolean migrate(JsonObject source);

}
