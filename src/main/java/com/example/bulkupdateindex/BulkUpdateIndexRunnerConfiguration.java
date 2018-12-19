package com.example.bulkupdateindex;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
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
	public ApplicationRunner runMigration() {
		return (arguments) -> {

		};
	}


}
