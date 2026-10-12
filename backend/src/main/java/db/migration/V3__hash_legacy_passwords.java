package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * One-off migration: the original application stored plaintext passwords.
 * Hash every row that is not already a BCrypt hash so that existing users can keep logging in.
 * Passwords are never logged.
 */
public class V3__hash_legacy_passwords extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Map<Long, String> legacy = new LinkedHashMap<>();

        try (Statement st = context.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, password FROM users WHERE password NOT LIKE '$2%' AND password <> '!'")) {
            while (rs.next()) {
                legacy.put(rs.getLong(1), rs.getString(2));
            }
        }

        try (PreparedStatement ps = context.getConnection()
                .prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
            for (Map.Entry<Long, String> e : legacy.entrySet()) {
                String plain = e.getValue();
                // BCrypt only uses the first 72 bytes; keep behaviour predictable for odd legacy values.
                if (plain.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                    plain = plain.substring(0, 72);
                }
                ps.setString(1, encoder.encode(plain));
                ps.setLong(2, e.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
