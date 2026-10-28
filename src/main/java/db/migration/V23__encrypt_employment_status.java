package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.aicareer.platform.shared.security.DataEncryption;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V23__encrypt_employment_status extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        DataEncryption.configure(resolveKey(context));

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("ALTER TABLE experiences ALTER COLUMN currently_employed TYPE text USING currently_employed::text");
        }

        try (PreparedStatement select = context.getConnection().prepareStatement(
                "SELECT id, currently_employed FROM experiences");
             ResultSet rows = select.executeQuery();
             PreparedStatement update = context.getConnection().prepareStatement(
                     "UPDATE experiences SET currently_employed = ? WHERE id = ?")) {
            while (rows.next()) {
                update.setString(1, DataEncryption.encrypt(rows.getString("currently_employed")));
                update.setObject(2, rows.getObject("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private String resolveKey(Context context) {
        String placeholder = context.getConfiguration().getPlaceholders().get("data-encryption-key-base64");
        if (placeholder != null && !placeholder.isBlank()) {
            return placeholder;
        }
        String systemProperty = System.getProperty("DATA_ENCRYPTION_KEY_BASE64");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        String environment = System.getenv("DATA_ENCRYPTION_KEY_BASE64");
        if (environment != null && !environment.isBlank()) {
            return environment;
        }
        throw new IllegalStateException("DATA_ENCRYPTION_KEY_BASE64 must be configured before V23");
    }
}
