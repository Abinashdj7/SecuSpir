package com.securebank.securebank.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Wires primary and replica datasources into a single routing datasource.
 *
 * Final bean hierarchy seen by JPA:
 *   LazyConnectionDataSourceProxy (@Primary)
 *     └─ ReplicationRoutingDataSource
 *           ├─ primaryDataSource  (HikariCP → MySQL primary / H2 in tests)
 *           └─ replicaDataSource  (HikariCP → MySQL replica / H2 in tests)
 *
 * LazyConnectionDataSourceProxy defers the real getConnection() call until
 * the first SQL is executed, by which point Spring has already set the
 * transaction's read-only flag on TransactionSynchronizationManager.
 * This lets ReplicationRoutingDataSource read that flag correctly.
 */
@Configuration
@EnableScheduling
public class DataSourceConfig {

    // ── Primary ───────────────────────────────────────────────────────────────

    @Bean("primaryDataSource")
    public DataSource primaryDataSource(
            @Value("${spring.datasource.primary.url}") String url,
            @Value("${spring.datasource.primary.username}") String username,
            @Value("${spring.datasource.primary.password:}") String password,
            @Value("${spring.datasource.primary.driver-class-name:}") String driverClassName) {
        return buildHikariDataSource(url, username, password, driverClassName);
    }

    // ── Replica ───────────────────────────────────────────────────────────────

    @Bean("replicaDataSource")
    public DataSource replicaDataSource(
            @Value("${spring.datasource.replica.url}") String url,
            @Value("${spring.datasource.replica.username}") String username,
            @Value("${spring.datasource.replica.password:}") String password,
            @Value("${spring.datasource.replica.driver-class-name:}") String driverClassName) {
        return buildHikariDataSource(url, username, password, driverClassName);
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    @Bean
    public ReplicationRoutingDataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("replicaDataSource") DataSource replica) {
        ReplicationRoutingDataSource routing = new ReplicationRoutingDataSource();
        routing.setTargetDataSources(Map.of(
                DataSourceType.PRIMARY, primary,
                DataSourceType.REPLICA, replica
        ));
        routing.setDefaultTargetDataSource(primary);
        return routing;
    }

    @Bean
    @Primary
    public DataSource dataSource(ReplicationRoutingDataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static DataSource buildHikariDataSource(
            String url, String username, String password, String driverClassName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driverClassName != null && !driverClassName.isBlank()) {
            config.setDriverClassName(driverClassName);
        }
        return new HikariDataSource(config);
    }
}
