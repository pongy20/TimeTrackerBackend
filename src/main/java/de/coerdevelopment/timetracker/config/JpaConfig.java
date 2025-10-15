package de.coerdevelopment.timetracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "de.coerdevelopment.timetracker")
@EnableJpaAuditing
public class JpaConfig {
}

