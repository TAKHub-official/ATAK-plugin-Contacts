package com.atakmap.android.contacts.plugin.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;

/**
 * Specialized error handler for UI-related issues.
 * Provides recovery strategies for common UI problems.
 */
public class UIErrorHandler {
    private static final String TAG = "ContactsUIErrorHandler";
    private static UIErrorHandler instance;
    private final Context context;
    private final Handler mainHandler;
    
    private UIErrorHandler(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get the singleton instance of UIErrorHandler
     */
    public static synchronized UIErrorHandler getInstance(Context context) {
        if (instance == null) {
            instance = new UIErrorHandler(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Handle a UI-related exception
     * 
     * @param component Description of the UI component that failed
     * @param exception The exception that was caught
     * @return true if recovery was successful, false otherwise
     */
    public boolean handleUIException(String component, Exception exception) {
        Log.e(TAG, "UI error in " + component + ": " + exception.getMessage(), exception);
        
        // Determine the type of UI error
        if (exception instanceof android.view.InflateException) {
            return handleInflateError(component, exception);
        } else if (exception instanceof NullPointerException) {
            return handleNullPointerError(component, exception);
        } else if (exception instanceof IllegalStateException) {
            return handleIllegalStateError(component, exception);
        } else {
            // Generic UI error
            return handleGenericUIError(component, exception);
        }
    }
    
    /**
     * Handle layout inflation errors
     */
    private boolean handleInflateError(String component, Exception exception) {
        Log.w(TAG, "Layout inflation error in " + component);
        
        // Show error message to user
        showErrorToast("Could not display " + component + ". Please restart the app.");
        
        // Inflation errors usually can't be recovered automatically
        return false;
    }
    
    /**
     * Handle null pointer errors in UI components
     */
    private boolean handleNullPointerError(String component, Exception exception) {
        Log.w(TAG, "Null pointer in UI component: " + component);
        
        // Try to determine if this is a view that wasn't found
        String message = exception.getMessage();
        if (message != null && message.contains("findViewById")) {
            showErrorToast("UI component not found. Please restart the app.");
            return false;
        }
        
        // For other null pointer errors, try to recover by refreshing the view
        return refreshUIComponent(component);
    }
    
    /**
     * Handle illegal state errors in UI components
     */
    private boolean handleIllegalStateError(String component, Exception exception) {
        Log.w(TAG, "Illegal state in UI component: " + component);
        
        // Check if this is a common RecyclerView error
        String message = exception.getMessage();
        if (message != null && message.contains("RecyclerView")) {
            return handleRecyclerViewError();
        }
        
        // For other illegal state errors, try to recover by refreshing the view
        return refreshUIComponent(component);
    }
    
    /**
     * Handle generic UI errors
     */
    private boolean handleGenericUIError(String component, Exception exception) {
        Log.w(TAG, "Generic UI error in " + component);
        
        // For generic errors, try to recover by refreshing the view
        return refreshUIComponent(component);
    }
    
    /**
     * Handle RecyclerView-specific errors
     */
    private boolean handleRecyclerViewError() {
        Log.w(TAG, "Attempting to recover from RecyclerView error");
        
        try {
            // Post to main thread to ensure we're not in the middle of a layout pass
            mainHandler.post(() -> {
                try {
                    // Try to find the main activity view
                    MapView mapView = MapView.getMapView();
                    if (mapView == null) {
                        Log.e(TAG, "MapView is null, cannot recover RecyclerView");
                        return;
                    }
                    
                    // Find the RecyclerView - this is a simplified example
                    // In a real app, you would need to find the specific RecyclerView
                    View rootView = mapView.getRootView();
                    if (rootView instanceof ViewGroup) {
                        findAndResetRecyclerView((ViewGroup) rootView);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during RecyclerView recovery: " + e.getMessage(), e);
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover from RecyclerView error: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Recursively find and reset RecyclerViews in a ViewGroup
     */
    private void findAndResetRecyclerView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            if (child instanceof androidx.recyclerview.widget.RecyclerView) {
                // Found a RecyclerView, try to reset it
                androidx.recyclerview.widget.RecyclerView recyclerView = 
                    (androidx.recyclerview.widget.RecyclerView) child;
                
                try {
                    // Reset adapter
                    androidx.recyclerview.widget.RecyclerView.Adapter adapter = recyclerView.getAdapter();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    
                    // Request layout
                    recyclerView.requestLayout();
                    
                    Log.d(TAG, "Reset RecyclerView");
                } catch (Exception e) {
                    Log.e(TAG, "Error resetting RecyclerView: " + e.getMessage(), e);
                }
            } else if (child instanceof ViewGroup) {
                // Recursively search child ViewGroups
                findAndResetRecyclerView((ViewGroup) child);
            }
        }
    }
    
    /**
     * Try to refresh a UI component
     */
    private boolean refreshUIComponent(String component) {
        Log.w(TAG, "Attempting to refresh UI component: " + component);
        
        try {
            // Post to main thread to ensure we're not in the middle of a layout pass
            mainHandler.post(() -> {
                try {
                    // Try to find the main activity view
                    MapView mapView = MapView.getMapView();
                    if (mapView == null) {
                        Log.e(TAG, "MapView is null, cannot refresh UI component");
                        return;
                    }
                    
                    // Request layout on the root view
                    View rootView = mapView.getRootView();
                    if (rootView != null) {
                        rootView.requestLayout();
                        rootView.invalidate();
                        Log.d(TAG, "Requested layout refresh for root view");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during UI refresh: " + e.getMessage(), e);
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh UI component: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Try to recreate a layout that failed to inflate
     */
    public View safeInflate(int layoutResId, ViewGroup parent, boolean attachToParent) {
        try {
            // Try normal inflation first
            return LayoutInflater.from(context).inflate(layoutResId, parent, attachToParent);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout " + layoutResId + ": " + e.getMessage(), e);
            
            try {
                // Try with a different LayoutInflater
                LayoutInflater inflater = LayoutInflater.from(MapView.getMapView().getContext());
                return inflater.inflate(layoutResId, parent, attachToParent);
            } catch (Exception e2) {
                Log.e(TAG, "Second attempt to inflate layout failed: " + e2.getMessage(), e2);
                
                // Create a fallback view
                return createFallbackView(parent);
            }
        }
    }
    
    /**
     * Create a simple fallback view when inflation fails
     */
    private View createFallbackView(ViewGroup parent) {
        // Create a simple View as fallback
        View fallbackView = new View(context);
        fallbackView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Show error message to user
        showErrorToast("Error displaying UI. Please restart the app.");
        
        return fallbackView;
    }
    
    /**
     * Show an error toast on the UI thread
     */
    private void showErrorToast(final String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
} 