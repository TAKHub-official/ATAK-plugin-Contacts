package com.atakmap.android.contacts.plugin.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized error handling utility for the Contacts plugin.
 * Provides consistent error logging, user feedback, and recovery strategies.
 */
public class ErrorHandler {
    private static final String TAG = "ContactsErrorHandler";
    private static ErrorHandler instance;
    private final Context context;
    private final Handler mainHandler;
    
    // Track error frequency to detect recurring issues
    private final ConcurrentHashMap<String, ErrorCounter> errorCounters = new ConcurrentHashMap<>();
    
    // Error severity levels
    public enum Severity {
        INFO,       // Informational only, no user impact
        WARNING,    // Minor issue, operation can continue
        ERROR,      // Operation failed but app can continue
        CRITICAL    // Severe error that may require app restart
    }
    
    private ErrorHandler(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get the singleton instance of ErrorHandler
     */
    public static synchronized ErrorHandler getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                try {
                    // Try to get MapView context as fallback
                    context = MapView.getMapView().getContext();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get context for ErrorHandler: " + e.getMessage());
                    throw new IllegalArgumentException("Cannot initialize ErrorHandler with null context");
                }
            }
            instance = new ErrorHandler(context);
        }
        return instance;
    }
    
    /**
     * Handle an exception with default severity (ERROR)
     * 
     * @param source The class or component where the error occurred
     * @param message A descriptive message about what operation failed
     * @param exception The exception that was caught
     * @return true if recovery was successful, false otherwise
     */
    public boolean handleException(String source, String message, Throwable exception) {
        return handleException(source, message, exception, Severity.ERROR, true);
    }
    
    /**
     * Handle an exception with specified severity
     * 
     * @param source The class or component where the error occurred
     * @param message A descriptive message about what operation failed
     * @param exception The exception that was caught
     * @param severity The severity level of the error
     * @param showToast Whether to show a toast message to the user
     * @return true if recovery was successful, false otherwise
     */
    public boolean handleException(String source, String message, Throwable exception, 
                                  Severity severity, boolean showToast) {
        // Generate a unique error ID for tracking
        String errorId = source + ":" + exception.getClass().getSimpleName();
        
        // Track error frequency
        ErrorCounter counter = errorCounters.computeIfAbsent(errorId, k -> new ErrorCounter());
        counter.increment();
        
        // Log the error
        String logMessage = message + ": " + exception.getMessage();
        switch (severity) {
            case INFO:
                Log.i(TAG, "[" + source + "] " + logMessage, exception);
                break;
            case WARNING:
                Log.w(TAG, "[" + source + "] " + logMessage, exception);
                break;
            case ERROR:
                Log.e(TAG, "[" + source + "] " + logMessage, exception);
                break;
            case CRITICAL:
                Log.e(TAG, "[CRITICAL] [" + source + "] " + logMessage, exception);
                // Write to error log file for critical errors
                writeToErrorLog(source, message, exception);
                break;
        }
        
        // Show toast on UI thread if requested
        if (showToast) {
            final String toastMessage = getToastMessage(severity, message, exception);
            mainHandler.post(() -> Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show());
        }
        
        // Apply recovery strategy based on error type and frequency
        boolean recovered = applyRecoveryStrategy(source, exception, counter.getCount());
        
        // Return recovery status
        return recovered;
    }
    
    /**
     * Get an appropriate toast message based on severity and error details
     */
    private String getToastMessage(Severity severity, String message, Throwable exception) {
        // For user-facing messages, simplify and make more user-friendly
        switch (severity) {
            case INFO:
                return "Info: " + message;
            case WARNING:
                return "Warning: " + message;
            case ERROR:
                return "Error: " + message;
            case CRITICAL:
                return "Critical Error: " + message + ". Please restart the app.";
            default:
                return "An error occurred: " + message;
        }
    }
    
    /**
     * Apply an appropriate recovery strategy based on the error type and frequency
     * 
     * @param source The source of the error
     * @param exception The exception that occurred
     * @param errorCount How many times this error has occurred
     * @return true if recovery was successful, false otherwise
     */
    private boolean applyRecoveryStrategy(String source, Throwable exception, int errorCount) {
        // Implement different recovery strategies based on the error type
        
        // Database errors
        if (source.contains("Database") || exception.getMessage().contains("database")) {
            if (errorCount > 5) {
                // If database errors are persistent, try more aggressive recovery
                Log.w(TAG, "Persistent database errors detected, attempting database reset");
                return resetDatabase();
            }
            return false;
        }
        
        // UI errors
        if (source.contains("UI") || exception instanceof android.view.InflateException) {
            // UI errors often can't be recovered automatically
            return false;
        }
        
        // Network errors
        if (exception instanceof java.net.UnknownHostException || 
            exception instanceof java.io.IOException) {
            // Network errors might resolve themselves
            return true;
        }
        
        // Default case - no specific recovery strategy
        return false;
    }
    
    /**
     * Reset the database as a last resort recovery method
     */
    private boolean resetDatabase() {
        // This is a drastic measure that should only be used as a last resort
        try {
            // Implementation would depend on your database structure
            // This is just a placeholder
            Log.w(TAG, "Database reset not fully implemented");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset database: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Write error details to a log file for later analysis
     */
    private void writeToErrorLog(String source, String message, Throwable exception) {
        try {
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String timestamp = dateFormat.format(new Date());
            
            File logFile = new File(logDir, "contacts_error_" + timestamp + ".log");
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile))) {
                writer.println("Timestamp: " + timestamp);
                writer.println("Source: " + source);
                writer.println("Message: " + message);
                writer.println("Exception: " + exception.getClass().getName());
                writer.println("Exception message: " + exception.getMessage());
                writer.println("\nStack trace:");
                exception.printStackTrace(writer);
            }
            
            Log.i(TAG, "Error details written to log file: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to write error log: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper class to track error frequency
     */
    private static class ErrorCounter {
        private int count = 0;
        private long lastOccurrence = 0;
        
        public synchronized void increment() {
            count++;
            lastOccurrence = System.currentTimeMillis();
        }
        
        public synchronized int getCount() {
            // Reset counter if last error was more than 1 hour ago
            if (System.currentTimeMillis() - lastOccurrence > 3600000) {
                count = 0;
            }
            return count;
        }
    }
} 