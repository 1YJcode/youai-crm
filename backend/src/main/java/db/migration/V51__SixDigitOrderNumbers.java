package db.migration;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Assign stable random numbers once, preserving a mapping of historical identifiers. */
public class V51__SixDigitOrderNumbers extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        var used = new HashSet<String>();
        var legacy = new ArrayList<String>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT order_no FROM crm_sales_order ORDER BY id")) {
            while (rows.next()) {
                String value = rows.getString(1);
                if (value.matches("[1-9][0-9]{5}")) used.add(value);
                else legacy.add(value);
            }
        }
        if (used.size() + legacy.size() > 900000) throw new IllegalStateException("Six-digit order number space exhausted");
        var random = new SecureRandom();
        try (var mapping = connection.prepareStatement("INSERT INTO crm_order_number_mapping(old_order_no, new_order_no) VALUES (?, ?)");
             var order = connection.prepareStatement("UPDATE crm_sales_order SET order_no = ? WHERE order_no = ?");
             var invitation = connection.prepareStatement("UPDATE crm_invitation_record SET related_order_no = ? WHERE related_order_no = ?")) {
            for (String old : legacy) {
                String number;
                do { number = String.valueOf(100000 + random.nextInt(900000)); } while (!used.add(number));
                mapping.setString(1, old);
                mapping.setString(2, number);
                mapping.executeUpdate();
                order.setString(1, number);
                order.setString(2, old);
                order.executeUpdate();
                invitation.setString(1, number);
                invitation.setString(2, old);
                invitation.executeUpdate();
            }
        }
    }
}
