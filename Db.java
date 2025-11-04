import java.sql.*;

public final class Db {
    private static final String URL = "jdbc:sqlite:evoting.db";

    private Db() {}

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void init() {
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "display_name TEXT, " +
                    "role TEXT, " +
                    "verified INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS candidates (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE, " +
                    "manifesto TEXT, " +
                    "votes INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS voted_users (" +
                    "username TEXT PRIMARY KEY)");

            s.executeUpdate("INSERT OR IGNORE INTO candidates(name, manifesto, votes) VALUES" +
                    "('Alice','Transparency and Innovation',0)");
            s.executeUpdate("INSERT OR IGNORE INTO candidates(name, manifesto, votes) VALUES" +
                    "('Bob','Community and Growth',0)");
            s.executeUpdate("INSERT OR IGNORE INTO users(username, display_name, role, verified) VALUES" +
                    "('voter1','Voter One','VOTER',0)");
            s.executeUpdate("INSERT OR IGNORE INTO users(username, display_name, role, verified) VALUES" +
                    "('admin','Administrator','ADMIN',1)");
            s.executeUpdate("INSERT OR IGNORE INTO settings(key, value) VALUES('electionActive','false')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}


