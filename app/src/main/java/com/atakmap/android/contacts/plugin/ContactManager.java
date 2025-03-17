package com.atakmap.android.contacts.plugin;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.contacts.plugin.adapter.ContactAdapter;
import com.atakmap.android.contacts.plugin.db.DatabaseHelper;
import com.atakmap.android.contacts.plugin.model.Contact;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;

import java.util.ArrayList;
import java.util.List;

// New imports for location functionality
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * Manager class for contacts that coordinates UI and database operations
 */
public class ContactManager implements ContactAdapter.OnContactClickListener {
    private static final String TAG = "ContactManager";
    
    private final Context pluginContext;
    private DatabaseHelper dbHelper;
    private final View mainView;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private TextView emptyView;
    private final List<Contact> contactList = new ArrayList<>();
    
    // Temporary variables for location information during contact creation
    private Double tempLatitude;
    private Double tempLongitude;
    private boolean hasLocationPermission = false;
    
    /**
     * Constructor for the ContactManager
     * @param context The application context
     * @param mainView The main view of the plugin
     */
    public ContactManager(Context context, View mainView) {
        try {
            if (context == null) {
                Log.e(TAG, "Context is null in ContactManager constructor");
                // Try to get a valid context
                context = mainView.getContext();
                if (context == null) {
                    // As a last resort, try to get the MapView context
                    try {
                        context = MapView.getMapView().getContext();
                        Log.d(TAG, "Using MapView context as fallback");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get MapView context: " + e.getMessage());
                        throw new IllegalArgumentException("Cannot initialize ContactManager with null context");
                    }
                } else {
                    Log.d(TAG, "Using view's context as fallback");
                }
            }
            
            this.pluginContext = context;
            this.mainView = mainView;
            Log.d(TAG, "Initializing DatabaseHelper with context: " + context);
            this.dbHelper = DatabaseHelper.getInstance(context);
            
            setupViews();
            loadContacts();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ContactManager: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Initializes the UI elements
     */
    protected void setupViews() {
        try {
            Button addButton = mainView.findViewById(R.id.btn_add_contact);
            recyclerView = mainView.findViewById(R.id.rv_contacts);
            emptyView = mainView.findViewById(R.id.tv_empty_view);
            EditText searchEditText = mainView.findViewById(R.id.et_search_contacts);
            ImageButton infoButton = mainView.findViewById(R.id.btn_info);
            
            // Set up RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
            adapter = new ContactAdapter(pluginContext, contactList, this);
            recyclerView.setAdapter(adapter);
            
            // Click listener for the "Add Contact" button
            addButton.setOnClickListener(v -> showAddContactDialog());
            
            // Click listener for the Info button
            infoButton.setOnClickListener(v -> showInfoDialog());
            
            // Set up TextWatcher for search
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed
                }
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter the contact list based on the search text
                    filterContacts(s.toString());
                }
                
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // Not needed
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up views", e);
        }
    }
    
