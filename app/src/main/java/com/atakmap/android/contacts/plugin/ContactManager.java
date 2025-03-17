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

import java.util.ArrayList;
import java.util.List;

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
            
            // Clear any search text
            try {
                EditText searchEditText = mainView.findViewById(R.id.et_search_contacts);
                if (searchEditText != null) {
                    searchEditText.setText("");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resetting search field: " + e.getMessage(), e);
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
        showContactDialog(null);
    }
    
    /**
     * Zeigt Dialog zum Hinzufügen oder Bearbeiten eines Kontakts
     * @param contact Wenn nicht null, wird der Kontakt bearbeitet, ansonsten wird ein neuer erstellt
     */
    private void showContactDialog(final Contact contact) {
        try {
            final boolean isEdit = contact != null;
            
            // ATAK MapView Context für Dialoge verwenden
            Context mapViewContext = MapView.getMapView().getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(mapViewContext);
            
            // Dialog-View laden
            View dialogView = PluginLayoutInflater.inflate(pluginContext, R.layout.dialog_add_edit_contact, null);
            
            // UI-Elemente initialisieren
            TextView titleView = dialogView.findViewById(R.id.tv_dialog_title);
            EditText nameEdit = dialogView.findViewById(R.id.et_name);
            EditText countryCodeEdit = dialogView.findViewById(R.id.et_country_code);
            EditText phoneEdit = dialogView.findViewById(R.id.et_phone);
            EditText notesEdit = dialogView.findViewById(R.id.et_notes);
            Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
            Button saveButton = dialogView.findViewById(R.id.btn_save);
            
            // Titel anpassen je nach Modus (Hinzufügen/Bearbeiten)
            if (isEdit) {
                titleView.setText("Edit Contact");
                nameEdit.setText(contact.getName());
                
                // Telefonnummer in Ländervorwahl und Nummer aufteilen
                String phoneNumber = contact.getPhoneNumber();
                if (phoneNumber != null && phoneNumber.startsWith("+")) {
                    // Finde das erste Leerzeichen oder den ersten Nicht-Ziffern-Charakter nach dem +
                    int spaceIndex = -1;
                    for (int i = 1; i < phoneNumber.length(); i++) {
                        if (!Character.isDigit(phoneNumber.charAt(i))) {
                            spaceIndex = i;
                            break;
                        }
                    }
                    
                    if (spaceIndex > 1) {
                        // Teile die Nummer in Ländervorwahl und Telefonnummer
                        countryCodeEdit.setText(phoneNumber.substring(0, spaceIndex));
                        phoneEdit.setText(phoneNumber.substring(spaceIndex).trim());
                    } else {
                        // Wenn kein Leerzeichen gefunden wurde, zeige die gesamte Nummer im Telefonnummernfeld
                        countryCodeEdit.setText("+");
                        phoneEdit.setText(phoneNumber.substring(1));
                    }
                } else {
                    // Wenn keine Ländervorwahl vorhanden ist, setze Standardwert
                    countryCodeEdit.setText("+49");
                    phoneEdit.setText(phoneNumber);
                }
                
                notesEdit.setText(contact.getNotes());
            } else {
                titleView.setText("Add Contact");
                // Standardwert für Ländervorwahl setzen
                countryCodeEdit.setText("+49");
            }
            
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();
            
            // Abbrechen-Button
            cancelButton.setOnClickListener(v -> dialog.dismiss());
            
            // Speichern-Button
            saveButton.setOnClickListener(v -> {
                String name = nameEdit.getText().toString().trim();
                String countryCode = countryCodeEdit.getText().toString().trim();
                String phoneLocal = phoneEdit.getText().toString().trim();
                String notes = notesEdit.getText().toString().trim();
                
                // Validierung
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(mapViewContext, "Please enter a name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Sicherstellen, dass die Ländervorwahl mit + beginnt
                if (!countryCode.startsWith("+")) {
                    countryCode = "+" + countryCode;
                }
                
                // Kombiniere Ländervorwahl und Telefonnummer
                String fullPhoneNumber = countryCode + " " + phoneLocal;
                
                Log.d(TAG, "Attempting to save contact - Name: " + name + ", Phone: " + fullPhoneNumber);
                
                try {
                    if (isEdit) {
                        // Kontakt aktualisieren
                        contact.setName(name);
                        contact.setPhoneNumber(fullPhoneNumber);
                        contact.setNotes(notes);
                        Log.d(TAG, "Updating existing contact with ID: " + contact.getId());
                        int result = dbHelper.updateContact(contact);
                        Log.d(TAG, "Contact updated, rows affected: " + result);
                        
                        if (result > 0) {
                            // Show success message
                            Toast.makeText(mapViewContext, "Contact updated", Toast.LENGTH_SHORT).show();
                            
                            // Reload contacts to refresh the list
                            loadContacts();
                            
                            // Close the dialog
                            dialog.dismiss();
                        } else {
                            Log.e(TAG, "Failed to update contact, no rows affected");
                            Toast.makeText(mapViewContext, "Failed to update contact", Toast.LENGTH_SHORT).show();
                            return; // Don't dismiss dialog on failure
                        }
                    } else {
                        // Neuen Kontakt erstellen
                        Contact newContact = new Contact(name, fullPhoneNumber, notes);
                        
                        // Kontakt in Datenbank speichern
                        long id = dbHelper.addContact(newContact);
                        
                        if (id > 0) {
                            newContact.setId(id);
                            
                            // Don't directly add to contactList, we'll reload to ensure consistency
                            Log.d(TAG, "Contact added successfully with ID: " + id);
                            
                            // Show success message
                            Toast.makeText(mapViewContext, "Contact added", Toast.LENGTH_SHORT).show();
                            
                            // Reload contacts to refresh the list
                            loadContacts();
                            
                            // Close the dialog
                            dialog.dismiss();
                            return;
                        } else {
                            Log.e(TAG, "Failed to add contact, ID returned: " + id);
                            
                            // Alternative approach if the first method fails
                            try {
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                if (db != null && db.isOpen()) {
                                    ContentValues values = new ContentValues();
                                    values.put(DatabaseHelper.KEY_NAME, name);
                                    values.put(DatabaseHelper.KEY_PHONE, fullPhoneNumber);
                                    values.put(DatabaseHelper.KEY_NOTES, notes);
                                    
                                    id = db.insert(DatabaseHelper.TABLE_CONTACTS, null, values);
                                    
                                    if (id > 0) {
                                        newContact.setId(id);
                                        Log.d(TAG, "Contact added with alternative method, ID: " + id);
                                        
                                        // Show success message
                                        Toast.makeText(mapViewContext, "Contact added with alternative method", Toast.LENGTH_SHORT).show();
                                        
                                        // Reload contacts to refresh the list
                                        loadContacts();
                                        
                                        // Close the dialog
                                        dialog.dismiss();
                                        return;
                                    } else {
                                        Toast.makeText(mapViewContext, "Failed to add contact", Toast.LENGTH_SHORT).show();
                                        return; // Don't dismiss dialog on failure
                                    }
                                } else {
                                    Log.e(TAG, "Database is not accessible in alternative approach");
                                    Toast.makeText(mapViewContext, "Failed to add contact: Database not accessible", Toast.LENGTH_SHORT).show();
                                    return; // Don't dismiss dialog on failure
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error in alternative approach: " + ex.getMessage(), ex);
                                Toast.makeText(mapViewContext, "Failed to add contact: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                return; // Don't dismiss dialog on failure
                            }
                        }
                    }
                    
                    // Kontakte neu laden und Dialog schließen
                    loadContacts();
                    dialog.dismiss();
                } catch (Exception e) {
                    Log.e(TAG, "Error saving contact: " + e.getMessage(), e);
                    Toast.makeText(mapViewContext, "Error saving contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing contact dialog", e);
        }
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
            
            // Kontaktdaten anzeigen
            nameView.setText(contact.getName());
            phoneView.setText(contact.getPhoneNumber());
            notesView.setText(contact.getNotes());
            
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
            
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();
            
            // Edit-Button
            editButton.setOnClickListener(v -> {
                dialog.dismiss();
                showContactDialog(contact);
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
     * Zeigt den Info-Dialog mit Versionsinformationen und Herausgeber
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
            versionView.setText("1.1.0");
            publisherView.setText("TAKHub");
            
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
            
            // Lade Kontakte neu (this will also reset adapter and UI)
            loadContacts();
        } catch (Exception e) {
            Log.e(TAG, "Error resetting ContactManager: " + e.getMessage(), e);
        }
    }
} 