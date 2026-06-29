#!/usr/bin/env bash
# scripts/replication-failover-test.sh
#
# Live MySQL replication failover test for SecureBank.
# Writes rows to primary, stops the primary container, then verifies
# the replica still serves reads with zero data loss.
#
# Prerequisites:
#   docker compose up -d mysql-primary mysql-replica mysql-replication-setup

set -uo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
PRIMARY="securebank-mysql-primary"
REPLICA="securebank-mysql-replica"
PRIMARY_PORT=3310
REPLICA_PORT=3311
DB_USER="root"
DB_PASS="rootpassword"
DB_NAME="securebank"
ROWS=100
TABLE="_repl_failover_test"   # underscore prefix keeps it away from app tables

# ── ANSI colours ──────────────────────────────────────────────────────────────
G='\033[0;32m'; R='\033[0;31m'; Y='\033[1;33m'
C='\033[0;36m'; B='\033[1m';    N='\033[0m'

# ── Helpers ───────────────────────────────────────────────────────────────────
divider() { echo -e "${C}========================================================${N}"; }

# Millisecond timestamp: GNU date → Node.js → seconds fallback
now_ms() {
  local t
  t=$(date +%s%3N 2>/dev/null) && [[ "$t" =~ ^[0-9]{10,}$ ]] && { printf '%s' "$t"; return 0; }
  node -e "process.stdout.write(String(Date.now()))" 2>/dev/null && return 0
  printf '%s' "$(($(date +%s) * 1000))"
}

# Run SQL on primary (suppress password warning)
p_sql() {
  docker exec "$PRIMARY" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" \
    --batch --skip-column-names -e "$1" 2>/dev/null
}

# Run SQL on replica (suppress password warning)
r_sql() {
  docker exec "$REPLICA" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" \
    --batch --skip-column-names -e "$1" 2>/dev/null
}

# ── Guard: containers must be running ────────────────────────────────────────
for c in "$PRIMARY" "$REPLICA"; do
  running=$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null || echo "false")
  if [[ "$running" != "true" ]]; then
    echo -e "${R}ERROR: container '$c' is not running.${N}"
    echo "  Run: docker compose up -d mysql-primary mysql-replica mysql-replication-setup"
    exit 1
  fi
done

