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

import com.example.bulkupdateindex.download.ModuleIndexer;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Run the migration on startup.
 *
 * @author Stephane Nicoll
 */
@Configuration
public class BulkUpdateIndexRunnerConfiguration {

	private final BulkUpdateIndex bulkUpdateIndex;

	public BulkUpdateIndexRunnerConfiguration(BulkUpdateIndex bulkUpdateIndex) {
		this.bulkUpdateIndex = bulkUpdateIndex;
	}

	@Bean
	public ApplicationRunner runMigration(ModuleIndexer indexer) {
		return (arguments) -> {
			indexer.indexModules(this.bulkUpdateIndex);
		};
	}

}
