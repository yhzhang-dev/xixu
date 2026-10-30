import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Local, read-only account check. Credentials arrive through stdin, never command arguments. */
public class AdminLoginProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("[A-Za-z0-9_]+")) {
            System.err.println("数据库名不合法");
            System.exit(2);
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String dbUser = input.readLine();
        String dbPassword = input.readLine();
        String username = input.readLine();
        String password = input.readLine();
        if (dbUser == null || dbPassword == null || username == null || password == null) {
            System.err.println("输入不完整");
            System.exit(2);
        }

        String url = "jdbc:mysql://127.0.0.1:3306/" + args[0] + "?useUnicode=true&characterEncoding=utf8";
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT role_code, status, password_hash FROM app_user WHERE username = ?")) {
            String databaseIdentity;
            try (Statement identityQuery = connection.createStatement();
                 ResultSet identity = identityQuery.executeQuery(
                     "SELECT CONCAT(COALESCE(DATABASE(), '(none)'), '@', @@hostname, ':', @@port)")) {
                identity.next();
                databaseIdentity = identity.getString(1);
            }
            statement.setString(1, username);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    System.out.println("db=" + databaseIdentity + " account=NOT_FOUND");
                    return;
                }
                String hash = row.getString("password_hash");
                boolean matches = hash != null && new BCryptPasswordEncoder().matches(password, hash);
                String fingerprint = hash == null ? "none" : HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.US_ASCII)))
                    .substring(0, 12);
                System.out.println("db=" + databaseIdentity + " role=" + row.getString("role_code") +
                    " status=" + row.getInt("status") + " password=" + (matches ? "MATCH" : "MISMATCH") +
                    " hashId=" + fingerprint);
            }
        } catch (SQLException error) {
            System.err.println("数据库连接或查询失败，SQLState=" + error.getSQLState());
            System.exit(1);
        }
    }
}
