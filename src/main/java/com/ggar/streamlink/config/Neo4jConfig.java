package com.ggar.streamlink.config;

import org.neo4j.driver.Driver;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableReactiveNeo4jRepositories(basePackages = "com.ggar.streamlink.repository")
@EnableTransactionManagement
public class Neo4jConfig {

    @Bean
    public ReactiveNeo4jTransactionManager reactiveTransactionManager(Driver driver,
        ReactiveDatabaseSelectionProvider databaseNameProvider) {
        return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider);
    }

    // @Bean
    // ApplicationRunner applicationRunner(ReactiveNeo4jClient neo4jClient) {
    //     return args -> {
    //         neo4jClient.query("CREATE CONSTRAINT streamer_name IF NOT EXISTS FOR (s:Streamer) REQUIRE s.name IS UNIQUE").run().subscribe();
    //         neo4jClient.query("CREATE CONSTRAINT platform_id IF NOT EXISTS FOR (p:Platform) REQUIRE p.id IS UNIQUE").run().subscribe();
    //     };
    // }

}
