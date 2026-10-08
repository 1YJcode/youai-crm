package com.youai.crm.customer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** scripts/test-mysql.ps1 supplies a fresh, isolated real-server database. */
@EnabledIfEnvironmentVariable(named = "TEST_MIGRATION_DB_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/youai_verify_migration\\?.*")
class MySqlUpgradeTest {
    @Test
    void upgradesLegacyDataWithoutGuessingOwnersOrChangingCustomerDetails() throws Exception {
        String url = System.getenv("TEST_MIGRATION_DB_URL");
        String username = System.getenv("TEST_DB_USERNAME");
        String password = System.getenv("TEST_DB_PASSWORD");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            assertEquals("youai_verify_migration", connection.getCatalog());
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"), "Verification requires an empty database");
            assertEquals(24, Flyway.configure().dataSource(url, username, password).target("24").load().migrate().migrationsExecuted);
            try (var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO crm_department(id,code,name,created_at) VALUES (1,'SALES','测试部门',NOW(6))");
                sql.executeUpdate("INSERT INTO crm_user(id,username,display_name,password_hash,department_id,enabled,created_at,updated_at) VALUES "
                        + "(1,'first','同名','test-only',1,1,NOW(6),NOW(6)),"
                        + "(2,'second','同名','test-only',1,1,NOW(6),NOW(6)),"
                        + "(3,'third','张三','test-only',1,1,NOW(6),NOW(6)),"
                        + "(4,'fourth','张三丰','test-only',1,0,NOW(6),NOW(6)),"
                        + "(5,'fifth','协作员工','test-only',1,1,NOW(6),NOW(6))");
            }
            String[] owners = {"张三", "同名", "不存在", "公海", "third", "白板", "张三丰"};
            try (var insert = connection.prepareStatement("INSERT INTO crm_customer(id,customer_no,name,phone,company,source,owner,stage,level,note,id_card,collaborator,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(6),NOW(6))")) {
                for (int i = 0; i < owners.length; i++) {
                    insert.setLong(1, i + 1);
                    insert.setString(2, "143003900" + i);
                    insert.setString(3, "迁移验证客户" + i);
                    insert.setString(4, "1380000123" + i);
                    insert.setString(5, "测试公司"); insert.setString(6, "测试来源");
                    insert.setString(7, owners[i]); insert.setString(8, "初步沟通"); insert.setString(9, "普通客户");
                    insert.setString(10, "中文和 emoji 数据保持不变 😀");
                    insert.setString(11, "120000199405060000");
                    insert.setString(12, "张三、同名、张三丰、协作员工、third");
                    insert.executeUpdate();
                }
            }
            List<List<String>> before = snapshot(connection);
            Flyway current = Flyway.configure().dataSource(url, username, password).load();
            assertEquals(2, current.migrate().migrationsExecuted);
            current.validate();
            assertEquals(before, snapshot(connection));
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT owner_id FROM crm_customer ORDER BY id")) {
                Long[] expected = {3L, null, null, null, 3L, null, 4L};
                for (Long id : expected) { assertTrue(rows.next()); assertEquals(id, rows.getObject(1, Long.class)); }
                assertFalse(rows.next());
            }
            assertEquals(3, scalar(connection, "SELECT COUNT(*) FROM crm_customer_collaborator WHERE customer_id = 1"));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM crm_customer_collaborator WHERE user_id IN (1,2) OR customer_id IN (4,6)"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE crm_user SET display_name = '新姓名' WHERE id = 3");
                assertEquals(3, scalar(connection, "SELECT owner_id FROM crm_customer WHERE id = 1"));
                assertTrue(assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE crm_customer SET owner_id = 99999 WHERE id = 1")).getSQLState().startsWith("23"));
                assertTrue(assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM crm_user WHERE id = 3")).getSQLState().startsWith("23"));
                statement.executeUpdate("DELETE FROM crm_user WHERE id = 5");
                assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM crm_customer_collaborator WHERE user_id = 5"));
            }
            assertEquals(0, current.migrate().migrationsExecuted, "Restart must not repeat identity resolution");
            assertEquals(before, snapshot(connection));
            assertEquals(26, scalar(connection, "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1"));
        }
    }

    private long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next()); return rows.getLong(1);
        }
    }

    private List<List<String>> snapshot(Connection connection) throws SQLException {
        var result = new ArrayList<List<String>>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT id,customer_no,name,phone,company,source,owner,note,id_card,collaborator,created_at,updated_at FROM crm_customer ORDER BY id")) {
            while (rows.next()) {
                var row = new ArrayList<String>();
                for (int column = 1; column <= 12; column++) row.add(rows.getString(column));
                result.add(row);
            }
        }
        return result;
    }
}
