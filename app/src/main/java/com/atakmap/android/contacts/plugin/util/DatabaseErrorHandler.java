package com.atakmap.android.contacts.plugin.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.atakmap.android.contacts.plugin.db.DatabaseHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Specialized error handler for database-related issues.
 * Provides recovery strategies for common database problems.
 */
public class DatabaseErrorHandler {
    private static final String TAG = "ContactsDBErrorHandler";
    private static DatabaseErrorHandler instance;
    private final Context context;
    private final String DATABASE_NAME = "contacts_db";
    
    private DatabaseErrorHandler(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Get the singleton instance of DatabaseErrorHandler
     */
    public static synchronized DatabaseErrorHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseErrorHandler(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Handle a database-related exception
     * 
     * @param operation Description of the database operation that failed
     * @param exception The exception that was caught
     * @return true if recovery was successful, false otherwise
     */
    public boolean handleDatabaseException(String operation, SQLiteException exception) {
        Log.e(TAG, "Database error during " + operation + ": " + exception.getMessage(), exception);
        
        // Determine the type of database error
        String errorMsg = exception.getMessage();
        if (errorMsg == null) {
            errorMsg = "";
        }
        
        // Handle specific database error types
        if (errorMsg.contains("no such table") || errorMsg.contains("table") && errorMsg.contains("doesn't exist")) {
            return handleMissingTableError();
        } else if (errorMsg.contains("database is locked")) {
            return handleDatabaseLockedError();
        } else if (errorMsg.contains("disk I/O error") || errorMsg.contains("disk full")) {
            return handleDiskError();
        } else if (errorMsg.contains("database disk image is malformed") || 
                  errorMsg.contains("database corrupted")) {
            return handleCorruptDatabaseError();
        } else {
            // Generic database error
            return handleGenericDatabaseError();
        }
    }
    
    /**
     * Handle missing table errors by recreating the table
     */
    private boolean handleMissingTableError() {
        Log.w(TAG, "Attempting to recover from missing table error");
        try {
            // Get a new database instance and recreate tables
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // Force recreation of tables
            db.execSQL("DROP TABLE IF EXISTS contacts");
            dbHelper.onCreate(db);
            
            Log.i(TAG, "Successfully recreated missing tables");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover from missing table error: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Handle database locked errors by waiting and retrying
     */
    private boolean handleDatabaseLockedError() {
        Log.w(TAG, "Database is locked, waiting for it to be released");
        
        // Wait a bit for the lock to be released
        try {
            Thread.sleep(1000);
            
            // Try to close any open connections
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            dbHelper.close();
            
            // Wait a bit more
            Thread.sleep(500);
            
            // Test if we can now access the database
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            boolean canAccess = db != null && db.isOpen();
            
            Log.i(TAG, "Database lock recovery " + (canAccess ? "successful" : "failed"));
            return canAccess;
        } catch (Exception e) {
            Log.e(TAG, "Error during database lock recovery: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Handle disk I/O errors or disk full errors
     */
    private boolean handleDiskError() {
        Log.w(TAG, "Disk error detected, checking storage");
        
        try {
            // Check if external storage is available
            File externalDir = context.getExternalFilesDir(null);
            if (externalDir == null) {
                Log.e(TAG, "External storage is not available");
                return false;
            }
            
            // Check available space
            long freeSpace = externalDir.getFreeSpace();
            long requiredSpace = 1024 * 1024; // 1MB minimum
            
            if (freeSpace < requiredSpace) {
                Log.e(TAG, "Not enough free space: " + freeSpace + " bytes available, need " + requiredSpace);
                return false;
            }
            
            // Try to move database to external storage if it's not already there
            File dbFile = context.getDatabasePath(DATABASE_NAME);
            if (dbFile.exists()) {
                File externalDbDir = new File(externalDir, "databases");
                if (!externalDbDir.exists()) {
                    externalDbDir.mkdirs();
                }
                
                File externalDbFile = new File(externalDbDir, DATABASE_NAME);
                
                // Only move if external file doesn't exist or is older
                if (!externalDbFile.exists() || externalDbFile.lastModified() < dbFile.lastModified()) {
                    copyDatabase(dbFile, externalDbFile);
                    Log.i(TAG, "Moved database to external storage: " + externalDbFile.getAbsolutePath());
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle disk error: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Handle corrupt database by attempting to restore from backup or recreate
     */
    private boolean handleCorruptDatabaseError() {
        Log.w(TAG, "Database corruption detected, attempting recovery");
        
        try {
            // First, create a backup of the corrupt database for analysis
            File dbFile = context.getDatabasePath(DATABASE_NAME);
            if (dbFile.exists()) {
                backupCorruptDatabase(dbFile);
            }
            
            // Try to delete the corrupt database
            boolean deleted = dbFile.delete();
            Log.d(TAG, "Deleted corrupt database: " + deleted);
            
            // Force recreation of the database
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // Verify the new database
            boolean isValid = db != null && db.isOpen();
            
            Log.i(TAG, "Database corruption recovery " + (isValid ? "successful" : "failed"));
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover from database corruption: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Handle generic database errors with a general recovery approach
     */
    private boolean handleGenericDatabaseError() {
        Log.w(TAG, "Attempting generic database error recovery");
        
        try {
            // Close any open database connections
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            dbHelper.close();
            
            // Wait a moment
            Thread.sleep(500);
            
            // Try to reopen the database
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            boolean success = db != null && db.isOpen();
            
            Log.i(TAG, "Generic database recovery " + (success ? "successful" : "failed"));
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed generic database recovery: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create a backup of a corrupt database for later analysis
     */
    private void backupCorruptDatabase(File dbFile) {
        try {
            // Create backup directory
            File backupDir = new File(context.getExternalFilesDir(null), "database_backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Create timestamped backup file
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String timestamp = dateFormat.format(new Date());
            File backupFile = new File(backupDir, "corrupt_" + DATABASE_NAME + "_" + timestamp);
            
            // Copy the database file
            copyDatabase(dbFile, backupFile);
            
            Log.i(TAG, "Created backup of corrupt database: " + backupFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to backup corrupt database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Copy a database file from source to destination
     */
    private void copyDatabase(File source, File destination) throws Exception {
        if (!source.exists()) {
            throw new Exception("Source database does not exist: " + source.getAbsolutePath());
        }
        
        try (FileChannel inChannel = new FileInputStream(source).getChannel();
             FileChannel outChannel = new FileOutputStream(destination).getChannel()) {
            
            outChannel.transferFrom(inChannel, 0, inChannel.size());
        }
    }
} 