package org.main.culturesolutioncalculation.service.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

    private static DatabaseConnector instance;
    private Connection connection;

    private DatabaseConnector(String url, String user, String password) {
        try {
            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("Successfully connected to the database.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static void disconnect(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Disconnected from the database.");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static DatabaseConnector getInstance(String url, String user, String password) {
        if (instance == null) {
            instance = new DatabaseConnector(url, user, password);
        }
        return instance;
    }

    public Connection getConnection() {
        return this.connection;
    }
}
