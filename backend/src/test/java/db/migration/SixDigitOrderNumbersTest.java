package db.migration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.api.migration.Context;

class SixDigitOrderNumbersTest {
    @Test
    void preservesFinancialAndInvitationReferencesAndStableNumbers() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection("jdbc:h2:mem:order_number_migration;MODE=MySQL")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE crm_sales_order(id BIGINT PRIMARY KEY, order_no VARCHAR(32) UNIQUE)");
                sql.execute("CREATE TABLE crm_order_number_mapping(old_order_no VARCHAR(32) PRIMARY KEY, new_order_no VARCHAR(6) UNIQUE)");
                sql.execute("CREATE TABLE crm_order_refund(order_no VARCHAR(32), amount INT, FOREIGN KEY(order_no) REFERENCES crm_sales_order(order_no) ON UPDATE CASCADE)");
                sql.execute("CREATE TABLE crm_ledger_account(order_no VARCHAR(32), amount INT, FOREIGN KEY(order_no) REFERENCES crm_sales_order(order_no) ON UPDATE CASCADE)");
                sql.execute("CREATE TABLE crm_invitation_record(related_order_no VARCHAR(32))");
                sql.execute("INSERT INTO crm_sales_order VALUES (1,'SO-OLD-1'), (2,'SO-OLD-2'), (3,'123456')");
                sql.execute("INSERT INTO crm_order_refund VALUES ('SO-OLD-1',100)");
                sql.execute("INSERT INTO crm_ledger_account VALUES ('SO-OLD-1',500)");
                sql.execute("INSERT INTO crm_invitation_record VALUES ('SO-OLD-1')");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);
            var migration = new V51__SixDigitOrderNumbers();
            migration.migrate(context);
            try (var sql = connection.createStatement()) {
                try (var rows = sql.executeQuery("SELECT order_no FROM crm_sales_order")) {
                    var numbers = new java.util.HashSet<String>();
                    while (rows.next()) {
                        String number = rows.getString(1);
                        assertTrue(number.matches("[1-9][0-9]{5}"));
                        assertTrue(numbers.add(number));
                    }
                    assertEquals(3, numbers.size());
                    assertTrue(numbers.contains("123456"));
                }
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM crm_order_refund r JOIN crm_ledger_account l ON r.order_no=l.order_no JOIN crm_invitation_record i ON i.related_order_no=r.order_no JOIN crm_sales_order o ON o.order_no=r.order_no WHERE o.id=1 AND r.amount=100 AND l.amount=500")) {
                    rows.next(); assertEquals(1, rows.getInt(1));
                }
                migration.migrate(context);
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM crm_order_number_mapping")) {
                    rows.next(); assertEquals(2, rows.getInt(1));
                }
            }
        }
    }
}
