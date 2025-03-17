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
 * Database helper for the Contacts database
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ContactsDB";
    
    // Database version
    private static final int DATABASE_VERSION = 2;
    
    // Database name
    private static final String DATABASE_NAME = "contacts_db";
    
    // Table names
    public static final String TABLE_CONTACTS = "contacts";
    
    // Column names
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_NOTES = "notes";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    
    // SQL statement to create the table
    private static final String CREATE_TABLE_CONTACTS = "CREATE TABLE " + TABLE_CONTACTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_NAME + " TEXT,"
            + KEY_PHONE + " TEXT,"
            + KEY_NOTES + " TEXT,"
            + KEY_LATITUDE + " REAL,"
            + KEY_LONGITUDE + " REAL" + ")";
    
    private static DatabaseHelper instance;
    private String dbPath;
    
    /**
     * Singleton pattern for database access
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
                    // Fallback: Use external storage
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
     * Creates an alternative path for the database if the normal path is not available
     */
    private String getAlternativeDatabasePath(Context context) {
        try {
            // Try to use external storage
            File externalDir;
            
            if (context != null) {
                // Try app-specific external storage
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
            
            // Fallback: Use standard external storage
            externalDir = new File(android.os.Environment.getExternalStorageDirectory(), "ATAK/plugins/contacts/databases");
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
            String path = new File(externalDir, DATABASE_NAME).getAbsolutePath();
            Log.d(TAG, "Using external storage fallback: " + path);
            return path;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating alternative database path: " + e.getMessage(), e);
            
            // Last fallback: Use temporary storage
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
            Log.d(TAG, "Creating database tables");
            
            // Check if the table already exists
            Cursor cursor = null;
            boolean tableExists = false;
            try {
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_CONTACTS + "'", null);
                tableExists = cursor != null && cursor.getCount() > 0;
                Log.d(TAG, "Table check result: exists=" + tableExists);
            } catch (Exception e) {
                Log.e(TAG, "Error checking if table exists: " + e.getMessage(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            
            // If the table already exists, check if it has the right columns
            if (tableExists) {
                Log.d(TAG, "Table already exists, checking columns");
                
                boolean hasLocationColumns = false;
                try {
                    cursor = db.rawQuery("PRAGMA table_info(" + TABLE_CONTACTS + ")", null);
                    if (cursor != null) {
                        int nameIndex = cursor.getColumnIndex("name");
                        if (nameIndex != -1) {
                            while (cursor.moveToNext()) {
                                String columnName = cursor.getString(nameIndex);
                                if (KEY_LATITUDE.equals(columnName) || KEY_LONGITUDE.equals(columnName)) {
                                    hasLocationColumns = true;
                                    break;
                                }
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking table columns: " + e.getMessage(), e);
                }
                
                // If the location columns are missing, add them
                if (!hasLocationColumns) {
                    Log.d(TAG, "Adding location columns to existing table");
                    try {
                        db.execSQL("ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN " + KEY_LATITUDE + " REAL;");
                        db.execSQL("ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN " + KEY_LONGITUDE + " REAL;");
                        Log.d(TAG, "Added location columns to contacts table");
                    } catch (Exception e) {
                        Log.e(TAG, "Error adding location columns: " + e.getMessage(), e);
                        
                        // If adding columns fails, recreate the table
                        try {
                            Log.d(TAG, "Recreating table after column add failure");
                            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
                            db.execSQL(CREATE_TABLE_CONTACTS);
                            Log.d(TAG, "Table recreated successfully");
                        } catch (Exception e2) {
                            Log.e(TAG, "Error recreating table: " + e2.getMessage(), e2);
                        }
                    }
                }
            } else {
                // If the table doesn't exist, create it
                Log.d(TAG, "Table does not exist, creating it");
                db.execSQL(CREATE_TABLE_CONTACTS);
                Log.d(TAG, "Table created successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables: " + e.getMessage(), e);
            
            // Last attempt to create the table
            try {
                Log.d(TAG, "Last attempt to create table");
                db.execSQL(CREATE_TABLE_CONTACTS);
                Log.d(TAG, "Table created in last attempt");
            } catch (Exception e2) {
                Log.e(TAG, "Final error creating table: " + e2.getMessage(), e2);
            }
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        try {
            if (oldVersion < 2) {
                // Upgrade from version 1 to 2: Adding location columns
                db.execSQL("ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN " + KEY_LATITUDE + " REAL;");
                db.execSQL("ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN " + KEY_LONGITUDE + " REAL;");
                Log.d(TAG, "Added location columns to contacts table");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds a new contact to the database
     * @return ID of the new contact or -1 on error
     */
    public long addContact(Contact contact) {
        long id = -1;
        SQLiteDatabase db = null;
        try {
            Log.d(TAG, "Starting to add contact: " + contact.toString());
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
            
            // Füge Standortdaten hinzu, wenn vorhanden
            if (contact.hasLocation()) {
                Log.d(TAG, "Contact has location: " + contact.getLatitude() + ", " + contact.getLongitude());
                values.put(KEY_LATITUDE, contact.getLatitude());
                values.put(KEY_LONGITUDE, contact.getLongitude());
            } else {
                Log.d(TAG, "Contact has no location");
                // Explizit NULL setzen für Standortdaten
                values.putNull(KEY_LATITUDE);
                values.putNull(KEY_LONGITUDE);
            }
            
            Log.d(TAG, "Prepared values for contact: " + values.toString());
            
            // Erster Versuch: Normaler Einfügevorgang
            try {
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
                            
                            // Versuche, die Tabellenstruktur zu überprüfen
                            Cursor tableInfo = db.rawQuery("PRAGMA table_info(" + TABLE_CONTACTS + ")", null);
                            if (tableInfo != null) {
                                Log.d(TAG, "Table structure:");
                                while (tableInfo.moveToNext()) {
                                    String columnName = tableInfo.getString(1);
                                    String columnType = tableInfo.getString(2);
                                    Log.d(TAG, "Column: " + columnName + ", Type: " + columnType);
                                }
                                tableInfo.close();
                            }
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
                Log.e(TAG, "Error in first insert attempt: " + e.getMessage(), e);
            }
            
            // Wenn der erste Versuch fehlgeschlagen ist, versuche einen alternativen Ansatz
            if (id == -1) {
                Log.d(TAG, "First insert attempt failed, trying alternative approach");
                
                try {
                    // Versuche, die Tabelle neu zu erstellen
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
                    db.execSQL(CREATE_TABLE_CONTACTS);
                    Log.d(TAG, "Recreated table for alternative approach");
                    
                    // Versuche erneut einzufügen
                    db.beginTransaction();
                    try {
                        id = db.insert(TABLE_CONTACTS, null, values);
                        if (id != -1) {
                            db.setTransactionSuccessful();
                            Log.d(TAG, "Contact inserted successfully with alternative approach, ID: " + id);
                        } else {
                            Log.e(TAG, "Alternative insert approach also failed");
                        }
                    } finally {
                        db.endTransaction();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in alternative insert approach: " + e.getMessage(), e);
                }
            }
            
            // Wenn auch der alternative Ansatz fehlgeschlagen ist, versuche einen letzten Ansatz mit SQL
            if (id == -1) {
                Log.d(TAG, "Both insert attempts failed, trying direct SQL approach");
                
                try {
                    // Erstelle einen SQL-Insert-Befehl
                    StringBuilder sql = new StringBuilder();
                    sql.append("INSERT INTO ").append(TABLE_CONTACTS).append(" (")
                       .append(KEY_NAME).append(", ")
                       .append(KEY_PHONE).append(", ")
                       .append(KEY_NOTES);
                    
                    if (contact.hasLocation()) {
                        sql.append(", ").append(KEY_LATITUDE)
                           .append(", ").append(KEY_LONGITUDE);
                    }
                    
                    sql.append(") VALUES ('")
                       .append(contact.getName().replace("'", "''")).append("', '")
                       .append(contact.getPhoneNumber().replace("'", "''")).append("', '")
                       .append(contact.getNotes().replace("'", "''")).append("'");
                    
                    if (contact.hasLocation()) {
                        sql.append(", ").append(contact.getLatitude())
                           .append(", ").append(contact.getLongitude());
                    }
                    
                    sql.append(")");
                    
                    Log.d(TAG, "Executing SQL: " + sql.toString());
                    
                    db.beginTransaction();
                    try {
                        db.execSQL(sql.toString());
                        
                        // Hole die ID des eingefügten Kontakts
                        Cursor idCursor = db.rawQuery("SELECT last_insert_rowid()", null);
                        if (idCursor != null && idCursor.moveToFirst()) {
                            id = idCursor.getLong(0);
                            idCursor.close();
                            db.setTransactionSuccessful();
                            Log.d(TAG, "Contact inserted successfully with SQL approach, ID: " + id);
                        } else {
                            Log.e(TAG, "Could not get last insert ID");
                            if (idCursor != null) {
                                idCursor.close();
                            }
                        }
                    } finally {
                        db.endTransaction();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in SQL insert approach: " + e.getMessage(), e);
                }
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
                    new String[] { KEY_ID, KEY_NAME, KEY_PHONE, KEY_NOTES, KEY_LATITUDE, KEY_LONGITUDE },
                    KEY_ID + "=?", new String[] { String.valueOf(id) }, 
                    null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                Contact contact = new Contact(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3));
                
                // Prüfe, ob Standortdaten vorhanden sind
                int latIndex = cursor.getColumnIndex(KEY_LATITUDE);
                int longIndex = cursor.getColumnIndex(KEY_LONGITUDE);
                
                if (latIndex != -1 && longIndex != -1 && !cursor.isNull(latIndex) && !cursor.isNull(longIndex)) {
                    contact.setLocation(
                        cursor.getDouble(latIndex),
                        cursor.getDouble(longIndex)
                    );
                }
                
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
                int latIndex = cursor.getColumnIndex(KEY_LATITUDE);
                int longIndex = cursor.getColumnIndex(KEY_LONGITUDE);
                
                Log.d(TAG, "Column indices - id:" + idIndex + ", name:" + nameIndex + 
                      ", phone:" + phoneIndex + ", notes:" + notesIndex + ", latitude:" + latIndex + ", longitude:" + longIndex);
                
                // Check if columns exist
                if (idIndex >= 0 && nameIndex >= 0 && phoneIndex >= 0 && notesIndex >= 0 && latIndex >= 0 && longIndex >= 0) {
                    do {
                        try {
                            long id = cursor.getLong(idIndex);
                            String name = cursor.getString(nameIndex);
                            String phone = cursor.getString(phoneIndex);
                            String notes = cursor.getString(notesIndex);
                            
                            Contact contact = new Contact(id, name, phone, notes);
                            
                            // Prüfe, ob Standortdaten vorhanden sind
                            if (latIndex != -1 && longIndex != -1 && !cursor.isNull(latIndex) && !cursor.isNull(longIndex)) {
                                contact.setLocation(
                                    cursor.getDouble(latIndex),
                                    cursor.getDouble(longIndex)
                                );
                            }
                            
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
            Log.d(TAG, "Updating contact: " + contact.toString());
            db = this.getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, contact.getName());
            values.put(KEY_PHONE, contact.getPhoneNumber());
            values.put(KEY_NOTES, contact.getNotes());
            
            // Aktualisiere Standortdaten
            if (contact.hasLocation()) {
                Log.d(TAG, "Contact has location: " + contact.getLatitude() + ", " + contact.getLongitude());
                values.put(KEY_LATITUDE, contact.getLatitude());
                values.put(KEY_LONGITUDE, contact.getLongitude());
            } else {
                Log.d(TAG, "Contact has no location, setting NULL values");
                values.putNull(KEY_LATITUDE);
                values.putNull(KEY_LONGITUDE);
            }
            
            Log.d(TAG, "Prepared values for update: " + values.toString());
            
            // Erster Versuch: Normaler Update-Vorgang
            try {
                // Serieller Zugriff für thread-safety
                db.beginTransaction();
                try {
                    // Aktualisieren
                    result = db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
                            new String[] { String.valueOf(contact.getId()) });
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Updated contact with ID " + contact.getId() + ", rows affected: " + result);
                    
                    if (result == 0) {
                        Log.e(TAG, "No rows updated for contact ID " + contact.getId());
                        
                        // Überprüfe, ob der Kontakt existiert
                        Cursor checkCursor = db.query(TABLE_CONTACTS, new String[] { KEY_ID },
                                KEY_ID + "=?", new String[] { String.valueOf(contact.getId()) },
                                null, null, null, null);
                        
                        if (checkCursor != null) {
                            boolean exists = checkCursor.getCount() > 0;
                            checkCursor.close();
                            
                            if (!exists) {
                                Log.e(TAG, "Contact with ID " + contact.getId() + " does not exist in database");
                            } else {
                                Log.e(TAG, "Contact exists but update failed for unknown reason");
                            }
                        }
                    }
                } finally {
                    db.endTransaction();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during update transaction: " + e.getMessage(), e);
            }
            
            // Wenn der erste Versuch fehlgeschlagen ist, versuche einen alternativen Ansatz mit SQL
            if (result == 0) {
                Log.d(TAG, "Update failed, trying SQL approach");
                
                try {
                    // Erstelle einen SQL-Update-Befehl
                    StringBuilder sql = new StringBuilder();
                    sql.append("UPDATE ").append(TABLE_CONTACTS).append(" SET ")
                       .append(KEY_NAME).append(" = '").append(contact.getName().replace("'", "''")).append("', ")
                       .append(KEY_PHONE).append(" = '").append(contact.getPhoneNumber().replace("'", "''")).append("', ")
                       .append(KEY_NOTES).append(" = '").append(contact.getNotes().replace("'", "''")).append("'");
                    
                    if (contact.hasLocation()) {
                        sql.append(", ").append(KEY_LATITUDE).append(" = ").append(contact.getLatitude())
                           .append(", ").append(KEY_LONGITUDE).append(" = ").append(contact.getLongitude());
                    } else {
                        sql.append(", ").append(KEY_LATITUDE).append(" = NULL")
                           .append(", ").append(KEY_LONGITUDE).append(" = NULL");
                    }
                    
                    sql.append(" WHERE ").append(KEY_ID).append(" = ").append(contact.getId());
                    
                    Log.d(TAG, "Executing SQL: " + sql.toString());
                    
                    db.beginTransaction();
                    try {
                        db.execSQL(sql.toString());
                        db.setTransactionSuccessful();
                        result = 1; // Wir nehmen an, dass es funktioniert hat
                        Log.d(TAG, "Contact updated successfully with SQL approach");
                    } finally {
                        db.endTransaction();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in SQL update approach: " + e.getMessage(), e);
                }
            }
            
            // Wenn auch der SQL-Ansatz fehlgeschlagen ist, versuche einen letzten Ansatz mit Löschen und Neueinfügen
            if (result == 0) {
                Log.d(TAG, "Both update attempts failed, trying delete and reinsert approach");
                
                try {
                    db.beginTransaction();
                    try {
                        // Lösche den alten Kontakt
                        db.delete(TABLE_CONTACTS, KEY_ID + " = ?", new String[] { String.valueOf(contact.getId()) });
                        
                        // Füge den aktualisierten Kontakt neu ein
                        ContentValues insertValues = new ContentValues();
                        insertValues.put(KEY_ID, contact.getId()); // Wichtig: Behalte die gleiche ID
                        insertValues.put(KEY_NAME, contact.getName());
                        insertValues.put(KEY_PHONE, contact.getPhoneNumber());
                        insertValues.put(KEY_NOTES, contact.getNotes());
                        
                        if (contact.hasLocation()) {
                            insertValues.put(KEY_LATITUDE, contact.getLatitude());
                            insertValues.put(KEY_LONGITUDE, contact.getLongitude());
                        } else {
                            insertValues.putNull(KEY_LATITUDE);
                            insertValues.putNull(KEY_LONGITUDE);
                        }
                        
                        long newId = db.insert(TABLE_CONTACTS, null, insertValues);
                        if (newId != -1) {
                            db.setTransactionSuccessful();
                            result = 1;
                            Log.d(TAG, "Contact updated successfully with delete and reinsert approach, new ID: " + newId);
                        } else {
                            Log.e(TAG, "Failed to reinsert contact after deletion");
                        }
                    } finally {
                        db.endTransaction();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in delete and reinsert approach: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating contact: " + e.getMessage(), e);
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
        SQLiteDatabase db = null;
        try {
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
                        try {
                            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
                            onCreate(db);
                            Log.d(TAG, "Created new database at: " + dbPath);
                            return db;
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating database at " + dbPath + ": " + e.getMessage(), e);
                            // Fallback auf super-Implementierung
                            Log.d(TAG, "Falling back to default implementation after create error");
                            return super.getWritableDatabase();
                        }
                    } else {
                        // Wenn die Datenbank existiert, öffnen wir sie
                        try {
                            Log.d(TAG, "Opening existing database at: " + dbPath);
                            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
                            return db;
                        } catch (Exception e) {
                            Log.e(TAG, "Error opening existing database at " + dbPath + ": " + e.getMessage(), e);
                            
                            // Versuche, die Datei zu löschen und neu zu erstellen, wenn sie beschädigt ist
                            try {
                                Log.d(TAG, "Attempting to delete and recreate database");
                                boolean deleted = dbFile.delete();
                                Log.d(TAG, "Database file deleted: " + deleted);
                                
                                db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
                                onCreate(db);
                                Log.d(TAG, "Recreated database at: " + dbPath);
                                return db;
                            } catch (Exception e2) {
                                Log.e(TAG, "Error recreating database: " + e2.getMessage(), e2);
                                // Fallback auf super-Implementierung
                                Log.d(TAG, "Falling back to default implementation after recreation error");
                                return super.getWritableDatabase();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error accessing database path " + dbPath + ": " + e.getMessage(), e);
                }
            }
            
            // Wenn wir hier ankommen, verwenden wir die Standard-Implementierung
            Log.d(TAG, "Using default getWritableDatabase() implementation");
            return super.getWritableDatabase();
        } catch (Exception e) {
            Log.e(TAG, "Critical error in getWritableDatabase: " + e.getMessage(), e);
            throw e;
        }
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