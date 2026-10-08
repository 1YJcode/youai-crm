package com.youai.crm.customer;

import db.migration.V23__Backfill_customer_identity;
import java.sql.DriverManager;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerIdentityMigrationTest {
    @Test
    void migratesOnlyUnambiguousOwnersAndExactCollaborators() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration_identity;MODE=MySQL", "sa", "")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE crm_user(id BIGINT PRIMARY KEY, username VARCHAR(64), display_name VARCHAR(64))");
                sql.execute("CREATE TABLE crm_customer(id BIGINT PRIMARY KEY, owner VARCHAR(64), collaborator VARCHAR(255))");
                var resource = new org.springframework.core.io.ClassPathResource("db/migration/V22__customer_identity.sql");
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection, resource);
                sql.execute("INSERT INTO crm_user VALUES (1, 'one', '同名'), (2, 'two', '同名'), (3, 'three', '张三'), (4, 'four', '张三丰')");
                sql.execute("INSERT INTO crm_customer(id,owner,collaborator) VALUES (1, '张三', '张三、同名、张三丰'), (2, '同名', ''), (3, '不存在', ''), (4, '公海', '张三'), (5, 'three', '')");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);
            new V23__Backfill_customer_identity().migrate(context);
            try (var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT id, owner_id FROM crm_customer ORDER BY id")) {
                assertTrue(rows.next()); assertEquals(3L, rows.getLong("owner_id"));
                for (int i = 0; i < 3; i++) { assertTrue(rows.next()); assertNull(rows.getObject("owner_id")); }
                assertTrue(rows.next()); assertEquals(3L, rows.getLong("owner_id"));
            }
            try (var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT customer_id, user_id FROM crm_customer_collaborator ORDER BY user_id")) {
                assertTrue(rows.next()); assertEquals(1L, rows.getLong(1)); assertEquals(3L, rows.getLong(2));
                assertTrue(rows.next()); assertEquals(1L, rows.getLong(1)); assertEquals(4L, rows.getLong(2));
                assertFalse(rows.next());
            }
        }
    }
}
