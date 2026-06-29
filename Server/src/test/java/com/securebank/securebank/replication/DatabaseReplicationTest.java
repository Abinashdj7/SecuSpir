package com.securebank.securebank.replication;

import com.securebank.securebank.config.DataSourceType;
import com.securebank.securebank.config.DataSourceHealthMonitor;
import com.securebank.securebank.config.ReplicationRoutingDataSource;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.TransactionRepository;
import com.securebank.securebank.repo.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the ReplicationRoutingDataSource correctly splits reads/writes
 * and fails over transparently when the primary database goes down.
 *
 * Both the primary and replica datasources point to the same H2 instance in
 * this profile, so data written through the primary pool is immediately
 * visible through the replica pool — the same way MySQL replication with
 * negligible lag would behave.
 *
 * "Bringing down" the primary is simulated by closing its HikariCP pool;
 * the replica pool (separate object, same underlying H2) keeps working.
 */
@SpringBootTest
@ActiveProfiles("replication")
class DatabaseReplicationTest {

    @Autowired private ReplicationRoutingDataSource routingDataSource;
    @Autowired private DataSourceHealthMonitor healthMonitor;
    @Autowired @Qualifier("primaryDataSource") private DataSource primaryDataSource;

    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUpAndResetRouting() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        routingDataSource.setPrimaryAvailable(true);
    }

    // ── 1. Normal read/write split ────────────────────────────────────────────

    @Test
    @DisplayName("Write transactions route to primary")
    void writeTransactionsRouteToPrimary() {
        userRepository.save(user("write@test.com"));

        assertThat(routingDataSource.getLastUsedKey())
                .as("A plain @Transactional save must use the PRIMARY datasource")
                .isEqualTo(DataSourceType.PRIMARY);
    }

    @Test
    @DisplayName("Read-only transactions route to replica")
    void readOnlyTransactionsRouteToReplica() {
        userRepository.save(user("read@test.com"));

        // Execute a read in an explicit readOnly transaction
        List<User> found = readOnly(() -> userRepository.findAll());

        assertThat(found).hasSize(1);
        assertThat(routingDataSource.getLastUsedKey())
                .as("A @Transactional(readOnly=true) query must use the REPLICA datasource")
                .isEqualTo(DataSourceType.REPLICA);
    }

    // ── 2. Failover: primary goes down ────────────────────────────────────────

    @Test
    @DisplayName("Reads succeed from replica when primary is marked unavailable")
    void whenPrimaryMarkedDown_readsSucceedFromReplica() {
        userRepository.save(user("failover-read@test.com"));

        routingDataSource.setPrimaryAvailable(false);   // simulate health-check detecting failure

        List<User> found = readOnly(() -> userRepository.findAll());

        assertThat(found).hasSize(1)
                .as("Reads must still return data after primary is marked down");
        assertThat(routingDataSource.getLastUsedKey())
                .as("All reads should route to REPLICA during failover")
                .isEqualTo(DataSourceType.REPLICA);
    }

    @Test
    @DisplayName("Writes failover to replica when primary is marked unavailable")
    void whenPrimaryMarkedDown_writesFailoverToReplica() {
        routingDataSource.setPrimaryAvailable(false);   // simulate failure

        userRepository.save(user("failover-write@test.com"));   // must not throw

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(routingDataSource.getLastUsedKey())
                .as("Writes must also route to REPLICA so the app stays available")
                .isEqualTo(DataSourceType.REPLICA);
    }

    // ── 3. Failover: health monitor detects actual connection failure ──────────

    @Test
    @DisplayName("Health monitor detects closed primary pool and triggers failover")
    @DirtiesContext   // closing HikariCP is permanent; force a fresh context afterwards
    void whenPrimaryPoolClosed_healthMonitorTriggersFailover() {
        userRepository.save(user("pool-down@test.com"));

        // Bring the primary database "down" by closing its connection pool
        HikariDataSource hikari = (HikariDataSource) primaryDataSource;
        hikari.close();

        // The health monitor runs on a schedule, but we call it directly here
        // to trigger the detection without waiting 5 seconds.
        healthMonitor.checkPrimaryHealth();

        assertThat(routingDataSource.isPrimaryAvailable())
                .as("Health monitor must mark primary unavailable after detecting closed pool")
                .isFalse();

        // Reads must still work through the replica pool (separate HikariCP, same H2)
        List<User> found = readOnly(() -> userRepository.findAll());
        assertThat(found).hasSize(1)
                .as("Reads must succeed from replica after primary pool is closed");
    }

    // ── 4. Recovery ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Health monitor restores normal routing when primary recovers")
    void whenPrimaryRecovers_healthMonitorRestoresNormalRouting() {
        // Simulate a prior failure state
        routingDataSource.setPrimaryAvailable(false);

        // Primary H2 is still alive (we haven't closed its pool), so the health
        // check will succeed and flip the flag back.
        healthMonitor.checkPrimaryHealth();

        assertThat(routingDataSource.isPrimaryAvailable())
                .as("Health monitor must restore primary-available flag on successful ping")
                .isTrue();

        // Writes must now go back to primary
        userRepository.save(user("recovered@test.com"));
        assertThat(routingDataSource.getLastUsedKey())
                .as("After recovery, writes must return to PRIMARY")
                .isEqualTo(DataSourceType.PRIMARY);
    }

    // ── 5. Dual-node visibility ───────────────────────────────────────────────

    @Test
    @DisplayName("Data written to primary is visible through replica connection")
    void dataWrittenToPrimaryIsVisibleThroughReplica() {
        // Write through primary (default routing for non-read-only transactions)
        userRepository.save(user("visibility@test.com"));
        assertThat(routingDataSource.getLastUsedKey()).isEqualTo(DataSourceType.PRIMARY);

        // Read through replica — same H2 instance, so data must be present
        // (mirrors what you'd see with a fully caught-up MySQL replica)
        long count = readOnly(() -> userRepository.count());
        assertThat(count).isEqualTo(1)
                .as("Replica must see data committed to primary (zero replication lag in H2)");
        assertThat(routingDataSource.getLastUsedKey()).isEqualTo(DataSourceType.REPLICA);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T readOnly(Supplier<T> action) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setReadOnly(true);
        return tx.execute(status -> action.get());
    }

    private User user(String email) {
        return User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password(passwordEncoder.encode("Test1234!"))
                .role(User.Role.ROLE_USER)
                .build();
    }
}
