package com.example.bulkupdateindex;

import com.example.bulkupdateindex.download.ModuleIndexer;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
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
			indexer.indexModules(bulkUpdateIndex);
		};
	}

}
