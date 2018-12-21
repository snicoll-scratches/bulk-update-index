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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;
import io.searchbox.action.BulkableAction;
import io.searchbox.core.Update;

/**
 * A container for {@link BulkableAction update actions} based on a root document.
 *
 * @author Stephane Nicoll
 */
public class IndexActionContainer {

	private final List<BulkableAction<?>> actions = new ArrayList<>();

	private final JsonObject document;

	private final String id;

	private final String index;

	private final String type;

	public IndexActionContainer(JsonObject document) {
		this.document = document;
		this.id = document.get("_id").getAsString();
		this.index = document.get("_index").getAsString();
		this.type = document.get("_type").getAsString();
	}

	public List<BulkableAction<?>> getActions() {
		return Collections.unmodifiableList(this.actions);
	}

	public void addUpdateAction(JsonObject updatedSource) {
		JsonObject object = new JsonObject();
		object.add("doc", updatedSource);
		this.actions.add(new Update.Builder(object).index(this.index).id(this.id)
				.type(this.type).build());
	}

	public JsonObject getDocument() {
		return this.document;
	}

	public JsonObject getSource() {
		return this.document.getAsJsonObject("_source");
	}

}
