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

// Neue Imports für Standortfunktionalität
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * Manager-Klasse für Kontakte, der die UI- und Datenbankoperationen koordiniert
 */
public class ContactManager implements ContactAdapter.OnContactClickListener {
    private static final String TAG = "ContactManager";
    
    private final Context pluginContext;
    private final DatabaseHelper dbHelper;
    private final View mainView;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private TextView emptyView;
    private final List<Contact> contactList = new ArrayList<>();
    
    // Temporäre Variablen für Standortinformationen während der Kontakterstellung
    private Double tempLatitude;
    private Double tempLongitude;
    private boolean hasLocationPermission = false;
    
    /**
     * Konstruktor für den ContactManager
     * @param context Der Kontext der Anwendung
     * @param mainView Die Hauptansicht des Plugins
     */
    public ContactManager(Context context, View mainView) {
        try {
            if (context == null) {
                Log.e(TAG, "Context is null in ContactManager constructor");
                // Versuche, einen gültigen Kontext zu bekommen
                context = mainView.getContext();
                if (context == null) {
                    // Als letzten Versuch, versuche den MapView-Kontext
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
     * Initialisiert die UI-Elemente
     */
    private void setupViews() {
        try {
            Button addButton = mainView.findViewById(R.id.btn_add_contact);
            recyclerView = mainView.findViewById(R.id.rv_contacts);
            emptyView = mainView.findViewById(R.id.tv_empty_view);
            EditText searchEditText = mainView.findViewById(R.id.et_search_contacts);
            ImageButton infoButton = mainView.findViewById(R.id.btn_info);
            
            // RecyclerView einrichten
            recyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
            adapter = new ContactAdapter(pluginContext, contactList, this);
            recyclerView.setAdapter(adapter);
            
            // Klick-Listener für den "Add Contact" Button
            addButton.setOnClickListener(v -> showAddContactDialog());
            
            // Klick-Listener für den Info-Button
            infoButton.setOnClickListener(v -> showInfoDialog());
            
            // TextWatcher für die Suche einrichten
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Nicht benötigt
                }
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter die Kontaktliste basierend auf dem Suchtext
                    filterContacts(s.toString());
                }
                
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // Nicht benötigt
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up views", e);
        }
    }
    
    /**
     * Filtert die Kontaktliste basierend auf dem Suchbegriff
     * @param query Suchbegriff
     */
    private void filterContacts(String query) {
        try {
            if (adapter != null) {
                Log.d(TAG, "Filtering contacts with query: '" + query + "'");
                
                // Apply the filter to the adapter
                adapter.filter(query);
                
                boolean isSearching = query != null && !query.isEmpty();
                int resultCount = adapter.getItemCount();
                
                Log.d(TAG, "Filter results: " + resultCount + " contacts found for query: '" + query + "'");
                
                // Anzeigen der leeren Ansicht, wenn keine Suchergebnisse gefunden wurden
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
     * Lädt alle Kontakte aus der Datenbank
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
    private void updateContactsUI() {
        try {
            // Check if we have any contacts to display
            boolean hasContacts = contactList != null && !contactList.isEmpty();
            Log.d(TAG, "Updating UI, has contacts: " + hasContacts + ", count: " + 
                  (contactList != null ? contactList.size() : 0));
            
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
     * Zeigt den Dialog zum Hinzufügen eines neuen Kontakts
     */
    private void showAddContactDialog() {
        try {
            Log.d(TAG, "Showing add contact dialog");
            
            // Verwende den MapView-Kontext für den Dialog, da dieser ein gültiger Activity-Kontext ist
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
            
            // Stelle sicher, dass der Dialog auf dem UI-Thread erstellt wird
            mapView.post(() -> {
                try {
                    // Dialog-Layout inflaten
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
                    
                    // UI-Elemente referenzieren mit Fehlerprüfung
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
            
                    // Standort-UI-Elemente
                    Button currentLocationButton = dialogView.findViewById(R.id.btn_current_location);
                    Button enterCoordinatesButton = dialogView.findViewById(R.id.btn_enter_coordinates);
                    LinearLayout coordinatesLayout = dialogView.findViewById(R.id.layout_coordinates);
                    EditText latitudeEditText = dialogView.findViewById(R.id.et_latitude);
                    EditText longitudeEditText = dialogView.findViewById(R.id.et_longitude);
                    Button clearLocationButton = dialogView.findViewById(R.id.btn_clear_location);
                    
                    // Prüfen, ob alle wichtigen UI-Elemente gefunden wurden
                    if (saveButton == null || cancelButton == null) {
                        Log.e(TAG, "Could not find essential buttons in dialog");
                        Toast.makeText(dialogContext, "Error: Dialog layout is incomplete", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Temporäre Standortvariablen zurücksetzen
                    tempLatitude = null;
                    tempLongitude = null;
                    
                    // Dialog erstellen
                    Log.d(TAG, "Creating dialog");
                    final AlertDialog dialog = builder.create();
                    
                    // Aktuellen Standort-Button
                    if (currentLocationButton != null) {
                        currentLocationButton.setOnClickListener(v -> {
                            try {
                                getCurrentLocation();
                                if (tempLatitude != null && tempLongitude != null && coordinatesLayout != null) {
                                    // Koordinaten anzeigen
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
                    
                    // Koordinaten eingeben-Button
                    if (enterCoordinatesButton != null) {
                        enterCoordinatesButton.setOnClickListener(v -> {
                            try {
                                showCoordinatesInputDialog(coordinates -> {
                                    try {
                                        if (coordinates != null && coordinates.length == 2) {
                                            tempLatitude = coordinates[0];
                                            tempLongitude = coordinates[1];
                                            
                                            // Koordinaten anzeigen
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
                    
                    // Standort löschen-Button
                    if (clearLocationButton != null && coordinatesLayout != null) {
                        clearLocationButton.setOnClickListener(v -> {
                            tempLatitude = null;
                            tempLongitude = null;
                            coordinatesLayout.setVisibility(View.GONE);
                        });
                    }
                    
                    // Koordinaten-Felder überwachen
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
                    
                    // Cancel-Button
            cancelButton.setOnClickListener(v -> dialog.dismiss());
            
                    // Save-Button
            saveButton.setOnClickListener(v -> {
                        try {
                            // Eingaben validieren
                            String name = nameEditText.getText().toString().trim();
                            String phone = phoneEditText != null ? phoneEditText.getText().toString().trim() : "";
                            String notes = notesEditText != null ? notesEditText.getText().toString().trim() : "";
                            
                if (TextUtils.isEmpty(name)) {
                                Toast.makeText(dialogContext, "Please enter a name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                            // Debug-Ausgabe für Standortdaten
                            Log.d(TAG, "Saving contact with location data - tempLatitude: " + tempLatitude + ", tempLongitude: " + tempLongitude);
                            
                            // Kontakt erstellen
                            Contact newContact = new Contact(name, phone, notes);
                            
                            // Standort separat setzen, wenn vorhanden
                            if (tempLatitude != null && tempLongitude != null) {
                                newContact.setLocation(tempLatitude, tempLongitude);
                                Log.d(TAG, "Setting location: " + tempLatitude + ", " + tempLongitude);
                                Log.d(TAG, "Contact has location: " + newContact.hasLocation());
                            }
                            
                            // Kontakt zur Datenbank hinzufügen
                            Log.d(TAG, "Adding contact to database: " + newContact.toString());
                            long id = dbHelper.addContact(newContact);
                            Log.d(TAG, "Database returned ID: " + id);
                            
                            if (id != -1) {
                                // Lade Kontakte neu, um die Liste zu aktualisieren
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
                    Log.e(TAG, "Error in UI thread showing dialog: " + e.getMessage(), e);
                    Toast.makeText(dialogContext, "Error showing dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing add contact dialog: " + e.getMessage(), e);
            try {
                // Versuche, eine detailliertere Fehlermeldung anzuzeigen
                Context context = MapView.getMapView().getContext();
                Toast.makeText(context, "Error showing dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                // Fallback, wenn alles andere fehlschlägt
                Log.e(TAG, "Could not show error toast: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Zeigt den Dialog zum Bearbeiten eines Kontakts
     */
    public void showEditContactDialog(Contact contact) {
        try {
            // Verwende den MapView-Kontext für den Dialog
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
            
            // Stelle sicher, dass der Dialog auf dem UI-Thread erstellt wird
            mapView.post(() -> {
                try {
                    // Dialog-Layout inflaten
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
                    View dialogView = LayoutInflater.from(pluginContext).inflate(R.layout.dialog_add_edit_contact, null);
                    builder.setView(dialogView);
                    
                    // UI-Elemente referenzieren
                    TextView titleTextView = dialogView.findViewById(R.id.tv_dialog_title);
                    EditText nameEditText = dialogView.findViewById(R.id.et_contact_name);
                    EditText phoneEditText = dialogView.findViewById(R.id.et_contact_phone);
                    EditText notesEditText = dialogView.findViewById(R.id.et_contact_notes);
                    Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
                    Button saveButton = dialogView.findViewById(R.id.btn_save);
                    
                    // Standort-UI-Elemente
                    Button currentLocationButton = dialogView.findViewById(R.id.btn_current_location);
                    Button enterCoordinatesButton = dialogView.findViewById(R.id.btn_enter_coordinates);
                    LinearLayout coordinatesLayout = dialogView.findViewById(R.id.layout_coordinates);
                    EditText latitudeEditText = dialogView.findViewById(R.id.et_latitude);
                    EditText longitudeEditText = dialogView.findViewById(R.id.et_longitude);
                    Button clearLocationButton = dialogView.findViewById(R.id.btn_clear_location);
                    
                    // Titel ändern
                    titleTextView.setText("Edit Contact");
                    
                    // Kontaktdaten einfüllen
                    nameEditText.setText(contact.getName());
                    phoneEditText.setText(contact.getPhoneNumber());
                    notesEditText.setText(contact.getNotes());
                    
                    // Temporäre Standortvariablen setzen
                    tempLatitude = contact.getLatitude();
                    tempLongitude = contact.getLongitude();
                    
                    // Standortdaten anzeigen, wenn vorhanden
                    if (contact.hasLocation()) {
                        coordinatesLayout.setVisibility(View.VISIBLE);
                        latitudeEditText.setText(String.valueOf(contact.getLatitude()));
                        longitudeEditText.setText(String.valueOf(contact.getLongitude()));
                        } else {
                        coordinatesLayout.setVisibility(View.GONE);
                    }
                    
                    // Dialog erstellen
                    final AlertDialog dialog = builder.create();
                    
                    // Aktuellen Standort-Button
                    currentLocationButton.setOnClickListener(v -> {
                        getCurrentLocation();
                        if (tempLatitude != null && tempLongitude != null) {
                            // Koordinaten anzeigen
                            coordinatesLayout.setVisibility(View.VISIBLE);
                            latitudeEditText.setText(String.valueOf(tempLatitude));
                            longitudeEditText.setText(String.valueOf(tempLongitude));
                    } else {
                            Toast.makeText(dialogContext, "Could not get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    // Koordinaten eingeben-Button
                    enterCoordinatesButton.setOnClickListener(v -> {
                        showCoordinatesInputDialog(coordinates -> {
                            if (coordinates != null && coordinates.length == 2) {
                                tempLatitude = coordinates[0];
                                tempLongitude = coordinates[1];
                                
                                // Koordinaten anzeigen
                                coordinatesLayout.setVisibility(View.VISIBLE);
                                latitudeEditText.setText(String.valueOf(tempLatitude));
                                longitudeEditText.setText(String.valueOf(tempLongitude));
                            }
                        });
                    });
                    
                    // Standort löschen-Button
                    clearLocationButton.setOnClickListener(v -> {
                        tempLatitude = null;
                        tempLongitude = null;
                        coordinatesLayout.setVisibility(View.GONE);
                    });
                    
                    // Koordinaten-Felder überwachen
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
                    
                    // Cancel-Button
                    cancelButton.setOnClickListener(v -> dialog.dismiss());
                    
                    // Save-Button
                    saveButton.setOnClickListener(v -> {
                        try {
                            // Eingaben validieren
                            String name = nameEditText.getText().toString().trim();
                            String phone = phoneEditText.getText().toString().trim();
                            String notes = notesEditText.getText().toString().trim();
                            
                            if (TextUtils.isEmpty(name)) {
                                Toast.makeText(dialogContext, "Please enter a name", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Debug-Ausgabe für Standortdaten
                            Log.d(TAG, "Updating contact with location data - tempLatitude: " + tempLatitude + ", tempLongitude: " + tempLongitude);
                            
                            // Kontakt aktualisieren
                            contact.setName(name);
                            contact.setPhoneNumber(phone);
                            contact.setNotes(notes);
                            
                            // Standort aktualisieren
                            if (tempLatitude != null && tempLongitude != null) {
                                contact.setLocation(tempLatitude, tempLongitude);
                                Log.d(TAG, "Setting location: " + tempLatitude + ", " + tempLongitude);
                                Log.d(TAG, "Contact has location: " + contact.hasLocation());
                            } else {
                                contact.clearLocation();
                                Log.d(TAG, "Clearing location");
                            }
                            
                            // Kontakt in der Datenbank aktualisieren
                            Log.d(TAG, "Updating contact in database: " + contact.toString());
                            int result = dbHelper.updateContact(contact);
                            Log.d(TAG, "Database update result: " + result);
                            
                            if (result > 0) {
                                // Lade Kontakte neu, um die Liste zu aktualisieren
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
                    Log.e(TAG, "Error in UI thread showing edit dialog: " + e.getMessage(), e);
                    Toast.makeText(dialogContext, "Error showing dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit contact dialog: " + e.getMessage(), e);
            Toast.makeText(pluginContext, "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Zeigt den Dialog zum Eingeben von Koordinaten
     */
    private void showCoordinatesInputDialog(CoordinatesInputListener listener) {
        try {
            // Verwende den MapView-Kontext für den Dialog
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
            
            // Stelle sicher, dass der Dialog auf dem UI-Thread erstellt wird
            mapView.post(() -> {
                try {
                    // Dialog-Layout inflaten
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
                    View dialogView = LayoutInflater.from(pluginContext).inflate(R.layout.dialog_coordinates_input, null);
                    builder.setView(dialogView);
                    
                    // UI-Elemente referenzieren
                    EditText latitudeEditText = dialogView.findViewById(R.id.et_dialog_latitude);
                    EditText longitudeEditText = dialogView.findViewById(R.id.et_dialog_longitude);
                    Button cancelButton = dialogView.findViewById(R.id.btn_dialog_cancel);
                    Button saveButton = dialogView.findViewById(R.id.btn_dialog_save);
                    
                    // Dialog erstellen
                    final AlertDialog dialog = builder.create();
                    
                    // Cancel-Button
                    cancelButton.setOnClickListener(v -> dialog.dismiss());
                    
                    // Save-Button
                    saveButton.setOnClickListener(v -> {
                        try {
                            // Eingaben validieren
                            String latStr = latitudeEditText.getText().toString().trim();
                            String lonStr = longitudeEditText.getText().toString().trim();
                            
                            if (TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lonStr)) {
                                Toast.makeText(dialogContext, "Please enter both latitude and longitude", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            double latitude = Double.parseDouble(latStr);
                            double longitude = Double.parseDouble(lonStr);
                            
                            // Koordinaten validieren
                            if (latitude < -90 || latitude > 90) {
                                Toast.makeText(dialogContext, "Latitude must be between -90 and 90", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            if (longitude < -180 || longitude > 180) {
                                Toast.makeText(dialogContext, "Longitude must be between -180 and 180", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Koordinaten zurückgeben
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
     * Ruft den aktuellen Standort ab
     */
    private void getCurrentLocation() {
        try {
            // Prüfen, ob Standortberechtigungen vorhanden sind
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(pluginContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(pluginContext, "Location permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Versuche, den Standort von ATAK zu bekommen
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
            
            // Fallback: Versuche, den Standort vom LocationManager zu bekommen
            LocationManager locationManager = (LocationManager) pluginContext.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                Location lastKnownLocation = null;
                
                // Versuche GPS-Standort
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                
                // Wenn GPS-Standort nicht verfügbar, versuche Netzwerk-Standort
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
            
            // Kein Standort verfügbar
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
     * Interface für Koordinateneingabe-Callback
     */
    private interface CoordinatesInputListener {
        void onCoordinatesEntered(Double[] coordinates);
    }
    
    /**
     * Zeigt den Dialog mit den Kontaktdetails
     */
    private void showContactDetailDialog(final Contact contact) {
        try {
            // ATAK MapView Context für Dialoge verwenden
            Context mapViewContext = MapView.getMapView().getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(mapViewContext);
            
            // Dialog-View laden
            View dialogView = PluginLayoutInflater.inflate(pluginContext, R.layout.dialog_contact_detail, null);
            
            // UI-Elemente initialisieren
            TextView nameView = dialogView.findViewById(R.id.tv_detail_name);
            TextView phoneView = dialogView.findViewById(R.id.tv_detail_phone);
            TextView notesView = dialogView.findViewById(R.id.tv_detail_notes);
            Button editButton = dialogView.findViewById(R.id.btn_edit);
            Button deleteButton = dialogView.findViewById(R.id.btn_delete);
            
            // Standort-UI-Elemente
            LinearLayout locationLayout = dialogView.findViewById(R.id.layout_location_details);
            LinearLayout locationButtonsLayout = dialogView.findViewById(R.id.layout_location_buttons);
            TextView latitudeView = dialogView.findViewById(R.id.tv_detail_latitude);
            TextView longitudeView = dialogView.findViewById(R.id.tv_detail_longitude);
            Button showOnMapButton = dialogView.findViewById(R.id.btn_show_on_map);
            Button copyCoordinatesButton = dialogView.findViewById(R.id.btn_copy_coordinates);
            
            // Kontaktdaten anzeigen
            nameView.setText(contact.getName());
            phoneView.setText(contact.getPhoneNumber());
            notesView.setText(contact.getNotes());
            
            // Dialog erstellen - vor der Verwendung in Listenern deklarieren
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();
            
            // Standortdaten anzeigen, wenn vorhanden
            if (contact.hasLocation()) {
                locationLayout.setVisibility(View.VISIBLE);
                locationButtonsLayout.setVisibility(View.VISIBLE);
                latitudeView.setText(String.valueOf(contact.getLatitude()));
                longitudeView.setText(String.valueOf(contact.getLongitude()));
                
                // Langes Klicken auf Latitude zum Kopieren
                latitudeView.setOnLongClickListener(v -> {
                    try {
                        // Latitude in die Zwischenablage kopieren
                        ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Latitude", String.valueOf(contact.getLatitude()));
                        clipboard.setPrimaryClip(clip);
                        
                        Toast.makeText(mapViewContext, "Latitude copied to clipboard", Toast.LENGTH_SHORT).show();
                        return true; // Event wurde behandelt
                    } catch (Exception e) {
                        Log.e(TAG, "Error copying latitude: " + e.getMessage(), e);
                        return false; // Event wurde nicht behandelt
                    }
                });
                
                // Langes Klicken auf Longitude zum Kopieren
                longitudeView.setOnLongClickListener(v -> {
                    try {
                        // Longitude in die Zwischenablage kopieren
                        ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Longitude", String.valueOf(contact.getLongitude()));
                        clipboard.setPrimaryClip(clip);
                        
                        Toast.makeText(mapViewContext, "Longitude copied to clipboard", Toast.LENGTH_SHORT).show();
                        return true; // Event wurde behandelt
                    } catch (Exception e) {
                        Log.e(TAG, "Error copying longitude: " + e.getMessage(), e);
                        return false; // Event wurde nicht behandelt
                    }
                });
                
                // "Copy Coordinates"-Button
                if (copyCoordinatesButton != null) {
                    copyCoordinatesButton.setOnClickListener(v -> {
                        try {
                            // Beide Koordinaten in die Zwischenablage kopieren
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
                
                // "Show on Map"-Button aktivieren
                showOnMapButton.setOnClickListener(v -> {
                    try {
                        // MapView-Instanz holen
                        MapView mapView = MapView.getMapView();
                        if (mapView != null) {
                            // GeoPoint aus den Koordinaten erstellen
                            com.atakmap.coremap.maps.coords.GeoPoint point = 
                                new com.atakmap.coremap.maps.coords.GeoPoint(
                                    contact.getLatitude(), 
                                    contact.getLongitude());
                            
                            // Karte zu diesem Punkt bewegen
                            mapView.getMapController().panTo(point, true);
                            
                            // Optional: Zoom-Level anpassen
                            mapView.getMapController().zoomTo(0.0002, true);
                            
                            // Marker erstellen und zur Karte hinzufügen
                            try {
                                // Marker-Typ für Kontakte definieren
                                String markerType = "a-f-G-U-C"; // Civilian Contact
                                
                                // Marker-ID generieren (Kontakt-ID + Name)
                                String markerId = "contact_" + contact.getId() + "_" + 
                                    contact.getName().replaceAll("\\s+", "_");
                                
                                // Marker erstellen
                                Marker marker = new Marker(point, markerType);
                                marker.setMetaString("callsign", contact.getName());
                                marker.setMetaString("type", "Contact");
                                marker.setMetaString("how", "h-g-i-g-o");
                                marker.setMetaBoolean("readiness", true);
                                marker.setMetaString("remarks", contact.getNotes());
                                marker.setMetaString("contact", contact.getPhoneNumber());
                                marker.setMetaString("uid", markerId);
                                marker.setTitle(contact.getName());
                                
                                // Marker zur Karte hinzufügen
                                mapView.getRootGroup().addItem(marker);
                                
                                Log.d(TAG, "Added marker for contact: " + contact.getName());
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating marker: " + e.getMessage(), e);
                            }
                            
                            // Dialog schließen
                            dialog.dismiss();
                            
                            // Feedback anzeigen
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
                // Standortbereich ausblenden, wenn keine Daten vorhanden sind
                locationLayout.setVisibility(View.GONE);
                locationButtonsLayout.setVisibility(View.GONE);
            }
            
            // Telefonnummer anklickbar machen
            phoneView.setOnClickListener(v -> {
                try {
                    // Telefonnummer formatieren (Leerzeichen entfernen)
                    String phoneNumber = contact.getPhoneNumber().replaceAll("\\s+", "");
                    
                    // Intent zum Anrufen erstellen
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phoneNumber));
                    
                    // Prüfen, ob eine App den Intent behandeln kann
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
            
            // Langes Klicken zum Kopieren der Telefonnummer
            phoneView.setOnLongClickListener(v -> {
                try {
                    // Telefonnummer in die Zwischenablage kopieren
                    ClipboardManager clipboard = (ClipboardManager) mapViewContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Phone Number", contact.getPhoneNumber());
                    clipboard.setPrimaryClip(clip);
                    
                    Toast.makeText(mapViewContext, "Phone number copied to clipboard", Toast.LENGTH_SHORT).show();
                    return true; // Event wurde behandelt
                } catch (Exception e) {
                    Log.e(TAG, "Error copying phone number: " + e.getMessage(), e);
                    Toast.makeText(mapViewContext, "Could not copy phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return false; // Event wurde nicht behandelt
                }
            });
            
            // Edit-Button
            editButton.setOnClickListener(v -> {
                dialog.dismiss();
                showEditContactDialog(contact);
            });
            
            // Delete-Button
            deleteButton.setOnClickListener(v -> {
                // Bestätigungsdialog anzeigen
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
     * Zeigt den Info-Dialog mit Versionsinformationen und Anleitung
     */
    private void showInfoDialog() {
        try {
            // ATAK MapView Context für Dialoge verwenden
            Context mapViewContext = MapView.getMapView().getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(mapViewContext);
            
            // Dialog-View laden
            View dialogView = PluginLayoutInflater.inflate(pluginContext, R.layout.dialog_info, null);
            
            // UI-Elemente initialisieren
            TextView versionView = dialogView.findViewById(R.id.tv_version);
            TextView publisherView = dialogView.findViewById(R.id.tv_publisher);
            Button closeButton = dialogView.findViewById(R.id.btn_close);
            
            // Versionsinformationen setzen
            versionView.setText("1.2.0");
            publisherView.setText("TAKHub");
            
            // Publisher-Link einrichten
            publisherView.setOnClickListener(v -> {
                try {
                    // URL zur TAKHub-Website
                    String url = "https://takhub.de/en/home/";
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    
                    // Prüfen, ob eine App den Intent behandeln kann
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
            
            // Close-Button
            closeButton.setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing info dialog", e);
        }
    }
    
    /**
     * Implementierung des ContactAdapter.OnContactClickListener Interface
     */
    @Override
    public void onContactClick(Contact contact) {
        showContactDetailDialog(contact);
    }
    
    /**
     * Methode zum Zurücksetzen des Managers beim erneuten Öffnen
     * Kann verwendet werden, um Ressourcen zu bereinigen und neu zu initialisieren
     */
    public void reset() {
        try {
            Log.d(TAG, "Resetting ContactManager");
            
            // Suchfeld zurücksetzen, falls vorhanden
            try {
                EditText searchEditText = mainView.findViewById(R.id.et_search_contacts);
                if (searchEditText != null) {
                    searchEditText.setText("");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resetting search field: " + e.getMessage(), e);
            }
            
            // Kontaktliste leeren
            contactList.clear();
            
            // Lade Kontakte neu (this will also reset adapter and UI)
            loadContacts();
            
            // Stelle sicher, dass die UI aktualisiert wird
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            
            // UI-Status aktualisieren
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
} 