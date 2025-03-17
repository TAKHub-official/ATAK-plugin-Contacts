package com.atakmap.android.contacts.plugin.util;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.view.View;

/**
 * Simple utility class to test the error handling system.
 * This is not a formal unit test, but a helper to manually trigger
 * different error scenarios to verify the error handling system.
 */
public class ErrorHandlerTest {
    private static final String TAG = "ErrorHandlerTest";
    
    private final Context context;
    private final ErrorHandler errorHandler;
    private final DatabaseErrorHandler dbErrorHandler;
    private final UIErrorHandler uiErrorHandler;
    
    public ErrorHandlerTest(Context context) {
        this.context = context;
        this.errorHandler = ErrorHandler.getInstance(context);
        this.dbErrorHandler = DatabaseErrorHandler.getInstance(context);
        this.uiErrorHandler = UIErrorHandler.getInstance(context);
    }
    
    /**
     * Run a series of tests to verify the error handling system
     */
    public void runTests() {
        Log.d(TAG, "Starting error handler tests");
        
        testGeneralErrorHandler();
        testDatabaseErrorHandler();
        testUIErrorHandler();
        
        Log.d(TAG, "Error handler tests completed");
    }
    
    /**
     * Test the general error handler
     */
    private void testGeneralErrorHandler() {
        Log.d(TAG, "Testing general error handler");
        
        // Test different severity levels
        errorHandler.handleException(TAG, "Info level test", 
                                   new Exception("Test info exception"), 
                                   ErrorHandler.Severity.INFO, false);
        
        errorHandler.handleException(TAG, "Warning level test", 
                                   new Exception("Test warning exception"), 
                                   ErrorHandler.Severity.WARNING, false);
        
        errorHandler.handleException(TAG, "Error level test", 
                                   new Exception("Test error exception"), 
                                   ErrorHandler.Severity.ERROR, false);
        
        errorHandler.handleException(TAG, "Critical level test", 
                                   new Exception("Test critical exception"), 
                                   ErrorHandler.Severity.CRITICAL, false);
        
        // Test with toast
        errorHandler.handleException(TAG, "Test with toast", 
                                   new Exception("This should show a toast"), 
                                   ErrorHandler.Severity.ERROR, true);
        
        // Test with nested exception
        try {
            try {
                throw new IllegalArgumentException("Inner exception");
            } catch (Exception e) {
                throw new RuntimeException("Outer exception", e);
            }
        } catch (Exception e) {
            errorHandler.handleException(TAG, "Nested exception test", e);
        }
    }
    
    /**
     * Test the database error handler
     */
    private void testDatabaseErrorHandler() {
        Log.d(TAG, "Testing database error handler");
        
        // Test missing table error
        SQLiteException noTableException = new SQLiteException("no such table: contacts");
        dbErrorHandler.handleDatabaseException("table access", noTableException);
        
        // Test database locked error
        SQLiteException lockedDbException = new SQLiteException("database is locked");
        dbErrorHandler.handleDatabaseException("write operation", lockedDbException);
        
        // Test disk error
        SQLiteException diskErrorException = new SQLiteException("disk I/O error");
        dbErrorHandler.handleDatabaseException("read operation", diskErrorException);
        
        // Test corrupt database error
        SQLiteException corruptDbException = new SQLiteException("database disk image is malformed");
        dbErrorHandler.handleDatabaseException("open database", corruptDbException);
        
        // Test generic database error
        SQLiteException genericException = new SQLiteException("some other database error");
        dbErrorHandler.handleDatabaseException("generic operation", genericException);
    }
    
    /**
     * Test the UI error handler
     */
    private void testUIErrorHandler() {
        Log.d(TAG, "Testing UI error handler");
        
        // Test inflate error
        android.view.InflateException inflateException = new android.view.InflateException("Error inflating layout");
        uiErrorHandler.handleUIException("dialog layout", inflateException);
        
        // Test null pointer error
        NullPointerException nullPointerException = new NullPointerException("findViewById returned null");
        uiErrorHandler.handleUIException("button click", nullPointerException);
        
        // Test illegal state error
        IllegalStateException illegalStateException = new IllegalStateException("RecyclerView has no adapter attached");
        uiErrorHandler.handleUIException("recycler view", illegalStateException);
        
        // Test generic UI error
        Exception genericException = new Exception("Some other UI error");
        uiErrorHandler.handleUIException("generic component", genericException);
    }
    
    /**
     * Helper method to trigger the error handler from a UI component
     */
    public void triggerErrorFromUI(View view) {
        try {
            // Simulate a UI error
            throw new IllegalStateException("Error triggered from UI");
        } catch (Exception e) {
            uiErrorHandler.handleUIException("button click", e);
        }
    }
} 