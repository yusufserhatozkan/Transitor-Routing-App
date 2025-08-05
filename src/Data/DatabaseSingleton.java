package Data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 * This class is used to access the database. The DataGetter class calls this class for each query
 * that it runs.
 */
public class DatabaseSingleton {
    private static Connection connection = null;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bcs7?useSSL=false&allowPublicKeyRetrieval=true";

    private DatabaseSingleton() { }

    public static Connection getConnection() {
        if (connection == null) {
            try {                
                    String username = "root";
                    String password = "new_password";
                    connection = DriverManager.getConnection(DB_URL, username, password);
                    System.out.println("connected to the database.");
            } catch (SQLException e) {
                System.out.println("Error connecting to the database: " + e.getMessage());
                return null;
            }
        }
        return connection;
    }
}