-- Runs once when the primary container is first created.
-- The replication-setup service also creates this user after both nodes are
-- healthy, so this script is a belt-and-suspenders fallback.

CREATE USER IF NOT EXISTS 'replicator'@'%'
    IDENTIFIED BY 'replicatorpassword';

GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';

FLUSH PRIVILEGES;
