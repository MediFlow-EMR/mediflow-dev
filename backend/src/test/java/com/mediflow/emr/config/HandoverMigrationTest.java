package com.mediflow.emr.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class HandoverMigrationTest {
    @Test void legacyContentSurvivesBaselineMigrationAndSecondStartup() throws Exception {
        String url = System.getenv().getOrDefault("MEDIFLOW_MIGRATION_TEST_URL", "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=MariaDB;DB_CLOSE_DELAY=-1");
        String username = System.getenv().getOrDefault("MEDIFLOW_MIGRATION_TEST_USER", "sa");
        String password = System.getenv().getOrDefault("MEDIFLOW_MIGRATION_TEST_PASSWORD", "");
        try (var connection = DriverManager.getConnection(url, username, password); var s = connection.createStatement()) {
            s.execute("CREATE TABLE handover (handover_id BIGINT PRIMARY KEY, ai_summary TEXT)");
            s.execute("INSERT INTO handover VALUES (1, 'legacy clinical content')");
            Flyway flyway = Flyway.configure().dataSource(url, username, password).baselineOnMigrate(true)
                    .baselineVersion("20261010").locations("classpath:db/migration").cleanDisabled(true).load();
            assertEquals(1, flyway.migrate().migrationsExecuted);
            assertEquals(0, flyway.migrate().migrationsExecuted);
            try (var rows = s.executeQuery("SELECT ai_summary, final_summary, source_type, status, version FROM handover WHERE handover_id=1")) {
                assertTrue(rows.next()); assertEquals("legacy clinical content", rows.getString(1));
                assertEquals(rows.getString(1), rows.getString(2)); assertEquals("AI", rows.getString(3));
                assertEquals("APPROVED", rows.getString(4)); assertEquals(0, rows.getLong(5));
            }
            s.execute("INSERT INTO handover_revision (handover_id, edited_by_id, edited_by_name, recorded_at, status, source_type, final_summary, record_version) VALUES (1,1,'test',CURRENT_TIMESTAMP,'APPROVED','AI','revision content',0)");
            assertEquals(0, flyway.migrate().migrationsExecuted);
            try (var rows = s.executeQuery("SELECT COUNT(*) FROM handover_revision")) { rows.next(); assertEquals(1,rows.getInt(1)); }
        }
    }
}
