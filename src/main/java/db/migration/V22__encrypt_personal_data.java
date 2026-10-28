package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import com.aicareer.platform.shared.security.DataEncryption;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Encrypts existing user-entered data before the JPA converters are enabled. */
public class V22__encrypt_personal_data extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        DataEncryption.configure(resolveKey(context));
        alterSchema(context.getConnection());

        encryptUserEmails(context.getConnection());
        encryptColumn(context.getConnection(), "users", "id", "display_name");
        encryptColumn(context.getConnection(), "experiences", "id", "company_name");
        encryptColumn(context.getConnection(), "experiences", "id", "role");
        encryptColumn(context.getConnection(), "experiences", "id", "start_date");
        encryptColumn(context.getConnection(), "experiences", "id", "end_date");
        encryptColumn(context.getConnection(), "experiences", "id", "department");
        encryptColumn(context.getConnection(), "experiences", "id", "title");
        encryptColumn(context.getConnection(), "experiences", "id", "description");
        encryptColumn(context.getConnection(), "experiences", "id", "tech_stack");
        encryptColumn(context.getConnection(), "projects", "id", "name");
        encryptColumn(context.getConnection(), "projects", "id", "role");
        encryptColumn(context.getConnection(), "projects", "id", "start_date");
        encryptColumn(context.getConnection(), "projects", "id", "end_date");
        encryptColumn(context.getConnection(), "projects", "id", "description");
        encryptColumn(context.getConnection(), "projects", "id", "tech_stack");
        encryptColumn(context.getConnection(), "projects", "id", "customer_or_domain");
        encryptColumn(context.getConnection(), "projects", "id", "repository_url");
        encryptColumn(context.getConnection(), "resumes", "id", "name");
        encryptColumn(context.getConnection(), "resume_versions", "id", "content");
        encryptColumn(context.getConnection(), "job_postings", "id", "title");
        encryptColumn(context.getConnection(), "job_postings", "id", "company_name");
        encryptColumn(context.getConnection(), "job_postings", "id", "description");
        encryptColumn(context.getConnection(), "job_postings", "id", "source_url");
        encryptColumn(context.getConnection(), "resume_application_questions", "id", "question");
        encryptColumn(context.getConnection(), "resume_application_questions", "id", "answer");
        encryptColumn(context.getConnection(), "ai_job_analyses", "id", "analysis");
        encryptColumn(context.getConnection(), "ai_resume_drafts", "id", "draft");
        encryptColumn(context.getConnection(), "ai_resume_drafts", "id", "source_references");
        encryptColumn(context.getConnection(), "ai_job_matches", "id", "analysis");
        encryptColumn(context.getConnection(), "ai_job_matches", "id", "source_references");
        encryptColumn(context.getConnection(), "embedding_chunks", "id", "content");

        try (PreparedStatement statement = context.getConnection().prepareStatement(
                "ALTER TABLE users ALTER COLUMN email_hash SET NOT NULL")) {
            statement.execute();
        }
        try (PreparedStatement statement = context.getConnection().prepareStatement(
                "ALTER TABLE users ADD CONSTRAINT uq_users_email_hash UNIQUE (email_hash)")) {
            statement.execute();
        }
    }

    private void alterSchema(Connection connection) throws SQLException {
        execute(connection, "ALTER TABLE users DROP CONSTRAINT uq_users_email");
        execute(connection, "ALTER TABLE users DROP CONSTRAINT ck_users_email_lowercase");
        execute(connection, "ALTER TABLE users ADD COLUMN email_hash VARCHAR(64)");
        execute(connection, "ALTER TABLE users ALTER COLUMN email TYPE TEXT");
        execute(connection, "ALTER TABLE users ALTER COLUMN display_name TYPE TEXT");

        execute(connection, "ALTER TABLE experiences DROP CONSTRAINT ck_experiences_dates");
        execute(connection, "ALTER TABLE projects DROP CONSTRAINT ck_projects_dates");
        alterToText(connection, "experiences", "company_name");
        alterToText(connection, "experiences", "role");
        alterToText(connection, "experiences", "start_date");
        alterToText(connection, "experiences", "end_date");
        alterToText(connection, "experiences", "department");
        alterToText(connection, "experiences", "title");
        alterToText(connection, "experiences", "tech_stack");
        alterToText(connection, "projects", "name");
        alterToText(connection, "projects", "role");
        alterToText(connection, "projects", "start_date");
        alterToText(connection, "projects", "end_date");
        alterToText(connection, "projects", "tech_stack");
        alterToText(connection, "projects", "customer_or_domain");
        alterToText(connection, "projects", "repository_url");
        alterToText(connection, "resumes", "name");
        alterToText(connection, "job_postings", "title");
        alterToText(connection, "job_postings", "company_name");
        alterToText(connection, "job_postings", "source_url");
        alterToText(connection, "resume_application_questions", "question");
        alterJsonToText(connection, "ai_job_analyses", "analysis");
        alterJsonToText(connection, "ai_resume_drafts", "draft");
        alterJsonToText(connection, "ai_resume_drafts", "source_references");
        alterJsonToText(connection, "ai_job_matches", "analysis");
        alterJsonToText(connection, "ai_job_matches", "source_references");

    }

    private void encryptUserEmails(Connection connection) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT id, email FROM users");
             ResultSet resultSet = select.executeQuery();
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE users SET email = ?, email_hash = ? WHERE id = ?")) {
            while (resultSet.next()) {
                String storedEmail = resultSet.getString("email");
                String email = DataEncryption.decrypt(storedEmail).trim().toLowerCase(Locale.ROOT);
                update.setString(1, DataEncryption.encrypt(email));
                update.setString(2, DataEncryption.blindIndex(email));
                update.setObject(3, resultSet.getObject("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private void encryptColumn(Connection connection, String table, String idColumn, String column) throws SQLException {
        String selectSql = "SELECT " + idColumn + ", " + column + " FROM " + table
                + " WHERE " + column + " IS NOT NULL";
        String updateSql = "UPDATE " + table + " SET " + column + " = ? WHERE " + idColumn + " = ?";
        try (PreparedStatement select = connection.prepareStatement(selectSql);
             ResultSet resultSet = select.executeQuery();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (resultSet.next()) {
                String storedValue = resultSet.getString(column);
                if (DataEncryption.isEncrypted(storedValue)) {
                    continue;
                }
                update.setString(1, DataEncryption.encrypt(storedValue));
                update.setObject(2, resultSet.getObject(idColumn));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private void alterToText(Connection connection, String table, String column) throws SQLException {
        execute(connection, "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE TEXT");
    }

    private void alterJsonToText(Connection connection, String table, String column) throws SQLException {
        execute(connection, "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE TEXT USING " + column + "::text");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private String resolveKey(Context context) {
        String configured = context.getConfiguration().getPlaceholders().get("data-encryption-key-base64");
        if (isUnresolvedPlaceholder(configured)) {
            configured = System.getProperty("DATA_ENCRYPTION_KEY_BASE64");
        }
        if (isUnresolvedPlaceholder(configured)) {
            configured = System.getenv("DATA_ENCRYPTION_KEY_BASE64");
        }
        if (isUnresolvedPlaceholder(configured)) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY_BASE64 must be configured before V22");
        }
        return configured;
    }

    private boolean isUnresolvedPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("${") && trimmed.endsWith("}");
    }
}
