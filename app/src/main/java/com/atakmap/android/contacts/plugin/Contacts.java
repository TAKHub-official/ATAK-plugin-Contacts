package com.atakmap.android.contacts.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class Contacts implements IPlugin {

    private static final String TAG = "ContactsPlugin";
    
    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane contactsPane;
    ContactManager contactManager;

    public Contacts(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
            
            // Check permissions early
            checkPermissions(pluginContext);
        }

        // obtain the UI service
        uiService = serviceController.getService(IHostUIService.class);

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                })
                .build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService == null)
            return;

        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService == null)
            return;

        uiService.removeToolbarItem(toolbarItem);
    }

    private void showPane() {
        try {
            // instantiate the plugin view if necessary
            if(contactsPane == null) {
                // Inflate the main layout
                View mainLayout = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);
                
                // Initialize ContactManager with the main layout
                contactManager = new ContactManager(pluginContext, mainLayout);
    
                contactsPane = new PaneBuilder(mainLayout)
                        // relative location is set to default; pane will switch location dependent on
                        // current orientation of device screen
                        .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                        // pane will take up 50% of screen width in landscape mode
                        .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                        // pane will take up 50% of screen height in portrait mode
                        .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                        .build();
            }

            // Check if the pane is already visible
            if(uiService.isPaneVisible(contactsPane)) {
                // If it's already visible, reset the manager which handles reloading safely
                if (contactManager != null) {
                    contactManager.reset();
                }
                // Optional: Toggle the pane if you want to close it when clicked again
                // uiService.hidePane(contactsPane);
            } else {
                // If the pane is not visible, show it and reload contacts
                uiService.showPane(contactsPane, null);
                
                // Reload contacts if needed
                if (contactManager != null) {
                    contactManager.reset(); // Use reset instead of just loadContacts
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing contacts pane", e);
        }
    }

    // Method to check and request permissions
    public void checkPermissions(final Context context) {
        Log.d(TAG, "Checking permissions");
        try {
            // Check for storage permissions
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                String[] permissions = {
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                };
                
                for (String permission : permissions) {
                    if (context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Log.w(TAG, "Missing permission: " + permission);
                        // Note: ATAK manages permissions differently, this is just for logging
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
        }
    }
} 