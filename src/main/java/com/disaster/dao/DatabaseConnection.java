package com.disaster.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Properties SETTINGS = loadSettings();
    
    static {
        try {
            Class.forName(getSetting("DB_DRIVER", "db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Unable to load the SQL Server JDBC driver.");
        }
    }
    
    public static Connection getConnection() throws SQLException {
        String url = getSetting("DB_URL", "db.url", buildDefaultUrl());
        String username = getSetting("DB_USERNAME", "db.username", "");
        String password = getSetting("DB_PASSWORD", "db.password", "");

        if (username.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }
    
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static Properties loadSettings() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Unable to load db.properties.");
        }
        return properties;
    }

    private static String buildDefaultUrl() {
        String host = getSetting("DB_HOST", "db.host", "localhost");
        String port = getSetting("DB_PORT", "db.port", "1433");
        String databaseName = getSetting("DB_NAME", "db.name", "disaster_mis");
        String encrypt = getSetting("DB_ENCRYPT", "db.encrypt", "true");
        String trustServerCertificate = getSetting("DB_TRUST_SERVER_CERTIFICATE", "db.trustServerCertificate", "true");

        return "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + databaseName
                + ";encrypt=" + encrypt
                + ";trustServerCertificate=" + trustServerCertificate;
    }

    private static String getSetting(String envKey, String propertyKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String fileValue = SETTINGS.getProperty(propertyKey);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue;
        }

        return defaultValue;
    }
}
