package com.example.bulkupdateindex;

import java.io.IOException;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BulkUpdateIndexApplication {

	public static void main(String[] args) {
		SpringApplication.run(BulkUpdateIndexApplication.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(DependenciesIdUpdater updater) throws IOException {
		return (args) -> {
			updater.indexDependencies("initializr-2015");
		};
	}
}
