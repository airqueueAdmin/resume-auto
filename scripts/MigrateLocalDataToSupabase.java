import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.postgresql.util.PGobject;

/**
 * One-off data migration utility. It merges source rows without replacing the target database.
 * Authentication/session tables are intentionally not included.
 */
public final class MigrateLocalDataToSupabase {

    private static final List<String> TABLE_ORDER = List.of(
            "users",
            "user_consents",
            "experiences",
            "projects",
            "job_postings",
            "resumes",
            "resume_versions",
            "resume_application_questions",
            "ai_job_analyses",
            "ai_job_matches",
            "ai_resume_drafts",
            "jobkorea_export_audits"
    );

    private MigrateLocalDataToSupabase() {
    }

    public static void main(String[] args) throws Exception {
        String sourceUrl = required("MIGRATION_SOURCE_DB_URL");
        String sourceUser = required("MIGRATION_SOURCE_DB_USERNAME");
        String sourcePassword = required("MIGRATION_SOURCE_DB_PASSWORD");
        String targetUrl = required("MIGRATION_TARGET_DB_URL");
        String targetUser = required("MIGRATION_TARGET_DB_USERNAME");
        String targetPassword = required("MIGRATION_TARGET_DB_PASSWORD");

        try (Connection source = DriverManager.getConnection(sourceUrl, sourceUser, sourcePassword);
             Connection target = DriverManager.getConnection(targetUrl, targetUser, targetPassword)) {
            target.setAutoCommit(false);
            try {
                Map<UUID, UUID> userIds = migrateUsers(source, target);
                for (String table : TABLE_ORDER.subList(1, TABLE_ORDER.size())) {
                    migrateTable(source, target, table, userIds);
                }
                target.commit();
                System.out.println("migration=committed");
            } catch (Exception exception) {
                target.rollback();
                throw new IllegalStateException("Migration rolled back", exception);
            }
        }
    }

    private static Map<UUID, UUID> migrateUsers(Connection source, Connection target) throws SQLException {
        List<Column> columns = columns(source, "users");
        int idIndex = columnIndex(columns, "id");
        int emailHashIndex = columnIndex(columns, "email_hash");
        String insertSql = insertSql("users", columns);
        Map<UUID, UUID> userIds = new HashMap<>();
        int inserted = 0;
        int matched = 0;

        try (PreparedStatement sourceRows = source.prepareStatement("SELECT * FROM public.users");
             ResultSet rows = sourceRows.executeQuery();
             PreparedStatement existingUser = target.prepareStatement(
                     "SELECT id FROM public.users WHERE email_hash = ?");
             PreparedStatement insert = target.prepareStatement(insertSql)) {
            while (rows.next()) {
                UUID sourceId = (UUID) rows.getObject(idIndex + 1);
                String emailHash = rows.getString(emailHashIndex + 1);
                UUID targetId = findUser(existingUser, emailHash);
                if (targetId != null) {
                    userIds.put(sourceId, targetId);
                    matched++;
                    continue;
                }

                bindRow(insert, rows, columns, Map.of());
                insert.addBatch();
                int[] result = insert.executeBatch();
                if (result.length != 1 || result[0] == 0) {
                    targetId = findUser(existingUser, emailHash);
                    if (targetId == null) {
                        throw new SQLException("Could not resolve migrated user row");
                    }
                    matched++;
                } else {
                    targetId = sourceId;
                    inserted++;
                }
                userIds.put(sourceId, targetId);
            }
        }
        System.out.println("users.inserted=" + inserted);
        System.out.println("users.matched=" + matched);
        return userIds;
    }

    private static UUID findUser(PreparedStatement statement, String emailHash) throws SQLException {
        statement.setString(1, emailHash);
        try (ResultSet result = statement.executeQuery()) {
            return result.next() ? (UUID) result.getObject(1) : null;
        }
    }

    private static void migrateTable(
            Connection source,
            Connection target,
            String table,
            Map<UUID, UUID> userIds
    ) throws SQLException {
        List<Column> columns = columns(source, table);
        int userIdIndex = columnIndexOrMinusOne(columns, "user_id");
        String insertSql = insertSql(table, columns);
        int inserted = 0;
        int skipped = 0;

        try (PreparedStatement sourceRows = source.prepareStatement("SELECT * FROM public." + quote(table));
             ResultSet rows = sourceRows.executeQuery();
             PreparedStatement insert = target.prepareStatement(insertSql)) {
            while (rows.next()) {
                UUID mappedUserId = null;
                Object sourceUserValue = userIdIndex < 0 ? null : rows.getObject(userIdIndex + 1);
                if (sourceUserValue != null) {
                    UUID sourceUserId = (UUID) sourceUserValue;
                    mappedUserId = userIds.get(sourceUserId);
                    if (mappedUserId == null) {
                        throw new SQLException("Missing user mapping for table " + table);
                    }
                }
                bindRow(insert, rows, columns, userIdIndex < 0 ? Map.of() : Map.of(userIdIndex, mappedUserId));
                int result = insert.executeUpdate();
                if (result == 0) {
                    skipped++;
                } else {
                    inserted += result;
                }
            }
        }
        System.out.println(table + ".inserted=" + inserted + ".skipped=" + skipped);
    }

    private static void bindRow(
            PreparedStatement statement,
            ResultSet row,
            List<Column> columns,
            Map<Integer, UUID> replacements
    ) throws SQLException {
        for (int index = 0; index < columns.size(); index++) {
            Object value = replacements.containsKey(index)
                    ? replacements.get(index)
                    : row.getObject(index + 1);
            Column column = columns.get(index);
            if (value == null) {
                statement.setNull(index + 1, column.jdbcType());
            } else if ("vector".equalsIgnoreCase(column.typeName())) {
                PGobject vector = new PGobject();
                vector.setType("vector");
                vector.setValue(row.getString(index + 1));
                statement.setObject(index + 1, vector);
            } else if (column.jdbcType() == Types.OTHER && value instanceof PGobject) {
                statement.setObject(index + 1, value);
            } else {
                statement.setObject(index + 1, value);
            }
        }
    }

    private static List<Column> columns(Connection connection, String table) throws SQLException {
        List<Column> columns = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getColumns(null, "public", table, "%")) {
            while (result.next()) {
                columns.add(new Column(
                        result.getString("COLUMN_NAME"),
                        result.getInt("DATA_TYPE"),
                        result.getString("TYPE_NAME"),
                        result.getInt("ORDINAL_POSITION")
                ));
            }
        }
        columns.sort((left, right) -> Integer.compare(left.ordinalPosition(), right.ordinalPosition()));
        if (columns.isEmpty()) {
            throw new SQLException("Table not found: " + table);
        }
        return columns;
    }

    private static String insertSql(String table, List<Column> columns) {
        String names = columns.stream().map(column -> quote(column.name())).reduce((left, right) -> left + ", " + right).orElseThrow();
        String values = columns.stream().map(column -> "?").reduce((left, right) -> left + ", " + right).orElseThrow();
        return "INSERT INTO public." + quote(table) + " (" + names + ") VALUES (" + values + ") ON CONFLICT DO NOTHING";
    }

    private static int columnIndex(List<Column> columns, String name) throws SQLException {
        int index = columnIndexOrMinusOne(columns, name);
        if (index < 0) {
            throw new SQLException("Column not found: " + name);
        }
        return index;
    }

    private static int columnIndexOrMinusOne(List<Column> columns, String name) {
        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index).name().equalsIgnoreCase(name)) {
                return index;
            }
        }
        return -1;
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private record Column(String name, int jdbcType, String typeName, int ordinalPosition) {
    }
}
