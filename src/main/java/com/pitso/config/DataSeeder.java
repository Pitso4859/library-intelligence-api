package com.pitso.config;

import com.pitso.model.*;
import com.pitso.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seeds sample data on application startup (dev profile only).
 * In production, data comes from Flyway migrations or the API.
 */
@Configuration
@Profile("!test & !prod")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedBooks(BookRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            log.info("Seeding sample book data...");

            repo.save(new EBook("The Pragmatic Programmer", "David Thomas", "067001617B", 2048));
            repo.save(new EBook("Clean Code", "Robert C. Martin", "032156840b", 3500));
            repo.save(new EBook("Design Patterns", "Gang of Four", "019873456B", 5120));

            repo.save(new PrintBook("The Mythical Man-Month", "Fred Brooks", "1367823245", 322, 420.0f));
            repo.save(new PrintBook("Refactoring", "Martin Fowler", "119873456B", 448, 680.5f));
            repo.save(new PrintBook("Domain-Driven Design", "Eric Evans", "1032156845", 560, 850.0f));

            log.info("Seeded {} books.", repo.count());
        };
    }
}
