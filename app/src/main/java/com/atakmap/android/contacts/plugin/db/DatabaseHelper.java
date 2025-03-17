package com.atakmap.android.contacts.plugin.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.atakmap.android.contacts.plugin.model.Contact;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Datenbank-Helper für die Contacts-Datenbank
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ContactsDB";
    
    // Datenbankversion
    private static final int DATABASE_VERSION = 1;
    
    // Datenbankname
    private static final String DATABASE_NAME = "contacts_db";
    
    // Tabellennamen
    public static final String TABLE_CONTACTS = "contacts";
    
    // Spaltennamen
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_NOTES = "notes";
    
    // SQL-Anweisung zum Erstellen der Tabelle
    private static final String CREATE_TABLE_CONTACTS = "CREATE TABLE " + TABLE_CONTACTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_NAME + " TEXT,"
            + KEY_PHONE + " TEXT,"
            + KEY_NOTES + " TEXT" + ")";
    
    private static DatabaseHelper instance;
    private String dbPath;
    
    /**
     * Singleton-Pattern für den Datenbankzugriff
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            try {
                instance = new DatabaseHelper(context.getApplicationContext());
                Log.d(TAG, "DatabaseHelper instance created");
            } catch (Exception e) {
                Log.e(TAG, "Error creating DatabaseHelper instance", e);
                throw e;
            }
        }
        return instance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DatabaseHelper constructor called");
        
        // Log database path
        try {
            if (context != null) {
                try {
                    dbPath = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
                    Log.d(TAG, "Database path from context: " + dbPath);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting database path from context: " + e.getMessage());
                    // Fallback: Verwende externen Speicher
                    dbPath = getAlternativeDatabasePath(context);
                }
            } else {
                Log.e(TAG, "Context is null, using fallback path");
                dbPath = getAlternativeDatabasePath(null);
            }
            
            // Check if parent directory exists
            File dbFile = new File(dbPath);
            File dbDir = dbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                boolean created = dbDir.mkdirs();
                Log.d(TAG, "Created database directory: " + created);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up database path: " + e.getMessage(), e);
        }
    }
    
    /**
     * Erzeugt einen alternativen Pfad für die Datenbank, falls der normale Pfad nicht verfügbar ist
     */
    private String getAlternativeDatabasePath(Context context) {
        try {
            // Versuche externen Speicher zu verwenden
            File externalDir;
            
            if (context != null) {
                // Versuche app-spezifischen externen Speicher
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    File[] externalDirs = context.getExternalFilesDirs(null);
                    if (externalDirs != null && externalDirs.length > 0 && externalDirs[0] != null) {
                        externalDir = new File(externalDirs[0], "databases");
                        if (!externalDir.exists()) {
                            externalDir.mkdirs();
                        }
                        String path = new File(externalDir, DATABASE_NAME).getAbsolutePath();
                        Log.d(TAG, "Using app-specific external storage: " + path);
                        return path;
                    }
                }
            }
            
            // Fallback: Verwende Standard-externen Speicher
            externalDir = new File(android.os.Environment.getExternalStorageDirectory(), "ATAK/plugins/contacts/databases");
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
            String path = new File(externalDir, DATABASE_NAME).getAbsolutePath();
            Log.d(TAG, "Using external storage fallback: " + path);
            return path;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating alternative database path: " + e.getMessage(), e);
            
            // Letzter Fallback: Verwende temporären Speicher
            String tempPath = System.getProperty("java.io.tmpdir");
            if (tempPath == null) {
                tempPath = "/data/local/tmp";
            }
            String path = new File(tempPath, DATABASE_NAME).getAbsolutePath();
            Log.d(TAG, "Using temp directory fallback: " + path);
            return path;
        }
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Tabellen erstellen
            Log.d(TAG, "Creating database with SQL: " + CREATE_TABLE_CONTACTS);
            db.execSQL(CREATE_TABLE_CONTACTS);
            Log.d(TAG, "Database created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating database: " + e.getMessage(), e);
            throw e; // Rethrow zur sofortigen Fehlererkennung
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            // Bei Upgrade alte Tabelle löschen und neu erstellen
            Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
            onCreate(db);
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database", e);
        }
    }
    
    /**
     * Fügt einen neuen Kontakt in die Datenbank ein
     * @return ID des neuen Kontakts oder -1 bei Fehler
     */
    public long addContact(Contact contact) {
        long id = -1;
        SQLiteDatabase db = null;
        try {
            Log.d(TAG, "Starting to add contact: " + contact.getName());
            db = this.getWritableDatabase();
            if (db == null) {
                Log.e(TAG, "Failed to get writable database");
                return -1;
            }
            
            if (!db.isOpen()) {
                Log.e(TAG, "Database is not open");
                return -1;
            }
            
            // Check if table exists
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_CONTACTS + "'", null);
                if (cursor != null && cursor.getCount() == 0) {
                    Log.e(TAG, "Table " + TABLE_CONTACTS + " does not exist");
                    // Try to recreate the table
                    db.execSQL(CREATE_TABLE_CONTACTS);
                    Log.d(TAG, "Recreated table " + TABLE_CONTACTS);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking if table exists: " + e.getMessage(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            
            ContentValues values = new ContentValues();
            
            values.put(KEY_NAME, contact.getName());
            values.put(KEY_PHONE, contact.getPhoneNumber());
            values.put(KEY_NOTES, contact.getNotes());
            
            Log.d(TAG, "Prepared values for contact: " + values.toString());
            
            // Datenbank seriell für thread-safety zugreifen
            db.beginTransaction();
            try {
                // Einfügen
                id = db.insert(TABLE_CONTACTS, null, values);
                if (id != -1) {
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Contact inserted successfully with ID: " + id);
                } else {
                    Log.e(TAG, "Insert operation returned -1, insertion failed");
                    
                    // Check if there's a specific SQLite error
                    try {
                        db.execSQL("SELECT * FROM " + TABLE_CONTACTS + " LIMIT 1");
                        Log.d(TAG, "Table seems to be accessible, but insert failed");
                    } catch (Exception e) {
                        Log.e(TAG, "Error accessing table: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during transaction: " + e.getMessage(), e);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding contact: " + e.getMessage(), e);
        }
        return id;
    }
    
    /**
     * Einen Kontakt anhand seiner ID abrufen
     */
    public Contact getContact(long id) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            
            Cursor cursor = db.query(TABLE_CONTACTS, 
                    new String[] { KEY_ID, KEY_NAME, KEY_PHONE, KEY_NOTES },
                    KEY_ID + "=?", new String[] { String.valueOf(id) }, 
                    null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                Contact contact = new Contact(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3));
                cursor.close();
                return contact;
            }
            
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact with id " + id, e);
        }
        
        return null;
    }
    
    /**
     * Alle Kontakte abrufen
     */
    public List<Contact> getAllContacts() {
        List<Contact> contactList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            Log.d(TAG, "Starting getAllContacts");
            db = this.getReadableDatabase();
            if (db == null || !db.isOpen()) {
                Log.e(TAG, "Failed to get readable database or database is not open");
                return contactList;
            }
            
            // Check if table exists
            try {
                Cursor tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_CONTACTS + "'", null);
                boolean tableExists = tableCheck != null && tableCheck.getCount() > 0;
                Log.d(TAG, "Table check result: exists=" + tableExists + 
                      ", count=" + (tableCheck != null ? tableCheck.getCount() : 0));
                if (tableCheck != null) {
                    tableCheck.close();
                }
                
                if (!tableExists) {
                    Log.e(TAG, "Table " + TABLE_CONTACTS + " does not exist");
                    // Try to recreate the table
                    try {
                        db.execSQL(CREATE_TABLE_CONTACTS);
                        Log.d(TAG, "Recreated table " + TABLE_CONTACTS);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to recreate table: " + e.getMessage(), e);
                        return contactList;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking if table exists: " + e.getMessage(), e);
                return contactList;
            }
            
            // Alle Kontakte abrufen
            String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " ORDER BY " + KEY_NAME + " COLLATE NOCASE ASC";
            Log.d(TAG, "Executing query: " + selectQuery);
            
            cursor = db.rawQuery(selectQuery, null);
            int cursorCount = cursor != null ? cursor.getCount() : 0;
            Log.d(TAG, "Query returned " + cursorCount + " rows");
            
            // Durch alle Zeilen iterieren und zur Liste hinzufügen
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(KEY_ID);
                int nameIndex = cursor.getColumnIndex(KEY_NAME);
                int phoneIndex = cursor.getColumnIndex(KEY_PHONE);
                int notesIndex = cursor.getColumnIndex(KEY_NOTES);
                
                Log.d(TAG, "Column indices - id:" + idIndex + ", name:" + nameIndex + 
                      ", phone:" + phoneIndex + ", notes:" + notesIndex);
                
                // Check if columns exist
                if (idIndex >= 0 && nameIndex >= 0 && phoneIndex >= 0 && notesIndex >= 0) {
                    do {
                        try {
                            long id = cursor.getLong(idIndex);
                            String name = cursor.getString(nameIndex);
                            String phone = cursor.getString(phoneIndex);
                            String notes = cursor.getString(notesIndex);
                            
                            Contact contact = new Contact(id, name, phone, notes);
                            contactList.add(contact);
                            
                            Log.d(TAG, "Added contact - id:" + id + ", name:" + name + 
                                  ", phone:" + phone);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing contact at cursor position: " + cursor.getPosition(), e);
                        }
                    } while (cursor.moveToNext());
                } else {
                    Log.e(TAG, "One or more required columns not found in table");
                }
                
                Log.d(TAG, "Loaded " + contactList.size() + " contacts from database");
            } else {
                Log.d(TAG, "No contacts found in database or cursor is null/empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all contacts: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                    Log.d(TAG, "Cursor closed successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor: " + e.getMessage(), e);
                }
            }
        }
        
        return contactList;
    }
    
    /**
     * Einen Kontakt aktualisieren
     * @return Anzahl der aktualisierten Zeilen
     */
    public int updateContact(Contact contact) {
        int result = 0;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, contact.getName());
            values.put(KEY_PHONE, contact.getPhoneNumber());
            values.put(KEY_NOTES, contact.getNotes());
            
            // Serieller Zugriff für thread-safety
            db.beginTransaction();
            try {
                // Aktualisieren
                result = db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
                        new String[] { String.valueOf(contact.getId()) });
                db.setTransactionSuccessful();
                Log.d(TAG, "Updated contact with ID " + contact.getId() + ", rows affected: " + result);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating contact with id " + contact.getId() + ": " + e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * Einen Kontakt löschen
     * @return Anzahl der gelöschten Zeilen
     */
    public int deleteContact(Contact contact) {
        int result = 0;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            
            // Serieller Zugriff für thread-safety
            db.beginTransaction();
            try {
                result = db.delete(TABLE_CONTACTS, KEY_ID + " = ?",
                        new String[] { String.valueOf(contact.getId()) });
                db.setTransactionSuccessful();
                Log.d(TAG, "Deleted contact with ID: " + contact.getId() + ", rows affected: " + result);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting contact with id " + contact.getId() + ": " + e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * Anzahl der Kontakte abrufen
     */
    public int getContactsCount() {
        int count = 0;
        try {
            String countQuery = "SELECT * FROM " + TABLE_CONTACTS;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(countQuery, null);
            count = cursor.getCount();
            cursor.close();
            Log.d(TAG, "Contact count: " + count);
        } catch (Exception e) {
            Log.e(TAG, "Error getting contacts count", e);
        }
        
        return count;
    }
    
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (dbPath != null) {
            try {
                File dbFile = new File(dbPath);
                File dbDir = dbFile.getParentFile();
                
                if (dbDir != null && !dbDir.exists()) {
                    boolean created = dbDir.mkdirs();
                    Log.d(TAG, "Created database directory: " + created);
                }
                
                // Prüfen, ob die Datenbank existiert
                if (!dbFile.exists()) {
                    // Wenn die Datenbank nicht existiert, erstellen wir sie
                    SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
                    onCreate(db);
                    Log.d(TAG, "Created new database at: " + dbPath);
                    return db;
                } else {
                    // Wenn die Datenbank existiert, öffnen wir sie
                    Log.d(TAG, "Opening existing database at: " + dbPath);
                    return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening writable database at " + dbPath + ": " + e.getMessage(), e);
            }
        }
        
        Log.d(TAG, "Falling back to default getWritableDatabase()");
        return super.getWritableDatabase();
    }
    
    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        if (dbPath != null) {
            try {
                File dbFile = new File(dbPath);
                
                // Prüfen, ob die Datenbank existiert
                if (dbFile.exists()) {
                    // Wenn die Datenbank existiert, öffnen wir sie
                    Log.d(TAG, "Opening existing database for reading at: " + dbPath);
                    return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening readable database at " + dbPath + ": " + e.getMessage(), e);
            }
        }
        
        Log.d(TAG, "Falling back to default getReadableDatabase()");
        return super.getReadableDatabase();
    }
} 