    /**
     * Filters the contact list based on the search term
     * @param query Search term
     */
    protected void filterContacts(String query) {
        try {
            if (adapter != null) {
                Log.d(TAG, "Filtering contacts with query: '" + query + "'");
                
                // Apply the filter to the adapter
                adapter.filter(query);
                
                boolean isSearching = query != null && !query.isEmpty();
                int resultCount = adapter.getItemCount();
                
                Log.d(TAG, "Filter results: " + resultCount + " contacts found for query: '" + query + "'");
                
                // Show empty view if no search results were found
                if (resultCount == 0) {
                    // Hide the recyclerview
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                    }
                    
                    // Show appropriate empty message
                    if (emptyView != null) {
                        if (isSearching) {
                            // We're searching but found nothing
                            emptyView.setText("No matching contacts found");
                        } else {
                            // No search, but no contacts
                            emptyView.setText("No contacts added yet");
                        }
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Show the recycler view with results
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    if (emptyView != null) {
                        emptyView.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering contacts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads all contacts from the database
     */
    public void loadContacts() {
        try {
            Log.d(TAG, "Loading contacts from database");
            
            // Safety check for adapter and recyclerView
            if (adapter == null || recyclerView == null) {
                Log.e(TAG, "Adapter or RecyclerView is null, cannot load contacts");
                return;
            }
            
            // Get fresh contacts from database
            contactList.clear();
            List<Contact> contacts = dbHelper.getAllContacts();
            
            // Debug the contacts retrieved from database
            if (contacts != null) {
                Log.d(TAG, "Retrieved " + contacts.size() + " contacts from database");
                for (Contact contact : contacts) {
                    Log.d(TAG, "Contact from DB: " + contact.getId() + ", " + contact.getName() + ", " + contact.getPhoneNumber());
                }
                contactList.addAll(contacts);
            } else {
                Log.e(TAG, "Database returned null contact list");
            }
            
            // Apply the data to the adapter
            adapter.updateContacts(new ArrayList<>(contactList));
            
            Log.d(TAG, "Updated adapter with " + contactList.size() + " contacts");
            
            // Force update UI based on current data
            updateContactsUI();
        } catch (Exception e) {
            Log.e(TAG, "Error loading contacts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates the UI based on whether there are contacts to display
     */
    protected void updateContactsUI() {
        try {
            // Check if we have any contacts to display
            boolean hasContacts = adapter != null && adapter.getItemCount() > 0;
            Log.d(TAG, "Updating UI, has contacts: " + hasContacts + ", count: " + 
                  (adapter != null ? adapter.getItemCount() : 0));
            
            if (hasContacts) {
                // Show the recycler view with contacts
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
                Log.d(TAG, "Showing recyclerView with contacts");
            } else {
                // Show empty view
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.GONE);
                }
                if (emptyView != null) {
                    emptyView.setText("No contacts added yet");
                    emptyView.setVisibility(View.VISIBLE);
                }
                Log.d(TAG, "No contacts found, showing empty view");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating contacts UI: " + e.getMessage(), e);
        }
    }
    
    /**
     * Shows the dialog for adding a new contact
     */
    private void showAddContactDialog() {
        try {
            Log.d(TAG, "Showing add contact dialog");
            
            // Use the MapView context for the dialog, as it is a valid Activity context
            MapView mapView = MapView.getMapView();
            if (mapView == null) {
                Log.e(TAG, "MapView is null");
                Toast.makeText(pluginContext, "Error: Could not access MapView", Toast.LENGTH_SHORT).show();
                return;
            }
            
            final Context dialogContext = mapView.getContext();
            if (dialogContext == null) {
                Log.e(TAG, "MapView context is null");
                Toast.makeText(pluginContext, "Error: Could not create dialog context", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Make sure the dialog is created on the UI thread
            mapView.post(() -> {
                try {
                    // Inflate dialog layout
                    Log.d(TAG, "Inflating dialog layout");
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
                    View dialogView;
                    try {
                        dialogView = LayoutInflater.from(pluginContext).inflate(R.layout.dialog_add_edit_contact, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error inflating dialog layout: " + e.getMessage(), e);
                        Toast.makeText(dialogContext, "Error: Could not create dialog layout", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    builder.setView(dialogView);
                    
                    // Reference UI elements with error checking
                    Log.d(TAG, "Finding UI elements");
                    EditText nameEditText = dialogView.findViewById(R.id.et_contact_name);
                    if (nameEditText == null) {
                        Log.e(TAG, "Could not find et_contact_name");
                        Toast.makeText(dialogContext, "Error: Dialog layout is incomplete", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    EditText phoneEditText = dialogView.findViewById(R.id.et_contact_phone);
                    EditText notesEditText = dialogView.findViewById(R.id.et_contact_notes);
            Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
            Button saveButton = dialogView.findViewById(R.id.btn_save);
            
                    // Location UI elements
                    Button currentLocationButton = dialogView.findViewById(R.id.btn_current_location);
                    Button enterCoordinatesButton = dialogView.findViewById(R.id.btn_enter_coordinates);
                    LinearLayout coordinatesLayout = dialogView.findViewById(R.id.layout_coordinates);
                    EditText latitudeEditText = dialogView.findViewById(R.id.et_latitude);
                    EditText longitudeEditText = dialogView.findViewById(R.id.et_longitude);
                    Button clearLocationButton = dialogView.findViewById(R.id.btn_clear_location);
                    
                    // Check if all important UI elements were found
                    if (saveButton == null || cancelButton == null) {
                        Log.e(TAG, "Could not find essential buttons in dialog");
                        Toast.makeText(dialogContext, "Error: Dialog layout is incomplete", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Reset temporary location variables
                    tempLatitude = null;
                    tempLongitude = null;
                    
                    // Create dialog
                    Log.d(TAG, "Creating dialog");
                    final AlertDialog dialog = builder.create();
                    
                    // Current location button
                    if (currentLocationButton != null) {
                        currentLocationButton.setOnClickListener(v -> {
                            try {
                                getCurrentLocation();
                                if (tempLatitude != null && tempLongitude != null && coordinatesLayout != null) {
                                    // Show coordinates
                                    coordinatesLayout.setVisibility(View.VISIBLE);
                                    if (latitudeEditText != null) latitudeEditText.setText(String.valueOf(tempLatitude));
                                    if (longitudeEditText != null) longitudeEditText.setText(String.valueOf(tempLongitude));
                    } else {
                                    Toast.makeText(dialogContext, "Could not get current location", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in current location button click: " + e.getMessage(), e);
                            }
                        });
                    }
                    
                    // Enter coordinates button
                    if (enterCoordinatesButton != null) {
                        enterCoordinatesButton.setOnClickListener(v -> {
                            try {
                                showCoordinatesInputDialog(coordinates -> {
                                    try {
                                        if (coordinates != null && coordinates.length == 2) {
                                            tempLatitude = coordinates[0];
                                            tempLongitude = coordinates[1];
                                            
                                            // Show coordinates
                                            if (coordinatesLayout != null) {
                                                coordinatesLayout.setVisibility(View.VISIBLE);
                                                if (latitudeEditText != null) latitudeEditText.setText(String.valueOf(tempLatitude));
                                                if (longitudeEditText != null) longitudeEditText.setText(String.valueOf(tempLongitude));
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing coordinates: " + e.getMessage(), e);
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error showing coordinates input dialog: " + e.getMessage(), e);
                            }
                        });
                    }
                    
                    // Clear location button
                    if (clearLocationButton != null && coordinatesLayout != null) {
                        clearLocationButton.setOnClickListener(v -> {
                            tempLatitude = null;
                            tempLongitude = null;
                            coordinatesLayout.setVisibility(View.GONE);
                        });
                    }
                    
                    // Monitor coordinate fields
                    if (latitudeEditText != null) {
                        latitudeEditText.addTextChangedListener(new android.text.TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            
                            @Override
                            public void afterTextChanged(android.text.Editable s) {
                                try {
                                    if (!TextUtils.isEmpty(s)) {
                                        tempLatitude = Double.parseDouble(s.toString());
            } else {
                                        tempLatitude = null;
                                    }
                                } catch (NumberFormatException e) {
                                    tempLatitude = null;
                                }
                            }
                        });
                    }
                    
                    if (longitudeEditText != null) {
                        longitudeEditText.addTextChangedListener(new android.text.TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            
                            @Override
                            public void afterTextChanged(android.text.Editable s) {
                                try {
                                    if (!TextUtils.isEmpty(s)) {
                                        tempLongitude = Double.parseDouble(s.toString());
                                    } else {
                                        tempLongitude = null;
                                    }
                                } catch (NumberFormatException e) {
                                    tempLongitude = null;
                                }
                            }
                        });
                    }
                    
                    // Cancel button
            cancelButton.setOnClickListener(v -> dialog.dismiss());
            
                    // Save button
            saveButton.setOnClickListener(v -> {
                        try {
                            // Validate inputs
                            String name = nameEditText.getText().toString().trim();
                            String phone = phoneEditText != null ? phoneEditText.getText().toString().trim() : "";
                            String notes = notesEditText != null ? notesEditText.getText().toString().trim() : "";
                            
                if (TextUtils.isEmpty(name)) {
                                Toast.makeText(dialogContext, "Please enter a name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                            // Debug output for location data
                            Log.d(TAG, "Saving contact with location data - tempLatitude: " + tempLatitude + ", tempLongitude: " + tempLongitude);
                            
                            // Create contact
                            Contact newContact = new Contact(name, phone, notes);
                            
                            // Set location separately if available
                            if (tempLatitude != null && tempLongitude != null) {
                                newContact.setLocation(tempLatitude, tempLongitude);
                                Log.d(TAG, "Setting location: " + tempLatitude + ", " + tempLongitude);
                                Log.d(TAG, "Contact has location: " + newContact.hasLocation());
                            }
                            
                            // Add contact to database
                            Log.d(TAG, "Adding contact to database: " + newContact.toString());
                            long id = dbHelper.addContact(newContact);
                            Log.d(TAG, "Database returned ID: " + id);
                            
                            if (id != -1) {
                                // Reload contacts to update the list
                                loadContacts();
                                
                                dialog.dismiss();
                                Toast.makeText(dialogContext, "Contact added successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(dialogContext, "Failed to add contact", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving contact: " + e.getMessage(), e);
                            Toast.makeText(dialogContext, "Error saving contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    Log.d(TAG, "Showing dialog");
                    dialog.show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing add contact dialog: " + e.getMessage(), e);
                    Toast.makeText(dialogContext, "Error showing dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing add contact dialog: " + e.getMessage(), e);
        }
    }
    
    /**
     * Shows the dialog for editing a contact
     */
    public void showEditContactDialog(Contact contact) {
        try {
            // Use the MapView context for the dialog
            MapView mapView = MapView.getMapView();
            if (mapView == null) {
                Log.e(TAG, "MapView is null");
                Toast.makeText(pluginContext, "Error: Could not access MapView", Toast.LENGTH_SHORT).show();
                return;
            }
            
            final Context dialogContext = mapView.getContext();
            if (dialogContext == null) {
                Log.e(TAG, "MapView context is null");
                Toast.makeText(pluginContext, "Error: Could not create dialog context", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Make sure the dialog is created on the UI thread
            mapView.post(() -> {
                try {
                    // Inflate dialog layout
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
                    View dialogView = LayoutInflater.from(pluginContext).inflate(R.layout.dialog_add_edit_contact, null);
                    builder.setView(dialogView);
                    
                    // Reference UI elements
                    TextView titleTextView = dialogView.findViewById(R.id.tv_dialog_title);
                    EditText nameEditText = dialogView.findViewById(R.id.et_contact_name);
                    EditText phoneEditText = dialogView.findViewById(R.id.et_contact_phone);
                    EditText notesEditText = dialogView.findViewById(R.id.et_contact_notes);
                    Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
                    Button saveButton = dialogView.findViewById(R.id.btn_save);
                    
                    // Location UI elements
                    Button currentLocationButton = dialogView.findViewById(R.id.btn_current_location);
                    Button enterCoordinatesButton = dialogView.findViewById(R.id.btn_enter_coordinates);
                    LinearLayout coordinatesLayout = dialogView.findViewById(R.id.layout_coordinates);
                    EditText latitudeEditText = dialogView.findViewById(R.id.et_latitude);
                    EditText longitudeEditText = dialogView.findViewById(R.id.et_longitude);
                    Button clearLocationButton = dialogView.findViewById(R.id.btn_clear_location);
                    
                    // Title change
                    titleTextView.setText("Edit Contact");
                    
                    // Fill contact data
                    nameEditText.setText(contact.getName());
                    phoneEditText.setText(contact.getPhoneNumber());
                    notesEditText.setText(contact.getNotes());
                    
                    // Set temporary location variables
                    tempLatitude = contact.getLatitude();
                    tempLongitude = contact.getLongitude();
                    
                    // Show location data if available
                    if (contact.hasLocation()) {
                        coordinatesLayout.setVisibility(View.VISIBLE);
                        latitudeEditText.setText(String.valueOf(contact.getLatitude()));
                        longitudeEditText.setText(String.valueOf(contact.getLongitude()));
                        } else {
                        coordinatesLayout.setVisibility(View.GONE);
                    }
                    
                    // Create dialog
                    final AlertDialog dialog = builder.create();
                    
                    // Current location button
                    currentLocationButton.setOnClickListener(v -> {
                        getCurrentLocation();
                        if (tempLatitude != null && tempLongitude != null) {
                            // Show coordinates
                            coordinatesLayout.setVisibility(View.VISIBLE);
                            latitudeEditText.setText(String.valueOf(tempLatitude));
                            longitudeEditText.setText(String.valueOf(tempLongitude));
                    } else {
                            Toast.makeText(dialogContext, "Could not get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    // Enter coordinates button
                    enterCoordinatesButton.setOnClickListener(v -> {
                        showCoordinatesInputDialog(coordinates -> {
                            if (coordinates != null && coordinates.length == 2) {
                                tempLatitude = coordinates[0];
                                tempLongitude = coordinates[1];
                                
                                // Show coordinates
                                coordinatesLayout.setVisibility(View.VISIBLE);
                                latitudeEditText.setText(String.valueOf(tempLatitude));
                                longitudeEditText.setText(String.valueOf(tempLongitude));
                            }
                        });
                    });
                    
                    // Clear location button
                    clearLocationButton.setOnClickListener(v -> {
                        tempLatitude = null;
                        tempLongitude = null;
                        coordinatesLayout.setVisibility(View.GONE);
                    });
                    
                    // Monitor coordinate fields
                    latitudeEditText.addTextChangedListener(new android.text.TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        
                        @Override
                        public void afterTextChanged(android.text.Editable s) {
                            try {
                                if (!TextUtils.isEmpty(s)) {
                                    tempLatitude = Double.parseDouble(s.toString());
                        } else {
                                    tempLatitude = null;
                                }
                            } catch (NumberFormatException e) {
                                tempLatitude = null;
                            }
                        }
                    });
                    
                    longitudeEditText.addTextChangedListener(new android.text.TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        
                        @Override
                        public void afterTextChanged(android.text.Editable s) {
                            try {
                                if (!TextUtils.isEmpty(s)) {
                                    tempLongitude = Double.parseDouble(s.toString());
                                } else {
                                    tempLongitude = null;
                                }
                            } catch (NumberFormatException e) {
                                tempLongitude = null;
                            }
                        }
                    });
                    
                    // Cancel button
                    cancelButton.setOnClickListener(v -> dialog.dismiss());
                    
                    // Save button
                    saveButton.setOnClickListener(v -> {
                        try {
                            // Validate inputs
                            String name = nameEditText.getText().toString().trim();
                            String phone = phoneEditText.getText().toString().trim();
                            String notes = notesEditText.getText().toString().trim();
                            
                            if (TextUtils.isEmpty(name)) {
                                Toast.makeText(dialogContext, "Please enter a name", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Debug output for location data
                            Log.d(TAG, "Updating contact with location data - tempLatitude: " + tempLatitude + ", tempLongitude: " + tempLongitude);
                            
                            // Update contact
                            contact.setName(name);
                            contact.setPhoneNumber(phone);
                            contact.setNotes(notes);
                            
                            // Update location
                            if (tempLatitude != null && tempLongitude != null) {
                                contact.setLocation(tempLatitude, tempLongitude);
                                Log.d(TAG, "Setting location: " + tempLatitude + ", " + tempLongitude);
                                Log.d(TAG, "Contact has location: " + contact.hasLocation());
                            } else {
                                contact.clearLocation();
                                Log.d(TAG, "Clearing location");
                            }
                            
                            // Update contact in database
                            Log.d(TAG, "Updating contact in database: " + contact.toString());
                            int result = dbHelper.updateContact(contact);
                            Log.d(TAG, "Database update result: " + result);
                            
                            if (result > 0) {
                                // Reload contacts to update the list
                                        loadContacts();
                                        
                                        dialog.dismiss();
                                Toast.makeText(dialogContext, "Contact updated successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                Toast.makeText(dialogContext, "Failed to update contact", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating contact: " + e.getMessage(), e);
                            Toast.makeText(dialogContext, "Error updating contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    dialog.show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing edit contact dialog: " + e.getMessage(), e);
                    Toast.makeText(pluginContext, "Error showing dialog", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit contact dialog: " + e.getMessage(), e);
            Toast.makeText(pluginContext, "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Shows the dialog for entering coordinates
     */
    private void showCoordinatesInputDialog(CoordinatesInputListener listener) {
        try {
            // Use the MapView context for the dialog
            MapView mapView = MapView.getMapView();
            if (mapView == null) {
                Log.e(TAG, "MapView is null");
                Toast.makeText(pluginContext, "Error: Could not access MapView", Toast.LENGTH_SHORT).show();
                return;
            }
            
            final Context dialogContext = mapView.getContext();
            if (dialogContext == null) {
                Log.e(TAG, "MapView context is null");
                Toast.makeText(pluginContext, "Error: Could not create dialog context", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Make sure the dialog is created on the UI thread
            mapView.post(() -> {
                try {
                    // Inflate dialog layout
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
                    View dialogView = LayoutInflater.from(pluginContext).inflate(R.layout.dialog_coordinates_input, null);
                    builder.setView(dialogView);
                    
                    // Reference UI elements
                    EditText latitudeEditText = dialogView.findViewById(R.id.et_dialog_latitude);
                    EditText longitudeEditText = dialogView.findViewById(R.id.et_dialog_longitude);
                    Button cancelButton = dialogView.findViewById(R.id.btn_dialog_cancel);
                    Button saveButton = dialogView.findViewById(R.id.btn_dialog_save);
                    
                    // Create dialog
                    final AlertDialog dialog = builder.create();
                    
                    // Cancel button
                    cancelButton.setOnClickListener(v -> dialog.dismiss());
                    
                    // Save button
                    saveButton.setOnClickListener(v -> {
                        try {
                            // Validate inputs
                            String latStr = latitudeEditText.getText().toString().trim();
                            String lonStr = longitudeEditText.getText().toString().trim();
                            
                            if (TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lonStr)) {
                                Toast.makeText(dialogContext, "Please enter both latitude and longitude", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            double latitude = Double.parseDouble(latStr);
                            double longitude = Double.parseDouble(lonStr);
                            
                            // Validate coordinates
                            if (latitude < -90 || latitude > 90) {
                                Toast.makeText(dialogContext, "Latitude must be between -90 and 90", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            if (longitude < -180 || longitude > 180) {
                                Toast.makeText(dialogContext, "Longitude must be between -180 and 180", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Return coordinates
                            listener.onCoordinatesEntered(new Double[] { latitude, longitude });
                    dialog.dismiss();
                        } catch (NumberFormatException e) {
                            Toast.makeText(dialogContext, "Please enter valid coordinates", Toast.LENGTH_SHORT).show();
                }
            });
            
            dialog.show();
        } catch (Exception e) {
                    Log.e(TAG, "Error in UI thread showing coordinates dialog: " + e.getMessage(), e);
                    Toast.makeText(dialogContext, "Error showing dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing coordinates input dialog: " + e.getMessage(), e);
            Toast.makeText(pluginContext, "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Gets the current location
     */
    private void getCurrentLocation() {
        try {
            // Check if location permissions are available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(pluginContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(pluginContext, "Location permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Try to get location from ATAK
            MapView mapView = MapView.getMapView();
            if (mapView != null) {
                Marker selfMarker = mapView.getSelfMarker();
                if (selfMarker != null) {
                    GeoPoint point = selfMarker.getPoint();
                    tempLatitude = point.getLatitude();
                    tempLongitude = point.getLongitude();
                    Log.d(TAG, "Got location from ATAK: " + tempLatitude + ", " + tempLongitude);
                    return;
                }
            }
            
            // Fallback: Try to get location from LocationManager
            LocationManager locationManager = (LocationManager) pluginContext.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                Location lastKnownLocation = null;
                
                // Try GPS location
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                
                // If GPS location is not available, try network location
                if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                
                if (lastKnownLocation != null) {
                    tempLatitude = lastKnownLocation.getLatitude();
                    tempLongitude = lastKnownLocation.getLongitude();
                    Log.d(TAG, "Got location from LocationManager: " + tempLatitude + ", " + tempLongitude);
                    return;
                }
            }
            
            // No location available
            Toast.makeText(pluginContext, "Could not get current location", Toast.LENGTH_SHORT).show();
            tempLatitude = null;
            tempLongitude = null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current location: " + e.getMessage(), e);
            Toast.makeText(pluginContext, "Error getting current location", Toast.LENGTH_SHORT).show();
            tempLatitude = null;
            tempLongitude = null;
        }
    }
    
    /**
     * Interface for coordinate input callback
     */
    private interface CoordinatesInputListener {
        void onCoordinatesEntered(Double[] coordinates);
    }
    
    /**
     * Shows the dialog with contact details
     */
    private void showContactDetailDialog(final Contact contact) {
        try {
            // ATAK MapView Context for dialogs
            Context mapViewContext = MapView.getMapView().getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(mapViewContext);
            
            // Load dialog view
            View dialogView = PluginLayoutInflater.inflate(pluginContext, R.layout.dialog_contact_detail, null);
            
            // Initialize UI elements
            TextView nameView = dialogView.findViewById(R.id.tv_detail_name);
            TextView phoneView = dialogView.findViewById(R.id.tv_detail_phone);
            TextView notesView = dialogView.findViewById(R.id.tv_detail_notes);
            Button editButton = dialogView.findViewById(R.id.btn_edit);
            Button deleteButton = dialogView.findViewById(R.id.btn_delete);
            
            // Location UI elements
            LinearLayout locationLayout = dialogView.findViewById(R.id.layout_location_details);
            LinearLayout locationButtonsLayout = dialogView.findViewById(R.id.layout_location_buttons);
            TextView latitudeView = dialogView.findViewById(R.id.tv_detail_latitude);
            TextView longitudeView = dialogView.findViewById(R.id.tv_detail_longitude);
            Button showOnMapButton = dialogView.findViewById(R.id.btn_show_on_map);
            Button copyCoordinatesButton = dialogView.findViewById(R.id.btn_copy_coordinates);
            
            // Show contact data
            nameView.setText(contact.getName());
            phoneView.setText(contact.getPhoneNumber());
            notesView.setText(contact.getNotes());
            
            // Create dialog - declare before use in listeners
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();
            
            // Show location data if available
            if (contact.hasLocation()) {
                locationLayout.setVisibility(View.VISIBLE);
                locationButtonsLayout.setVisibility(View.VISIBLE);
                latitudeView.setText(String.valueOf(contact.getLatitude()));
                longitudeView.setText(String.valueOf(contact.getLongitude()));
                
                // Long click on latitude to copy
                latitudeView.setOnLongClickListener(v -> {
                    try {
                        // Copy latitude to clipboard
                        ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Latitude", String.valueOf(contact.getLatitude()));
                        clipboard.setPrimaryClip(clip);
                        
                        Toast.makeText(mapViewContext, "Latitude copied to clipboard", Toast.LENGTH_SHORT).show();
                        return true; // Event was handled
                    } catch (Exception e) {
                        Log.e(TAG, "Error copying latitude: " + e.getMessage(), e);
                        return false; // Event was not handled
                    }
                });
                
                // Long click on longitude to copy
                longitudeView.setOnLongClickListener(v -> {
                    try {
                        // Copy longitude to clipboard
                        ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Longitude", String.valueOf(contact.getLongitude()));
                        clipboard.setPrimaryClip(clip);
                        
                        Toast.makeText(mapViewContext, "Longitude copied to clipboard", Toast.LENGTH_SHORT).show();
                        return true; // Event was handled
                    } catch (Exception e) {
                        Log.e(TAG, "Error copying longitude: " + e.getMessage(), e);
                        return false; // Event was not handled
                    }
                });
                
                // "Copy Coordinates" button
                if (copyCoordinatesButton != null) {
                    copyCoordinatesButton.setOnClickListener(v -> {
                        try {
                            // Copy both coordinates to clipboard
                            String coordinates = contact.getLatitude() + "," + contact.getLongitude();
                            ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Coordinates", coordinates);
                            clipboard.setPrimaryClip(clip);
                            
                            Toast.makeText(mapViewContext, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error copying coordinates: " + e.getMessage(), e);
                            Toast.makeText(mapViewContext, "Error copying coordinates", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                // "Show on Map" button activate
                showOnMapButton.setOnClickListener(v -> {
                    try {
                        // Get MapView instance
                        MapView mapView = MapView.getMapView();
                        if (mapView != null) {
                            // Create GeoPoint from coordinates
                            com.atakmap.coremap.maps.coords.GeoPoint point = 
                                new com.atakmap.coremap.maps.coords.GeoPoint(
                                    contact.getLatitude(), 
                                    contact.getLongitude());
                            
                            // Move map to this point
                            mapView.getMapController().panTo(point, true);
                            
                            // Optional: Adjust zoom level
                            mapView.getMapController().zoomTo(0.0002, true);
                            
                            // Create marker and add to map
                            try {
                                // Define marker type for contacts
                                String markerType = "a-f-G-U-C"; // Civilian Contact
                                
                                // Generate marker ID (Contact ID + Name)
                                String markerId = "contact_" + contact.getId() + "_" + 
                                    contact.getName().replaceAll("\\s+", "_");
                                
                                // Create marker
                                Marker marker = new Marker(point, markerType);
                                marker.setMetaString("callsign", contact.getName());
                                marker.setMetaString("type", "Contact");
                                marker.setMetaString("how", "h-g-i-g-o");
                                marker.setMetaBoolean("readiness", true);
                                marker.setMetaString("remarks", contact.getNotes());
                                marker.setMetaString("contact", contact.getPhoneNumber());
                                marker.setMetaString("uid", markerId);
                                marker.setTitle(contact.getName());
                                
                                // Add marker to map
                                mapView.getRootGroup().addItem(marker);
                                
                                Log.d(TAG, "Added marker for contact: " + contact.getName());
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating marker: " + e.getMessage(), e);
                            }
                            
                            // Close dialog
                            dialog.dismiss();
                            
                            // Show feedback
                            Toast.makeText(mapViewContext, 
                                "Showing location for " + contact.getName(), 
                                Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mapViewContext, 
                                "Could not access map view", 
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing location on map: " + e.getMessage(), e);
                        Toast.makeText(mapViewContext, 
                            "Error showing location on map", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Hide location area if no data
                locationLayout.setVisibility(View.GONE);
                locationButtonsLayout.setVisibility(View.GONE);
            }
            
            // Make phone number clickable
            phoneView.setOnClickListener(v -> {
                try {
                    // Format phone number (remove spaces)
                    String phoneNumber = contact.getPhoneNumber().replaceAll("\\s+", "");
                    
                    // Create intent to call
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phoneNumber));
                    
                    // Check if an app can handle the intent
                    if (callIntent.resolveActivity(mapViewContext.getPackageManager()) != null) {
                        mapViewContext.startActivity(callIntent);
                    } else {
                        Toast.makeText(mapViewContext, "No app available to handle calls", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initiating call: " + e.getMessage(), e);
                    Toast.makeText(mapViewContext, "Could not initiate call: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // Long click to copy phone number
            phoneView.setOnLongClickListener(v -> {
                try {
                    // Copy phone number to clipboard
                    ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Phone Number", contact.getPhoneNumber());
                    clipboard.setPrimaryClip(clip);
                    
                    Toast.makeText(mapViewContext, "Phone number copied to clipboard", Toast.LENGTH_SHORT).show();
                    return true; // Event was handled
                } catch (Exception e) {
                    Log.e(TAG, "Error copying phone number: " + e.getMessage(), e);
                    Toast.makeText(mapViewContext, "Could not copy phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return false; // Event was not handled
                }
            });
            
            // Edit button
            editButton.setOnClickListener(v -> {
                dialog.dismiss();
                showEditContactDialog(contact);
            });
            
            // Delete button
            deleteButton.setOnClickListener(v -> {
                // Confirmation dialog
                new AlertDialog.Builder(mapViewContext)
                        .setTitle("Delete Contact")
                        .setMessage("Are you sure you want to delete this contact?")
                        .setPositiveButton("Delete", (dialogInterface, which) -> {
                            try {
                                int result = dbHelper.deleteContact(contact);
                                if (result > 0) {
                                    Log.d(TAG, "Contact deleted successfully, ID: " + contact.getId());
                                    
                                    // Show success message
                                    Toast.makeText(mapViewContext, "Contact deleted", Toast.LENGTH_SHORT).show();
                                    
                                    // Reload contacts to refresh the list
                                    loadContacts();
                                    
                                    // Close the dialog
                                    dialog.dismiss();
                                } else {
                                    Log.e(TAG, "Failed to delete contact, no rows affected");
                                    Toast.makeText(mapViewContext, "Failed to delete contact", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deleting contact", e);
                                Toast.makeText(mapViewContext, "Error deleting contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing contact detail dialog", e);
        }
    }
    
    /**
     * Shows the Info dialog with version information and instructions
     */
    private void showInfoDialog() {
        try {
            // ATAK MapView Context for dialogs
            Context mapViewContext = MapView.getMapView().getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(mapViewContext);
            
            // Load dialog view
            View dialogView = PluginLayoutInflater.inflate(pluginContext, R.layout.dialog_info, null);
            
            // Initialize UI elements
            TextView versionView = dialogView.findViewById(R.id.tv_version);
            TextView publisherView = dialogView.findViewById(R.id.tv_publisher);
            Button closeButton = dialogView.findViewById(R.id.btn_close);
            
            // Set version information
            versionView.setText("1.2.0");
            publisherView.setText("TAKHub");
            
            // Set publisher link
            publisherView.setOnClickListener(v -> {
                try {
                    // URL to TAKHub website
                    String url = "https://takhub.de/en/home/";
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    
                    // Check if an app can handle the intent
                    if (browserIntent.resolveActivity(mapViewContext.getPackageManager()) != null) {
                        mapViewContext.startActivity(browserIntent);
                    } else {
                        Toast.makeText(mapViewContext, "No browser app available", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening website: " + e.getMessage(), e);
                    Toast.makeText(mapViewContext, "Error opening website", Toast.LENGTH_SHORT).show();
                }
            });
            
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();
            
            // Close button
            closeButton.setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing info dialog", e);
        }
    }
    
    /**
     * Implementation of ContactAdapter.OnContactClickListener Interface
     */
    @Override
    public void onContactClick(Contact contact) {
        showContactDetailDialog(contact);
    }
    
    /**
     * Method to reset the manager when opening again
     * Can be used to clean up and reinitialize
     */
    public void reset() {
        try {
            Log.d(TAG, "Resetting ContactManager");
            
            // Reset search field if available
            try {
                EditText searchEditText = mainView.findViewById(R.id.et_search_contacts);
                if (searchEditText != null) {
                    searchEditText.setText("");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resetting search field: " + e.getMessage(), e);
            }
            
            // Clear contact list
            contactList.clear();
            
            // Reload contacts (this will also reset adapter and UI)
            loadContacts();
            
            // Ensure UI is updated
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            
            // Update UI status
            updateContactsUI();
        } catch (Exception e) {
            Log.e(TAG, "Error resetting ContactManager: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the empty view message based on current state
     */
    private void updateEmptyView() {
        if (emptyView != null) {
            if (contactList == null || contactList.isEmpty()) {
                emptyView.setText("No contacts added yet");
                emptyView.setVisibility(View.VISIBLE);
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.GONE);
                }
            } else {
                emptyView.setVisibility(View.GONE);
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Setter for adapter (used for testing)
     */
    public void setAdapter(ContactAdapter adapter) {
        this.adapter = adapter;
    }
    
    /**
     * Setter for databaseHelper (used for testing)
     */
    public void setDatabaseHelper(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
} 