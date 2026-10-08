package db.migration;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** One-time resolution. Ambiguous or unknown legacy names never gain access later. */
public class V26__Backfill_customer_identity extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<String, Set<Long>> aliases = new HashMap<>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, username, display_name FROM crm_user")) {
            while (rows.next()) {
                for (String column : new String[]{"username", "display_name"}) {
                    String alias = rows.getString(column);
                    if (alias != null) aliases.computeIfAbsent(key(alias), ignored -> new HashSet<>()).add(rows.getLong("id"));
                }
            }
        }
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, owner, collaborator FROM crm_customer");
             var ownerUpdate = connection.prepareStatement("UPDATE crm_customer SET owner_id = ? WHERE id = ? AND owner_id IS NULL");
             var collaboratorInsert = connection.prepareStatement("INSERT INTO crm_customer_collaborator(customer_id, user_id) VALUES (?, ?)")) {
            while (rows.next()) {
                String owner = rows.getString("owner");
                if ("公海".equals(owner) || "白板".equals(owner)) continue;
                Long ownerId = uniqueId(aliases, owner);
                if (ownerId != null) {
                    ownerUpdate.setLong(1, ownerId);
                    ownerUpdate.setLong(2, rows.getLong("id"));
                    ownerUpdate.executeUpdate();
                }
                String collaborators = rows.getString("collaborator");
                Set<Long> ids = new HashSet<>();
                if (collaborators != null) for (String alias : collaborators.split("[、,，]")) {
                    Long id = uniqueId(aliases, alias);
                    if (id != null && ids.add(id)) {
                        collaboratorInsert.setLong(1, rows.getLong("id"));
                        collaboratorInsert.setLong(2, id);
                        collaboratorInsert.executeUpdate();
                    }
                }
            }
        }
    }

    private static String key(String value) { return value.trim().toLowerCase(Locale.ROOT); }

    private static Long uniqueId(Map<String, Set<Long>> aliases, String alias) {
        Set<Long> ids = alias == null ? null : aliases.get(key(alias));
        return ids != null && ids.size() == 1 ? ids.iterator().next() : null;
    }
}