# Drop any leftover table from a previous interrupted run
docker exec "$PRIMARY" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" \
  --batch -e "DROP TABLE IF EXISTS \`$TABLE\`;" 2>/dev/null || true

# ── Test start ────────────────────────────────────────────────────────────────
TOTAL_START=$(now_ms)
PASS=true

echo ""
divider
echo -e "${B}  MySQL Replication Failover Test${N}"
divider
echo ""

# ── [1/5] Replication status ──────────────────────────────────────────────────
echo "[1/5] Replication status"

# Capture SHOW REPLICA STATUS in vertical format (no --batch so \G works)
REP=$(docker exec "$REPLICA" mysql -u"$DB_USER" -p"$DB_PASS" \
  -e "SHOW REPLICA STATUS\G" 2>/dev/null || echo "")

IO=$(echo  "$REP" | grep  "Replica_IO_Running:"    | awk '{print $2}' | tr -d '[:space:]' || echo "")
SQL=$(echo "$REP" | grep  "Replica_SQL_Running:"   | awk '{print $2}' | tr -d '[:space:]' || echo "")
LAG=$(echo "$REP" | grep  "Seconds_Behind_Source:" | awk '{print $2}' | tr -d '[:space:]' || echo "")
[[ -z "$IO"  ]] && IO="Unknown"
[[ -z "$SQL" ]] && SQL="Unknown"
[[ -z "$LAG" ]] && LAG="?"

echo -e "  ${C}PRIMARY${N}   $PRIMARY  (localhost:$PRIMARY_PORT)"
echo -e "  ${Y}REPLICA${N}   $REPLICA  (localhost:$REPLICA_PORT)"
if [[ "$IO" == "Yes" && "$SQL" == "Yes" ]]; then
  echo    "  Replica lag: ${LAG}s"
else
  echo -e "  ${R}WARNING: replication not active (IO=$IO, SQL=$SQL)${N}"
  echo -e "  ${Y}Run: docker compose up -d mysql-replication-setup${N}"
fi
echo ""

# ── [2/5] Write rows to primary ───────────────────────────────────────────────
echo "[2/5] Writing $ROWS rows to primary"

p_sql "CREATE TABLE \`$TABLE\` (
  id  INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
  val VARCHAR(100) NOT NULL,
  ts  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;"

# Build a single multi-row INSERT (much faster than 100 round-trips)
INSERT_SQL="INSERT INTO \`$TABLE\` (val) VALUES "
for i in $(seq 1 $ROWS); do
  [[ $i -gt 1 ]] && INSERT_SQL+=","
  INSERT_SQL+="('row-$i')"
done

WRITE_START=$(now_ms)
p_sql "${INSERT_SQL};"
WRITE_END=$(now_ms)
WRITE_MS=$(( WRITE_END - WRITE_START ))

WRITTEN=$(p_sql "SELECT COUNT(*) FROM \`$TABLE\`;" || echo "0")
echo "  Inserted  : $WRITTEN rows"
echo "  Write time: ${WRITE_MS} ms"
echo ""

# ── [3/5] Pre-failover read on replica ───────────────────────────────────────
echo "[3/5] Pre-failover read check (replica)"

sleep 1   # brief pause for replication to flush

PRE_ROWS=$(r_sql "SELECT COUNT(*) FROM \`$TABLE\`;" || echo "0")
echo "  Rows readable on replica before failover: $PRE_ROWS"

if [[ "$PRE_ROWS" -eq "$ROWS" ]]; then
  echo -e "  ${G}✓ All $ROWS rows replicated${N}"
else
  echo -e "  ${R}✗ Only $PRE_ROWS/$ROWS rows visible — replication may be lagging${N}"
  PASS=false
fi
echo ""

# ── [4/5] Stop primary ───────────────────────────────────────────────────────
echo "[4/5] Stopping primary: $PRIMARY"

STOP_START=$(now_ms)
docker stop "$PRIMARY" > /dev/null
STOP_END=$(now_ms)
STOP_MS=$(( STOP_END - STOP_START ))

echo "  $PRIMARY stopped  (${STOP_MS} ms)"
echo ""

# ── [5/5] Post-failover reads from replica ───────────────────────────────────
echo "[5/5] Post-failover read check (replica)"

READ_START=$(now_ms)
POST_ROWS=$(r_sql "SELECT COUNT(*) FROM \`$TABLE\`;" || echo "0")
READ_END=$(now_ms)
READ_MS=$(( READ_END - READ_START ))

echo "  Rows readable on replica after failover : $POST_ROWS"
echo "  Read time after failover                : ${READ_MS} ms"

if [[ "$POST_ROWS" -eq "$ROWS" ]]; then
  echo -e "  ${G}✓ Replica served reads without interruption${N}"
else
  echo -e "  ${R}✗ Data loss detected: only $POST_ROWS/$ROWS rows after failover${N}"
  PASS=false
fi
echo ""

# ── Restart primary ───────────────────────────────────────────────────────────
echo -e "  Restarting $PRIMARY (rejoins as replication primary)..."
docker start "$PRIMARY" > /dev/null

# Wait for primary to accept TCP connections again
for i in $(seq 1 30); do
  docker exec "$PRIMARY" mysqladmin ping -u"$DB_USER" -p"$DB_PASS" \
    -h127.0.0.1 --silent 2>/dev/null && break || true
  sleep 2
done
echo "  $PRIMARY restarted"
echo ""

# Allow replica IO thread to reconnect, then clean up test table
sleep 4
docker exec "$PRIMARY" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" \
  --batch -e "DROP TABLE IF EXISTS \`$TABLE\`;" 2>/dev/null || true

# ── Results ───────────────────────────────────────────────────────────────────
TOTAL_MS=$(( $(now_ms) - TOTAL_START ))
DATA_LOSS=$(( ROWS - POST_ROWS ))

divider
echo -e "${B}  RESULTS${N}"
divider

printf "  %-28s: %s\n" "Primary container"    "$PRIMARY (localhost:$PRIMARY_PORT)"
printf "  %-28s: %s\n" "Replica container"    "$REPLICA (localhost:$REPLICA_PORT)"
printf "  %-28s: %s\n" "Rows written"         "$WRITTEN"
printf "  %-28s: %s\n" "Write time"           "${WRITE_MS} ms"
printf "  %-28s: %s\n" "Rows before failover" "$PRE_ROWS"
printf "  %-28s: %s\n" "Rows after failover"  "$POST_ROWS"
printf "  %-28s: " "Data loss"

if [[ "$DATA_LOSS" -eq 0 ]]; then
  echo -e "${G}0 rows${N}"
else
  echo -e "${R}${DATA_LOSS} rows${N}"
fi

printf "  %-28s: %s\n" "Total test duration" "${TOTAL_MS} ms"
echo ""

if [[ "$PASS" == true ]]; then
  echo -e "  ${G}${B}RESULT: PASS — zero data loss, replica survived primary failure${N}"
else
  echo -e "  ${R}${B}RESULT: FAIL — see errors above${N}"
fi
echo ""
divider
echo ""
