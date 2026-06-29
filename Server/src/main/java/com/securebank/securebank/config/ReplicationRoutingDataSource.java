package com.securebank.securebank.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes connections to primary for writes and replica for reads.
 * When primary is unavailable, all traffic fails over to replica.
 *
 * LazyConnectionDataSourceProxy MUST wrap this class so that
 * determineCurrentLookupKey() is called after the transaction's
 * read-only flag has been set on TransactionSynchronizationManager.
 */
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    private volatile boolean primaryAvailable = true;
    private volatile DataSourceType lastUsedKey;

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType key;
        if (!primaryAvailable) {
            key = DataSourceType.REPLICA;
        } else {
            key = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                    ? DataSourceType.REPLICA
                    : DataSourceType.PRIMARY;
        }
        lastUsedKey = key;
        return key;
    }

    public void setPrimaryAvailable(boolean available) {
        this.primaryAvailable = available;
    }

    public boolean isPrimaryAvailable() {
        return primaryAvailable;
    }

    /** Returns the datasource key chosen for the most recent getConnection() call. */
    public DataSourceType getLastUsedKey() {
        return lastUsedKey;
    }
}
