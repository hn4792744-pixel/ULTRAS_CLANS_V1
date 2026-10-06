package me.uc.hussein.ultrasclans.database;

public enum DatabaseType {
    SQLITE,
    MYSQL;

    public static DatabaseType fromString(String value) {
        if (value == null) {
            return SQLITE;
        }
        try {
            return DatabaseType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SQLITE;
        }
    }
}
