package com.securebank.securebank.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Periodically probes the primary datasource and updates the routing flag.
 *
 * When the primary becomes unreachable, all traffic (reads and writes)
 * is transparently redirected to the replica until the primary recovers.
 * The check interval defaults to 5 s and is tunable via
 * {@code db.health.check.interval-ms}.
 */
@Component
@Slf4j
public class DataSourceHealthMonitor {

    private final DataSource primaryDataSource;
    private final ReplicationRoutingDataSource routingDataSource;

    public DataSourceHealthMonitor(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            ReplicationRoutingDataSource routingDataSource) {
        this.primaryDataSource = primaryDataSource;
        this.routingDataSource = routingDataSource;
    }

    @Scheduled(fixedDelayString = "${db.health.check.interval-ms:5000}")
    public void checkPrimaryHealth() {
        try (Connection connection = primaryDataSource.getConnection()) {
            boolean valid = connection.isValid(1);
            if (valid && !routingDataSource.isPrimaryAvailable()) {
                log.info("Primary database recovered — resuming normal read/write routing");
                routingDataSource.setPrimaryAvailable(true);
            }
        } catch (Exception e) {
            if (routingDataSource.isPrimaryAvailable()) {
                log.warn("Primary database unreachable ({}), failing over to replica", e.getMessage());
                routingDataSource.setPrimaryAvailable(false);
            }
        }
    }
}
